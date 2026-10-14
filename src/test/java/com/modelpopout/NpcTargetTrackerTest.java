package com.modelpopout;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import org.junit.Test;
import static org.junit.Assert.*;

public class NpcTargetTrackerTest
{
    private static final class World extends TestWorldView
    {
        final Map<Integer,NPC> live = new LinkedHashMap<>();
        int scans;
        final IndexedObjectSet<NPC> set = new IndexedObjectSet<NPC>()
        {
            @Override public NPC byIndex(int index) { return live.get(index); }
            @Override public Iterator<NPC> iterator() { scans++; return live.values().iterator(); }
        };
        @Override public IndexedObjectSet<NPC> npcs() { return set; }
    }

    private static final class Player extends TestPlayer
    {
        final World world = new World();
        Actor target;
        @Override public WorldView getWorldView() { return world; }
        @Override public Actor getInteracting() { return target; }
    }

    private static final class Npc extends TestNpc
    {
        final World world;
        final int index;
        Actor target;
        int combat = 1, id = 1, animation = -1;
        boolean dead;
        Npc(Player player,int index) { world=player.world; this.index=index; world.live.put(index,this); }
        @Override public WorldView getWorldView() { return world; }
        @Override public int getIndex() { return index; }
        @Override public int getCombatLevel() { return combat; }
        @Override public int getId() { return id; }
        @Override public int getAnimation() { return animation; }
        @Override public boolean isDead() { return dead; }
        @Override public Actor getInteracting() { return target; }
    }

    @Test public void targetsSwitchAndSeedScanIsNotRepeatedPerFrame()
    {
        Player player = new Player();
        Npc a = new Npc(player,1), b = new Npc(player,2);
        a.target = b.target = player;
        player.target = b;
        NpcTargetTracker tracker = new NpcTargetTracker();
        NpcTargetTracker.Selection first = tracker.select(player,10);
        assertSame(b,first.npc);
        for(int i = 0; i < 20; i++) assertSame(b,tracker.select(player,10).npc);
        assertEquals(1,player.world.scans);
        player.target = a;
        NpcTargetTracker.Selection switched = tracker.select(player,10);
        assertSame(a,switched.npc);
        assertTrue(first.key != switched.key);
        player.target = null;
        assertSame(a,tracker.select(player,10).npc);
    }

    @Test public void despawnAndReusedNpcIndexCannotReuseSelectionIdentity()
    {
        Player player = new Player();
        Npc old = new Npc(player,7);
        player.target = old;
        NpcTargetTracker tracker = new NpcTargetTracker();
        NpcTargetTracker.Selection first = tracker.select(player,10);
        player.world.live.remove(7);
        assertEquals(first.key,tracker.despawned(old));
        assertNull(tracker.select(player,10).npc); // Player still holds a stale native pointer.
        Npc replacement = new Npc(player,7);
        replacement.target = player;
        tracker.spawned(replacement);
        NpcTargetTracker.Selection next = tracker.select(player,10);
        assertSame(replacement,next.npc);
        assertTrue(first.key != next.key);
    }

    @Test public void livingNpcIsRemovedWhenBothInteractionsEnd()
    {
        Player player = new Player();
        Npc npc = new Npc(player,1);
        npc.target = player;
        NpcTargetTracker tracker = new NpcTargetTracker();
        assertSame(npc,tracker.select(player,10).npc);
        npc.target = null; player.target = null;
        tracker.interaction(npc,null);
        assertNull(tracker.select(player,10).npc);
    }

    @Test public void deathPoseRetainedUntilDespawnAndNewTargetCanInterruptIt()
    {
        Player player = new Player();
        Npc dying = new Npc(player,1), other = new Npc(player,2);
        player.target = dying;
        NpcTargetTracker tracker = new NpcTargetTracker();
        NpcTargetTracker.Selection first = tracker.select(player,10);
        dying.dead = true; dying.animation = 100; player.target = null;
        tracker.interaction(dying,null);
        assertSame(dying,tracker.select(player,10).npc);
        player.target = other;
        assertSame(other,tracker.select(player,10).npc);
        player.target = null;
        assertNull(tracker.select(player,10).npc);
        player.world.live.remove(1);
        assertEquals(-1,tracker.despawned(dying));
        assertTrue(first.key != tracker.serial());
    }

    @Test public void finishedDeathAnimationRemovesPersistentCorpseEvenWithStalePlayerInteraction()
    {
        Player player = new Player();
        Npc corpse = new Npc(player,1);
        player.target = corpse;
        NpcTargetTracker tracker = new NpcTargetTracker();
        tracker.select(player,10);
        corpse.dead = true; corpse.animation = 100;
        assertSame(corpse,tracker.select(player,10).npc);
        corpse.animation = -1;
        assertNull(tracker.select(player,10).npc);
        assertSame(corpse,player.world.live.get(1)); // Has not despawned; the death action has ended.
    }

    @Test public void transformsPreserveIdentityAndCleanupCreatesFreshSession()
    {
        Player player = new Player();
        Npc npc = new Npc(player,1);
        player.target = npc;
        NpcTargetTracker tracker = new NpcTargetTracker();
        NpcTargetTracker.Selection first = tracker.select(player,10);
        npc.id = 200;
        tracker.spawned(npc);
        assertEquals(first.key,tracker.select(player,10).key);
        tracker.clear(); // Same cleanup operation is used by window close, logout, and plugin stop.
        NpcTargetTracker.Selection reopened = tracker.select(player,11);
        assertSame(npc,reopened.npc);
        assertTrue(first.key != reopened.key);
        assertEquals(2,player.world.scans);
    }
}
