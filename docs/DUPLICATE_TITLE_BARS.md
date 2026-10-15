> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Duplicate title bars — correction within pending 0.1.3

## Investigation

The first window-controls pass missed RuneLite's global Swing decoration setting.
The cached RuneLite **1.13.2** `net/runelite/client/ui/ClientUI.java` enables
`JFrame.setDefaultLookAndFeelDecorated(true)` in its Linux custom-chrome branch.
It also uses `JRootPane.FRAME` for RuneLite's own chrome. JFrame construction can
therefore install a look-and-feel title pane on each new Model Popout window.
`setUndecorated(true)` suppresses AWT/native decorations, but does not clear the
separate Swing root-pane decoration style. Installing WindowTitleBar beneath that
inherited pane produces two functional bars in **one JFrame**.

The native regression now reproduces exactly that one-frame case with both
RuneLiteLAF and Metal: root decoration style FRAME, undecorated=true, and an
inherited title pane. Automatic mode retains that pane, with its control buttons,
and no WindowTitleBar child. This establishes a concrete duplication path without
requiring overlapping main/inspection windows. Both production call sites create
one ViewerWindow each; only inspectors intentionally create additional windows.
There is no second plugin JFrame wrapper or second custom title-bar insertion.

No screenshot was attached to the correction request and no live Model Popout
window was available for inspection. We cannot establish the outer bar's identity
in that particular in-game capture. The available display identifies KWin under
XWayland; the inspected root client list contained only Xwayland Video Bridge.
The available KWin rule file had no matching forced-decoration rule. These facts
do not establish that the reported bar was server-side KDE decoration. The
single-frame inherited-Swing reproduction is the verified software defect.

