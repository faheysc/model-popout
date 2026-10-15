package com.modelpopout;

import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionSessionTest
{
    @Test public void npcTransformationRefitsOnlyItsInspectorAndRejectsTheEarlierInFlightFrame() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] window=new InspectionSession[1];Mesh large=new Mesh();large.y[2]=-3000;
        ModelSnapshot transformed=ModelSnapshot.capture(large,null);
        SwingUtilities.invokeAndWait(() -> {
            window[0]=new InspectionSession(config,scheduler,() -> {},value -> {});
            window[0].accept(new InspectionCapture.Packet(ModelSnapshot.capture(new Mesh(),null),TextureSet.EMPTY,null,100,0,true,1));
            window[0].state.setCamera(window[0].state.camera().drag(35,10));
            window[0].accept(new InspectionCapture.Packet(transformed,TextureSet.EMPTY,null,101,0,true,2));
        });
        worker.run();SwingUtilities.invokeAndWait(() -> assertNull("Old transformation must not publish",window[0].panel.image));
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(.35,window[0].panel.imageCamera.yaw,1e-9);
            java.awt.image.BufferedImage expected=new ModelRenderer().render(transformed,window[0].state.camera(),ModelFrame.initial(transformed));
            assertArrayEquals(expected.getRGB(0,0,512,512,null,0,512),window[0].panel.image.getRGB(0,0,512,512,null,0,512));
            window[0].close();
        });
    }
    @Test public void freezeBeforeTheFirstRenderUsesTheCapturedTextureCycle() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] window=new InspectionSession[1];
        int[] pixels=new int[64*64];pixels[0]=0xffffff;pixels[1]=0xff0000;
        TextureSet textures=new TextureSet(java.util.Collections.singletonMap(0,new TextureSet.Pixels(pixels,1,1,0,0,100)),100,false);
        ModelSnapshot mesh=ModelSnapshot.capture(new Mesh(),null);
        SwingUtilities.invokeAndWait(() -> {
            // Occupy the scheduler so we can inspect the pending frozen request before it starts.
            scheduler.request("blocker",() -> new InspectionRenderScheduler.Work()
            {
                @Override public void render() {}
                @Override public void failed(Throwable failure) { fail(); }
                @Override public void complete() {}
            });
            window[0]=new InspectionSession(config,scheduler,() -> {},value -> {});
            window[0].accept(new InspectionCapture.Packet(mesh,textures,null,100,0,false));
            LiveViewState.RenderRequest request=window[0].state.begin();
            assertEquals(100,request.texturePhase,0);window[0].state.finish(request);
            window[0].close();
        });
        finish(worker);
    }
    private final ModelPopoutConfig config=new ModelPopoutConfig()
    { @Override public RenderTarget renderTarget() { return RenderTarget.UNLIMITED; } };
    private InspectionCapture.Packet captured(boolean live)
    {
        return new InspectionCapture.Packet(ModelSnapshot.capture(new Mesh(),null),TextureSet.EMPTY,null,100,0,live);
    }
    private void finish(Worker worker) throws Exception
    {
        for (int i=0;i<20 && !worker.tasks.isEmpty();i++)
        { worker.run();SwingUtilities.invokeAndWait(() -> {}); }
        assertTrue("Render requests must drain without a catch-up backlog",worker.tasks.isEmpty());
    }

    @Test public void twoWindowsKeepTheirCamerasImagesAndCloseLifecycleIndependent() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] windows=new InspectionSession[2];AtomicInteger closed=new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            windows[0]=new InspectionSession(config,scheduler,closed::incrementAndGet,value -> {});
            windows[1]=new InspectionSession(config,scheduler,closed::incrementAndGet,value -> {});
            windows[0].accept(captured(true));windows[1].accept(captured(true));
            windows[0].state.setCamera(windows[0].state.camera().drag(40,-10));windows[0].requestRender();
            assertEquals(CameraView.DEFAULT.yaw,windows[1].state.camera().yaw,0);
        });
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            assertNotNull(windows[0].panel.image);assertNotNull(windows[1].panel.image);
            assertNotSame(windows[0].panel.image,windows[1].panel.image);
            windows[0].close();windows[0].close();assertEquals(1,closed.get());assertNull(windows[0].panel.image);
            assertNotNull(windows[1].panel.image);windows[1].close();
        });
        assertEquals(2,closed.get());
    }

    @Test public void frozenGeometryCanRotateAndZoomAfterLogoutWithoutLiveCapture() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] window=new InspectionSession[1];
        SwingUtilities.invokeAndWait(() -> {
            window[0]=new InspectionSession(config,scheduler,() -> {},value -> {});
            window[0].accept(captured(false));
            assertTrue(window[0].panel.status.contains("last captured"));assertNull(window[0].panel.hud);
        });
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            window[0].state.setCamera(window[0].state.camera().drag(50,20));
            window[0].panel.dispatchEvent(new MouseWheelEvent(window[0].panel,MouseWheelEvent.MOUSE_WHEEL,0,0,20,20,0,false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-2));
        });
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(.5,window[0].panel.imageCamera.yaw,1e-9);
            assertTrue(window[0].panel.imageCamera.zoom>1);assertNotNull(window[0].panel.image);window[0].close();
        });
    }

    @Test public void closeRejectsQueuedCaptureAndLateRasterWithoutRevivingResources() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] window=new InspectionSession[1];
        SwingUtilities.invokeAndWait(() -> {
            window[0]=new InspectionSession(config,scheduler,() -> {},value -> {});
            window[0].accept(captured(true));assertEquals(1,worker.tasks.size());
            window[0].close();window[0].accept(captured(true));window[0].requestRender();
        });
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(window[0].closed());assertNull(window[0].panel.image);assertNull(window[0].panel.hud);
            assertFalse(window[0].state.needsRender());assertEquals(0,scheduler.queued());
        });
    }

    @Test public void minimizedWindowStopsQueuedWorkButRetainsTheFinishingFrameForRestore() throws Exception
    {
        Worker worker=new Worker();InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
        InspectionSession[] window=new InspectionSession[1];
        SwingUtilities.invokeAndWait(() -> {
            window[0]=new InspectionSession(config,scheduler,() -> {},value -> {});
            window[0].accept(captured(true));window[0].setVisible(false);
        });
        finish(worker);
        SwingUtilities.invokeAndWait(() -> {
            assertNotNull(window[0].panel.image);assertNull(window[0].panel.hud);
            window[0].setVisible(true);assertTrue(worker.tasks.isEmpty());window[0].close();
        });
    }

    @Test public void unavailableBeforeCaptureShowsAnExplicitEmptyState() throws Exception
    {
        Worker worker=new Worker();
        SwingUtilities.invokeAndWait(() -> {
            InspectionSession window=new InspectionSession(config,new InspectionRenderScheduler(worker,SwingUtilities::invokeLater),() -> {},value -> {});
            window.accept(new InspectionCapture.Packet(null,TextureSet.EMPTY,null,100,0,false));
            assertTrue(window.panel.status.contains("no model captured"));assertNull(window.panel.image);
            assertTrue(worker.tasks.isEmpty());window.close();
        });
    }
}
