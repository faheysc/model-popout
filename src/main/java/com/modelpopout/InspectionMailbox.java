package com.modelpopout;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** One replaceable capture/HUD packet and one EDT handoff per inspector. Close fences racing offers. */
final class InspectionMailbox
{
    private final AtomicReference<InspectionCapture.Packet> latest = new AtomicReference<>();
    private final AtomicBoolean queued = new AtomicBoolean();
    private final Executor edt;
    private final Consumer<InspectionCapture.Packet> receive;
    private volatile boolean closed;

    InspectionMailbox(Executor edt, Consumer<InspectionCapture.Packet> receive) { this.edt=edt; this.receive=receive; }

    synchronized void offer(InspectionCapture.Packet packet)
    {
        if (closed) return;
        latest.set(packet);
        schedule();
    }

    synchronized void close() { closed=true; latest.set(null); }

    private void schedule()
    {
        if (queued.compareAndSet(false,true)) edt.execute(this::drain);
    }

    private void drain()
    {
        try
        {
            InspectionCapture.Packet packet=latest.getAndSet(null);
            if (!closed && packet != null) receive.accept(packet);
        }
        finally
        {
            queued.set(false);
            if (!closed && latest.get()!=null) schedule();
        }
    }
}
