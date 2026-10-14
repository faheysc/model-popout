package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class StableFramingTest
{
    private static final int TORSO = 0xee5533, AXE = 0x33aaee;

    /** The torso never moves; the axe changes both vertical and horizontal bounds. */
    private ModelSnapshot pose(float axeY, float reach)
    {
        return new ModelSnapshot(new float[]{-20,20,-20,20,20,-20,reach,reach+25,reach+10},
            new float[]{-100,-100,0,-100,0,0,axeY,axeY,axeY+30},new float[9],
            new int[]{0,3,6},new int[]{2,5,8},new int[]{1,4,7},
            new int[]{TORSO,TORSO,AXE},new int[]{TORSO,TORSO,AXE},
            new int[]{TORSO,TORSO,AXE},new byte[3]);
    }

    private BufferedImage render(ModelRenderer renderer, LiveViewState.RenderRequest request)
    {
        return renderer.render(request.model, request.camera, request.framing);
    }

    @Test public void unmovingTorsoStaysAtSamePixelPositionAndSizeWhileAxeSwings()
    {
        LiveViewState state = new LiveViewState();
        state.setCamera(new CameraView(0, 0, 1));
        state.updateModel(pose(-80,45), false);
        LiveViewState.RenderRequest first = state.begin();
        ModelRenderer renderer = new ModelRenderer();
        boolean[] baseline = colorMask(render(renderer, first), TORSO);
        int[] originalBounds = colorBounds(render(renderer, first), TORSO);
        assertTrue(originalBounds[2] > originalBounds[0]);
        assertTrue(originalBounds[3] > originalBounds[1]);
        state.finish(first);
        for (int swing = 1; swing <= 12; swing++)
        {
            state.updateModel(pose(-80 - swing * 12,45 + swing * 5), false);
            LiveViewState.RenderRequest next = state.begin();
            assertNotNull(next);
            assertSame(first.framing, next.framing);
            assertSame(first.camera, next.camera);
            BufferedImage image = render(renderer, next);
            assertTrue("Stable body pixels despite changing animated bounds",
                Arrays.equals(baseline, colorMask(image, TORSO)));
            assertArrayEquals(originalBounds, colorBounds(image, TORSO));
            assertEquals(first.framing.scale(first.camera), next.framing.scale(next.camera), 0);
            state.finish(next);
        }
    }

    @Test public void extendedAnimationFitsReservedFrameAtDefaultZoomDuringOrbit()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(-80,45), false);
        LiveViewState.RenderRequest first = state.begin();
        state.finish(first);
        state.updateModel(pose(-210,100), false);
        ModelRenderer renderer = new ModelRenderer();
        for (int angle = 0; angle < 16; angle++)
        {
            state.setCamera(new CameraView(angle * Math.PI / 8, Math.toRadians(angle % 2 == 0 ? 75 : -75), 1));
            LiveViewState.RenderRequest request = state.begin();
            assertSame(first.framing, request.framing);
            assertVerticesInside(request);
            BufferedImage image = render(renderer, request);
            for (int edge = 0; edge < ModelRenderer.SIZE; edge++)
            {
                assertEquals(0, image.getRGB(edge,0) >>> 24);
                assertEquals(0, image.getRGB(edge,511) >>> 24);
                assertEquals(0, image.getRGB(0,edge) >>> 24);
                assertEquals(0, image.getRGB(511,edge) >>> 24);
            }
            state.finish(request);
        }
        state.setCamera(new CameraView(0,0,1));
        BufferedImage front = render(renderer,state.begin());
        int[] axeBounds = colorBounds(front,AXE);
        assertTrue("Raised axe visible",axeBounds[2] >= axeBounds[0]);
        assertTrue(axeBounds[1] > 0 && axeBounds[3] < 511);
    }

    @Test public void manualCaptureEquipmentChangeAndTemporaryModelLossDoNotRefit()
    {
        LiveViewState state = new LiveViewState();
        CameraView camera = new CameraView(1, .5, 1.5);
        state.setCamera(camera);
        state.updateModel(pose(-80,45),false);
        LiveViewState.RenderRequest first = state.begin();
        state.finish(first);
        state.updateModel(pose(-210,100),true);
        LiveViewState.RenderRequest manual = state.begin();
        assertSame(first.framing,manual.framing);
        assertSame(camera,manual.camera);
        state.finish(manual);
        state.clear();
        state.updateModel(pose(-180,90),false);
        LiveViewState.RenderRequest restored = state.begin();
        assertSame(first.framing,restored.framing);
        assertSame(camera,restored.camera);
    }

    @Test public void newViewerSessionEstablishesNewReferenceWithoutChangingOldRequest()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(-80,45),false);
        LiveViewState.RenderRequest old = state.begin();
        double oldCenter = old.framing.centerY;
        state.resetFraming();
        state.updateModel(pose(-210,100),false);
        assertNull(state.begin()); // Retains actual worker backpressure across reopen.
        assertFalse(state.finish(old));
        LiveViewState.RenderRequest fresh = state.begin();
        assertNotSame(old.framing,fresh.framing);
        assertTrue(oldCenter != fresh.framing.centerY);
        assertEquals(oldCenter,old.framing.centerY,0);
    }

    @Test public void manualRotationAndZoomChangeProjectionWithoutChangingReference()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(-80,45),false);
        LiveViewState.RenderRequest first = state.begin();
        double originalScale = first.framing.scale(first.camera);
        state.finish(first);
        CameraView turned = first.camera.drag(50,-20).wheel(-2);
        state.setCamera(turned);
        LiveViewState.RenderRequest rotated = state.begin();
        assertSame(first.framing,rotated.framing);
        assertSame(turned,rotated.camera);
        assertEquals(originalScale * turned.zoom, rotated.framing.scale(turned),1e-12);
        assertTrue(first.camera.yaw != turned.yaw);
        assertTrue(first.camera.pitch != turned.pitch);
        state.finish(rotated);
        state.updateModel(pose(-210,100),false);
        LiveViewState.RenderRequest animated = state.begin();
        assertSame(turned,animated.camera);
        assertEquals(rotated.framing.scale(turned),animated.framing.scale(turned),0);
    }

    private void assertVerticesInside(LiveViewState.RenderRequest request)
    {
        ModelSnapshot mesh = request.model;
        ModelFrame frame = request.framing;
        // The reference sphere encloses the reserved motion, guaranteeing containment for any orbit.
        double usableRadius = ModelRenderer.SIZE / 2.0 / frame.scale(request.camera);
        for(int i = 0; i < mesh.x.length; i++)
        {
            double x = mesh.x[i]-frame.centerX, y = mesh.y[i]-frame.centerY, z = mesh.z[i]-frame.centerZ;
            assertTrue(Math.sqrt(x*x+y*y+z*z) < usableRadius);
        }
    }

    private boolean[] colorMask(BufferedImage image, int color)
    {
        boolean[] mask = new boolean[512*512];
        for(int y = 0; y < 512; y++) for(int x = 0; x < 512; x++)
            mask[y*512+x] = (image.getRGB(x,y) & 0xffffff) == color;
        return mask;
    }

    private int[] colorBounds(BufferedImage image, int color)
    {
        int minX=512,minY=512,maxX=-1,maxY=-1;
        for(int y = 0; y < 512; y++) for(int x = 0; x < 512; x++)
        {
            if((image.getRGB(x,y) & 0xffffff) != color) continue;
            minX=Math.min(minX,x); minY=Math.min(minY,y);
            maxX=Math.max(maxX,x); maxY=Math.max(maxY,y);
        }
        return new int[]{minX,minY,maxX,maxY};
    }
}
