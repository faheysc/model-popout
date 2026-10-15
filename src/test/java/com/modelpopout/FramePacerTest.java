package com.modelpopout;

import org.junit.Test;
import static org.junit.Assert.*;

public class FramePacerTest
{
    @Test public void renderAndPresentationHaveIndependentDeadlinesAtAllTargets()
    {
        for (RenderTarget target : RenderTarget.values())
        {
            FramePacer pacer = new FramePacer(); pacer.setTarget(target);
            long now = -1000000000L;
            assertEquals(0,pacer.renderDelay(now));
            assertEquals(0,pacer.presentationDelay(now));
            pacer.rendered(now);
            assertEquals(0,pacer.presentationDelay(now));
            if (target.fps > 0) assertTrue(pacer.renderDelay(now) > 0);
            pacer.presented(now+1000);
            long period = target.fps == 0 ? 0 : 1000000000L/target.fps;
            assertEquals(period,pacer.presentationDelay(now+1000));
            assertEquals(0,pacer.renderDelay(now+10000000000L));
            pacer.rendered(now+10000000000L);
            assertEquals(period,pacer.renderDelay(now+10000000000L));
        }
    }

    @Test public void targetAllowsMoreRenderStartsThanFiftyCaptureTicksWithoutCatchUpBursts()
    {
        FramePacer pacer = new FramePacer(); pacer.setTarget(RenderTarget.FPS_240);
        long now = 0; int starts = 0;
        while (now < 1000000000L)
        {
            long delay = pacer.renderDelay(now);
            if (delay > 0) now += delay;
            else { pacer.rendered(now); starts++; now++; }
        }
        assertTrue(starts >= 240 && starts <= 241);
        now += 10000000000L;
        assertEquals(0,pacer.renderDelay(now));
        pacer.rendered(now);
        assertTrue(pacer.renderDelay(now) > 0);
        pacer.setTarget(RenderTarget.UNLIMITED);
        assertEquals(0,pacer.renderDelay(now));
        assertEquals(1,FramePacer.delayMillis(1));
        assertEquals(5,FramePacer.delayMillis(4166666));
    }

}
