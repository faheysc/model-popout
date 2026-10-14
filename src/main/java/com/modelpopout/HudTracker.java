package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Client-thread identity/slot state; synchronized clear() may release references from the EDT. */
final class HudTracker
{
    private Object player, npc;
    private long playerSerial, npcKey = -1, generation;
    private final Slots playerSlots = new Slots(), npcSlots = new Slots();
    private HudHealth playerHealth = HudHealth.UNKNOWN, npcHealth = HudHealth.UNKNOWN;
    private boolean dirty = true;
    private HudSnapshot cached;

    synchronized long generation()
    {
        return generation;
    }

    /** Teardown cannot interleave identity binding and publication, or revive an old capture. */
    synchronized boolean inGeneration(long expected, Runnable update)
    {
        if (expected != generation) return false;
        update.run();
        return true;
    }

    /** Model capture also binds HUD identity and must not revive state after closing. */
    synchronized long selectPlayer(long expected, Object actor, BooleanSupplier current)
    {
        if (expected != generation || !current.getAsBoolean()) return -1;
        return selectPlayer(actor);
    }

    synchronized long selectPlayer(Object actor)
    {
        if (player != actor)
        {
            player = actor;
            playerSerial++;
            playerSlots.clear();
            playerHealth = HudHealth.UNKNOWN;
            dirty = true;
        }
        return playerSerial;
    }

    synchronized void selectNpc(Object actor, long key)
    {
        if (npc != actor || npcKey != key)
        {
            npc = actor;
            npcKey = actor == null ? -1 : key;
            npcSlots.clear();
            npcHealth = HudHealth.UNKNOWN;
            dirty = true;
        }
    }

    synchronized boolean add(Object actor, int type, int amount, int expires, int cycle,
        HitsplatStyle style, BufferedImage image)
    {
        if (actor == null || type < 0 || expires - cycle <= 0 || style == null || image == null) return false;
        Slots slots = actor == player ? playerSlots : actor == npc ? npcSlots : null;
        if (slots == null) return false;
        boolean added = slots.add(type, amount, expires, cycle, style, image);
        dirty |= added;
        return added;
    }

    synchronized HudSnapshot sample(long token, int cycle, HudHealth nextPlayer, HudHealth nextNpc,
        HudAssets.HealthSkin skin)
    {
        dirty |= playerSlots.expire(cycle) | npcSlots.expire(cycle);
        dirty |= !playerHealth.same(nextPlayer) || !npcHealth.same(nextNpc);
        playerHealth = nextPlayer;
        npcHealth = nextNpc;
        boolean ticking = playerSlots.any() || npcSlots.any();
        if (cached != null && !dirty && cached.token == token && cached.skin == skin
            && (!ticking || cached.cycle == cycle)) return cached;
        cached = new HudSnapshot(token, cycle, npcKey,
            new HudSnapshot.ActorHud(playerSerial, playerHealth, playerSlots.copy()),
            new HudSnapshot.ActorHud(npcKey, npcHealth, npcSlots.copy()), skin);
        dirty = false;
        return cached;
    }

    synchronized void clear()
    {
        generation++;
        player = null;
        npc = null;
        playerSerial++;
        npcKey = -1;
        playerSlots.clear();
        npcSlots.clear();
        playerHealth = npcHealth = HudHealth.UNKNOWN;
        cached = null;
        dirty = true;
    }

    private static final class Slots
    {
        private final HudSnapshot.Hit[] hits = new HudSnapshot.Hit[4];
        private int next;

        boolean add(int type, int amount, int expires, int cycle, HitsplatStyle style, BufferedImage image)
        {
            expire(cycle);
            int chosen = -1;
            for (int i = 0; i < hits.length; i++)
            {
                int slot = (next + i) % hits.length;
                if (hits[slot] == null)
                {
                    chosen = slot;
                    break;
                }
            }
            if (chosen < 0)
            {
                if (style.replaceMode != 0 && style.replaceMode != 1) return false;
                chosen = 0;
                for (int i = 1; i < hits.length; i++)
                {
                    boolean preferred = style.replaceMode == 1 ? hits[i].amount < hits[chosen].amount
                        : hits[i].expires - cycle < hits[chosen].expires - cycle;
                    if (preferred) chosen = i;
                }
                if (style.replaceMode == 1 && amount <= hits[chosen].amount) return false;
            }
            hits[chosen] = new HudSnapshot.Hit(type, amount, expires, chosen, style, image);
            next = (chosen + 1) % hits.length;
            return true;
        }

        boolean expire(int cycle)
        {
            boolean changed = false;
            for (int i = 0; i < hits.length; i++)
            {
                if (hits[i] != null && !hits[i].active(cycle))
                {
                    hits[i] = null;
                    changed = true;
                }
            }
            return changed;
        }

        boolean any()
        {
            for (HudSnapshot.Hit hit : hits) if (hit != null) return true;
            return false;
        }

        List<HudSnapshot.Hit> copy()
        {
            List<HudSnapshot.Hit> result = new ArrayList<>(4);
            for (HudSnapshot.Hit hit : hits) if (hit != null) result.add(hit);
            return result;
        }

        void clear()
        {
            Arrays.fill(hits, null);
            next = 0;
        }
    }
}
