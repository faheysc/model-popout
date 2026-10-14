# Plugin Hub submission preparation

Reviewed against official sources on 2026-10-13. This preparation is not Plugin Hub approval. The reviewed Hub version is [1.13.1](https://github.com/runelite/plugin-hub/blob/master/runelite.version).

## Prepared metadata

| Field | Value |
| --- | --- |
| Display name | Model Popout |
| Repository / Gradle project / proposed Hub identifier | `model-popout` |
| Plugin class | `com.modelpopout.ModelPopoutPlugin` |
| Author | `sfahey` — confirmed by the owner |
| License | BSD 2-Clause — existing copyright retained |
| Tags | `model,player,npc,popout` |
| Version | `0.1.0` |
| Hub build mode | `standard` |
| Persisted configuration group | `livemodelviewer` — compatibility only |

Description: “Display your live character and interacting NPC in a movable, resizable window with animated models, camera controls, and adjustable transparency.”

The sidebar icons are original programmatic Java drawings. There is no game art or vendored RuneLite implementation in production resources. A separate Hub `icon.png` is optional and has not been added. RuneLite reference-derived rendering and hitsplat archive-format notices and the plugin license are packaged under `META-INF`; Gradle distribution notices are preserved under `docs/licenses`.

## Technical findings

The [official Hub guide](https://github.com/runelite/plugin-hub) recommends Java 11, documents `build=standard`, dependency verification for additional runtime libraries, optional icons, and the repository/commit marker submission. The project follows the [official template](https://github.com/runelite/example-plugin) for repositories, compile-only RuneLite client, test launcher dependencies, JUnit, and Java target. Production depends only on the Java runtime and RuneLite's supplied public APIs/transitive libraries.

Source inspection and production-class scanning found no reflection, JNI/JNA, subprocess execution, network services, direct disk access, input simulation, menu actions, internal/obfuscated client references, or game model/animation/orientation mutation. There is no draw-callback replacement or game-clock adjustment. The renderer copies public models/textures on the client thread and processes owned data on its worker; desktop interaction stays within its own Swing window. Offline native-client research tools and assets were removed from the public tree, while preserving them locally and in the verified backup.

The [forbidden-language/API rules](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#forbidden-language-features) apply to executing plugin code. Python validation tooling and JUnit test infrastructure are development-only; none is loaded or packaged as production classes. No new independent runtime dependency needs verification-template approval.

Public hitsplat definitions are decoded from config archive 32 using `Client.getIndexConfig().loadData`; sprites are requested through `SpriteManager`. No reflection/internal actor arrays are read and no game art is distributed. The archive schema is version-dependent and is not an explicit stable typed hitsplat-definition API; malformed/unknown data uses an explicit visual fallback. See [HUD.md](HUD.md) for API references and fidelity limitations. This extra archive access should be disclosed in review.

The current [packager v5 standard build](https://github.com/runelite/plugin-hub-tooling/blob/v5/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle) supplies compile-only RuneLite client, Lombok/annotation processing and JetBrains annotations and targets Java 11. Its [source-copy implementation](https://github.com/runelite/plugin-hub-tooling/blob/v5/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java) copies `src/main/java`, `src/main/resources` and optional `lombok.config`; it does not copy test launchers, private fixtures or project build scripts. Model Popout uses no Lombok, custom generated sources, runtime third-party additions or custom resource-processing requirements. Standard-build execution and Hub API-recorder verification still require the official tooling/CI; source inspection is not a substitute for those checks.

## Policy questions requiring review

These are **potential blockers, not confirmed violations**. No fundamental feature was removed during cleanup. Always on Top and the combat HUD were separately authorized additions and are included in this review.

| Feature | Assessment and applicable rule |
| --- | --- |
| Independent window and native controls | The [rejected-feature list](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#removed-in-the-past) rejects manipulation of the client's window. This plugin moves/resizes/restores/focuses only its own window in response to user input; it does not minimize or refocus the RuneLite client. The public rule does not explicitly approve a separate model window. Ask reviewers to assess that distinction. |
| Live NPC/boss animations in a separate display | [Jagex's guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) prohibit additional boss-mechanic indicators and certain NPC-focus information. Enlarged/mirrored live NPC animations and attacker fallback may be interpreted as additional combat information even though there is no prediction, guidance, timer, label, or automation. This is the main feature-review risk. Disclose it explicitly. |
| CPU re-rendering of observed poses | The [rejected-feature list](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#not-currently-being-considered) rejects content simulation. This viewer holds actual captured poses and scrolls actual game textures; it has no encounter simulation or independent NPC logic. Whether that distinction is acceptable remains a reviewer decision. |
| Player/NPC hitsplats and health bars | Passive reads of `HitsplatApplied`, public hitsplat expiration, native sprite/config archives, and actor health ratios; own-player Hitpoints refine only an already visible combat bar. No exact NPC HP is inferred, no damage totals/history, attack predictions, encounter counters, timers, prayer hints, other-player scouting, or game input are added. Nevertheless, a persistent enlarged combat display can communicate information outside the native view, including when a model is not natively drawn. [Jagex's additional boss-mechanic/interface restrictions](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and [RuneLite's high-end PvM restriction](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#not-currently-being-considered) require explicit reviewer assessment. Access to public APIs does not settle feature policy. This is a potential submission blocker, not a confirmed blanket prohibition on hitsplats or health bars. |
| Always on Top | Uses Java `Window.setAlwaysOnTop` on the popout only, with support/security handling. It never changes RuneLite's main-window stacking. Include it in the same standalone-window policy review; Java API support alone is not Hub approval. |
| Adjustable opacity | Alpha changes the viewer's background only. No inventory background, game interface, or in-game click zone is modified. There is no game-input passthrough. Native decorations/hit testing are desktop-dependent. No explicit standalone-opacity approval was found. |
| Independent high-FPS rendering | Capture follows client updates; on-demand rendering changes only private images. Game timing, RuneLite FPS, and GPU callbacks are unchanged. No explicit rule prohibits this private render target, but that is not an approval guarantee. |

The [2024 client-guideline update](https://secure.runescape.com/m=news/a=13/undead-pirates-tweaks-varlamore-cas--more?oldschool=1) and linked current guidelines should be rechecked immediately before submission. No Spellbook, Prayer, or other game-interface click zone is resized here.

Before submission, provide reviewers with a short video of ordinary player/NPC use, native window controls, opacity, safe-mode cape animation, both combat HUD toggles, hitsplat durations/styles, and scaled NPC health. Explain that camera input never interacts with game entities. Ask for clarification on the live combat/NPC display, hitsplats/health visibility, and standalone window; do not remove or redesign those verified features without the owner's decision.

## Build and desktop gates

Java 11 direct compilation/JUnit and deterministic fallback packaging passed in the audit environment. The Gradle wrapper matched the official template, but full Gradle execution was prevented by a read-only default cache and then sandbox socket restrictions even with a writable Gradle home. The owner subsequently confirmed that the Java 11 clean Gradle build passed in their normal terminal. The local Gradle gate is satisfied by that confirmation; **official Plugin Hub CI remains unverified**. The verified clean-build command is:

```sh
./gradlew --no-daemon --console=plain clean build -PruneLiteVersion=1.13.1
./gradlew desktopPresentationTest -PruneLiteVersion=1.13.1
./gradlew run -PruneLiteVersion=1.13.1
```

The owner has now verified the final Model Popout implementation in-game on Bazzite/KDE: player/NPC animations, combat HUD, animated capes, FPS, camera/framing, opacity, Always on Top, and no flicker. The owner accepts KDE's independent native title-bar opacity; no correction is planned. The Hub currently targets RuneLite 1.13.1. Other platforms are not certified. In particular, desktops that honor the undecorated window request may omit native controls.

## Publication and submission steps — only after approval

1. Resolve policy questions, finish the build/desktop checks, and confirm public author credit and GitHub owner.
2. Create an empty **public** GitHub repository named `model-popout` under that confirmed account. Do not initialize it with a competing README/license.
3. Git is already initialized in this project directory only, on branch `main`, with no commits. After the required Gradle build passes, inspect `git status`, stage the source/build/docs/wrapper files, and review `git diff --cached`. Do not initialize another repository or alter existing history. Confirm `.gitignore` excludes builds, IDE data, private files, backups, and local research. Then make the approved initial commit and push it to the repository. The owner confirmed the required Gradle build passed and authorized the initial local commit; no pushing or publishing is authorized.
4. Obtain the full 40-character hash of the approved pushed commit with `git rev-parse HEAD`.
5. Fork `runelite/plugin-hub`, create a submission branch, and create `plugins/model-popout`. Fill the prepared [marker template](plugin-hub/model-popout.template) with the actual HTTPS repository URL and that full hash. Do not submit placeholders or a JAR.
6. Commit/push only that marker change in the Hub fork and open one pull request. Include the description, passive-observation behavior, standalone-window/NPC policy questions, validation evidence, platform limitations, and video.
7. Review Hub CI and maintainer requests. Any plugin change needs a new approved plugin commit and an updated marker hash in the same submission PR. Approval is determined by maintainers; successful local tests do not imply acceptance.

The actual repository URL and commit cannot be finalized before the owner's publishing approval. A local Git repository has been initialized; no GitHub repository, commit, push, publication, or Hub pull request has been created.

The owner authorized the local initial commit only after a successful Gradle build. Publishing remains unauthorized. The following commands describe the later stages; do not push without publishing approval:

```sh
# Run from the model-popout directory; first confirm the actual GitHub account.
# Git is already initialized; inspect the existing repository instead.
git status --short
git add .gitignore .run build.gradle settings.gradle gradlew gradlew.bat gradle runelite-plugin.properties LICENSE README.md docs src tools
git status --short
git diff --cached --stat
git diff --cached
# Only after reviewing that staged source:
git commit -m "Initial release: Model Popout"
git remote add origin https://github.com/<github-user>/model-popout.git
git push -u origin main
git rev-parse HEAD
```

Use that last hash in `plugins/model-popout` in the approved Plugin Hub fork submission. These commands are instructions for the later approval stage and were not executed during preparation.
