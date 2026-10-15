# Model Popout

Display your live character and interacting NPC in a movable, resizable window with animated models, camera controls, and adjustable transparency.

Enable **Model Popout**, then click its character icon in RuneLite's sidebar to open the window. Use RuneLite's standard Model Popout plugin configuration for capture rate, rendering target, NPC rendering, opacity, Always on Top, hitsplats, HP bars, and right-click inspection. Settings from the former Live Model Viewer name are retained.

## Controls and appearance

- **Left-click drag:** rotate the viewer camera, with vertical movement inverted to feel like OSRS camera dragging.
- **Mouse wheel:** zoom in or out. Framing stays stable while your character animates.
- **NPC rendering:** shows your current NPC target, or an NPC attacking you. The NPC is mirrored where appropriate so the models face one another. This changes only the displayed image.
- **Window controls:** automatic controls reuse RuneLite's inherited Swing title bar when available, with built-in controls as fallback. Drag, minimize, maximize/restore, and close each window independently; resize using its edges. The main viewer remembers reachable window bounds.
- **Always on Top:** disabled by default. Enable it in the RuneLite plugin configuration to keep the popout above other application windows. Changes apply immediately and are saved for reopening. Unsupported desktops may reject or ignore the request; RuneLite's main window is unaffected.
- **Hitsplats and HP bars:** enabled by default, with independent saved switches in RuneLite configuration. Damage, misses, healing, poison, venom, and other available styles appear above the corresponding actor. Bars follow available combat health data; NPC bars show a scaled fraction, never an invented exact HP value.
- **Background opacity:** 100% is solid, 50% is translucent, and 0% shows only the models and enabled combat HUD. Hovering at 0% temporarily reveals the background. Models retain their original transparency at every setting; the combat HUD is independent of background opacity.

Models follow live animations and equipment changes. Actual game textures are used where supported, including animated Fire Cape and Infernal Cape lava. Viewer interactions do not send actions to the game.

## Right-click inspection

**Enable Right-Click Inspection** defaults to enabled. Right-click a scene player or NPC and select **Popout** to open an independent model window. Popout appears inside an existing actor submenu when its API identity is unambiguous, before Report when present; otherwise it remains in the outer menu. No new submenu is created. No combat or interaction is required. Each selection opens a new window with its own camera and position; the main Model Popout window can stay open or closed independently. Existing lookup and game menu actions are preserved.

Inspectors follow the selected actor's live model, equipment, textures, animations, observed hitsplats, and available scaled health data. Background opacity, Always on Top, capture/render targets, and HUD switches use the existing shared plugin settings. Disabling right-click inspection removes the menu entries but keeps already open inspectors.

When an actor despawns, leaves the scene, or becomes unavailable during logout/world hopping, the inspector retains its last captured model and textures. Rotation and zoom continue to work; live combat HUD clears. It never follows a replacement actor occupying the same index. If the actor vanished before any capture, the window explicitly reports that no model was captured. Open a fresh Popout to inspect an actor again after disappearance.

Multiple inspectors share a separate bounded rendering worker and fair capture scheduling. Minimized inspectors pause model/HUD updates. Large windows and several animated actors still increase CPU and memory use; capture and render targets are not guaranteed rates under load. Kill Clog and Hiscores integration is not active and no associated lookup setting is exposed in this stage.

Release candidate **0.2.3** includes accepted development revisions through **0.1.3**. See the [changelog](docs/CHANGES.md) and [development history](docs/DEVELOPMENT_HISTORY.md). Windows VM verification of the final corrected build is required before Plugin Hub submission.

## Capture rate and rendering target

**Model capture FPS** controls how often poses are copied from the client, up to 50 captures per second. The client update cycle, activity, and actual pose changes can limit that rate.

**Rendering target** offers 60, 120, 144, 165, 240 FPS and Unlimited (on demand). Camera and texture updates can render between model captures. The most recent pose is held; the viewer does not interpolate skeletal poses or create additional character animation frames.

Capture and rendering rates remain independent. Opt-in developer paint diagnostics remain available; the secondary settings sidebar and its performance labels have been removed.

This is a CPU renderer. Higher targets, animated textures, and displaying an NPC can increase CPU use and allocation pressure. Start at 60 FPS and reduce the capture rate or disable NPC rendering if needed. Unlimited renders only when something changes, but can still consume substantial CPU during animation. Capture stops while the viewer is closed or minimized; texture extrapolation is bounded when client activity slows.

## Desktop compatibility and limitations

Main and inspection windows select window controls automatically: inherited RuneLite Swing decorations when available, otherwise compact built-in controls. No window-control setting is exposed; previously saved mode preferences are ignored. Built-in controls and resize targets remain visible at zero background opacity. Transparency depends on Java/compositor capabilities and falls back to an opaque viewport when unavailable. Automated native checks cover the available Linux desktop; Windows VM verification of the final corrected build remains a submission requirement. See [the title-bar investigation and verification](docs/DUPLICATE_TITLE_BARS.md) and [fullscreen Always on Top research](docs/FULLSCREEN_ALWAYS_ON_TOP.md).

Other desktops may omit decorations on this window style. Use the operating system's window menu or close shortcut if necessary. Windows, macOS, and other Linux desktops have not been verified; native controls and fully transparent hit testing are not guaranteed there.

The viewer uses an orthographic CPU renderer rather than RuneLite's native scene renderer. Lighting, filtered texture edges, and some overlapping or complex materials can differ. Missing or unsupported textures use the game's fallback face colors. Raster work and texture cache sizes are bounded; unusually complex models may retain the last completed frame.

Hitsplats use native sprite parts and public game-cycle expiration where available. RuneLite's small font is used for their values. Up to four simultaneous hits are tracked per actor; opening the viewer does not recover hits applied while it was closed. Custom health-bar variants, secondary paired hitsplats, native font metrics, hidden-actor flags, and exact native placement are not fully exposed by the APIs used here. Missing art falls back to a colored hitsplat or red/green bar.

## Troubleshooting

- **No model:** log in, keep the viewer visible, and allow a client update. Reopen the window if necessary.
- **No NPC:** interact with an NPC or enter combat, and check that NPC rendering is enabled. Targets clear when they are no longer eligible or despawn.
- **No combat HUD:** enable its independent switches and wait for new combat events. Health bars are hidden when native health information is unavailable or the normal combat bar has expired.
- **Opaque background:** check desktop compositing and Java transparency support. The custom title bar intentionally remains opaque.
- **High CPU or uneven movement:** select 60 FPS and reduce capture FPS. A high rendering target cannot overcome a slow client pose stream.
- **Rendering artifacts:** report the equipment/NPC, desktop, Java version, capture/render settings, and whether GPU or Animation Smoothing is enabled. The GPU plugin is not required.

[Developer build instructions](docs/DEVELOPMENT.md) · [Rendering notes](docs/RENDERING.md) · [BSD 2-Clause license](LICENSE)
