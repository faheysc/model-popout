package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudTrackerTest
{
    private final Object player=new Object(),npc=new Object();
    private final HudTracker tracker=new HudTracker();
    private final BufferedImage image=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);
    private final HitsplatStyle nativeStyle=HitsplatStyle.decode(new byte[]{9,0,50,0});
    private HudSnapshot sample(int cycle)
    { return tracker.sample(1,cycle,HudHealth.UNKNOWN,HudHealth.UNKNOWN,HudAssets.HealthSkin.FALLBACK); }
    private boolean hit(Object actor,int amount,int expiry,int cycle,HitsplatStyle style)
    { return tracker.add(actor,16,amount,expiry,cycle,style,image); }

    @Test public void multipleHitsKeepValuesSeparateAndExpireOnNativeCycles()
    {
        tracker.selectPlayer(player);tracker.selectNpc(npc,7);
        assertTrue(hit(player,0,150,100,nativeStyle));
        assertTrue(tracker.add(player,65,8,151,100,nativeStyle,image));
        assertTrue(tracker.add(player,6,12,152,100,nativeStyle,image));
        assertTrue(hit(player,30,153,100,nativeStyle));
        assertTrue(hit(npc,90,150,100,nativeStyle));
        assertFalse(hit(new Object(),99,150,100,nativeStyle));
        assertFalse(hit(null,99,150,100,nativeStyle));
        assertFalse(hit(player,99,154,100,nativeStyle)); // Native default rejects a fifth visible hit.
        HudSnapshot first=sample(100);
        assertEquals(4,first.player.hits.size());assertEquals(1,first.npc.hits.size());
        HashSet<Integer> slots=new HashSet<>();for(HudSnapshot.Hit h:first.player.hits) slots.add(h.slot);
        assertEquals(4,slots.size());assertEquals(0,first.player.hits.get(0).amount);
        assertEquals(65,first.player.hits.get(1).type);assertEquals(6,first.player.hits.get(2).type);
        assertEquals(4,sample(149).player.hits.size());assertEquals(3,sample(150).player.hits.size());
        assertTrue(sample(153).player.hits.isEmpty());assertTrue(sample(153).npc.hits.isEmpty());
        assertEquals(4,first.player.hits.size()); // Already published data remains unchanged.
        try { first.player.hits.clear();fail(); } catch(UnsupportedOperationException expected) { }
    }

    @Test public void targetSwitchDespawnLogoutAndReopeningCannotTransferHits()
    {
        long firstKey=tracker.selectPlayer(player);tracker.selectNpc(npc,7);
        hit(player,5,150,100,nativeStyle);hit(npc,8,150,100,nativeStyle);
        tracker.selectNpc(new Object(),8);assertTrue(sample(101).npc.hits.isEmpty());
        assertEquals(1,sample(101).player.hits.size());
        tracker.selectNpc(npc,9);hit(npc,8,150,101,nativeStyle);
        tracker.selectNpc(null,-1);assertEquals(-1,sample(102).npcKey);assertTrue(sample(102).npc.hits.isEmpty());
        assertFalse(hit(npc,9,150,102,nativeStyle));
        tracker.clear();assertTrue(sample(103).player.hits.isEmpty());
        assertTrue(tracker.selectPlayer(player)>firstKey);assertTrue(sample(104).player.hits.isEmpty());
        tracker.selectNpc(npc,10);hit(npc,8,150,105,nativeStyle);
        tracker.selectNpc(npc,11);assertTrue(sample(106).npc.hits.isEmpty()); // Reused identity, new lifecycle key.
    }

    @Test public void replacementPrioritiesMatchFourSlotBehavior()
    {
        tracker.selectPlayer(player);
        for(int i=0;i<4;i++) hit(player,10+i,150+i,100,nativeStyle);
        HitsplatStyle smallest=HitsplatStyle.decode(new byte[]{12,1,0});
        assertFalse(hit(player,10,170,100,smallest));assertTrue(hit(player,20,170,100,smallest));
        assertEquals(20,sample(100).player.hits.get(0).amount);
        HitsplatStyle earliest=HitsplatStyle.decode(new byte[]{12,0,0});
        assertTrue(hit(player,1,180,100,earliest));assertEquals(1,sample(100).player.hits.get(1).amount);
        assertFalse(hit(player,99,180,100,HitsplatStyle.decode(new byte[]{12,2,0})));
    }

    @Test public void idlePacketsAreReusedAndDelayedHitsRespectStartAndCycleWrap()
    {
        tracker.selectPlayer(player);HudSnapshot idle=sample(10);assertSame(idle,sample(11));
        assertTrue(hit(player,1,200,100,nativeStyle));HudSnapshot delayed=sample(100);
        assertFalse(delayed.player.hits.get(0).visible(149));assertTrue(delayed.player.hits.get(0).visible(150));
        assertNotSame(delayed,sample(101));assertSame(sample(101),sample(101));
        tracker.clear();tracker.selectPlayer(player);
        int start=Integer.MAX_VALUE-10,expiry=start+50;
        assertTrue(hit(player,1,expiry,start,nativeStyle));assertEquals(1,sample(start+49).player.hits.size());
        assertTrue(sample(expiry).player.hits.isEmpty());
    }

    @Test public void deathHealingUnknownHealthAndHiddenBarsAreIndependentOfHits()
    {
        tracker.selectPlayer(player);tracker.selectNpc(npc,7);hit(npc,8,150,100,nativeStyle);
        HudSnapshot dead=tracker.sample(1,100,HudHealth.player(0,30,0,99),HudHealth.scaled(0,30),HudAssets.HealthSkin.FALLBACK);
        assertEquals(0,dead.npc.health.fraction,0);assertEquals(1,dead.npc.hits.size());
        HudSnapshot healed=tracker.sample(1,101,HudHealth.player(15,30,80,99),HudHealth.scaled(15,30),HudAssets.HealthSkin.FALLBACK);
        assertEquals(.5,healed.npc.health.fraction,0);assertEquals(80.0/99,healed.player.health.fraction,0);
        HudSnapshot hidden=sample(102);assertFalse(hidden.npc.health.visible);assertEquals(1,hidden.npc.hits.size());
    }
    @Test public void teardownRejectsInFlightHudWorkBeforeIdentityOrPublicationCanBeRevived()
    {
        long obsolete=tracker.generation();tracker.selectPlayer(player);tracker.clear();
        assertFalse(tracker.inGeneration(obsolete,() -> fail("old event must not revive actor references or hits")));
        assertTrue(tracker.inGeneration(tracker.generation(),() -> {
            tracker.selectPlayer(player);assertTrue(hit(player,8,150,100,nativeStyle));
        }));
        assertEquals(1,sample(100).player.hits.size());tracker.clear();assertTrue(sample(101).player.hits.isEmpty());
    }
    @Test public void inFlightModelCaptureCannotRebindHudIdentityAfterCloseOrLogout()
    {
        long obsolete=tracker.generation();tracker.clear();
        assertEquals(-1,tracker.selectPlayer(obsolete,player,() -> true));
        assertEquals(-1,tracker.selectPlayer(tracker.generation(),player,() -> false));
        assertFalse(hit(player,8,150,100,nativeStyle));
        long restored=tracker.selectPlayer(tracker.generation(),player,() -> true);
        assertTrue(restored>=0);assertTrue(hit(player,8,150,100,nativeStyle));
        assertEquals(restored,sample(100).player.key);
    }
}
