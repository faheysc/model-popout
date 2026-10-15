# Release 0.2.3 verification

Release preparation: 2026-10-15. Published baseline: `38be0a9edc07c04462d1317b5bc63b9f3e1f50fd`. Accepted revision `0.1.3` was recorded before metadata advanced to candidate `0.2.3`. Hub publication is pending.

## Review

Reviewed the tracked/untracked public inventory and changes from the published baseline: standalone scene actor inspection, existing-submenu placement, shared automatic window controls and duplicate-title correction, reachable main-window geometry, shared viewport input, sidebar/performance-label removal, tests and documentation. No accidental production changes or partially wired lookup setting/sender were identified. Kill Clog/Hiscores remains a specification only.

Private checkpoints, generated output, IDE state and game-cache fixtures are ignored and excluded from staging and production packaging. Portable Java test doubles remain test source; they contain synthetic geometry/menu data, not private game captures. Existing renderer, texture rasterizer, combat HUD and transparency behavior retain regression coverage.

## Final automated checks

Java 11.0.32; RuneLite **1.13.2**, matching the current Hub `runelite.version`:

- Gradle clean compilation, tests, JAR and native task passed. Final forced rerun after metadata changed also passed.
- Headless: **205 passed, 0 failures/errors, 15 skipped** (12 opt-in native checks and 3 optional private-asset checks).
- Native Linux desktop: **12 passed, 0 skipped/failures/errors**.
- Independent offline Java 11 validator: **205 passed, 15 skipped, 0 failed** without private fixture inputs.
- Source/artifact audit: **45 production sources, 79 production classes, zero errors**; Java 11 bytecode, metadata, restricted API patterns and packaged licenses checked.
- Whitespace check passed. No private fixture data or checkpoint files in the publishable inventory.

Commands:

```sh
./gradlew --offline clean test jar desktopPresentationTest -PruneLiteVersion=1.13.2
./gradlew --offline test jar desktopPresentationTest -PruneLiteVersion=1.13.2 --rerun-tasks
python3 tools/validate.py --runelite-version 1.13.2
python3 tools/audit.py
git diff --check
```

The independent validator's production-only JAR SHA-256 is `063d7076796a0d211c8e507101fd03a838a0294b828555ed04620665cfa3e132`. This is a local validation artifact, not the Hub-distributed JAR.

## Standard-build compatibility

Downloaded the [official v5 tooling bundle](https://github.com/runelite/plugin-hub-tooling/releases/tag/v5), copied only production source/resources into an isolated temporary project, and used its embedded standard build/settings scripts and unmodified `target_init.gradle`. Java 11 ran `runelitePluginHubPackage runelitePluginHubManifest` with the bundled API recorder and RuneLite 1.13.2. Both tasks passed; the production artifact contains 79 classes and no test/launcher/fixture classes. The first offline attempt lacked Hub's annotations dependency; the online rerun resolved it and passed. No project-specific build task was required.

This establishes local standard compilation/packaging compatibility, not completed Hub CI, API-policy approval or acceptance. Gradle reports its existing deprecation warning for Gradle 9.

## Submission gate

**Windows VM verification of the final corrected release source has not been confirmed.** Do not submit the Hub PR yet. Build the pushed release commit on Java 11/RuneLite 1.13.2 and run the Windows checklist in [DUPLICATE_TITLE_BARS.md](DUPLICATE_TITLE_BARS.md) and [TARGETED_CLEANUP.md](TARGETED_CLEANUP.md): main viewer plus independent player/NPC inspectors, one title bar, working controls, opacity 0/30/50/100%, animation/HUD, config changes, Always on Top, minimize/restore, close and plugin restart. Record the tested source hash and result. Earlier builds and Linux native tests do not satisfy this gate.

Once confirmed, update only `plugins/model-popout` in the Hub checkout to the exact pushed source hash. Preserve the repository URL and all unrelated manifests. Open the PR for review; do not merge it.
