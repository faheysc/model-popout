> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Local commit and submission verification

Historical review below predates the custom window-controls pass. Its KDE title-bar
assumption was not portable. Current window controls await target-desktop verification;
see WINDOW_CONTROLS.md. No commit or publication is authorized by the current task.

## Initial precommit review status

A new repository was initialized in `model-popout` only, on branch `main`. No prior
history existed there. The parent directory's existing empty `.git` was untouched.
No files have been staged or committed. There is no commit hash, and the working
tree contains the intended untracked project files. No remote was added, and
nothing was pushed, published or submitted.

The owner subsequently confirmed that the Java 11 clean Gradle build passed in
their normal terminal and authorized completion of staging and the initial local
commit with message `Initial release: Model Popout`. The sandbox failure described
below is historical and no longer blocks the local commit. Git author identity
must be supplied before creating the commit.

## File and packaging review

The intended tree contains production sources, tests and helper models, resources,
README, BSD 2-Clause license and attribution notices, Gradle wrapper/build files,
Hub metadata, development tooling/docs and a portable IntelliJ run configuration.
The source audit checks every public file for private paths/secrets and scans
production source/bytecode for restricted APIs. No actual credentials or personal
absolute paths were found. A broad secondary scan matched the literal private-key
detection regex in the audit tool; it was inspected and is not a secret.

`.gitignore` excludes builds, compiled artifacts, Gradle/IDE user state, logs,
private fixture directories, diagnostic traces, local audit/research, backup and
checkpoint archives, credentials and common private key formats. The required
`gradle/wrapper/gradle-wrapper.jar` is explicitly retained. Ignore rules were
checked with `git check-ignore`; no generated/private artifacts are in the intended
file list. Actual backups, fixtures and collected traces remain outside this
repository. See the private local precommit-review inventory for per-file hashes.

The production JAR contains Java 11 classes plus packaged BSD/third-party notices.
It contains no tests, launchers or fixture data. Root and packaged licenses match.
Runtime icons are generated in Java and game artwork is read through the client;
there are no file-path-dependent image resources or distributed game fixtures.

## Build verification

Java 11 + Gradle 8.10 wrapper:

- Normal `clean build -PruneLiteVersion=1.13.1` could not start: the default Gradle
  home is read-only in this execution environment.
- A writable Gradle home and the cached wrapper distribution removed that problem,
  but even `--no-daemon` forks a single-use daemon whose local socket creation is
  denied: `java.net.SocketException: Operation not permitted`.
- Gradle did not reach project compilation or tests. This is an environment
  restriction, not evidence of a plugin compile failure or a successful build.
  No source/build modification can responsibly establish Gradle success here.

Independent offline Java 11 validation was rerun:

- Private regression fixtures: **149 passed, 0 failed, 5 native desktop checks
  skipped**.
- Public tree without private fixtures: **146 passed, 0 failed, 8 skipped** (the
  same five native checks plus three optional fixture checks).
- Both produced identical deterministic production JAR SHA-256:
  `ca03ee900c4011cd384fc1a3360a2a2e90e40eeff42ed1e740315d637f8ee251`.
- Source, metadata, production bytecode and packaged-resource audits passed.
- Standalone validation requires cached development dependencies, but the plugin
  and official standard build do not depend on that validation tool or cache.

These checks do not replace Gradle, the official Hub package/API checks, or desktop
verification. The owner supplied the desktop verification separately.

## Metadata and policy

Display name Model Popout; identifier/project `model-popout`; entry point
`com.modelpopout.ModelPopoutPlugin`; author `sfahey`; BSD 2-Clause; version `0.1.0`;
`build=standard`; tags `model,player,npc,popout`. PluginDescriptor matches the
properties description and tags. The legacy configuration group remains
`livemodelviewer` to preserve saved settings.

Official requirements were rechecked against the current Hub guide, Java 11 CI,
RuneLite version (1.13.1), v5 packager standard build/source copying, rejected-feature
list and Jagex guidelines. Production uses only JDK and public RuneLite APIs and
client-supplied transitive dependencies. Standard mode replaces custom Gradle
files and ignores test launchers; production uses ordinary source/resources and
needs no custom build steps. Actual official packaging/API checking remains
unverified until those tools/CI execute.

Independent desktop presentation, native window controls/Always on Top, enlarged
player/NPC animations, combat hitsplats and health bars need explicit maintainer
policy review. The NPC/boss/combat display is the main possible blocker: passive
API reads do not establish acceptance under additional combat-information rules.
No feature was removed or redesigned. [SUBMISSION.md](SUBMISSION.md) documents the
individual concerns and authoritative sources.

## Next required action

Run in an unrestricted development terminal, from this project, with Java 11:

```sh
./gradlew --no-daemon --console=plain clean build -PruneLiteVersion=1.13.1
```

The owner has confirmed this clean build passed. Review and stage only intended
files, check the complete staged diff and ignored artifacts, then
create the authorized local commit and record its full hash. Publishing and Hub
submission remain separate approval stages. Future publication uses a public
GitHub repository named `model-popout` and a Hub marker `plugins/model-popout` with
the HTTPS repository URL and full approved commit hash. No marker should contain
placeholders or point to an unverified commit.
