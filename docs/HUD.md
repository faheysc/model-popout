# Combat HUD implementation and limits

Show Hitsplats and Show HP Bars are independent RuneLite booleans, both default true. They use the retained configuration group and apply without reopening. Always on Top remains a separate default-false viewer-only setting.

## Public API and native assets

- [`HitsplatApplied`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/events/HitsplatApplied.java) supplies the actor and public hitsplat. It fires even when the native four slots cannot display a new hit. The tracker therefore retains four slots per actor and follows available replacement metadata instead of showing every event indiscriminately.
- [`Hitsplat`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Hitsplat.java) supplies raw type, amount, and disappearance game cycle. Zero is a displayed miss, not a missing value. Expiration uses game-cycle subtraction and does not depend on new geometry. A style's duration controls delayed start, fade, and drift within that lifetime.
- Public `Client.getIndexConfig()` / [`IndexDataBase.loadData`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/IndexDataBase.java) expose config archive 32. The bounded decoder follows the native format documented by RuneLite's [`HitSplatLoader`](https://github.com/runelite/runelite/blob/master/cache/src/main/java/net/runelite/cache/definitions/loaders/HitSplatLoader.java). Its reference-derived BSD notice is retained in packaged resources. Public varbit/varp values select transformed styles.
- [`SpriteManager.getSprite`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/SpriteManager.java) obtains the actual sprite parts on the client thread. Parts are assembled once per event with definition colors and RuneLite's small font. No game art is bundled, downloaded from another service, or copied from screenshots.
- [`Actor.getHealthRatio/getHealthScale`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Actor.java) determine native combat-bar visibility and a scaled fraction. Missing/invalid values hide the bar. Only the local player's visible bar can use public real/boosted Hitpoints; NPC exact HP is never calculated. Default native 30-pixel front/back sprites are used where available.

Definition decoding rejects unknown opcodes, truncated input, oversized data, and excessive transformation chains. Missing art/data has a colored fallback, rather than a claim of native visual identity. The schema can change between game versions; there is no supported typed definition/actor-slot API used by this implementation.

## Ownership and lifecycle

HUD packets contain immutable values/lists and complete images, not live Actor/Model references. Actor identity and monotonic NPC selection keys prevent transfer on switch, death/despawn removal, or slot reuse. Displayed model identity/camera/frame must match before drawing. Four hit slots belong to each actor separately. Published images are not overwritten.

Updates use the existing client event loop and one replaceable EDT mailbox, not a new render worker or unbounded queue. Expiration and health repaint the independent HUD over the latest completed model image without rebuilding/rasterizing a model. Generation-checked atomic updates prevent an in-flight event from reviving cleared references. Closing, logout, and shutdown clear hits and keys; reopening collects new events. Restoring from minimization refreshes native timing before redisplaying retained hits. An idle HUD reuses its snapshot.

HUD painting uses the existing full-opacity model layer after 3D painting; it cannot change the background alpha, model geometry, raster plan, animation clock, or native top-level clear behavior. It is 2D and does not receive perspective occlusion or lighting from 3D faces.

## Fidelity limits and verification

Native sprites and primary amounts/types are supported. The event does not expose the complete native primary/secondary paired-hit record or actor's visible-slot array; exact parity for paired/rare replacement sequences is not guaranteed. Native game-font IDs are decoded but RuneLite's public small font supplies text, so metrics/placement can differ. Actor-specific custom/boss bar variants, interpolation/fade rules, and hidden actor rendering flags are not fully exposed through the APIs used here. A stable initial model-space head anchor avoids animated-bounds jitter but is not a native skeletal attachment. Large art/models and extreme zoom may clip or overlap near viewport edges.

The automatic tests use synthetic packets/pixels plus optional locally extracted native definitions for miss, damage, max damage, poison, venom, healing, and burn. These are data/logic/painting tests, not live visual verification. Follow IN_GAME_CHECKLIST.md in safe mode with the software renderer first.

## Submission policy

API legality and visual fidelity are separate from feature approval. The standalone combat display, target selection, and live NPC/boss animation may provide additional information outside the native view. This requires explicit Plugin Hub assessment under the sourced Jagex and RuneLite rules in SUBMISSION.md. No encounter prediction, cumulative damage tracking, NPC exact HP inference, combat instruction, other-player scouting, or game input is implemented. That scope does not guarantee approval; no feature has been removed or redesigned to preempt review.
