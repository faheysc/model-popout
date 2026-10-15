#!/usr/bin/env python3
"""Offline Java 11 validation using cached RuneLite dependencies; Gradle remains the primary build."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--runelite-version', default='1.13.1')
parser.add_argument('--cape-fixtures', type=Path)
parser.add_argument('--hud-fixtures', type=Path)
parser.add_argument('--benchmark', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
java_home = os.environ.get('JAVA_HOME')
if not java_home:
    javac = shutil.which('javac')
    if not javac:
        parser.error('Set JAVA_HOME to a Java 11 JDK, or put javac on PATH.')
    java_home = str(Path(javac).resolve().parent.parent)
jdk = Path(java_home) / 'bin'
version = subprocess.run([str(jdk/'java'), '-version'], capture_output=True, text=True, check=True).stderr
if 'version "11.' not in version:
    parser.error('Validation requires a Java 11 JDK. Set JAVA_HOME accordingly.')
cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home()/'.gradle'))) / 'caches/modules-2/files-2.1'
# Only dependencies available to the official standard build; avoid unrelated cached libraries/native client code.
groups = {'net.runelite', 'net.runelite.arn', 'com.google.inject', 'com.google.inject.extensions',
          'com.google.guava', 'com.google.code.findbugs', 'javax.inject', 'org.slf4j',
          'com.google.code.gson', 'org.hamcrest', 'junit', 'aopalliance', 'net.sf.jopt-simple'}
# RuneLiteLAF tests need the same FlatLaf dependency as the selected client.
flatlaf_versions = set()
for pom in (cache/'net.runelite/client'/args.runelite_version).glob('*/*.pom'):
    tree = ET.parse(pom)
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    for dep in tree.findall('m:dependencies/m:dependency', ns):
        if dep.findtext('m:groupId', namespaces=ns) == 'net.runelite' and dep.findtext('m:artifactId', namespaces=ns) == 'flatlaf':
            flatlaf_versions.add(dep.findtext('m:version', namespaces=ns))
jars = []
for p in sorted(cache.rglob('*.jar')):
    relative = p.relative_to(cache)
    if relative.parts[0] not in groups or p.name.endswith(('-sources.jar', '-javadoc.jar')):
        continue
    if relative.parts[0] == 'net.runelite':
        allowed = (relative.parts[1] in {'client', 'runelite-api', 'jshell'} and relative.parts[2] == args.runelite_version
                   or relative.parts[1] == 'flatlaf' and relative.parts[2] in flatlaf_versions)
        if not allowed:
            continue
    jars.append(p)
if not any(p.name == 'client-'+args.runelite_version+'.jar' for p in jars):
    parser.error('Requested RuneLite dependencies are not cached; run ./gradlew build online first.')
cp = os.pathsep.join(map(str,jars))
main, test = root/'build/direct-main', root/'build/direct-test'
for out in (main,test):
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
def run(*arguments):
    subprocess.run(list(map(str,arguments)),cwd=root,check=True)
run(jdk/'javac','--release','11','-Xlint:all,-serial,-processing','-cp',cp,'-d',main,
    *sorted((root/'src/main').rglob('*.java')))
run(jdk/'javac','--release','11','-cp',str(main)+os.pathsep+cp,'-d',test,
    root/'tools/JavaTestRunner.java',*sorted((root/'src/test').rglob('*.java')))
options = ['-Djava.awt.headless=true','-ea']
if args.cape_fixtures:
    options.append('-Dmodelpopout.capeFixtures='+str(args.cape_fixtures.resolve()))
if args.hud_fixtures:
    options.append('-Dmodelpopout.hudFixtures='+str(args.hud_fixtures.resolve()))
runtime = str(test)+os.pathsep+str(main)+os.pathsep+cp
run(jdk/'java',*options,'-cp',runtime,'JavaTestRunner',
    *['com.modelpopout.'+p.stem for p in sorted((root/'src/test/java/com/modelpopout').glob('*Test.java'))])
if args.benchmark:
    run(jdk/'java',*options,'-cp',runtime,'com.modelpopout.TextureRenderBenchmark')
metadata = dict(line.split('=',1) for line in (root/'runelite-plugin.properties').read_text().splitlines() if '=' in line)
jar = root/f"build/libs/model-popout-{metadata['version']}.jar"
jar.parent.mkdir(parents=True,exist_ok=True)
# Deterministic fallback artifact; production classes and licenses only, never test fixtures.
entries = {str(p.relative_to(main)):p.read_bytes() for p in main.rglob('*') if p.is_file()}
entries.update({'runelite-plugin.properties':(root/'runelite-plugin.properties').read_bytes(),
                'META-INF/LICENSE':(root/'LICENSE').read_bytes()})
for p in (root/'src/main/resources').rglob('*'):
    if p.is_file():
        entries[str(p.relative_to(root/'src/main/resources'))] = p.read_bytes()
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as archive:
    for name,data in sorted(entries.items()):
        info = zipfile.ZipInfo(name,(1980,1,1,0,0,0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        archive.writestr(info,data)
(root/'build/validation-dependencies.json').write_text(json.dumps(
    {str(p.relative_to(cache)):hashlib.sha256(p.read_bytes()).hexdigest() for p in jars},indent=2))
print('Packaged',jar.name,'SHA-256',hashlib.sha256(jar.read_bytes()).hexdigest())
