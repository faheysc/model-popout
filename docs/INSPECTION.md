> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Right-click inspection development pass

This pass extends the approved `0.1.0` baseline at commit `38be0a9edc07c04462d1317b5bc63b9f3e1f50fd`. A verified Git recovery bundle was made outside the source tree before editing. The owner accepted the first in-game player inspection and authorized the submenu stage. Accepted inspection work is recorded as development revision `0.1.1`; no release increment or publication has occurred. The implementation remains uncommitted. See [VERSIONING.md](VERSIONING.md), [development history](DEVELOPMENT_HISTORY.md), and [the current submenu update](SUBMENUS.md).

## Scope and configuration

`Enable Right-Click Inspection` defaults to true, under the existing `livemodelviewer` group with key `rightClickInspection`. RuneLite's standard plugin configuration exposes the checkbox. Disabling it removes only this plugin's own menu entries and fences callbacks from a menu already open. Existing inspectors remain usable.

Each scene player/NPC selection adds exactly `Popout` without creating a submenu, consuming a native player-menu slot, sending a game-server action, or modifying another plugin's options. The subsequent [submenu pass](SUBMENUS.md) uses existing actor submenus where appropriate and otherwise retains the original outer-menu placement above Cancel. Reentrant handling deduplicates owned callbacks by actor identity. Chat, friends lists, and other name-only interfaces cannot supply a live scene model and are outside scope.

Associated Player Lookup is deliberately deferred. Neither provider is called, no lookup setting is exposed, and no HTTP requests or synthetic clicks are introduced.

## Ownership and lifecycle

- `InspectionActor` binds the original actor, original WorldView object/id, and index. Both world resolution and indexed membership must still return those exact objects. Despawn events, logout/hopping, and membership loss permanently release the binding. Names and indexes alone cannot reattach it. A fresh click creates a fresh binding, including repeated inspections of the same actor.
- `InspectionManager` owns client-thread capture/HUD sources, the active registry, and a separate inspector executor. The existing main controller receives only additive event forwarding and startup/shutdown hooks. Its capture, renderer, worker slot, texture state, publication, and window code remain intact.
- `InspectionCapture` owns copied geometry/textures and actor-specific HUD tracking. Equipment changes, animations, and same-object NPC transformations are detected by actual snapshot content. A null/failed model capture keeps the prior owned snapshot. Remote players and NPCs use scaled health only. Four observed hitsplat slots are retained per inspector; no historical hits are reconstructed when opening or restoring a window.
- `InspectionMailbox` keeps one replaceable packet with one pending EDT delivery. Packets contain no native Actor/Model/WorldView references. Closing fences racing offers and queued callbacks.
- `InspectionSession` owns a camera, stable framing, panel, texture clock, render pacing, and optional native window. The single-actor path uses existing renderer and HUD slot infrastructure for either actor type. Every session owns its model scratch/cache data. An actual transformed NPC definition change refits only that inspector and invalidates an in-flight frame from the prior form, preserving the user's camera. The additive `LiveViewState.reframeModel` method is used only by inspectors; main-view framing behavior is unchanged.
- Disappearance preserves the latest successful capture and captured texture phase, clears HUD and native actor/world references, and leaves camera-only rendering available even after logout. Missing initial data displays an explicit unavailable message. Frozen sessions stop live capture and HUD asset sampling. Region loading pauses capture and clears HUD; resumption validates the original binding. No automatic name-based reattachment occurs.
- Closing removes the session from the registry and fair queue, fences packet delivery, clears source data on the client thread, stops EDT timers, clears panel/frame references, and disposes only its own window. Current raster work keeps its real worker slot until finishing, then cannot revive a closed session. Plugin shutdown closes all inspectors and lets current raster work finish without interruption.

## Rendering invariants and resource bounds

Reuse `ViewerWindow`, `WindowPresentation`, `WindowTransparency`, `WindowStacking`, `ModelSnapshot`, `ModelRenderer`, camera/framing/texture classes, and HUD assets/painters. No changes are made to their raster or compositing implementations. Inspector panel repaints ascend to their whole owned `ViewerWindow.Surface`, matching the main window's corrected routing. UI/background alpha never multiplies the model or HUD. Native opaque fallback remains above the top-level private presentation buffer.

There is one inspector rendering worker, separate from the main viewer's executor. Its FIFO has one replaceable pending supplier per open inspector; work snapshots are selected only when that inspector reaches the front. One running job completes before another begins. Busy windows rejoin behind waiting windows. Each window has its own requested target/pacing state; targets are not guaranteed throughput under aggregate load.

Client-tick capture rotates fairly through visible inspectors, permits at most two model captures and two independent material samples per tick, and stops scheduling more work once four milliseconds have elapsed. This is a soft budget checked between samples, not a preemptive bound on an individual native read/copy. Independent material sampling keeps cape scrolling separate from the pose-capture throttle, using the same retained owned geometry. Per-inspector pose sampling uses the existing capture-rate setting and fractional deadlines, with no catch-up burst. The initial user selection attempts one immediate capture. Minimized windows stop queued rendering and model/HUD/material sampling, while identity validation continues. A running raster may finish and retain its image for restore.

Existing snapshot, texture, hitsplat, fragment, and raster-work limits apply. Native window-sized pixel buffers still use approximately `12 * width * height` bytes per window, in addition to model/raster/texture data. CPU and memory scale with open windows, model complexity, and window dimensions. No hardware performance guarantee or unlimited-window claim is made. Framing remains stable across poses/equipment and refits on a changed NPC definition; extreme equipment size changes can require manual zoom adjustment, as with existing rendering limits.

## Automated validation

