> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Window controls — pending development pass 0.1.3

Published Hub version: 0.1.0. Accepted development passes: 0.1.1 inspection,
0.1.2 submenu placement. Metadata records 0.1.2; this pass remains pending acceptance.
No Hub release increment, commit, push, manifest change or publication is included.

Current behavior includes the [duplicate-title-bar correction](DUPLICATE_TITLE_BARS.md)
within this same pending pass. Automatic reuses inherited look-and-feel controls;
built-in controls are the fallback. Automatic selection is unconditional; no mode
setting remains and old saved mode values are ignored.

## Recovery

Before edits, all 130 Git-listed tracked/untracked files were archived with SHA-256
verification. Persistent checkpoint: `checkpoints/window-controls-before/` contains
`workspace.tar.gz`, `sha256.json`, `tracked.patch`, and `status.txt`. It is ignored
by Git and excluded from packaging. A separately verified copy is at
`/tmp/model-popout-recovery-20261015-000623/`. Extract to a separate directory to
review/recover; do not overwrite newer work blindly.

## Root cause and audit

ViewerWindow explicitly disables native decorations. Before this pass there was
no application-supplied title bar or move/minimize/maximize/close handler. Six-pixel
resize edges already existed. The first audit incorrectly attributed the visible
Linux bar to KDE. RuneLite 1.13.2 globally enables Swing look-and-feel decorations
on Linux; an undecorated JFrame can inherit a Swing title bar. Adding a second bar
caused the regression. The correction detects that inherited root-pane style,
reuses it in Automatic mode and falls back to built-in controls when absent.
Actual compositor decorations cannot be reliably detected by supported Java APIs.
InspectionSession already creates ViewerWindow and therefore inherits both the
original defect and the shared correction. Inspection identity, capture, camera,
animation, HUD, scheduler, and independent completed frames are unchanged.

