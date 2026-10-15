> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Targeted cleanup — pending 0.1.3

No version, Plugin Hub manifest, commit, publication, or acceptance change was made.
Published version remains 0.1.0; accepted development revision remains 0.1.2;
window controls remain pending acceptance as 0.1.3.

## Recovery and scope

Before editing, the tracked and untracked working tree was archived in
`checkpoints/targeted-cleanup-before-20261015-004324/`. The archive's 138 files
were verified individually against its SHA-256 manifest. It also contains the
original working-tree status and binary tracked diff. Previous uncommitted work
is retained.

Production changes:

- `ModelPopoutPlugin.java`: retains the priority-10 character launch button and
  its existing EDT open/restore behavior. Removes the secondary settings button,
  settings-only deferred-creation fence, synchronization and performance labels.
  The one-second diagnostics timer now runs only when paint diagnostics are enabled.
  Separate trace/counter diagnostics remain intact.
- Removed `ViewerSettingsPanel.java` and `ViewerPerformance.java`: the latter's
  measurements were consumed only by the deleted panel. No replacement UI exists.
- `LiveCaptureGate.java`: removes the `force` argument and timing bypass. Its sole
  production caller previously always passed false. Deadlines, fractional timing,
  backpressure, epoch fencing and rate changes retain the same normal behavior.
  `LiveViewState.updateModel(..., force)` remains unchanged.
- Added `ModelViewport.java`: a small shared JPanel base containing only existing
  camera mouse handling, nonopaque setup and whole-surface repaint forwarding.
  `ModelPanel` and `InspectionPanel` use it with separate camera states and render
  callbacks. Model/HUD drawing stays in the original panel implementations.

`ModelPopoutConfig.java` is unchanged: all eight settings, labels, keys, defaults,
legacy group and dormant preferences remain intact. Standard RuneLite config events
still apply directly to open main and inspection windows. There is no coupling to
RuneLite's own display settings.

Renderer, texture/material/cache logic, native background recovery, title bars,
transparency, alpha composition, Window.paint buffering, geometry, Always on Top,
HUD, actor tracking, menu/submenu integration, workers and scheduling are unchanged
apart from removal of the explicitly unused gate branch and sidebar measurements.
`InspectionSession`, `InspectionManager` and the Kill Clog contract were not edited.

Test changes: updated `AutomaticWindowControlsTest`, `BackgroundOpacityTest`,
`ConfigCompatibilityTest`, `FramePacerTest`, and `LiveCaptureGateTest`; added
`ConfigTestSupport`, `LaunchNavigationTest`, `ModelViewportTest`, and
`NativePluginConfigurationTest`. Deleted-panel tests are replaced by real RuneLite
config load/save/event checks. The native integration test exercises launch/reuse,
configuration events in open main/inspection windows, opacity, stacking, targets,
main capture cadence, and independent close behavior. `build.gradle` includes it
in the existing native test task.

Documentation updated: README, DEVELOPMENT, DUPLICATE_TITLE_BARS, INSPECTION,
and this report. Historical test records elsewhere retain their original scope.

## Validation consistency (separate investigation)

Gradle defaults to `latest.release`, while `tools/validate.py` defaults to 1.13.1.
They can therefore test different APIs without overrides. Both were explicitly
run against 1.13.2 here. DEVELOPMENT documents matching arguments as the smallest
correction for validation runs. Neither default nor any dependency version changed.

The offline validator also excluded RuneLite's FlatLaf fork, preventing compilation
of existing RuneLiteLAF tests. Its allowlist now admits the exact FlatLaf version
specified by the selected cached client POM. This is an existing client dependency,
not a new project dependency. The offline validator still is not a general dependency
resolver; Gradle remains authoritative.

## Results

Java 11 / RuneLite 1.13.2, with cape and HUD fixtures:

- Clean Gradle `test jar desktopPresentationTest`: passed.
- Headless suite: 208 passed; 12 native tests skipped as intended.
- Native desktop suite: all 12 passed on the available Linux/KWin display.
- Offline validator with explicit 1.13.2 and fixtures: 208 passed, 12 native skips.
- Source/packaging audit: 45 production sources, 79 classes, zero errors.
- `git diff --check`: passed.

The new launcher restoration test initially asserted before KWin processed native
state changes. Robot.waitForIdle between minimize/restore requests fixed test
synchronization; window-control production behavior was not changed.
Gradle reports an existing deprecated-feature warning for future Gradle 9 use.
No Windows, macOS, or in-game verification is claimed. Automated native tests do
not establish actual game animation, combat HUD, or platform-specific acceptance.

## Manual review gate (Windows and Bazzite separately)

1. Enable the plugin: only the original character launch icon remains at its
   existing sidebar position. Click to open; minimize and click again to restore.
2. Check all eight standard RuneLite configuration settings and existing saved
   values. Change them with the main viewer and two player/NPC inspectors open;
   check immediate effects and persistence across reopening/restarting.
3. Drag inside each model to rotate, including inverted vertical movement; wheel
   zoom. Release and move, and try other buttons: no unintended camera movement.
   Cameras remain independent. Title-bar dragging remains separate.
4. Test 0/30/50/100% background opacity, opaque models, material alpha, HUD,
   animation, resize, minimize/maximize/restore, Always on Top and monitor movement.
   Look for flicker and confirm one usable title bar per window.
5. Close the main viewer and each inspector independently; other windows and
   RuneLite remain usable. Disable/re-enable the plugin with inspectors open.
6. Confirm Popout submenu placement and disappearance/logout frozen-model behavior.

Primary remaining risk is game/desktop integration outside automated fixtures;
configuration test reflection is confined to tests and tied to the requested
RuneLite 1.13.2 baseline. No production reflection or native platform hacks added.