Run with a Java 11 JDK:

```sh
./gradlew --no-daemon --console=plain clean build -PruneLiteVersion=1.13.2
./gradlew --no-daemon --console=plain desktopPresentationTest -PruneLiteVersion=1.13.2
python3 tools/audit.py
```

New checks exercise exact menu labels, enable/disable, preservation of lookup actions, reentrant menu deduplication, two actor kinds/WorldViews, index reuse, equipment and transformation data, owned textures, scaled health/hitsplats, pause/detach/frozen controls, independent cameras, native close cleanup, shutdown races, fair bounded queues, capture-rate cadence, packet coalescing, and inspector repaint/alpha behavior. All existing cape/texture/flicker/repaint regressions remain in the complete suite.

Native window tests are opt-in and separate from the normal headless suite. Optional real Fire/Infernal cape and raw hitsplat-definition fixtures remain external and are skipped when absent. Automated native tests validate Java window and compositing behavior, not visual certification of game menu layout or live actors.

Validation on 2026-10-14, Java 11 / RuneLite 1.13.2: clean build passed; the final complete suite reported 195 tests, 186 passed, nine skipped, and no failures/errors. The nine normal-suite skips are six opt-in native tests and three absent real-asset fixture tests. The separate desktop task then passed all six native tests with no skips. Source/artifact audit passed with 45 production sources, 73 production classes, and zero errors. In-game checks below remain outstanding.

## Required in-game verification

1. Launch the development client on RuneLite 1.13.2. Begin with software rendering, capture 20 FPS, rendering target 60 FPS, background opacity 100%, HUD switches enabled, and Always on Top disabled. Confirm the new checkbox defaults enabled.
2. Keep the main Model Popout window closed. Right-click another player and an ordinary noncombat NPC. Confirm one exact `Popout` entry per actor, no submenu, correct menu height/click region, and unchanged normal actions. Do not attack/interact first. Select each and verify the correct live model opens.
3. Enable Kill Clog and Hiscores with their normal player-menu actions. Confirm both original lookups still open their normal sidebars. Popout must open only the inspector and must not initiate either lookup. Repeat with other player-menu plugins enabled and crowded/overlapping actors; Popout must not consume their native slots or inspect the wrong target.
4. Open the main viewer plus at least three inspectors, including two for the same actor. Move/resize them separately; rotate/zoom each. Confirm other cameras/windows and the main viewer do not move or reset. Close/reopen the main viewer while inspectors remain open, then close one inspector while the others continue.
5. Observe a player changing equipment and animating. Inspect Fire/Infernal cape textures from front, back, and sides; check colors, intrinsic alpha, cutouts, and lava motion. Observe an NPC transformation and available animation; verify the same actor remains selected without swapping to another NPC.
6. Observe combat on inspected actors. Verify hitsplats only belong to the selected actor, no HP is borrowed from your local character, scaled bars disappear when native health data expires, and HUD toggles affect all relevant windows. Inspect a noncombat actor with no health data; do not expect a fabricated bar.
7. Test opacity in order 100% → 50% → 0% → 100%, with resizing, camera movement, animations, and HUD activity. Check every window for flashing/black rectangles, fading models, cape speckles, clipped HUD updates, and stale pixels. Check zero-opacity hover controls and native title-bar behavior.
8. Toggle Always on Top on/off. Verify it affects owned popout windows and does not change RuneLite's main-window stacking. Move windows across available displays and retest opacity/fallback behavior.
9. Minimize one inspector while others animate. Restore it and confirm fresh model/HUD data, responsive controls, and no stale hits. Compare CPU/memory with one, three, and several visible inspectors; repeat with inspectors minimized and closed. Pay attention to the main viewer and client responsiveness at normal settings, then higher render targets.
10. Let an inspected player leave the scene or an NPC despawn. Verify its last capture stays visible with the unavailable status, combat HUD clears, and rotation/zoom still works. Let another actor occupy a recycled slot; the frozen window must never change to that actor. Use a fresh Popout to inspect the new actor.
11. Open an actor's menu, allow it to disappear before selecting Popout, then select. Verify an explicit unavailable/no-capture window rather than a replacement or fabricated model. Test region loading, world hopping, logout, and login: frozen models must stay controllable and must not reattach to new actors.
12. Disable right-click inspection through RuneLite plugin configuration. New menus must contain no Popout; existing inspectors remain usable. Re-enable and confirm one entry per actor. Disable/re-enable the plugin with multiple inspectors open; all inspectors must close, and newly opened windows must work without stale callbacks. Repeat core appearance/opacity checks with GPU and Animation Smoothing if normally used.

Record the actor/equipment/NPC, platform/desktop, settings, GPU/smoothing state, and any reproduction steps for failures. In-game verification is the release gate for this pass.

The normal `run` task enables safe mode, so begin with `./gradlew run -PruneLiteVersion=1.13.2`. For the Kill Clog/other Hub menu coexistence check, safe mode must be omitted: `./gradlew run -PruneLiteVersion=1.13.2 --args='--developer-mode --debug'`. Ensure only one Model Popout implementation is enabled during development testing.

## Plugin Hub review scope

The implementation uses public APIs, Java 11, no runtime third-party dependencies, reflection, model mutation, game inputs, networking, or cross-plugin class dependencies. The existing main release is approved, but arbitrary player/NPC inspection and multiple persistent combat displays expand its scope. Reviewers may assess PvP scouting or additional combat-information implications under [RuneLite's restrictions](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) and [Jagex's guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1). Public API use and passing tests are not approval of that expanded scope. No targeting advice, predictions, exact remote HP, group summaries, publication/crowdsourcing, or detached game interaction is added.
