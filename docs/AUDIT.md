# Final code audit

Audit date: 2026-10-13. Scope: every production Java file, all source/build/metadata/docs, regression tests, generated artifact contents, configuration compatibility, thread ownership, lifecycle, and current official submission rules. The owner's separately authorized additions are Always on Top and the player/NPC combat HUD; no other feature was added.

## Recovery and baseline

The entire pre-audit implementation, including generated files, cached research fixtures, and local project state, was archived and verified file by file before editing. The archive and SHA-256 manifest are outside this project. Baseline: 99 passing tests and 2 skipped native desktop tests, Java 11 / RuneLite 1.13.1. Source/artifact hashes and cached validation dependency hashes were recorded beside that checkpoint. Previous verified backups were not modified.

A second verified checkpoint preserved the renamed state before adding Always on Top. A verified complete checkpoint also preserved the audited/stacking-enabled state before HUD changes. Final source/artifact checkpoints are kept outside the public tree. Local research and raw cache assets were moved outside this project, rather than discarded.

## Corrections and cleanup

Confirmed issues corrected:

- Required/optional model arrays shorter than their declared counts were silently zero-padded by `Arrays.copyOf`. That could invent vertices, face colors, or texture assignments. Capture now rejects missing/incomplete required arrays and short optional arrays, while allowing spare capacity. Geometry/raster calculations are unchanged for valid data.
- A delayed startup-sidebar callback checked only `running`, so an old callback could create UI after a stop/restart had set that flag true again. A monotonic startup generation now rejects that obsolete callback. Existing capture/render lifecycle tickets and worker backpressure are preserved.
- The synthetic texture benchmark still used the winding rejected by the verified cape correction. Its test-only triangles now have valid native winding, so it performs nonempty raster work.

Focused defensive/maintenance changes:

- Teardown tolerates an absent model panel.
- Removed unused imports, a redundant texture-ID cast, unused image-cache convenience methods, and unused texture capture timestamps/arguments. Retained the actual origin-cycle clock and all frame caches.
- Replaced reflection-driven private snapshot reconstruction in optional asset tests with explicit JSON parsing/public model test doubles. Offline obfuscated-client extraction tools are outside the public project.
- Consolidated obsolete investigations into current rendering/development/submission notes. Kept useful synthetic regressions, optional real-asset regression support, benchmark, and the renamed IntelliJ launcher.
- Made offline validation portable, narrowed its cached dependency classpath, and made fallback JAR output deterministic. The Gradle archive configuration also requests stable ordering/timestamps; full Gradle execution remains an external gate.

Always on Top defaults off, persists through the unchanged RuneLite configuration group, is applied when opening and on config changes, and uses only the popout's Java Window API on the EDT. Unsupported/security failures are contained. No compositor or rendering calculation was changed for this feature.

## File-by-file production review

