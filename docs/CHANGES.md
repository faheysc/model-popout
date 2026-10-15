# Changelog

## 0.2.3 — release candidate

- Add right-click Popout inspection for scene players and NPCs, placed in existing actor submenus where identity is unambiguous. Existing actions remain intact.
- Open independent inspectors with their own cameras, live models, textures, animation and optional combat HUD. Retain the last captured model after disappearance or logout without following reused actor slots.
- Bound inspector capture and rendering work; pause updates while minimized.
- Select window controls automatically: reuse inherited Swing chrome or provide built-in drag, minimize, maximize/restore and close controls. Correct duplicate title bars and keep controls visible independently of viewport opacity.
- Remember reachable main-window bounds and share camera input across viewports.
- Remove the secondary settings sidebar and performance labels. Use the standard RuneLite plugin configuration; retain saved settings and the original launch icon.
- Kill Clog/Hiscores integration is not implemented. The integration document is a specification only.

Includes accepted development revisions 0.1.1–0.1.3. Windows VM verification of the final corrected build is required before Hub submission. The published baseline remains 0.1.0 until the update is accepted.

---

The following is the historical initial-release inventory and validation record.

# File inventory and final validation

The project directory is now `model-popout`; production/test packages use `com.modelpopout`. Historical settings retain their original group identifier deliberately. Paths below are repository-relative. This inventory compares the complete verified pre-audit implementation with the final public source tree, including the owner-authorized Always on Top and combat HUD additions.

## Renamed files

