package com.modelpopout;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** One replaceable packet and at most one queued/running EDT delivery, including racing offers. */
final class LatestHudMailbox
{
    private final AtomicReference<HudSnapshot> latest = new AtomicReference<>();
    private final AtomicBoolean queued = new AtomicBoolean();
    private final Executor dispatch;
    private final Consumer<HudSnapshot> receive;

    LatestHudMailbox(Executor dispatch, Consumer<HudSnapshot> receive)
    {
        this.dispatch = dispatch;
        this.receive = receive;
    }

    void offer(HudSnapshot packet)
    {
        latest.set(packet);
        schedule();
    }

    void clear()
    {
        latest.set(null);
    }

    private void schedule()
    {
        if (queued.compareAndSet(false, true)) dispatch.execute(this::drain);
    }

    private void drain()
    {
        try
        {
            HudSnapshot packet = latest.getAndSet(null);
            if (packet != null) receive.accept(packet);
        }
        finally
        {
            queued.set(false);
            if (latest.get() != null) schedule();
        }
    }
}
