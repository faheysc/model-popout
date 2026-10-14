package com.modelpopout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;

/** API reads occur on the client thread. clear() only releases references and may run on the EDT. */
final class NpcTargetTracker
{
    private final Set<NPC> attackers = Collections.newSetFromMap(new IdentityHashMap<>());
    private Player player;
    private WorldView world;
    private NPC selected;
    private long epoch = -1, serial;

    synchronized Selection select(Player currentPlayer, long currentEpoch)
    {
        WorldView currentWorld = currentPlayer.getWorldView();
        if (player != currentPlayer || world != currentWorld || epoch != currentEpoch)
        {
            clear();
            player = currentPlayer;
            world = currentWorld;
            epoch = currentEpoch;
            // One seed scan when opening/enabling/entering a world; no full-world per-frame scan.
            if (world != null) for (NPC npc : world.npcs())
            {
                if (npc != null && npc.getInteracting() == player && npc.getCombatLevel() > 0) attackers.add(npc);
            }
        }
        if (world == null) return new Selection(null,-1);
        Actor interacting = player.getInteracting();
        NPC direct = interacting instanceof NPC ? (NPC) interacting : null;
        attackers.removeIf(npc -> !present(npc) || (npc.getInteracting() != player && !npc.isDead())
            || (npc.isDead() && npc.getAnimation() < 0));
        List<NpcSelector.Candidate<NPC>> candidates = new ArrayList<>(attackers.size() + 2);
        for (NPC npc : attackers) candidates.add(candidate(npc));
        if (direct != null) candidates.add(candidate(direct));
        if (selected != null && selected != direct && !attackers.contains(selected)) candidates.add(candidate(selected));
        NPC next = NpcSelector.select(direct, selected, candidates);
        if (next != selected) { selected = next; serial++; }
        return new Selection(next,next == null ? -1 : serial);
    }

    static final class Selection
    {
        final NPC npc;
        final long key;
        Selection(NPC npc, long key) { this.npc = npc; this.key = key; }
    }

    private boolean present(NPC npc)
    {
        return world != null && npc.getIndex() >= 0 && npc.getWorldView() == world && world.npcs().byIndex(npc.getIndex()) == npc;
    }

    private NpcSelector.Candidate<NPC> candidate(NPC npc)
    {
        boolean present = present(npc);
        boolean dead = present && npc.isDead();
        return new NpcSelector.Candidate<>(npc, npc.getIndex(), present,
            present && npc.getInteracting() == player, dead, npc.getCombatLevel() > 0,
            dead && npc.getAnimation() >= 0);
    }

    synchronized void interaction(Actor source, Actor target)
    {
        if (!(source instanceof NPC) || player == null) return;
        NPC npc = (NPC) source;
        if (npc.getWorldView() == world && target == player && npc.getCombatLevel() > 0) attackers.add(npc);
        else attackers.remove(npc);
    }

    synchronized void spawned(NPC npc)
    {
        if (player != null && npc.getWorldView() == world && npc.getInteracting() == player
            && npc.getCombatLevel() > 0) attackers.add(npc);
    }

    synchronized long despawned(NPC npc)
    {
        attackers.remove(npc);
        if (selected != npc) return -1;
        long removed = serial;
        selected = null;
        serial++;
        return removed;
    }

    synchronized Selection currentSelection()
    { return new Selection(selected,selected==null?-1:serial); }

    synchronized long serial() { return serial; }

    synchronized void clear()
    {
        attackers.clear();
        player = null; world = null; selected = null; epoch = -1;
        serial++; // Never reuse a selection identity across close/reopen or slot recycling.
    }
}
