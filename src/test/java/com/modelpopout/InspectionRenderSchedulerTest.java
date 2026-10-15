package com.modelpopout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionRenderSchedulerTest
{
    private InspectionRenderScheduler.Work work(String name,List<String> completed)
    {
        return new InspectionRenderScheduler.Work()
        {
            @Override public void render() {}
            @Override public void failed(Throwable failure) { fail(failure.toString()); }
            @Override public void complete() { completed.add(name); }
        };
    }

    @Test public void queueIsBoundedPerOwnerAndBusyWindowsCannotStarveOtherWindows() throws Exception
    {
        Worker worker=new Worker();List<String> done=new ArrayList<>();
        InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        SwingUtilities.invokeAndWait(() -> {
            scheduler.request("A",() -> work("A",done));
            for (int i=0;i<10000;i++)
            { scheduler.request("A",() -> work("latest A",done));scheduler.request("B",() -> work("latest B",done)); }
            assertEquals(2,scheduler.queued());assertEquals(1,worker.tasks.size());assertTrue(scheduler.busy());
        });
        worker.run();SwingUtilities.invokeAndWait(() -> {});
        worker.run();SwingUtilities.invokeAndWait(() -> {});
        worker.run();SwingUtilities.invokeAndWait(() -> assertFalse(scheduler.busy()));
        assertEquals(java.util.Arrays.asList("A","latest A","latest B"),done);
    }

    @Test public void removalAndShutdownRetainTheRealWorkerSlotUntilCompletion() throws Exception
    {
        Worker worker=new Worker();List<String> done=new ArrayList<>();
        InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        SwingUtilities.invokeAndWait(() -> {
            scheduler.request("A",() -> work("running A",done));
            scheduler.request("B",() -> { fail("closed window must never start");return null; });
            scheduler.remove("B");scheduler.close();
            scheduler.request("C",() -> { fail("shutdown must not accept work");return null; });
            assertTrue(scheduler.busy());assertEquals(0,scheduler.queued());
        });
        worker.run();SwingUtilities.invokeAndWait(() -> assertFalse(scheduler.busy()));
        assertEquals(java.util.Collections.singletonList("running A"),done);assertTrue(worker.tasks.isEmpty());
    }

    @Test public void failingAndRejectedWorkDoesNotWedgeOrLoseOtherWindows() throws Exception
    {
        Worker worker=new Worker();List<String> done=new ArrayList<>();
        InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        SwingUtilities.invokeAndWait(() -> {
            scheduler.request("bad",() -> new InspectionRenderScheduler.Work()
            {
                @Override public void render() { throw new IllegalArgumentException("malformed model"); }
                @Override public void failed(Throwable failure) { assertTrue(failure instanceof IllegalArgumentException); }
                @Override public void complete() { done.add("failure"); }
            });
            scheduler.request("good",() -> work("good",done));
        });
        worker.run();SwingUtilities.invokeAndWait(() -> {});worker.run();SwingUtilities.invokeAndWait(() -> {});
        assertEquals(java.util.Arrays.asList("failure","good"),done);
        Executor rejected=task -> { throw new java.util.concurrent.RejectedExecutionException(); };
        SwingUtilities.invokeAndWait(() -> {
            InspectionRenderScheduler stopped=new InspectionRenderScheduler(rejected,SwingUtilities::invokeLater);
            stopped.request("owner",() -> new InspectionRenderScheduler.Work()
            {
                @Override public void render() { fail(); }
                @Override public void failed(Throwable failure) { assertTrue(failure instanceof java.util.concurrent.RejectedExecutionException); }
                @Override public void complete() { done.add("rejected"); }
            });
            assertFalse(stopped.busy());
        });
        assertEquals("rejected",done.get(2));
    }

    @Test public void blockedInspectorExecutorDoesNotHoldTheSeparateViewerExecutor() throws Exception
    {
        Worker inspectors=new Worker(),viewer=new Worker();List<String> done=new ArrayList<>();
        InspectionRenderScheduler first=new InspectionRenderScheduler(inspectors,SwingUtilities::invokeLater);
        InspectionRenderScheduler independent=new InspectionRenderScheduler(viewer,SwingUtilities::invokeLater);
        SwingUtilities.invokeAndWait(() -> {
            first.request("inspector",() -> work("inspector",done));
            independent.request("viewer",() -> work("viewer",done));
        });
        viewer.run();SwingUtilities.invokeAndWait(() -> {});
        assertEquals(java.util.Collections.singletonList("viewer"),done);assertEquals(1,inspectors.tasks.size());
        inspectors.run();SwingUtilities.invokeAndWait(() -> {});
    }
}