| File | Reviewed behavior and disposition |
| --- | --- |
| `ModelPopoutPlugin` | Client-thread extraction; EDT publication; bounded single-worker queue; config events; close/logout/shutdown/reopen; stale tickets. Generation guard and null-safe cleanup added; renamed metadata; viewer-only stacking wiring; HUD events, native-cycle sampling, completed-frame identity/anchor handoff, and teardown-generation rejection. |
| `ModelPopoutConfig` | Existing group/keys/defaults retained; display metadata renamed; persisted Always on Top boolean added, default false; independent hitsplat/HP bar booleans default true. |
| `LiveCaptureGate` | Short synchronized ticket/throttle operations; monotonic deadlines; no queued catch-up. Unchanged. |
| `LiveViewState` | One active request; retained slot until actual completion; invalidation across close/logout/NPC removal. Unchanged. |
| `ModelSnapshot` | Owned arrays; index/nonfinite validation; deferred RGB conversion; bounded pose caches. Added shape validation, removed unused cache overloads; color math unchanged. |
| `ModelFrame` | One reference frame plus motion headroom; no per-animation refit. Unchanged. |
| `ModelProjection` | Camera projection and UV cache per pose/view. Unchanged. |
| `ModelRenderer` | Completed private ARGB output; work limit; face-alpha/depth handling; verified native winding rejection. Unchanged except package. |
| `TextureRasterPlan` | Bounded fragments, priority/depth ties, static base, moving-cutout fallback, native winding rejection. Unchanged except package. |
| `TextureCoordinates` | Explicit basis validation/degenerate fallback; orthographic planar UV solve. Unchanged. |
| `FaceOrdering` | Depth sorting and native-style priorities 0–11. Unchanged. |
| `TextureSet` | Client-thread pixel copies, 32-material cap, dimensions/metadata guards, reused owned pixels, software origin cycles. Removed unused timestamp and cast; sampling/animation math unchanged. |
| `TextureClock` | At most one fractional client cycle ahead; no runaway motion when updates stop. Unchanged. |
| `PixelBlend` | Intrinsic alpha and lighting channel bounds. Unchanged. |
| `BackgroundOpacity` | UI/background alpha only; opaque desktop fallback. Unchanged. |
| `CameraView` | Finite values; existing yaw/pitch/zoom limits and inverted vertical drag. Unchanged. |
| `ModelScenePainter` | Completed-image scaling and NPC mirror only; source pixels unmodified. Unchanged. |
| `NpcTargetTracker` | Client-thread reads under synchronized selection; one seed scan; identity/world/serial guards; despawn/death cleanup. Added a synchronized current-selection accessor; selection policy unchanged. |
| `NpcSelector` | Explicit target precedence; eligible attacker/death fallback. Unchanged. |
| `FramePacer` | Monotonic independent render/presentation deadlines; bounded timer wakeups. Unchanged. |
| `RenderTarget` | Existing targets including on-demand Unlimited. Unchanged. |
| `ViewerPerformance` | Separate captures, changed scene poses, renders, paints, texture frames. Unchanged. |
| `ViewerSettingsPanel` | EDT UI/config sync; no feedback saves during loading; renamed labels and saved stacking/HUD checkboxes. |
| `ViewerWindow` | Per-pixel UI/model layers; native fallback; edge resizing; buffered top-level paint. Rendering unchanged; obsolete hover comment corrected; stacking wrapper added. |
| `WindowPresentation` | EDT native Window.paint boundary buffer prevents exposed AWT clearing. Unchanged. |
| `WindowGeometry` | Resize anchors, edges/cursors/minimum bounds. Unchanged. |
| `WindowStacking` | New focused desktop capability/exception policy; delegates only to the supplied viewer setter. |
| `HudHealth` | Native visibility gate; clamped scaled NPC fractions; actual own Hitpoints only where available. |
| `HitsplatStyle` | Bounded public-archive decoding; native color/parts/timing/transforms; malformed fallback through asset policy; BSD reference notice retained. |
| `HudAssets` | Client-thread definitions/vars/SpriteManager calls; bounded 64-style cache and eight-step resolution; complete event images; native default bar art with retry limit. |
| `HudSnapshot` | Immutable values/lists; complete owned hit images; exact expiry and delayed start; native-cycle drift/fade. No live actors cross publication. |
| `HudTracker` | Actor identities and selection keys; four slots each; expiration/replacement; idle snapshot reuse; synchronized teardown generation prevents old events reviving state. |
| `LatestHudMailbox` | One latest packet, one queued/running EDT callback; racing offers and close/drop covered. |
| `HudAnchor` | Initial model-space head location projected with the completed camera; animation bounds never refit it; viewport constraints. |
| `HudPainter` | Separate readable 2D HUD above 3D, intrinsic sprite alpha, native bar clipping; identity/flags prevent stale target display; background opacity independent. |


Before HUD integration, for the original 26 classes, 20 matched the pre-audit source after package/display-name normalization. Later Always on Top changes are isolated to config, sidebar, plugin/window wiring, and the new stacking helper. HUD integration changes only those three wiring files and a current-selection accessor in the existing NPC tracker. The 3D renderer, cached raster path, texture clock, camera calculations, WindowPresentation, and ViewerWindow are byte-identical to the pre-HUD checkpoint. No broad rendering refactor was performed.

HUD-specific corrections made during review: stale completed NPC frames clear their HUD anchor; minimization restore refreshes native-cycle data before retained hits reappear; an in-flight event or model capture cannot revive HUD references after teardown. Unavailable native art now uses the raw hitsplat type's fallback plate color, not the native white text color, preserving miss/damage/poison/healing distinctions. Public sprite identifiers use the generated supported API rather than deprecated constants. Regression tests cover these data/lifecycle boundaries and independent settings. Exact live appearance remains a desktop gate.

