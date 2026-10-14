# Final in-game regression checklist

Use the renamed development plugin on Java 11. First test RuneLite safe mode with GPU disabled, matching the verified environment. Record desktop/display server, Java version, capture rate, and render target. Repeat with other plugin combinations only if used normally.

- [ ] Existing capture FPS, render target, NPC setting, opacity, Always on Top, Show Hitsplats, and Show HP Bars survive the rename and a full client restart.
- [ ] Model Popout appears under the new name; both sidebar icons work; the window has no duplicate controls or obsolete buttons/instructions.
- [ ] Player idle, walking, woodcutting, combat, and equipment changes animate correctly.
- [ ] An explicit NPC target takes precedence. Attacker fallback, target switches, transformations, death animations, despawns, and reused NPC slots behave correctly.
- [ ] NPC mirroring remains visual-only and both actors animate normally.
- [ ] Left-drag horizontal rotation, inverted vertical rotation/pitch limits, and mouse-wheel zoom work.
- [ ] Framing stays stable during woodcutting and other large animation movements; camera zoom does not reset between poses.
- [ ] Fire Cape and Infernal Cape retain correct geometry, lava colors, animation speed/direction, trim, and alpha. Orbit front/back/sides and pitch extremes; check for red speckles or missing faces.
- [ ] Texture scrolling continues with unchanged geometry and through UV wrap boundaries, then slows/stops appropriately with reduced client activity.
- [ ] Test 60/120/144/165/240 and Unlimited targets. Record captures, unique scene poses, completed renders, Swing paints, texture frames, and CPU/memory separately; do not equate them.
- [ ] Always on Top defaults off. Enable it while the viewer is open, focus another application, and confirm the popout stays above it. Disable it and verify normal stacking. Close/reopen and restart RuneLite to check persistence; RuneLite's main window must remain unaffected. Check unsupported-desktop handling where available.
- [ ] Moving, edge resizing, minimize/restore, maximize/restore, and closing work through native desktop controls where available.
- [ ] At 0%, 50%, and 100% opacity, models preserve original alpha. Hover behavior at 0% is usable. Native title-bar opacity remains a desktop setting.
- [ ] Show Hitsplats and Show HP Bars default on. Toggle each independently while the window is open, confirm immediate response, and verify saved values after closing/reopening and restarting RuneLite.
- [ ] Compare player/NPC normal damage, blue zero hits, healing (where a native hitsplat is emitted), poison, venom, maximum hits, and other special styles against the native software client. Check values, colors, game-cycle duration, fade/drift, and up to four simultaneous hits.
- [ ] Player damage/healing updates the bar; boosted Hitpoints do not overflow it. NPC bars use the native scaled fraction. Unknown/expired health hides the bar; zero health leaves an empty bar during death until removal.
- [ ] Switch NPC targets rapidly, kill/despawn an NPC, and encounter a reused NPC slot. Old hits/health must never appear on the new NPC or on an empty slot. Reopening begins with new events, not old combat hits.
- [ ] Rotate, zoom, resize, and mirror the NPC during combat: HUD remains above the correct actor, text stays readable, and animation bounds do not make it jump. Check very tall NPCs and zoom/pitch limits for clipping or overlap.
- [ ] At 0%, 50%, and 100% background opacity, hitsplats/HP bars preserve their own visual alpha, while capes and models are unchanged. Expired HUD must clear without flicker or lingering pixels.
- [ ] Minimize during combat then restore; expired hits must not flash. Logout/login and close/reopen must clear previous-player HUD and preserve checkbox settings.
- [ ] Verify the HUD repaint correction during both idle and combat using [HUD_REPAINT_REGRESSION.md](HUD_REPAINT_REGRESSION.md). If flicker remains, gather its optional paint trace rather than assuming alpha or reducing FPS.
- [ ] No flicker, incomplete frame, doubled UI, stale NPC, or resize artifact appears during sustained animation.
- [ ] Minimize/unfocus RuneLite and the viewer, restore them, and confirm bounded work, correct textures, and responsive controls.
- [ ] Logout/login, world changes, repeated close/reopen, plugin disable/enable, and rapid toggling while rendering do not leave windows, duplicate sidebar entries, stale frames, or running capture loops.
- [ ] Plugin shutdown releases the window and renderer; disabling the plugin does not affect RuneLite timing or other rendering plugins.

Headless tests do not replace these desktop checks. Submit only after the final renamed build passes this checklist and the outstanding build/policy review items in SUBMISSION.md are resolved.
