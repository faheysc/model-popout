# KDE opaque-layer repaint regression

## Runtime evidence

The four user-collected traces in the private local audit directory used rendering
60 FPS, capture 50 FPS, UI opacity 100%, and Always on Top disabled:

| Case | User observed | Final draws outside Window.paint | Window/root/layer/surface |
| --- | --- | ---: | --- |
| A: fixed image, live HUD | No flicker | 0 | All false |
| B: live image, HUD bypassed | No flicker | 0 | All false |
| C: live image and HUD | Player flicker | 2213 | All true |
| D: live player/NPC and HUD | Both flicker | 2039 | All true |

C switches from all false at 22:34:23 to all true at 22:34:24. D switches between
22:35:40 and 22:35:41. Both transitions precede model publication and HUD packet
receipt. Therefore these records do not establish that a HUD event itself changes
opacity. A/B test differences correlate with the failure, but do not prove the
trigger for the different desktop window states.

All 7716 recorded render completions across A–D were nonblank and error-free. Every
sampled before/after HUD source-image check was unchanged. No trace overflow was
reported. This supports a native presentation bypass rather than blank/incomplete
model replacement or HUD source-image mutation.

## Confirmed failure and responsible code

`ViewerWindow.applyBackgroundOpacity` previously required the *current* graphics
configuration to be translucency capable. Its graphicsConfiguration callback
reapplies that policy when AWT moves/reconfigures the window. If the current
configuration becomes ordinary, the policy selects `setBackground` with alpha 255
even when that graphics device supports per-pixel translucency.

Java 11 `Window.setBackground` calls its private `setLayersOpaque(this,true)` on an
alpha-below-255 to alpha-255 transition. That sets exactly the observed root pane,
layered pane and content surface opaque. Java's `JComponent.paintImmediately`
ascends past nonopaque ancestors, but stops at an opaque Swing component and calls
its `_paintImmediately` directly. The previous child-to-whole-surface repaint fix
still submits the correct full dirty rectangle, but an opaque surface paints that
rectangle outside `ViewerWindow.paint` and its `WindowPresentation` buffer. The
three layer flags are routing invariants, not a reflection of the configured UI
opacity.

The trace did not log graphics configuration/background setters, so the specific
caller of the first alpha transition is not independently confirmed. The callback
is a confirmed source path capable of producing it; new opt-in logs record actual
configuration changes, opacity decisions and alpha-transition caller stacks. An
external background setter is covered by the same correction. FlatLaf root-pane
background installation was inspected; its window update only replaces null or
UIResource backgrounds, whereas our transparent background is an ordinary Color.
There is no HUD call to setOpaque, setBackground, paintImmediately or direct native
painting. Always on Top does not change the background or component opacity.

## Focused correction

- Override `ViewerWindow.setBackground` and restore the root, layered and content
  Swing components to nonopaque after the JDK setter, including its opaque fallback
  and exception paths. Handle JFrame's constructor-time virtual call safely.
- Keep the device support check and supported opaque fallback, but stop treating
  an ordinary current configuration as device-wide lack of support. Java's public
  Window background API negotiates a capable configuration when required.
- Preserve the previous full-surface dirty routing. No renderer, snapshot, HUD,
  frame publication, compositing calculations, pacing or lifecycle code changed.
- Add only opt-in transition logging, with no new timers or threads.

A native opaque window remains legitimate on an unsupported desktop. Its Swing
layers still stay nonopaque so updates reach the protected window paint boundary.
UI opacity is still applied to the private UI image only. The model/HUD image is
still composited at its original opacity. A failed or pending render still does
not clear the displayed completed image.

## Regression validation

A headless test reproduces the JDK layer side effect, observes the failure to
propagate to the native parent, then checks the corrected routing for model,
surface, root and layered-pane origins at multiple sizes. With layer repair
removed (simulating the pre-correction transition) the full suite reports 143
passed, 1 failed, 4 skipped. With repair enabled: 144 passed, 0 failed, 4 skipped.
Java 11 compilation and cape/HUD fixtures are included.

A new opt-in native test invokes the actual Window.setBackground alpha transition
and checks all layers and immediate-paint routing. It is skipped here along with
the three existing native tests because a working desktop is unavailable. The
headless test is proof of the Swing routing failure, not proof of flicker-free
KDE presentation.

## User verification

Repeat C and D using the existing collector, with normal HUD and live animation.
For convenience the same collector can run A–D again. At the original settings,
verify both models stop flickering. The outside-Window.paint counter should remain
zero, and root/layer/surface should remain false even if windowOpaque becomes true
for a genuine native fallback. On a transparency-capable desktop the window should
remain nonopaque at UI opacity 100% as well as 50% and 0%.

Then test UI opacity 0/50/100%, hover reveal, moving/resizing, camera rotation and
zoom, player/NPC animations, hitsplats/HP bars, Fire/Infernal cape scrolling, target
switching, closing/reopening and Always on Top. If flicker persists, send the new
trace including `Model Popout graphics configuration changed`, `background policy`
and `window background alpha` messages and their caller stacks. Also verify once
with all diagnostic properties removed because tracing adds CPU overhead.

No desktop flicker-resolution claim is made until user verification.