- `.run/RuneLite - Live Model Viewer.run.xml` → `.run/Model Popout.run.xml`
- `src/main/java/com/livemodelviewer/BackgroundOpacity.java` → `src/main/java/com/modelpopout/BackgroundOpacity.java`
- `src/main/java/com/livemodelviewer/CameraView.java` → `src/main/java/com/modelpopout/CameraView.java`
- `src/main/java/com/livemodelviewer/FaceOrdering.java` → `src/main/java/com/modelpopout/FaceOrdering.java`
- `src/main/java/com/livemodelviewer/FramePacer.java` → `src/main/java/com/modelpopout/FramePacer.java`
- `src/main/java/com/livemodelviewer/LiveCaptureGate.java` → `src/main/java/com/modelpopout/LiveCaptureGate.java`
- `src/main/java/com/livemodelviewer/LiveModelViewerConfig.java` → `src/main/java/com/modelpopout/ModelPopoutConfig.java`
- `src/main/java/com/livemodelviewer/LiveModelViewerPlugin.java` → `src/main/java/com/modelpopout/ModelPopoutPlugin.java`
- `src/main/java/com/livemodelviewer/LiveViewState.java` → `src/main/java/com/modelpopout/LiveViewState.java`
- `src/main/java/com/livemodelviewer/ModelFrame.java` → `src/main/java/com/modelpopout/ModelFrame.java`
- `src/main/java/com/livemodelviewer/ModelProjection.java` → `src/main/java/com/modelpopout/ModelProjection.java`
- `src/main/java/com/livemodelviewer/ModelRenderer.java` → `src/main/java/com/modelpopout/ModelRenderer.java`
- `src/main/java/com/livemodelviewer/ModelScenePainter.java` → `src/main/java/com/modelpopout/ModelScenePainter.java`
- `src/main/java/com/livemodelviewer/ModelSnapshot.java` → `src/main/java/com/modelpopout/ModelSnapshot.java`
- `src/main/java/com/livemodelviewer/NpcSelector.java` → `src/main/java/com/modelpopout/NpcSelector.java`
- `src/main/java/com/livemodelviewer/NpcTargetTracker.java` → `src/main/java/com/modelpopout/NpcTargetTracker.java`
- `src/main/java/com/livemodelviewer/PixelBlend.java` → `src/main/java/com/modelpopout/PixelBlend.java`
- `src/main/java/com/livemodelviewer/RenderTarget.java` → `src/main/java/com/modelpopout/RenderTarget.java`
- `src/main/java/com/livemodelviewer/TextureClock.java` → `src/main/java/com/modelpopout/TextureClock.java`
- `src/main/java/com/livemodelviewer/TextureCoordinates.java` → `src/main/java/com/modelpopout/TextureCoordinates.java`
- `src/main/java/com/livemodelviewer/TextureRasterPlan.java` → `src/main/java/com/modelpopout/TextureRasterPlan.java`
- `src/main/java/com/livemodelviewer/TextureSet.java` → `src/main/java/com/modelpopout/TextureSet.java`
- `src/main/java/com/livemodelviewer/ViewerPerformance.java` → `src/main/java/com/modelpopout/ViewerPerformance.java`
- `src/main/java/com/livemodelviewer/ViewerSettingsPanel.java` → `src/main/java/com/modelpopout/ViewerSettingsPanel.java`
- `src/main/java/com/livemodelviewer/ViewerWindow.java` → `src/main/java/com/modelpopout/ViewerWindow.java`
- `src/main/java/com/livemodelviewer/WindowGeometry.java` → `src/main/java/com/modelpopout/WindowGeometry.java`
- `src/main/java/com/livemodelviewer/WindowPresentation.java` → `src/main/java/com/modelpopout/WindowPresentation.java`
- `src/test/java/com/livemodelviewer/AnimatedTextureTest.java` → `src/test/java/com/modelpopout/AnimatedTextureTest.java`
- `src/test/java/com/livemodelviewer/BackgroundOpacityTest.java` → `src/test/java/com/modelpopout/BackgroundOpacityTest.java`
- `src/test/java/com/livemodelviewer/CameraViewTest.java` → `src/test/java/com/modelpopout/CameraViewTest.java`
- `src/test/java/com/livemodelviewer/CapeSurfaceTest.java` → `src/test/java/com/modelpopout/CapeSurfaceTest.java`
- `src/test/java/com/livemodelviewer/FramePacerTest.java` → `src/test/java/com/modelpopout/FramePacerTest.java`
- `src/test/java/com/livemodelviewer/LiveCaptureGateTest.java` → `src/test/java/com/modelpopout/LiveCaptureGateTest.java`
- `src/test/java/com/livemodelviewer/LiveModelViewerLauncher.java` → `src/test/java/com/modelpopout/ModelPopoutLauncher.java`
- `src/test/java/com/livemodelviewer/LiveViewStateTest.java` → `src/test/java/com/modelpopout/LiveViewStateTest.java`
- `src/test/java/com/livemodelviewer/MinimalWindowTest.java` → `src/test/java/com/modelpopout/MinimalWindowTest.java`
- `src/test/java/com/livemodelviewer/ModelRendererTest.java` → `src/test/java/com/modelpopout/ModelRendererTest.java`
- `src/test/java/com/livemodelviewer/ModelScenePainterTest.java` → `src/test/java/com/modelpopout/ModelScenePainterTest.java`
- `src/test/java/com/livemodelviewer/ModelSnapshotTest.java` → `src/test/java/com/modelpopout/ModelSnapshotTest.java`
- `src/test/java/com/livemodelviewer/NativeWindowPresentationTest.java` → `src/test/java/com/modelpopout/NativeWindowPresentationTest.java`
- `src/test/java/com/livemodelviewer/NpcSceneTest.java` → `src/test/java/com/modelpopout/NpcSceneTest.java`
- `src/test/java/com/livemodelviewer/NpcSelectorTest.java` → `src/test/java/com/modelpopout/NpcSelectorTest.java`
- `src/test/java/com/livemodelviewer/NpcTargetTrackerTest.java` → `src/test/java/com/modelpopout/NpcTargetTrackerTest.java`
- `src/test/java/com/livemodelviewer/StableFramingTest.java` → `src/test/java/com/modelpopout/StableFramingTest.java`
- `src/test/java/com/livemodelviewer/TestActor.java` → `src/test/java/com/modelpopout/TestActor.java`
- `src/test/java/com/livemodelviewer/TestModel.java` → `src/test/java/com/modelpopout/TestModel.java`
- `src/test/java/com/livemodelviewer/TestNpc.java` → `src/test/java/com/modelpopout/TestNpc.java`
- `src/test/java/com/livemodelviewer/TestPlayer.java` → `src/test/java/com/modelpopout/TestPlayer.java`
- `src/test/java/com/livemodelviewer/TestTexture.java` → `src/test/java/com/modelpopout/TestTexture.java`
- `src/test/java/com/livemodelviewer/TestWorldView.java` → `src/test/java/com/modelpopout/TestWorldView.java`
- `src/test/java/com/livemodelviewer/TextureRenderBenchmark.java` → `src/test/java/com/modelpopout/TextureRenderBenchmark.java`
- `src/test/java/com/livemodelviewer/TextureRenderingTest.java` → `src/test/java/com/modelpopout/TextureRenderingTest.java`
- `src/test/java/com/livemodelviewer/WindowCompositingTest.java` → `src/test/java/com/modelpopout/WindowCompositingTest.java`
- `src/test/java/com/livemodelviewer/WindowPresentationTest.java` → `src/test/java/com/modelpopout/WindowPresentationTest.java`

## Modified existing files

- `.gitignore`
- `README.md`
- `build.gradle`
- `runelite-plugin.properties`
- `settings.gradle`
- `tools/validate.py`

Renamed Java files also received package/class references and focused edits described in AUDIT.md. Runtime changes are snapshot validation, startup generation checks/defensive cleanup, unused metadata removal, viewer-only stacking, and independent HUD wiring. The 3D renderer, cached texture raster path, camera, texture animation clock, and top-level compositor are unchanged by the HUD addition.

## Added files

