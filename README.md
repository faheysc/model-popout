# Model Popout

Display your live character and interacting NPC in a movable, resizable window with animated models, camera controls, and adjustable transparency.

Enable **Model Popout**, then click its character icon in RuneLite's sidebar to open the window. The separate settings icon controls capture rate, rendering target, NPC rendering, opacity, Always on Top, hitsplats, and HP bars. Settings from the former Live Model Viewer name are retained.

## Controls and appearance

- **Left-click drag:** rotate the viewer camera, with vertical movement inverted to feel like OSRS camera dragging.
- **Mouse wheel:** zoom in or out. Framing stays stable while your character animates.
- **NPC rendering:** shows your current NPC target, or an NPC attacking you. The NPC is mirrored where appropriate so the models face one another. This changes only the displayed image.
- **Window controls:** use your desktop's title bar to move, minimize, maximize, and close the window where available. The window edges also support resizing.
- **Always on Top:** disabled by default. Enable it in the RuneLite plugin configuration or Model Popout settings to keep the popout above other application windows. Changes apply immediately and are saved for reopening. Unsupported desktops may reject or ignore the request; RuneLite's main window is unaffected.
- **Hitsplats and HP bars:** enabled by default, with independent saved switches in RuneLite configuration and the sidebar. Damage, misses, healing, poison, venom, and other available styles appear above the corresponding actor. Bars follow available combat health data; NPC bars show a scaled fraction, never an invented exact HP value.
- **Background opacity:** 100% is solid, 50% is translucent, and 0% shows only the models and enabled combat HUD. Hovering at 0% temporarily reveals the background. Models retain their original transparency at every setting; the combat HUD is independent of background opacity.

Models follow live animations and equipment changes. Actual game textures are used where supported, including animated Fire Cape and Infernal Cape lava. Viewer interactions do not send actions to the game.

## Capture rate and rendering target

**Model capture FPS** controls how often poses are copied from the client, up to 50 captures per second. The client update cycle, activity, and actual pose changes can limit that rate.

**Rendering target** offers 60, 120, 144, 165, 240 FPS and Unlimited (on demand). Camera and texture updates can render between model captures. The most recent pose is held; the viewer does not interpolate skeletal poses or create additional character animation frames.

The sidebar distinguishes captures, unique captured scene poses, completed renders, Swing paints, and texture frames. Swing paints are not monitor refreshes, and none of these measurements is interchangeable with unique character animation FPS.

This is a CPU renderer. Higher targets, animated textures, and displaying an NPC can increase CPU use and allocation pressure. Start at 60 FPS and reduce the capture rate or disable NPC rendering if needed. Unlimited renders only when something changes, but can still consume substantial CPU during animation. Capture stops while the viewer is closed or minimized; texture extrapolation is bounded when client activity slows.

## Desktop compatibility and limitations

The current transparent window style has been verified on Bazzite/KDE. Java per-pixel transparency and native decorations depend on the desktop, display server, Java runtime, and compositor. KDE's native title bar opacity is controlled by KDE, independently of the plugin setting. Unsupported transparency falls back to a solid background.

Other desktops may omit decorations on this window style. Use the operating system's window menu or close shortcut if necessary. Windows, macOS, and other Linux desktops have not been verified; native controls and fully transparent hit testing are not guaranteed there.

The viewer uses an orthographic CPU renderer rather than RuneLite's native scene renderer. Lighting, filtered texture edges, and some overlapping or complex materials can differ. Missing or unsupported textures use the game's fallback face colors. Raster work and texture cache sizes are bounded; unusually complex models may retain the last completed frame.

Hitsplats use native sprite parts and public game-cycle expiration where available. RuneLite's small font is used for their values. Up to four simultaneous hits are tracked per actor; opening the viewer does not recover hits applied while it was closed. Custom health-bar variants, secondary paired hitsplats, native font metrics, hidden-actor flags, and exact native placement are not fully exposed by the APIs used here. Missing art falls back to a colored hitsplat or red/green bar.

## Troubleshooting

- **No model:** log in, keep the viewer visible, and allow a client update. Reopen the window if necessary.
- **No NPC:** interact with an NPC or enter combat, and check that NPC rendering is enabled. Targets clear when they are no longer eligible or despawn.
- **No combat HUD:** enable its independent switches and wait for new combat events. Health bars are hidden when native health information is unavailable or the normal combat bar has expired.
- **Opaque background:** check desktop compositing and Java transparency support. The native title bar may remain opaque.
- **High CPU or uneven movement:** select 60 FPS, reduce capture FPS, and compare the separate performance counters. A high rendering target cannot overcome a slow client pose stream.
- **Rendering artifacts:** report the equipment/NPC, desktop, Java version, capture/render settings, and whether GPU or Animation Smoothing is enabled. The GPU plugin is not required.

[Developer build instructions](docs/DEVELOPMENT.md) · [Rendering notes](docs/RENDERING.md) · [BSD 2-Clause license](LICENSE)
