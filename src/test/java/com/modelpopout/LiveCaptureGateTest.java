package com.modelpopout;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiveCaptureGateTest
{
    private LiveCaptureGate ready()
    {
        LiveCaptureGate gate = new LiveCaptureGate();
        gate.open();
        gate.setLoggedIn(true);
        return gate;
    }

    @Test public void averagesTwentySamplesPerSecondOnTwentyMillisecondTicks()
    {
        LiveCaptureGate gate = ready();
        int samples = 0;
        for (long ms = 0; ms < 1000; ms += 20)
        {
            long ticket = gate.acquire(ms * 1_000_000, false);
            if (ticket >= 0) { samples++; assertTrue(gate.complete(ticket)); }
        }
        assertEquals(20, samples);
    }

    @Test public void slowTicksSampleFreshStateWithoutCatchUpBursts()
    {
        LiveCaptureGate gate = ready();
        int samples = 0;
        for (long ms = 0; ms < 1000; ms += 200)
        {
            long ticket = gate.acquire(ms * 1_000_000, false);
            assertTrue(ticket >= 0);
            samples++;
            assertTrue(gate.complete(ticket));
            assertEquals(-1, gate.acquire(ms * 1_000_000, false));
        }
        assertEquals(5, samples);
    }

    @Test public void slowEdtAllowsOnlyOnePendingHandoff()
    {
        LiveCaptureGate gate = ready();
        long ticket = gate.acquire(0, false);
        for (long ms = 20; ms < 10000; ms += 20)
        {
            assertEquals(-1, gate.acquire(ms * 1_000_000, false));
            assertEquals(-1, gate.acquire(ms * 1_000_000, true));
        }
        assertTrue(gate.complete(ticket));
        assertTrue(gate.acquire(10_000_000_000L, false) >= 0);
    }

    @Test public void closeAndLogoutRejectOldCallbacksAndStopCapturing()
    {
        LiveCaptureGate gate = ready();
        long old = gate.acquire(0, false);
        gate.close();
        assertFalse(gate.complete(old));
        assertFalse(gate.current(old));
        assertEquals(-1, gate.acquire(100_000_000, true));
        gate.open();
        long fresh = gate.acquire(100_000_000, false);
        assertFalse(gate.complete(old));
        assertEquals(-1, gate.acquire(200_000_000, false));
        assertTrue(gate.complete(fresh));
        gate.setLoggedIn(false);
        assertEquals(-1, gate.acquire(300_000_000, true));
        assertFalse(gate.complete(fresh));
        gate.setLoggedIn(true);
        assertTrue(gate.acquire(300_000_000, false) >= 0);
    }

    @Test public void manualCaptureBypassesTimingButNotBackpressure()
    {
        LiveCaptureGate gate = ready();
        long ticket = gate.acquire(0, false);
        gate.complete(ticket);
        assertEquals(-1, gate.acquire(1, false));
        long manual = gate.acquire(1, true);
        assertTrue(manual >= 0);
        assertEquals(-1, gate.acquire(2, true));
        gate.complete(manual);
        assertEquals(-1, gate.acquire(3, false));
    }

    @Test public void configurationChangesRateImmediatelyAndClampsIt()
    {
        LiveCaptureGate gate = ready();
        gate.setRate(0);
        gate.complete(gate.acquire(0, false));
        assertEquals(-1, gate.acquire(999_999_999, false));
        gate.complete(gate.acquire(1_000_000_000, false));
        gate.setRate(1000);
        gate.complete(gate.acquire(1_000_000_001, false));
        assertEquals(-1, gate.acquire(1_020_000_000, false));
        assertTrue(gate.acquire(1_020_000_001, false) >= 0);
    }

    @Test public void negativeMonotonicClockValuesWork()
    {
        LiveCaptureGate gate = ready();
        gate.complete(gate.acquire(-1_000_000_000, false));
        assertEquals(-1, gate.acquire(-960_000_000, false));
        assertTrue(gate.acquire(-940_000_000, false) >= 0);
    }
}
