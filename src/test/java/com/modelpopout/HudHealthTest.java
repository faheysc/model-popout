package com.modelpopout;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudHealthTest
{
    @Test public void scaledHealthNeverInventsExactNpcHp()
    {
        HudHealth health=HudHealth.scaled(7,30);
        assertTrue(health.visible);assertFalse(health.exact);assertEquals(7.0/30,health.fraction,0);
        assertEquals(-1,health.current);assertEquals(-1,health.maximum);
        assertEquals(0,HudHealth.scaled(0,30).fraction,0);assertEquals(1,HudHealth.scaled(40,30).fraction,0);
    }
    @Test public void missingOrInvalidNativeHealthDoesNotDisplayABar()
    {
        assertSame(HudHealth.UNKNOWN,HudHealth.scaled(-1,30));
        assertSame(HudHealth.UNKNOWN,HudHealth.scaled(7,-1));assertSame(HudHealth.UNKNOWN,HudHealth.scaled(7,0));
        assertSame(HudHealth.UNKNOWN,HudHealth.player(-1,30,90,99));
        assertSame(HudHealth.UNKNOWN,HudHealth.player(7,0,90,99));
    }
    @Test public void ownHitpointsRefineVisibleBarButDoNotInventCombatVisibility()
    {
        HudHealth health=HudHealth.player(15,30,40,99);
        assertTrue(health.exact);assertEquals(40.0/99,health.fraction,0);
        assertEquals(40,health.current);assertEquals(99,health.maximum);
        assertEquals(.5,HudHealth.player(15,30,-1,99).fraction,0);
        assertFalse(HudHealth.player(15,30,20,0).exact);
        assertEquals(1,HudHealth.player(30,30,110,99).fraction,0);
    }
}
