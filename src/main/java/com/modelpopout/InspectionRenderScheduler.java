package com.modelpopout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/** EDT-owned FIFO: one worker and at most one pending supplier per open inspector. */
final class InspectionRenderScheduler
{
    interface Work
    {
        void render();
        void failed(Throwable failure);
        void complete();
    }

    private final Executor worker, edt;
    private final Map<Object,Supplier<Work>> waiting = new LinkedHashMap<>();
    private boolean busy, closed;

    InspectionRenderScheduler(Executor worker, Executor edt) { this.worker = worker; this.edt = edt; }

    void request(Object owner, Supplier<Work> work)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (closed) return;
        waiting.put(owner,work); // Replacing pending work does not move it ahead of other windows.
        pump();
    }

    void remove(Object owner) { waiting.remove(owner); }
    void close() { closed = true; waiting.clear(); }
    int queued() { return waiting.size(); }
    boolean busy() { return busy; }

    private void pump()
    {
        if (busy || closed) return;
        while (!waiting.isEmpty())
        {
            Object owner = waiting.keySet().iterator().next();
            Work work = waiting.remove(owner).get();
            if (work == null) continue;
            busy = true;
            try
            {
                worker.execute(() -> {
                    try { work.render(); }
                    catch (Throwable failure) { work.failed(failure); }
                    finally { edt.execute(() -> finish(work)); }
                });
            }
            catch (java.util.concurrent.RejectedExecutionException failure)
            { work.failed(failure); finish(work); }
            return;
        }
    }

    private void finish(Work work)
    {
        try { work.complete(); }
        finally { busy = false; pump(); }
    }
}
