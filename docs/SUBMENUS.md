> Historical development record. Current release status and accepted revisions are recorded in [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md); final validation is recorded in [RELEASE_0.2.3.md](RELEASE_0.2.3.md). Earlier authorization and pending-state statements below apply to that original pass.

# Existing actor-submenu placement

The owner accepted the initial right-click player inspection and authorized this focused menu update. The accepted working tree was archived outside the repository before editing. Inspector capture/rendering/window/HUD implementations are unchanged in this pass. Changes are limited to menu traversal/placement/event priority, regression tests, integration handoff documentation, and version-source bookkeeping. No commit, push, or Hub update is authorized or performed.

## Placement and preservation

RuneLite 1.13.2's [Interface Styles plugin](https://github.com/runelite/runelite/blob/runelite-parent-1.13.2/runelite-client/src/main/java/net/runelite/client/plugins/interfacestyles/InterfaceStylesPlugin.java#L138) condenses player actions at `MenuOpened` priority 1. Model Popout now runs at priority -1, after this grouping and ordinary priority-0 handlers. It snapshots the existing menu tree through public `MenuEntry.getSubMenu()` and inserts its own entry using that existing `Menu.createMenuEntry()`. It never calls `createSubMenu()`, replaces a parent, recreates native actions, or writes the whole entry array.

Actor identity comes from `MenuEntry.getActor()` and the original actor/world/index binding. A blank condensed actor row can establish ownership, or consistent actor-bearing child entries can identify a group whose parent has no actor. Parent/child identities must agree. Shared/mixed menus and cosmetic color/style submenus are not assumed to belong to an actor just because their displayed names or identifiers look similar. Conflicts retain an outer fallback. Multiple actors, equal displayed names, equal indexes in different worlds, and nested groups remain distinct by API identity.

Only one owned Popout is kept per actor across the tree. Existing actor submenus take precedence over outer placement. If a submenu appears on a later notification, only Model Popout's own fallback entry may migrate into it. Original actions, callback objects, submenu objects, parameters, and relative order remain intact. Disabling recursively removes only owned entries and fences old callbacks; re-enabling adds one fresh entry. Lifecycle validation, copied actor bindings, frozen models, and stale-world callback protections are unchanged.

Because menu arrays are displayed in reverse index order, Popout is inserted at `Report`'s index plus one when present. Thus Follow / Trade / Lookup / Kill Clog keep their order, followed by Popout / Report. With no Report entry, Popout appears below existing submenu actions. Report's action label determines placement only, never actor identity. With no appropriate submenu, the original outer insertion above Cancel is retained. NPC groups use the same identity rules; this plugin does not create NPC grouping when another source has not provided it.

Traversal uses identity-based visited sets, preventing duplicate processing or cycles in shared menu references. No textures, render scratch buffers, windows, or capture scheduling are changed.

## Limitations and verification status

- Actor-less groups without consistent API actor-bearing children cannot be matched safely; use outer fallback instead of parsing player names/levels.
- A group containing conflicting actors is unsuitable; actors use separate appropriate groups or the outer fallback.
- Report placement relies on the standard English action label; alternative custom labels retain bottom-of-submenu placement without reordering anything.
- Interface Styles grouping is verified in current source, and event ordering is covered by tests. A custom plugin that regroups menus at a later/lower priority may require its own compatibility assessment. Model Popout does not rewrite another plugin's grouping or force private state.
- Actual menu sizing, pointer hit regions, and coexistence with installed plugins remain in-game checks.

Java 11 / current Hub RuneLite 1.13.2 is the validation target. On 2026-10-14, `clean build desktopPresentationTest` passed: the complete suite reported 207 tests, 198 passed, nine skipped, and no failures/errors. All six opt-in native tests then passed separately with no skips. The three unexecuted fixture tests require real Fire/Infernal cape and native hitsplat-definition fixtures. Source/artifact audit passed with 45 production sources, 76 production classes, and zero errors. Python version-tooling syntax and diff whitespace checks passed. The generated development JAR is `model-popout-0.1.1.jar`; no publication occurred.

Submenu regressions cover ordering, preserved original callbacks, matching by child identity, overlapping players with identical text, NPC groups/fallback, mixed groups, nested menus, reentrant migration/deduplication, recursive disable, recycled indexes, empty groups, cycles, and event priority. A byte comparison against the accepted working-tree checkpoint found production Java changes only in `InspectionMenu.java` and the main plugin's menu-event priority; all inspector rendering/window/capture/HUD implementations remain unchanged. In-game submenu checks below remain outstanding.

## In-game checklist for this pass

1. Launch the development client with `./gradlew run -PruneLiteVersion=1.13.2`. Enable right-click inspection. Enable Interface Styles' condensed player options; do not enable two Model Popout implementations at once.
2. Right-click a player with Follow, Trade, Lookup, and Report. Open its existing submenu and verify one exact Popout before Report, no new parent/submenu, correct menu dimensions/hit regions, and unchanged other action order.
3. Select Popout and verify the selected player's current model. Repeat for overlapping players of different levels/names, then reopen the same menu repeatedly. Each actor must have one Popout and open its own correct window.
4. Disable condensed player options and verify outer Popout still works. Re-enable grouping and repeat; do not expect Model Popout itself to create grouping.
5. With normal Hub plugins enabled, verify Kill Clog and Hiscores options remain in the appropriate menu and their own clicks still work. The development task uses safe mode by default; use `./gradlew run -PruneLiteVersion=1.13.2 --args='--developer-mode --debug'` for this coexistence check. Popout must not trigger any lookup in this stage.
6. Inspect an ordinary NPC. If its existing menu provider groups NPCs, verify Popout inside that actor's group, preserving Talk-to/Examine/custom actions. Otherwise verify one working outer Popout. Do not put Popout inside Tag color/Tag style menus. Repeat with overlapping NPCs.
7. Disable inspection with a menu already open, then reopen it: only Popout disappears from both outer/submenus. Follow, Trade, Lookup, Kill Clog, Report, and NPC actions remain. Existing inspectors remain usable. Re-enable and confirm no duplicate entries.
8. Open multiple inspectors alongside the main window; rotate/zoom independently. Verify equipment/animation updates, NPC transformations, opacity 100% → 50% → 0% → 100%, Always on Top, HUD, minimize/restore, and native controls have no regressions.
9. Let a selected actor leave/despawn, or hop/logout. Verify the inspector retains its captured model, clears HUD, remains rotatable, and never switches to a reused slot. Try an actor disappearing before a submenu Popout click; expect the established unavailable/no-capture behavior.
10. Disable/re-enable the plugin with several inspectors open. All close; a fresh submenu Popout opens normally. Check client/main-view responsiveness with several windows.

Record the grouping provider/settings and reproduction steps for menu failures. See [KILL_CLOG_INTEGRATION.md](KILL_CLOG_INTEGRATION.md) for the receiver handoff and [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md) for accepted/pending revisions. The submenu pass remains pending owner verification.
