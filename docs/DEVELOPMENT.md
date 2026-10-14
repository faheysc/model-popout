# Development and validation

Use a Java 11 JDK and the checked-in Gradle 8.10 wrapper, matching the [official plugin template](https://github.com/runelite/example-plugin). Import this directory as a Gradle project in IntelliJ. Run the shared **Model Popout** configuration, the `run` Gradle task, or `com.modelpopout.ModelPopoutLauncher` with the test runtime classpath. The development launcher enables safe mode; GPU is not required. Jagex-account login requires RuneLite's [development-client setup](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

```sh
./gradlew clean test jar
./gradlew run
./gradlew desktopPresentationTest
```

The default RuneLite dependency is `latest.release`. To reproduce the audited API version, use `-PruneLiteVersion=1.13.1`. Dependencies are RuneLite client (compile-only), RuneLite client/jshell (test-only), and JUnit 4.12. No independent production dependency is bundled. Plugin Hub uses `build=standard` and replaces the project build scripts; production must remain independent of custom build tasks.

Headless tests cover frame publication, stale results, bounded scheduling, transparency, resize geometry, camera framing, NPC tracking/mirroring, texture mapping and animation, reverse-face rejection, hitsplat types/expiration/slot priorities, actor identity, scaled health, HUD alpha/anchors, and bounded HUD delivery. Native window tests are opt-in via `desktopPresentationTest` and require an actual desktop. Headless results cannot establish native painting or desktop appearance.

Optional real Fire/Infernal cache fixtures are local test data, not distributed art. With compatible fixture JSON supplied separately:

```sh
./gradlew test -PcapeFixtures=/path/to/local/cape-fixtures
```

Optional local hitsplat definition fixtures (`hitsplat-<id>.bin`) can also be supplied with `-PhudFixtures=/path/to/local/hud-fixtures`. They are raw config definitions read from the game cache for validation and are not distributed. Without fixtures, the two real-cape tests and one native hitsplat-definition test skip explicitly. Synthetic cape regressions always run. Parsing is explicit and uses public model test doubles; no obfuscated client classes or private snapshot deserialization are in the public project.

When Gradle cannot run but dependencies are already cached, a portable fallback is available:

```sh
# JAVA_HOME must name a Java 11 JDK; GRADLE_USER_HOME is optional.
python3 tools/validate.py
python3 tools/validate.py --cape-fixtures /path/to/local/cape-fixtures --hud-fixtures /path/to/local/hud-fixtures --benchmark
```

This compiles with `--release 11`, runs JUnit, records dependency hashes, and creates a deterministic production-only JAR. It is an offline fallback, not proof that Plugin Hub CI or Gradle passed. Its synthetic benchmark reports raster milliseconds per frame, not capture rate, monitor refresh, or real desktop throughput.

Before publishing, run the [in-game checklist](IN_GAME_CHECKLIST.md) and review the [submission notes](SUBMISSION.md). Keep game-cache assets, local research, generated output, IDE state, and verified backups outside the public source tree. Do not publish a shaded RuneLite development client as the Plugin Hub artifact.
