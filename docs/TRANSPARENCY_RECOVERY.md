# Native transparency recovery after the flicker correction

The user verified that the nonopaque Swing hierarchy correction eliminates flicker,
but reported a black opaque background at 30% UI opacity. That working version was
archived and verified before these changes. All previous checkpoints remain intact.

## Confirmed software defect

The previous correction allowed a window on an ordinary graphics configuration to
retain its transparent background Color when the device advertised translucency.
However, Java 11 Window.setBackground returns immediately for an equal Color. Its
alpha-capable graphics-configuration negotiation only runs on an opaque-to-
transparent transition. Consequently a transparent background Color and advertised
device support do not establish that the current native surface can carry alpha.
Calling setBackground with the same transparent Color cannot recover that state.

The Java 11 X11 peer source further shows that setOpaque is a no-op; the X11 visual
and corresponding graphics configuration are what permit pixel alpha. A visual
change requires native peer recreation, which AWT performs as part of its supported
Window background/configuration negotiation. No internal peer API is used here.

This native configuration recovery defect is reproduced by tests of the JDK's
setter state machine. It is consistent with a correctly rendered model over a black
background, but there is no new KDE runtime trace establishing the exact native
state during the user's observation. The previous A–D traces predate this reported
transparency regression. Do not claim that KDE caused the defect or that desktop
transparency has been restored merely from headless results.

## Correction

WindowTransparency applies the native background separately from the UI opacity:

1. Check advertised device support as before.
2. If the current configuration lacks translucency while the background is already
   transparent, perform an opaque-to-transparent background transition on the EDT.
   This invokes Java's configuration negotiation instead of its equal-color no-op.
3. Verify the resulting configuration is actually translucency capable and the
   background alpha is zero before reporting supported transparency.
4. Retain a solid, clearly reported fallback if negotiation is rejected or no usable
   configuration exists.

The repair occurs only on a lost-capability configuration. Healthy windows do not
have their native opacity toggled on normal slider changes or model frames. No new
threads, timers or queues were added. AWT may recreate the native peer during a
configuration repair; movement, stacking, resizing and controls need KDE checking
across that transition.

ViewerWindow.setBackground still restores all Swing presentation layers to
nonopaque after each operation. The full-window presentation buffer and previous
child-to-whole-surface repaint routing remain intact. No 3D, HUD, texture, animation,
capture or frame pacing calculations changed. Only one diagnostic log line was
added to the existing metrics timer.

The Java window remains undecorated because per-pixel translucency requires it.
The pending 0.1.3 pass selects one application title bar: inherited Swing controls
when available, otherwise built-in controls with independent opaque presentation.
The original assumption that the existing Linux bar was compositor-provided was
incorrect; RuneLite enables inherited Swing decorations. Selection is now
unconditionally Automatic; saved mode preferences are ignored. See DUPLICATE_TITLE_BARS.md.
All application controls stay inside the existing full-frame buffer.

## Validation and evidence

Full Java 11 suite with private cape/HUD fixtures: 149 passed, 0 failed, 5 native
desktop checks skipped. The four new native-policy tests fail when the previous
policy is substituted (145 passed, 4 failed, 5 skipped), then pass with recovery.
Source audit passed. Compilation and the complete suite were rerun after review.

The real Surface + WindowPresentation test exercises repeated transitions
100 -> 30 -> 0 -> 50 -> 100 -> 0 -> 30, asserting final alpha 255/77/0/128, UI control
alpha and unchanged fully opaque player/NPC pixels. Existing previous-frame,
repaint-bypass, opacity/fallback, resizing, model/HUD and texture tests remain.
An additional opt-in native desktop test checks real Window background alpha,
configuration capability, layer routing and final buffer alpha at 0/30/50/100.
Native tests cannot run against a working KDE display in this environment.

## KDE verification

Launch normally with live HUD and player/NPC enabled. Test 0%, 30%, 50%, and 100%:
move the mouse outside the window at 0%, since the existing hover reveal temporarily
shows its background. Verify the desktop is visible at fractional opacity, models
and HUD retain opacity, and flicker remains absent. Also move/resize the window,
rotate/zoom, minimize/restore, toggle Always on Top, and close/reopen.

For a diagnostic run use only `-Dmodelpopout.paintDiagnostics=true` (no freeze or
HUD-disable flags). The existing collector's C/D runs do this. Set 30% after opening
and keep it there for several seconds, then try 0/50/100 and move the window. Send
that run's console log if anything fails. No four-case isolation is required again.

Expected diagnostics on a capable desktop:

- draws outside Window.paint remains **0**;
- root/layer/surface opaque remain **false**;
- requested/effective equal the slider value, supported=true;
- nativeAlpha=0, configurationCapable=true, undecorated=true;
- viewport background alpha is 00/4d/80/ff at 0/30/50/100 percent (without hover);
  in the custom-controls pass surfaceBackground samples the independently opaque resize frame;
- finalBufferBackground records the outermost ARGB border pixel; a desktop/LAF
  border can differ from the interior surface background;
- destination identifies the color model used for the final draw.

If native alpha/configuration or effective opacity is wrong, inspect the background
policy/configuration/transition messages. If the surface ARGB is correct but the
final window buffer becomes opaque, investigate outer Swing/LAF painting. If both
buffers and the native configuration retain alpha but the desktop still shows
black, investigate AWT/X11 destination behavior and KDE composition rather than
changing the renderer. These logs distinguish stages without assuming a KDE fault.
