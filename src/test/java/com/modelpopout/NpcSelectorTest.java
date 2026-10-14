package com.modelpopout;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class NpcSelectorTest
{
    private NpcSelector.Candidate<Object> npc(Object identity, int index, boolean present, boolean attacking, boolean dead)
    {
        return new NpcSelector.Candidate<>(identity,index,present,attacking,dead,true);
    }

    @Test public void directTargetWinsOverSeveralAttackersAndOldTarget()
    {
        Object direct = new Object(), old = new Object(), third = new Object();
        assertSame(direct,NpcSelector.select(direct,old,Arrays.asList(
            npc(old,1,true,true,false),npc(third,2,true,true,false),npc(direct,10,true,false,false))));
    }

    @Test public void keepsAttackerToAvoidFlickeringBetweenSeveralNpcs()
    {
        Object a = new Object(), b = new Object();
        assertSame(b,NpcSelector.select(null,b,Arrays.asList(npc(a,1,true,true,false),npc(b,2,true,true,false))));
        assertSame(a,NpcSelector.select(null,null,Arrays.asList(npc(b,2,true,true,false),npc(a,1,true,true,false))));
    }

    @Test public void combatEndingOrDespawnRemovesLivingNpc()
    {
        Object npc = new Object();
        assertNull(NpcSelector.select(null,npc,Arrays.asList(npc(npc,1,true,false,false))));
        assertNull(NpcSelector.select(npc,npc,Arrays.asList(npc(npc,1,false,true,false))));
    }

    @Test public void selectedDeathAnimationRemainsOnlyWhilePresentUnlessNewTargetWins()
    {
        Object corpse = new Object(), other = new Object();
        assertSame(corpse,NpcSelector.select(null,corpse,Arrays.asList(npc(corpse,1,true,false,true),npc(other,2,true,true,false))));
        assertSame(other,NpcSelector.select(other,corpse,Arrays.asList(npc(corpse,1,true,false,true),npc(other,2,true,true,false))));
        assertSame(other,NpcSelector.select(null,corpse,Arrays.asList(npc(corpse,1,false,false,true),npc(other,2,true,true,false))));
    }

    @Test public void nonCombatFollowersAreNotFallbackAttackers()
    {
        Object follower = new Object();
        assertNull(NpcSelector.select(null,null,Arrays.asList(new NpcSelector.Candidate<>(follower,1,true,true,false,false))));
    }
}
