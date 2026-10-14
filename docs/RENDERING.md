# Rendering invariants

Model Popout is a passive model observer. All player/NPC/model/texture API reads occur on RuneLite's client thread. Snapshot arrays are owned copies; malformed or incomplete native arrays are rejected before publication. Captured geometry is compared by contents, not model identity. RGB conversion, projection, UV calculation, rasterization, and texture-only rendering run on one bounded executor.

The EDT owns camera/view state, window components, scheduling timers, and frame publication. There is one active rendering job and one replaceable completed frame. Closing, logout, NPC identity/visibility changes, and shutdown invalidate stale results. The worker slot is retained until the actual work finishes, including across close/reopen. Timers do not enqueue catch-up bursts, and rendering targets never change RuneLite's game cycle or draw callbacks.

## Verified corrections to preserve

- **Flicker:** `WindowPresentation` buffers above `Window.paint`, including AWT's transparent-window background clear. `ViewerWindow.Surface` separately composites the UI at configured alpha and draws the completed model image afterward. Do not remove either compositing boundary or use `JFrame.setOpacity()`.
- **Cape speckles:** both `ModelRenderer` and `TextureRasterPlan` reject non-front-facing triangles before depth testing or caching. Opposing red reverse faces of the Fire Cape occupy the same physical surface as its lava faces. Drawing both windings caused floating-point depth competition. Native software winding rejection removed the defect without changing material data or animation.
- **Stable framing:** `ModelFrame.initial` is established once per player window session and once per NPC identity. Animation bounds do not repeatedly refit the camera.
- **NPC lifetime:** selection uses object identity, world identity, and monotonic selection keys. An explicit target wins; eligible attackers and a selected death animation may remain visible until they become ineligible or despawn. Removed targets invalidate old rendered scenes.
- **Model opacity:** alpha belongs to original faces/texture cutouts. Background/UI opacity never scales the model image. NPC mirroring is applied only when painting the completed scene.

## Texture and cache behavior

Only referenced, visible face textures are copied, with at most 32 materials per capture and power-of-two dimensions up to 256. Software-provider pixels can already be scrolled; each owned material records its source cycle to avoid applying that movement twice. Fractional texture motion advances no more than one client cycle ahead. GPU texture metadata can be sampled through public APIs without taking over GPU rendering.

A pose retains one RGB conversion, one projection/UV cache, and one completed image. Texture-only updates can reuse a plan with at most 65,536 fragments. Moving cutouts or face transparency use the general raster path where needed to preserve blending. Raster work is limited to 16 million bounding-box samples. These limits bound caches and rendering work, not the memory required by an arbitrarily large desktop window.

Published `BufferedImage`s are not reused as writable work buffers. Window-sized compositing buffers are EDT-owned and replaced on resize. The three UI/native buffers use approximately `12 * width * height` bytes for pixel storage, in addition to actor frames, snapshots, caches, and temporary raster depth arrays. Large windows and high targets increase allocation and paint costs.

No skeletal interpolation is implemented: posed geometry does not expose a reliable complete animation/skeleton state through the supported APIs used here. Unique captured scene poses, completed renders, Swing paints, and texture frames are separate counters. OS-generated repaints can repaint a fixed completed image, so Swing paints are not monitor refresh or unique animation frames.

Lighting/ordering are approximations of native rendering, with orthographic projection and affine UV interpolation appropriate to that projection. There is no native scene renderer, GPU shader replacement, model mutation, hidden widget, canvas extraction, or game-input path.

## Window stacking

Always on Top is stored under the existing configuration group, defaults to false, and is applied on the EDT to the popout only. Each open reads the saved value; configuration events apply it immediately. `WindowStacking` checks Java support when enabling and contains unsupported/security failures. Disabling still attempts normal stacking. The setting does not alter window presentation, alpha, frame publication, camera state, or any RuneLite main-window property. Native compositor/focus behavior needs desktop verification beyond Java property tests.

## Combat HUD

The HUD adds no rendering thread or 3D render request. Client-thread hitsplat events build one small owned image per accepted event and retain at most four slots per actor. Health and game-cycle expiration are sampled independently of geometry capture. A mailbox holds one replaceable completed HUD snapshot with at most one queued/running EDT delivery; idle snapshots are reused. HUD changes repaint the existing model layer above the completed 3D frame. The top-level compositor and software raster paths are unchanged.

Actor/selection keys are compared with the identities of the displayed completed frame. Target changes and missing NPC frames suppress mismatched HUD; close/logout/shutdown release hits and actor references. The HUD uses the completed frame's camera and its stable initial head anchor, so in-flight camera requests cannot move the HUD ahead of the model. Window sizing reprojects that anchor without recalculating model framing. NPC mirroring moves the anchor only; text/art stays readable. Background/UI alpha never scales HUD output; native intrinsic sprite alpha/fade is retained.

There are at most 64 decoded definitions, eight transform-resolution steps, 4 KiB per accepted raw definition, and 256×128 pixels per hit image. Public SpriteManager owns its shared bounded sprite cache. Up to eight retained hit images use at most approximately 1 MiB of pixels; normal native hits are much smaller. Active hits produce small snapshots at client-cycle frequency and can repaint the window, so Swing paint counts also include HUD activity. These are not extra completed 3D frames or unique animation poses. No desktop CPU/GC improvement is claimed. Timing follows native game cycles, including reduced activity; it does not extrapolate combat events with wall-clock time.

See [HUD.md](HUD.md) for supported native art, fallback behavior, API limits, and compliance review needs.

## HUD repaint routing

Model-panel invalidations are promoted to the whole owned compositing surface, matching the established completed-model publication path. A child dirty rectangle must not introduce a second clipped native update path for HUD delivery, expiration, or settings changes. This changes invalidation scope only: frame pacing, render frequency, private buffers, alpha layers, and HUD game-cycle timing remain unchanged. Optional read-only diagnostics use the existing metrics timer; see [HUD_REPAINT_REGRESSION.md](HUD_REPAINT_REGRESSION.md). Native desktop verification remains required.