Java 11 requires an undecorated Frame for a background color with alpha below 255.
See [Window.setBackground](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Window.html#setBackground(java.awt.Color)).
Decorating the JFrame would sacrifice the current per-pixel transparency design.

Audit classification:

| Behavior | Classification | Decision |
| --- | --- | --- |
| Assumption that the existing Linux bar was necessarily KDE-provided | Incorrect attribution | Detect inherited Swing root-pane decorations; select one application title bar; retain safe automatic controls without OS heuristics |
| Native background/configuration recovery | B: Java/AWT correction | Preserve capability checks, opaque-to-transparent negotiation, verification and opaque fallback |
| Nonopaque root/layer/content panes, including after background setters | B | Preserve; prevents direct Swing subtree drawing outside Window.paint |
| Top-level Window.paint buffering and full-frame compositing | B | Preserve; includes AWT background clear |
| Surface painting origin, disabled shared buffering, immutable published render images | B | Preserve |
| Resize geometry and Always on Top capability checks | B | Preserve |
| Hover reveal at zero background opacity | General discoverability behavior | Preserve; title controls no longer depend on it |
| KDE/KWin API calls, OS-name switches, obsolete desktop-specific runtime branches | None found | No such code removed; do not mislabel Java corrections as KDE dependencies |

Historical flicker reports retain their original test provenance. They establish
why the cross-platform fixes exist, not compatibility of this new pass.

## Implementation

When using the built-in fallback, WindowTitleBar uses ordinary Swing buttons and a draggable title label. Button
listeners are separate from drag listeners. Double-click toggles maximize/restore.
Dragging a maximized window is disabled until restored. Main title is Model Popout;
inspection titles retain actor identity. Automatic uses the inherited Swing title
bar instead when present. The plugin never installs two application title bars.
External compositor overrides cannot be detected reliably by Java; no platform
heuristic or exposed override setting is used.

Surface omits chrome from its faded UI layer, clips the model pass below chrome,
and paints chrome once at full alpha into its existing private composition buffer.
With the built-in fallback the six-pixel resize frame also stays opaque: fully transparent native pixels may
be click-through on Windows, so invisible alpha-zero resize targets are unreliable.
Viewport opacity remains independent; frame and controls intentionally stay visible.
WindowPresentation still performs the only final native blit. There is no second
repaint manager, timer, global event listener, native library, direct screen draw,
whole-window opacity call, or completed model image mutation.

When using the built-in fallback, maximize uses public AWT bounds and screen insets on the current monitor, retaining
normal bounds separately per window. This geometry-based maximize works without
requiring the desktop to support Frame.MAXIMIZED_BOTH. It does not advertise a
native maximized state or implement OS snap layouts. Minimize uses Frame.ICONIFIED;
restore uses the desktop/taskbar as normal. Close dispatches WINDOW_CLOSING and
DISPOSE_ON_CLOSE, preserving existing windowClosed cleanup without exiting RuneLite.
Always on Top is unchanged. Edge/corner resize remains on the six-pixel surface
border, outside title controls and model-camera handlers.

Geometry uses AWT logical coordinates, screen locations, negative monitor origins
and usable screen insets; it does not multiply by device scaling. Native placement
remains subject to the window manager. Main viewer normal bounds are saved through
RuneLite ConfigManager on close and restored when a useful portion of the title
bar intersects an available monitor. Missing/disconnected monitor positions fall
back to platform placement. Inspection windows remain independently placed; no
per-actor persistent position policy is introduced. Moving a maximized window to
another monitor requires restore, drag, then maximize. Display changes still
reapply the existing transparency policy; verify native peer recreation in-game.

## Windows verification (physical desktop and VM separately)

1. Build with Java 11 and RuneLite 1.13.2. Run `./gradlew clean test jar
   desktopPresentationTest -PruneLiteVersion=1.13.2` on that desktop and retain XML
   reports. Launch RuneLite with the plugin; record Windows/JDK versions, display
   scaling, VM guest driver/3D acceleration, and compositor/transparency capability.
2. Open main Model Popout. Confirm one title bar, Model Popout title, and three
   usable buttons. Drag title area; drag each button while holding it and confirm
   the window does not move. Drag the model and confirm only the camera rotates.
3. Resize each of four edges and four corners; shrink against minimum 480×300
   (inspection 300×300). Confirm opposite edges remain anchored and models never
   cover controls. Double-click title, use maximize/restore button repeatedly,
   confirm monitor taskbar remains usable and original normal bounds return.
4. Minimize, restore from taskbar, and repeat while maximized. Confirm rendering
   resumes. Close using × and the desktop/native close command (Alt+F4); RuneLite
   must remain open. Reopen: main normal position/size should return. Close while
   maximized, reopen, and confirm normal bounds were saved.
5. At 0%, 30%, 50%, 100%, move pointer outside viewport (hover reveal is retained).
   Confirm title/buttons stay solid, desktop shows at supported alpha, model and
   material/texture alpha remain correct, HUD remains correct, and no flashing
   occurs during camera/animation/hitsplats/HP updates. Where translucency is
   unavailable, confirm reported opaque fallback and usable controls.
6. Toggle Always on Top, focus RuneLite/other apps, minimize/restore and repeat.
   Open two player/NPC inspections. Move/resize/maximize/minimize independently;
   close one and confirm main/other inspection continue. Native-close an inspector
   and confirm its capture/rendering cleanup, then reopen it.
7. On monitors with different scaling (e.g. 100/150/200%), drag between monitors
   in both directions, including a monitor left/above primary. Resize/maximize/
   restore on each. Change scaling/resolution and disconnect a monitor; verify
   accessible controls, fallback placement on reopening, transparency recovery,
   and Always on Top. Record any WM-imposed positioning limits.

## Bazzite/KDE verification

1. Record Java, Plasma, X11/XWayland/Wayland session, monitors and scaling. Run the
   same build/native test command and all seven Windows checklist steps on KDE,
   including multiple inspectors, native close and desktop minimize/restore.
2. Repeat the previous live player/NPC + HUD flicker scenarios at 100%, then
   0/30/50/100%. Include animation, capes/textures, target switching and reopening.
   Confirm the new bar is the only application title bar; remove any local KWin
   forced-decoration override only if needed to investigate a genuinely external
   bar; record the rule and restore it after testing. Selection remains Automatic.
3. Move across differently scaled monitors and change display configuration while
   open. Test transparency recovery and opaque fallback. Restore before moving a
   maximized window, then maximize on the destination monitor.
4. If regression occurs, enable `-Dmodelpopout.paintDiagnostics=true` and collect
   background/configuration messages. Draws outside Window.paint must stay zero;
   root/layer/surface must stay nonopaque. The added root painting origin covers inherited title controls. On capable desktops nativeAlpha=0,
   configurationCapable=true and effective opacity matches requested opacity.
   Repeat once without diagnostics to assess visible flicker/performance.

## macOS considerations and limits

Run the same desktop test task and controls/alpha/multiple-window checklist on
macOS. Verify Dock minimize/restore, menu-bar/Dock insets, Retina/non-Retina movement,
Spaces and Always on Top. Custom maximize fills usable monitor bounds; it is not
macOS native fullscreen/Spaces integration. Native snapping, system title menus,
Windows Snap Layout hover UI and desktop decoration themes are not implemented.
Java/desktop capability and window-placement restrictions still apply. No Windows
or macOS runtime result is inferred from Linux or headless tests.

## Plugin Hub review considerations

Production uses Java 11 AWT/Swing only; no native APIs, process execution, reflection,
new network endpoints or global input hooks. Robot is confined to opt-in test code.
The added saved rectangle uses the existing plugin config group. Existing actor
inspection policy and independent rendering remain unchanged. Static audit is not
Hub approval. The checkpoint, fixtures and tests must not enter the production jar.

## Initial implementation results (historical)

Current Automatic-only results: see [DUPLICATE_TITLE_BARS.md](DUPLICATE_TITLE_BARS.md).

Java 11 / RuneLite 1.13.2 clean build and subsequent full suite runs include local
cape and HUD fixtures: 205 headless tests passed, zero failures; eight native tests
are skipped in the headless task and run separately. All eight opt-in native desktop tests
passed on the available Linux display, including real native translucency tests.
This is not in-game Windows, Bazzite visual/flicker, or macOS verification.
Source/artifact audit: 46 production sources, 80 production classes, zero errors;
production bytecode targets Java 11. `git diff --check` passed. Gradle reports
existing deprecated features relevant to future Gradle 9 migration.

The added native all-edge resize test initially failed when it issued multiple
geometry changes in one EDT turn: asynchronous native configure events left stale
component sizes between gestures. It now lets the window manager settle between
independent gestures using test-only Robot.waitForIdle; all eight directions pass.
Production resize behavior was unchanged by that test-harness correction.
