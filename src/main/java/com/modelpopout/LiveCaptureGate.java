package com.modelpopout;

/** Short synchronized operations shared by the client thread and Swing EDT. */
final class LiveCaptureGate
{
    private long epoch;
    private boolean open, loggedIn, pending, scheduled;
    private long nextDue;
    private int fps = 20;

    synchronized void setRate(int fps)
    {
        this.fps = Math.max(1, Math.min(50, fps));
        scheduled = false;
    }

    synchronized long open()
    {
        open = true;
        invalidate();
        return epoch;
    }

    synchronized void close()
    {
        open = false;
        invalidate();
    }

    synchronized long setLoggedIn(boolean value)
    {
        if (loggedIn != value)
        {
            loggedIn = value;
            invalidate();
        }
        return epoch;
    }

    private void invalidate()
    {
        epoch++;
        pending = false;
        scheduled = false;
    }

    /** Returns a lifecycle ticket, or -1 if throttled, inactive, or awaiting an EDT handoff. */
    synchronized long acquire(long now, boolean force)
    {
        if (!open || !loggedIn || pending) return -1;
        if (!force && scheduled && now - nextDue < 0) return -1;
        long period = 1_000_000_000L / fps;
        if (force || !scheduled)
        {
            nextDue = now + period;
        }
        else
        {
            // Keep fractional tick timing at 20 FPS (50ms) on a 20ms tick cadence.
            // Skip missed deadlines rather than scheduling catch-up captures.
            nextDue = now + period - (now - nextDue) % period;
        }
        scheduled = true;
        pending = true;
        return epoch;
    }

    synchronized boolean complete(long ticket)
    {
        if (ticket != epoch || !open || !loggedIn) return false;
        pending = false;
        return true;
    }

    synchronized boolean current(long ticket)
    {
        return open && ticket == epoch;
    }

    synchronized boolean viewing()
    {
        return open;
    }

    synchronized boolean active()
    {
        return open && loggedIn;
    }

    synchronized long epoch()
    {
        return epoch;
    }
}
