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

    @Test public void measurementsDoNotConfuseSourcePosesRendersAndPaints()
    {
        ViewerPerformance stats = new ViewerPerformance();
        stats.sample(1000000000L);
        for (int i=0;i<50;i++) stats.capture(i<20);
        for (int i=0;i<120;i++) stats.rendered(i>=20);
        for (int i=0;i<80;i++) stats.presented();
        String result = stats.sample(2000000000L);
        assertTrue(result.contains("Capture 50.0/s"));
        assertTrue(result.contains("unique poses 20.0/s"));
        assertTrue(result.contains("completed renders 120.0/s"));
        assertTrue(result.contains("Swing paints 80.0/s"));
        assertTrue(result.contains("texture frames 100.0/s"));
        assertTrue(result.contains("pose interpolation off"));
    }
}
