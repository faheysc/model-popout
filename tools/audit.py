#!/usr/bin/env python3
"""Repeatable source/artifact checks; this is not official Plugin Hub approval."""
from pathlib import Path
import hashlib
import json
import re
import struct
import zipfile

def class_strings(data):
    count=struct.unpack('>H',data[8:10])[0]
    offset=10;index=1;strings=set()
    while index<count:
        tag=data[offset];offset+=1
        if tag==1:
            length=struct.unpack('>H',data[offset:offset+2])[0];offset+=2
            strings.add(data[offset:offset+length].decode('utf-8',errors='replace'));offset+=length
        elif tag in {3,4,9,10,11,12,17,18}:offset+=4
        elif tag in {5,6}:offset+=8;index+=1
        elif tag in {7,8,16,19,20}:offset+=2
        elif tag==15:offset+=3
        else:raise ValueError('Invalid class constant-pool tag: '+str(tag))
        index+=1
    return strings

root=Path(__file__).resolve().parent.parent
production=root/'src/main/java/com/modelpopout'
errors=[]
for path in sorted(production.glob('*.java')):
    source=path.read_text()
    if 'package com.modelpopout;' not in source:
        errors.append(str(path.relative_to(root))+': wrong package')
    patterns=[r'java\.lang\.reflect',r'java\.io\.',r'java\.net\.',r'\bRuntime\b',r'\bProcessBuilder\b',
              r'\bKeyboardFocusManager\b',r'\bRobot\b',r'\bThread\.(?:sleep|interrupt)\(',
              r'new\s+(?:Gson|OkHttpClient)\b',r'\.(?:menuAction|runScript|hopToWorld|setDrawCallbacks)\(',
              r'(?:com\.sun\.|sun\.|net\.runelite\.rs\.|net\.runelite\.mixins\.|com\.sun\.jna|org\.lwjgl)']
    for pattern in patterns:
        if re.search(pattern,source):errors.append(str(path.relative_to(root))+': review forbidden/restricted pattern '+pattern)
props=dict(line.split('=',1) for line in (root/'runelite-plugin.properties').read_text().splitlines() if '=' in line)
expected={'displayName':'Model Popout','plugins':'com.modelpopout.ModelPopoutPlugin','build':'standard','tags':'model,player,npc,popout'}
for key,value in expected.items():
    if props.get(key)!=value:errors.append('Metadata mismatch: '+key)
plugin=(production/'ModelPopoutPlugin.java').read_text()
if props['description'] not in plugin:errors.append('PluginDescriptor description differs from properties')
if 'String GROUP = "livemodelviewer";' not in (production/'ModelPopoutConfig.java').read_text():
    errors.append('Legacy configuration group changed without migration')
if (root/'LICENSE').read_bytes()!=(root/'src/main/resources/META-INF/LICENSE').read_bytes():
    errors.append('Packaged license differs from root license')
files=[]
for path in sorted(root.rglob('*')):
    relative=path.relative_to(root)
    if not path.is_file() or any(p in {'build','.gradle','.git','.idea'} for p in relative.parts):continue
    if any(p in {'.idea','.aws','.codex','.agents','backups'} for p in relative.parts):
        errors.append('Private/local directory in publishable tree: '+str(relative))
    if path.name.startswith('.env') or path.name=='local.properties' or path.suffix in {'.class','.log','.hprof','.iml','.pem','.key','.p12','.jks'}:
        errors.append('Generated/private file in public tree: '+str(relative))
    if path.suffix in {'.java','.md','.py','.gradle','.properties','.xml','.txt','.template'}:
        text=path.read_text()
        if re.search(r'/(?:var/)?home/|[A-Z]:\\Users\\',text):errors.append('Personal absolute path: '+str(relative))
        if re.search(r'AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',text):
            errors.append('Possible credential/key material: '+str(relative))
    files.append({'path':str(relative),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
jar=root/'build/libs/model-popout-0.1.0.jar'
classes=0
if not jar.exists():errors.append('Run validation/build before auditing the artifact')
else:
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.endswith('.class'):
                classes+=1
                data=archive.read(name)
                if struct.unpack('>H',data[6:8])[0]!=55:errors.append('Non-Java-11 bytecode: '+name)
                if not name.startswith('com/modelpopout/') or 'Launcher' in name or 'Test' in name:
                    errors.append('Nonproduction class packaged: '+name)
                strings=class_strings(data)
                forbidden={'java/lang/Runtime','java/lang/ProcessBuilder','java/awt/Robot','java/awt/KeyboardFocusManager'}
                restricted_prefixes=('java/lang/reflect/','net/runelite/rs/','net/runelite/mixins/','com/sun/jna/','org/lwjgl/')
                for symbol in strings:
                    if symbol in forbidden or symbol.startswith(restricted_prefixes):
                        errors.append('Restricted production bytecode: '+name+' '+symbol)
        for name in ['META-INF/LICENSE','META-INF/THIRD_PARTY_NOTICES']:
            if name not in archive.namelist():errors.append('Missing packaged notice: '+name)
report={'production_sources':len(list(production.glob('*.java'))),'production_classes':classes,
        'metadata':props,'public_files':files,'errors':errors,
        'limitations':['Static checks cannot establish policy approval, Gradle success, or desktop behavior.']}
(root/'build').mkdir(exist_ok=True)
(root/'build/source-audit.json').write_text(json.dumps(report,indent=2))
for error in errors:print('FAIL:',error)
print(f'Audited {report["production_sources"]} production sources, {classes} production classes, {len(files)} public files; {len(errors)} errors.')
raise SystemExit(bool(errors))