[Java's JFrame decoration API](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/swing/JFrame.html#setDefaultLookAndFeelDecorated(boolean))
describes the global look-and-feel hint. `JRootPane.getWindowDecorationStyle()`
reliably identifies Swing's requested title-bar style; `isUndecorated()` identifies
AWT decoration policy. Neither API reports what an external compositor actually
paints, nor do frame insets establish that minimize/maximize/close controls exist.
[KWin rules](https://docs.kde.org/stable_kf6/en/kwin/kcontrol/windowspecific/attributes.html)
can independently control titlebar/frame presentation. No OS/environment-name,
insets, screenshot, X11 property, or native library heuristic is used in production.
Read-only xprop was used only for the investigation.

## Automatic-only selection

Main and inspection windows always use the same Automatic path: reuse inherited
FRAME-style Swing decorations when supported by the current look and feel;
otherwise provide built-in controls. Unknown desktops retain the built-in fallback.
No OS-name, environment-variable or compositor heuristic is involved.

The temporary Window controls setting has been removed from both RuneLite's
configuration and the Model Popout sidebar. The old `windowControlMode` key is
never read or written. Existing `BUILT_IN` and `DESKTOP` values can remain dormant
in a user's profile; they have no effect and need no migration or reset. The enum,
mode argument, change handler and mode-specific state have been removed.

Automatic still does not detect actual external compositor bars. A compositor
forced-decoration rule can override an undecorated request; the plugin cannot
guarantee the absence of such external chrome through supported Java APIs. This
limitation does not justify hiding controls on an unfamiliar desktop.

The existing provider selection, layout revalidation and presentation repaint
behavior are preserved. Windows and inspection sessions are not recreated.

## Rendering invariants

All frames remain undecorated for Java per-pixel transparency. Background/opacity
policy, configuration recovery, Always on Top, model images, model/HUD alpha,
actor identity and independent lifecycle behavior are unchanged. Removing the
custom bar removes its reserved layout space; the model remains clipped to its
content area and cannot cover an inherited root-pane title bar.

The nonopaque root is now also a painting origin. This is necessary because an
inherited title pane is a sibling of Surface, not its child. Opaque title buttons
and title-pane dirty paints must ascend through the same buffered Window.paint.
Shared Swing double buffering is disabled across the entire root hierarchy before
that paint. Surface/WindowPresentation compositing and the single native blit are
preserved. No independent painting mechanism is introduced.

Diagnostics include a per-window identity, inheritedSwingDecorations,
controls=Automatic, swingDecorationStyle and builtInControls. Inherited controls
expect style=1/builtInControls=false; fallback expects style=0/true. These describe
Java state, not compositor state.

## Brief in-game verification

**Windows (physical desktop and VM separately):** Open the main viewer and two
actor inspections. Confirm one bar per window, no Window controls setting in
plugin configuration, and working drag/resize/minimize/taskbar restore/
maximize/restore/close. Close one inspector and confirm RuneLite/other windows
remain open. Test 0/30/50/100% opacity, animation/HUD, Always on Top and movement
between differently scaled monitors. Previously saved Built-in/Desktop preferences
must not change the selected controls after reopening.

**Bazzite/KDE:** Keep existing KDE settings and open only the main viewer first.
Confirm one inherited bar with working controls; then open two inspections at
separate positions and repeat. Check opacity, live player/NPC/HUD/capes, monitor
movement and graphics-configuration recovery with no flicker. Reopen windows and
restart the plugin to verify there is no mode preference to select. If an external
forced-decoration rule causes extra chrome, record it as a compositor override;
there is no automatic external-decoration detector or hidden platform workaround.

**Other Linux/macOS:** Selection is the same Automatic path. Verify controls,
transparency and independent windows; no compatibility claim follows from Linux
results alone. Fullscreen visibility is a separate, researched limitation described
in [FULLSCREEN_ALWAYS_ON_TOP.md](FULLSCREEN_ALWAYS_ON_TOP.md).

## Scope and recovery checkpoint

Published version stays 0.1.0; metadata stays accepted 0.1.2. This is a correction
inside pending 0.1.3. No commit, push, Hub manifest update or publication.
The pre-correction tracked/untracked tree is preserved with verified hashes in
`checkpoints/duplicate-title-before/` (134 files). Earlier checkpoints remain.

## Automated validation

Java 11 / RuneLite 1.13.2 clean build and final complete regression runs succeeded:
208 headless tests passed; eleven native tests are explicitly skipped in that task
and all eleven passed in `desktopPresentationTest` on the available Linux display.
Local cape/HUD fixtures were included. No Windows or macOS runtime test was run.

Current coverage includes automatic selection/fallback, absence of the obsolete
config/sidebar selector and writes on sync, the one-frame legacy duplication,
real RuneLiteLAF and Metal title panes and their button availability, one
application title pane per window, independent inspectors across settings updates,
opaque-sibling repaint routing, and title/viewport alpha at 0/30/50/100%. Existing native close/cleanup, drag, resize,
minimize/restore, maximize/restore and rendering regressions continue to pass.

Build command (with Java 11 selected and fixtures supplied locally):

```sh
./gradlew --offline clean test jar desktopPresentationTest -PruneLiteVersion=1.13.2 \
  -PcapeFixtures=../local-audit/model-popout/cape-fixtures \
  -PhudFixtures=../local-audit/model-popout/hud-fixtures
```

Source/artifact audit passed: 46 production sources, 81 production classes, zero errors. Java 11 bytecode and production-only packaging were checked; the new
native tests and checkpoint are not packaged. `git diff --check` passed. Gradle
continues to report its existing deprecation notice for a future Gradle 9 migration.

The 138-file pre-simplification tree is additionally archived and hash-verified in
`checkpoints/automatic-only-before/`. This simplification and the separate
fullscreen research remain within the same pending development pass 0.1.3.

A final native run exposed an existing test timing race: title dragging was checked
before KWin had consumed the geometry request. NativeWindowControlsTest now waits
for native events between window creation, dragging, resetting bounds, maximize and
restore. The full suite then passed with all 11 native tests; no production drag or
stacking behavior was changed to address the test race. Core WindowStacking,
WindowTransparency, WindowPresentation, ModelRenderer and InspectionPanel sources
were hash-checked unchanged against the pre-simplification checkpoint.

The subsequent targeted cleanup removed the secondary settings sidebar entirely.
Automatic control selection and the painting behavior described here are unchanged.
