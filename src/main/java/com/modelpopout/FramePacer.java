package com.modelpopout;

/** Independent, monotonic render/presentation deadlines. No catch-up burst after a stall. */
final class FramePacer
{
    private long period, nextRender, nextPresentation;
    private boolean renderingScheduled, presentationScheduled;
    void setTarget(RenderTarget target)
    {
        period = target.fps == 0 ? 0 : 1_000_000_000L / target.fps;
        renderingScheduled = presentationScheduled = false;
    }
    long renderDelay(long now) { return renderingScheduled ? Math.max(0,nextRender-now) : 0; }
    long presentationDelay(long now) { return presentationScheduled ? Math.max(0,nextPresentation-now) : 0; }
    void rendered(long now) { nextRender = deadline(nextRender,renderingScheduled,now); renderingScheduled = true; }
    void presented(long now) { nextPresentation = deadline(nextPresentation,presentationScheduled,now); presentationScheduled = true; }
    private long deadline(long previous,boolean scheduled,long now)
    {
        long next = previous+period;
        return scheduled && period > 0 && next-now > 0 ? next : now+period;
    }
    static int delayMillis(long nanos) { return (int) Math.max(1,Math.min(Integer.MAX_VALUE,(nanos+999_999)/1_000_000)); }
}