## Thread, resource, and performance findings

Client calls are public getter/model/texture observation calls; no game input, model mutation, rendering callback takeover, or game-clock update occurs. ClientToolbar's supported add/remove methods already marshal to the EDT. Snapshot publication uses executor/EDT handoff ordering, and published images are never overwritten as work buffers. Swing painting and window mutations stay on the EDT.

At most one real rendering job is active, with one replaceable completed frame. Obsolete lifecycle/scene results are discarded. Queue size is bounded, timers are nonrepeating, and deadlines skip missed work rather than accumulating a backlog. Renderer shutdown is graceful so an active job can release its slot; its result is invalidated, not presented. NPC references, texture references, and image/view state are released on close/logout/shutdown. These conclusions are source/test findings, not a measured desktop stress result.

No material rendering-performance change was attempted. An illustrative synthetic benchmark after correcting its fixture measured 2.350 ms/general frame versus 0.527 ms/cached texture frame, 150 frames each and 3,403 cached fragments. This compares existing paths, not before/after audit performance, real cape/player FPS, monitor presentation, or CPU use at a particular target. Target-specific CPU/memory measurement remains in the desktop checklist.

Caches/work are bounded as documented in RENDERING.md. Window buffers still scale with window area and fresh completed frames still allocate pixel storage; high targets and large windows can create GC pressure. This known tradeoff was preserved to avoid rewriting verified frame publication. No unbounded queue or additional render thread was introduced. HUD snapshots add bounded client-cycle/EDT work during combat without requesting 3D frames; at most four hits per actor and 64 decoded styles are retained. At the maximum allowed hit image size, eight retained images use about 1 MiB; typical sprites are smaller. Native sprites come from the existing SpriteManager cache. No measured real desktop overhead is claimed.

## Validation and limits

The final full suite compiled/ran under Java 11 with the optional actual Fire/Infernal and hitsplat-definition fixture data enabled: **140 passed, 0 failed, 3 native desktop tests skipped**. The default run without private assets passes 137 tests and skips 6. Two independent direct builds produced identical JAR hashes; see CHANGES.md. Regression coverage includes malformed snapshot arrays, legacy config values, zero/partial texels, cached/general path parity, cape front/back visibility, animation clocks, camera framing/zoom, NPC changes/despawns/mirroring, completed-frame immutability, resize geometry, and compositing. Stacking tests cover immediate updates, persistence/recreated targets, unsupported/security behavior, and an opt-in native window lifecycle test. HUD tests cover actual local native definitions, raw values including zero/healing/poison, timing/fade/slots, unknown/scaled health, NPC lifecycle/slot reuse, stale capture generations, bounded publication, viewport/camera anchors, readable mirroring, preserved completed images, and alpha independence.

Production source/bytecode, packaged class names/Java level/licenses, metadata, public files, legacy config identifier, personal paths, and prohibited API/dependency patterns are checked by `python3 tools/audit.py`. These static checks are deliberately not represented as official Hub review.

Gradle wrapper bytes/version/checksum match the official template. Full Gradle execution is blocked here by sandbox cache/socket restrictions; Plugin Hub CI has not run. Deterministic direct-build reproducibility is checked separately. Native desktop tests remain skipped in headless validation. The final renamed/stacking/HUD-enabled build needs the complete in-game checklist, including rapid plugin toggling and actual compositor stacking under focus changes.

See SUBMISSION.md for sourced compliance findings, potential blockers, metadata, and exact owner-approved publication/submission steps. No repository, commit, push, or submission was created.

## Subsequent HUD repaint regression review

The owner's desktop report invalidated any assumption that the final headless build established stable native appearance. Comparing the verified checkpoints confirmed that the new HUD used child-panel invalidations rather than the whole-surface path of model frames. A focused `ModelPanel.repaint` override now routes requests to its owned compositing surface. Two tests failed before the correction and pass afterward; lifecycle/detached routing is also covered. No FPS, rasterizer, alpha, timing, queue, or compositor behavior was changed. Optional paint diagnostics add only counters/read-only flag inspection on the existing metrics timer. See [HUD_REPAINT_REGRESSION.md](HUD_REPAINT_REGRESSION.md) for evidence, limitations, and desktop validation. The source-level routing defect is confirmed; the full cause of continuous native flicker still requires desktop evidence.
