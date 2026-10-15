# Kill Clog integration handoff for 420KC

## Readiness and source findings

This is a proposed receiver change, not a modification of Kill Clog's repository. Model Popout currently sends no lookup messages and exposes no associated-lookup setting. End-to-end integration requires 420KC to implement and publish a compatible receiver, followed by a separate Model Popout sender/configuration pass and joint verification.

Reviewed against published Kill Clog commit `aefd3fd5cb52e8795d7b99d4d178bc7238b26095`, matching [its Plugin Hub marker](https://github.com/runelite/plugin-hub/blob/master/plugins/kill-clog), and RuneLite 1.13.2. [`ClogLookupMenu.handlePlayerLookup`](https://github.com/420kc/kill-clog-plugin/blob/aefd3fd5cb52e8795d7b99d4d178bc7238b26095/src/main/java/com/killclog/ClogLookupMenu.java) obtains the API player and calls `Text.toJagexName(player.getName())` before its existing callback. [`KillClogPlugin.openPanelAndLookup`](https://github.com/420kc/kill-clog-plugin/blob/aefd3fd5cb52e8795d7b99d4d178bc7238b26095/src/main/java/com/killclog/KillClogPlugin.java#L863) is the private shared entry point used by both player and widget menus: it queues EDT work to open the existing navigation button, set the player name, and run the panel lookup. No lookup `PluginMessage` subscriber exists in the published plugin.

[`PluginMessage`](https://github.com/runelite/runelite/blob/runelite-parent-1.13.2/runelite-client/src/main/java/net/runelite/client/events/PluginMessage.java) is RuneLite's public shared event for communication between Hub plugins. Use this shared class plus ordinary Java payload values; neither plugin needs a dependency on the other's classes.

## API version 1 contract

| Field | Exact value/type |
| --- | --- |
| Namespace | `kill-clog` |
| Event name | `player-lookup` |
| Data `apiVersion` | Java `Integer` with value `1` |
| Data `playerName` | Java `String`, selected API player's normalized username |

Example request: `new PluginMessage("kill-clog", "player-lookup", Map.of("apiVersion", 1, "playerName", "Zezima"))`, posted once on RuneLite's injected `EventBus`. This is a proposed sender expression only; no production sender is implemented here.

The future sender must derive the name from the selected API player binding, never from the menu's displayed target text, and normalize with `Text.toJagexName`. Capture the selected player kind/name on the client thread before handing off to Swing; retain that metadata if the actor disappears. Open the inspector first, then post exactly one request after successful window creation, using the captured immutable values. Do not post from capture/render ticks, mailbox delivery, submenu construction, or each repaint. That utility replaces underscores, hyphens, and nonbreaking spaces with spaces, retains ASCII, and trims edges; it preserves case. Names have at most 12 characters and at least one alphanumeric character. Sender payloads are immutable `Map.of` maps with no Actor, WorldView, sidebar, appearance, or model objects. Only a player selection may send a request. An unavailable/recycled actor slot must never change the requested player's identity/name.

Unknown namespaces/events and unsupported, absent, or incorrectly typed API versions are ignored. Unknown additional data keys are ignored for compatible extensions. No acknowledgment, capability probe, automatic fallback, retry, or queue is part of version 1. Delivery is best effort: absent/disabled/old receivers ignore the event, and model inspection still works. One request means one attempt through normal Kill Clog behavior, not a guarantee of successful results.

## Minimal receiver change

All runtime changes can remain in `src/main/java/com/killclog/KillClogPlugin.java`; leave `ClogLookupMenu` and all lookup/network services untouched.

1. Import `net.runelite.client.events.PluginMessage` and `java.util.Map` (if not already imported).
2. Change the existing navigation field to `private volatile NavigationButton navButton;` and add `private volatile boolean lookupReady;` (initially false).
3. Set `lookupReady = true;` only at the end of successful `startUp()`, after the navigation button and panel are fully initialized.
4. Set `lookupReady = false;` as the **first** statement of `shutDown()`, before removing navigation or shutting down the panel.
5. Add the subscriber below next to the existing menu subscribers.
6. Add the lifecycle checks shown below to the existing `openPanelAndLookup()` method. This retains the same path for Kill Clog's existing menus.

```java
@Subscribe
public void onPluginMessage(PluginMessage message)
{
    if (!lookupReady || !"kill-clog".equals(message.getNamespace())
        || !"player-lookup".equals(message.getName()))
    {
        return;
    }
    Map<String, Object> data = message.getData();
    if (data == null || !Integer.valueOf(1).equals(data.get("apiVersion")))
    {
        return;
    }
    Object value = data.get("playerName");
    if (!(value instanceof String))
    {
        return;
    }
    String rawName = (String) value;
    if (rawName.length() > RsnInputPolicy.MAX_LENGTH || !RsnInputPolicy.isValid(rawName))
    {
        return;
    }
    String playerName = Text.toJagexName(rawName);
    if (!RsnInputPolicy.isValid(playerName))
    {
        return;
    }
    openPanelAndLookup(playerName);
}

private void openPanelAndLookup(String name)
{
    NavigationButton button = navButton;
    if (!lookupReady || button == null)
    {
        return;
    }
    SwingUtilities.invokeLater(() ->
    {
        if (!lookupReady || navButton != button)
        {
            return;
        }
        clientToolbar.openPanel(button);
        panel.setPlayerName(name);
        panel.doLookup();
    });
}
```

[`RsnInputPolicy`](https://github.com/420kc/kill-clog-plugin/blob/aefd3fd5cb52e8795d7b99d4d178bc7238b26095/src/main/java/com/killclog/RsnInputPolicy.java) already supplies the bounded length/shape checks within the same package. Validate the raw name before normalization so unsupported Unicode or markup cannot be silently stripped into a different valid username. The receiver can safely accept the existing underscore/hyphen/nonbreaking-space spellings, then normalize them. Never accept the menu's level suffix, tags, an empty/all-punctuation name, or arbitrary object values.

## Threading, lifecycle, and compatibility

EventBus delivery runs on the posting thread; do not assume it is the EDT. The subscriber reads only immutable primitive/String payload values and lifecycle flags. The shared method schedules all panel/toolbar work on the EDT. No client-thread actor reads are required in Kill Clog. Extract and validate values before scheduling, so a mutable caller map cannot change the queued target.

Capture the existing navigation button by **reference identity**, not `equals()`. Kill Clog's startup constructs a fresh navigation button each time. The queued identity check prevents an old request from opening the new panel after disable/re-enable, even if `lookupReady` has become true again. The ready flag prevents requests during partial startup, shutdown, or disabled state. No second navigation button/panel or UI traversal is introduced.

Normal menu actions still call the same private method. Do not change labels, registration, callbacks, menu-location configuration, or service implementations. Do not gate the message by menu-visibility settings: those control where Kill Clog's own option appears, while the future Model Popout provider selection is a deliberate lookup request. Kill Clog remains authoritative for lookup validation, world/profile behavior, cache use, asynchronous networking, sidebar contents, and errors.

The existing panel's `doLookup()` suppresses a request while a lookup is in flight. Preserve that behavior; do not force cancellation or add a retry loop solely for integration. API version 1 does not authenticate the posting plugin; namespace is routing, not proof of origin. Do not treat extra sender claims as trusted identity or add unsolicited publication/sync behavior.

Older Kill Clog versions continue to work normally and simply lack this receiver. New receivers ignore unsupported versions. No Plugin Hub cross-classloader dependency, reflection, synthetic game menu events, HTTP implementation, or runtime dependency is needed in Model Popout. 420KC chooses his own release number; none is invented here.

## Receiver tests and joint acceptance

- Valid `Integer(1)` and `"Zezima"` produce exactly one scheduled call to the existing lookup-and-open path.
- Ignore wrong namespace/name, missing/unsupported/string/long API version, non-String name, length over 12, markup, Unicode, empty/all-space/all-punctuation names. Accept canonical names and normalize existing underscore/hyphen/nonbreaking-space spellings consistently. Ignore unknown extra keys.
- Verify no Swing UI operation occurs on the posting thread and payload mutation after posting cannot change the queued username.
- Disable before EDT delivery: no lookup/sidebar reopening. Disable/re-enable before delivery: the old navigation identity must fail. Messages during startup/disabled state are ignored.
- Ordinary Kill Clog menu lookups and busy/error/cache behavior remain unchanged. Missing/disabled Kill Clog must never block model inspection.
- After compatible receiver publication, implement the sender/configuration in a separate Model Popout pass. Jointly verify: None sends zero requests; Kill Clog sends exactly one for a player and zero for an NPC; multiple windows/model ticks never repeat the request; normal Kill Clog options remain functional; the existing sidebar opens with normal results.

## Eventual provider setting and Hiscores

Use a single enum setting named **Associated Player Lookup**, default **None**, with exactly these visible choices: **None**, **Kill Clog**, **RuneLite Hiscores**. Implement capability/readiness or clearly disclose unavailable choices; do not silently choose another provider or enable a setting that appears functional against the old receiver. The current pass exposes none of these choices.

Current [HiscorePlugin](https://github.com/runelite/runelite/blob/runelite-parent-1.13.2/runelite-client/src/main/java/net/runelite/client/plugins/hiscore/HiscorePlugin.java#L255) has a package-private lookup-and-open method and private panel/navigation fields, with no external lookup message handler. Its public panel lookup method alone does not expose the existing sidebar-opening operation. Hiscores needs an upstream public integration hook. No fabricated `MenuOptionClicked`, subscriber invocation, reflected state, duplicate panel, or direct lookup networking is proposed.