- `docs/AUDIT.md`
- `docs/CHANGES.md`
- `docs/DEVELOPMENT.md`
- `docs/HUD.md`
- `docs/HUD_REPAINT_REGRESSION.md`
- `docs/IN_GAME_CHECKLIST.md`
- `docs/RENDERING.md`
- `docs/SUBMISSION.md`
- `docs/licenses/GRADLE_LICENSE.txt`
- `docs/plugin-hub/model-popout.template`
- `src/main/java/com/modelpopout/HitsplatStyle.java`
- `src/main/java/com/modelpopout/HudAnchor.java`
- `src/main/java/com/modelpopout/HudAssets.java`
- `src/main/java/com/modelpopout/HudHealth.java`
- `src/main/java/com/modelpopout/HudPainter.java`
- `src/main/java/com/modelpopout/HudSnapshot.java`
- `src/main/java/com/modelpopout/HudTracker.java`
- `src/main/java/com/modelpopout/LatestHudMailbox.java`
- `src/main/java/com/modelpopout/WindowStacking.java`
- `src/main/resources/META-INF/LICENSE`
- `src/main/resources/META-INF/THIRD_PARTY_NOTICES`
- `src/test/java/com/modelpopout/CapeFixtureReader.java`
- `src/test/java/com/modelpopout/ConfigCompatibilityTest.java`
- `src/test/java/com/modelpopout/HitsplatStyleTest.java`
- `src/test/java/com/modelpopout/HudAssetsTest.java`
- `src/test/java/com/modelpopout/HudHealthTest.java`
- `src/test/java/com/modelpopout/HudPainterTest.java`
- `src/test/java/com/modelpopout/HudRepaintRoutingTest.java`
- `src/test/java/com/modelpopout/HudTrackerTest.java`
- `src/test/java/com/modelpopout/LatestHudMailboxTest.java`
- `src/test/java/com/modelpopout/WindowStackingTest.java`
- `tools/audit.py`

## Removed from the publishable project

- `CAPE_RENDERING_FIX.md`
- `FEASIBILITY.md`
- `LIVE_ANIMATIONS.md`
- `NPC_AND_TRANSPARENCY.md`
- `PRESENTATION_FIX.md`
- `RENDERING_UPGRADES.md`
- `tools/CapeCacheFixtures.java`
- `tools/cape_cache_fixtures.py`
- `tools/checkpoint.py`

These files remain in the verified pre-audit archive; obsolete investigations/research tools were also preserved outside the public tree. Generated classes/logs/cache assets, build/Gradle caches, and IDE user state are excluded. No previous backup was deleted or modified. The Gradle wrapper scripts/JAR/properties and BSD license text are unchanged.

## Final test/build record

- Java 11.0.32 / `--release 11`; inspected Plugin Hub API version 1.13.1.
- Full JUnit suite with actual local cape and hitsplat-definition fixtures: **140 passed, 0 failed, 3 skipped**. Skips are native desktop presentation/stacking checks.
- Default portable run without private fixtures: **137 passed, 0 failed, 6 skipped**. The three additional skips are optional actual cape/hitsplat data.
- Two independent direct compilations/packages produced the same JAR SHA-256: `613709aee2227a6861ca2fce4af788fa3a61f6eef3c3e55636fda3345c66c7ab`.
- Production static audit: **35 source files, 56 packaged Java 11 classes, no flagged prohibited API/private-path/metadata/license errors**. This is a project check, not official Hub CI.
- Gradle wrapper matches the official template; full Gradle execution is blocked by the environment's cache/socket permissions. Run the commands in DEVELOPMENT.md outside this sandbox before submission.
- The final renamed build, Always on Top, and combat HUD require the desktop/in-game checklist; no headless test establishes OS stacking or live appearance.

## Final source layout

```text
model-popout/
  .gitignore
  .run/Model Popout.run.xml
  build.gradle, settings.gradle
  gradlew, gradlew.bat
  gradle/wrapper/
  runelite-plugin.properties
  LICENSE, README.md
  docs/
    AUDIT.md, CHANGES.md, DEVELOPMENT.md, RENDERING.md, HUD.md, HUD_REPAINT_REGRESSION.md
    IN_GAME_CHECKLIST.md, SUBMISSION.md
    licenses/GRADLE_LICENSE.txt
    plugin-hub/model-popout.template
  src/main/java/com/modelpopout/
  src/main/resources/META-INF/
  src/test/java/com/modelpopout/
  tools/JavaTestRunner.java, validate.py, audit.py
```

`build/`, `.gradle/`, and `.idea/` are generated/ignored and must not be staged. Backups, raw game assets, private fixtures, and historical research stay outside this project. No Git repository was initialized and no commit, push, publication, or Hub submission was performed.

## Subsequent repaint correction

The HUD now invalidates the whole owned compositing surface through a focused ModelPanel repaint override. Tests exercise the real component, native-parent rectangle forwarding, coalescing targets, and detached/reopened ownership. Optional diagnostics inspect paint-source counts and opacity flags on the existing metrics timer; ViewerWindow adds only a read-only getter. The compositor, rasterizer, frame publication, and FPS settings are preserved. See HUD_REPAINT_REGRESSION.md for the confirmed routing defect and unresolved native verification.
