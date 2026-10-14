# Combat HUD repaint regression investigation

Investigated on 2026-10-13 after the owner reported continuous desktop flickering. The pre-audit working checkpoint and the renamed pre-HUD checkpoint were compared with the current source. A complete verified checkpoint was made before editing; previous verified archives were preserved.

## Confirmed source regression and correction

Completed model frames request `window.getContentPane().repaint()`, invalidating the whole compositing surface. The new HUD delivery and configuration handlers instead called `panel.repaint()`, marking the child model panel dirty. The two requests have different Swing dirty-component identities and rectangles. The surface's painting-origin behavior translates a child rectangle upward but does not enlarge it to the whole surface. The translated native update is clipped to the model panel, including after the private full-frame compositor has painted its image.

| Update | Previous target | Corrected target |
| --- | --- | --- |
| Completed model/texture/camera frame | Whole surface | Whole surface, unchanged |
| HUD packet, expiration, health change | Model-panel child rectangle | Whole surface |
| HUD checkbox change | Model-panel child rectangle | Whole surface |
| Other model-panel invalidations | Model-panel child rectangle | Its own whole surface |

`ModelPanel.repaint(long, int, int, int, int)` now forwards attached-panel invalidations to its actual `ViewerWindow.Surface` parent. Detached panels and other parents retain normal Swing behavior. This fixes the inconsistent invalidation path at its source, rather than changing rendering speed, alpha math, or model/HUD timing. Renderer and HUD requests can now be coalesced under the same Swing dirty component. It does not guarantee that requests arriving in different event-loop batches produce only one paint, nor does it change the configured FPS.

The model-panel constructor accepts its initial config and the nested class is package-visible so regression tests exercise the real component without reflection or a headful JFrame. Runtime initialization otherwise remains the same.

**Evidence:** two new tests failed with the inherited child repaint behavior while all 137 existing headless tests passed. They observed a child-only dirty target and two dirty-component identities for interleaved HUD/model updates. After the correction, requests target the entire surface across multiple sizes and 0/50/100% opacity, and actual JDK `paintImmediately` routing submits a full-surface rectangle to the native-parent stand-in. A further test checks close/reopen ownership and detached-component behavior. Existing fixed-image/new-frame, private-clear, HUD-expiration, image-immutability, and alpha tests still pass.

This establishes a **code-level repaint-routing regression** relative to the verified presentation path. It does **not** establish that this is the sole cause of continuous Bazzite/KDE flicker. Native desktop reproduction is unavailable in this environment; in-game verification is required.

## Other investigated paths

- `WindowPresentation`, the 3D renderer, texture-only raster path, animation clock, camera math, frame pacing, and scene-image painter are unchanged from the pre-HUD checkpoint. The window's presentation implementation is also unchanged; only a read-only diagnostic getter was added.
- HUD pixels are drawn into the existing private composed model layer above the completed model image. There is no separate native HUD clear/blit and no evidence here of incomplete image publication or published-image mutation.
- HUD publication uses a bounded latest-packet mailbox and EDT delivery. It introduces no new timer or render thread. Model rendering retains its bounded worker and completed-frame replacement.
- JDK 11 source confirms that `JFrame.update` already delegates to `paint`. An unbuffered inherited `Container.update` clear is therefore not the explanation for this JFrame.
- The rename changes display/package/launcher names; it does not change painting calculations. KDE rules matching the old caption or application class may need review if such rules exist.
- Always on Top uses the supported viewer-only Window API on opening/config changes. It is not invoked per animation frame.
- Audit cleanup's snapshot validation rejects incomplete data; unsuccessful captures retain the previous completed frame. No repeated component rebuilding or animation-driven layout change was added.

## Optional paint diagnostics

Diagnostics are off by default, add no window controls, and use the existing once-per-second metrics timer. Enable them for the development launcher, which already uses debug logging:

```sh
JAVA_TOOL_OPTIONS="-Dmodelpopout.paintDiagnostics=true" ./gradlew run -PruneLiteVersion=1.13.1
```

For IntelliJ, set that environment variable in the Gradle run configuration, or add `-Dmodelpopout.paintDiagnostics=true` to the JVM options when running the Java launcher directly. Remove it after gathering a short trace. There is no configuration/profile migration or game-state change.

The `Model Popout paint audit` log records cumulative HUD deliveries, completed-model publications, composed model draws, draws outside `Window.paint`, player identity rebindings, window/root/layer/surface opaque flags, and HUD toggles. No player names, NPC identities, damage values, or health values are logged.

In normal per-pixel mode, window/root/layer/surface should be non-opaque and model draws should run inside the protected `Window.paint` boundary. Draws outside that boundary may be normal in an opaque fallback, but are important evidence if the native window remains non-opaque. Player rebindings should occur on actual lifecycle/identity changes, not continuously during one session. Counters distinguish fixed completed-image repainting from new model publication; they are not monitor refresh or unique animation FPS.

## Validation and in-game checks

Java 11 compilation and the full fixture-enabled suite passed: **140 passed, 0 failed, 3 native desktop tests skipped**. A headful attempt could not connect to the X11 display; its toolkit initialization errors are an environment limitation, not evidence of a native rendering fix. Gradle/desktop validation remains subject to the previously documented sandbox restrictions.

Test the corrected build in the same safe-mode client first, with both HUD toggles enabled:

1. Compare idle and combat at the previously used FPS, then at 60 and 240; no target was lowered by this patch.
2. Check 0%, 50%, and 100% background opacity, model/HUD alpha, sustained cape texture animation, camera motion, and resizing.
3. Exercise multiple hits, health changes, expiration, NPC switches/death/despawn, logout, and close/reopen.
4. Toggle Always on Top while focusing another application. Check native controls and any existing KDE window rules.
5. If flicker remains, collect about 15 seconds of diagnostic output while idle and during combat, including a comparison with both HUD toggles disabled. The counters/opaque flags are needed before claiming another cause or changing the compositor.

No desktop flicker-free claim is made from headless tests. No commit, push, or submission was performed.
