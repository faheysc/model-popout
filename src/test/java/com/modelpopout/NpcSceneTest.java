package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class NpcSceneTest
{
    private ModelSnapshot pose(float height,int color)
    {
        return new ModelSnapshot(new float[]{-40,40,0},new float[]{-height,-height,0},new float[3],
            new int[]{0},new int[]{2},new int[]{1},new int[]{color},new int[]{color},new int[]{color},new byte[1]);
    }

    private LiveViewState state()
    {
        LiveViewState state = new LiveViewState();
        state.setCamera(new CameraView(0,0,1));
        state.setNpcEnabled(true);
        state.updateModel(pose(100,0xff0000),false);
        return state;
    }

    @Test public void npcAnimationsAndTransformationsKeepBothReferencesAndCameraStable()
    {
        LiveViewState state = state();
        state.updateNpc(pose(120,0x00ff00),7);
        LiveViewState.RenderRequest first = state.begin();
        ModelRenderer renderer = new ModelRenderer();
        BufferedImage initial = renderer.renderScene(first);
        BufferedImage playerRaster = renderer.render(first.model,first.camera,first.framing);
        assertEquals(1024,initial.getWidth());
        int[] left = initial.getRGB(0,0,512,512,null,0,512);
        state.finish(first);
        state.updateNpc(pose(220,0x0000ff),7); // Same NPC object changes its native form and pose.
        LiveViewState.RenderRequest transformed = state.begin();
        assertSame(first.framing,transformed.framing);
        assertSame(first.npcFraming,transformed.npcFraming);
        assertSame(first.camera,transformed.camera);
        BufferedImage changed = renderer.renderScene(transformed);
        assertSame("NPC changes reuse the unchanged player's raster",playerRaster,
            renderer.render(transformed.model,transformed.camera,transformed.framing));
        assertArrayEquals(left,changed.getRGB(0,0,512,512,null,0,512));
        assertFalse(Arrays.equals(initial.getRGB(512,0,512,512,null,0,512),changed.getRGB(512,0,512,512,null,0,512)));
        state.finish(transformed);
        assertFalse(state.updateNpc(pose(220,0x0000ff),7));
        assertNull(state.begin());
    }

    @Test public void switchInvalidatesOldImageAndInitializesOnlyNewNpcFraming()
    {
        LiveViewState state = state();
        state.updateNpc(pose(100,0x00ff00),1);
        LiveViewState.RenderRequest old = state.begin();
        state.updateNpc(pose(300,0x0000ff),2);
        assertNull(state.begin());
        assertFalse(state.finish(old));
        LiveViewState.RenderRequest next = state.begin();
        assertSame(old.framing,next.framing);
        assertNotSame(old.npcFraming,next.npcFraming);
        assertEquals(2,next.npcKey);
        assertFalse(state.removeNpc(1)); // Late removal from an old target cannot remove the new one.
    }

    @Test public void despawnClearsOnlyNpcAndKeepsPairViewportWithoutRefittingPlayer()
    {
        LiveViewState state = state();
        state.updateNpc(pose(120,0x00ff00),7);
        LiveViewState.RenderRequest old = state.begin();
        state.finish(old);
        assertTrue(state.removeNpc(7));
        assertFalse(state.hasNpc());
        LiveViewState.RenderRequest removed = state.begin();
        assertSame(old.framing,removed.framing);
        BufferedImage image = new ModelRenderer().renderScene(removed);
        assertEquals(1024,image.getWidth());
        assertEquals(0,image.getRGB(768,256) >>> 24);
    }

    @Test public void temporaryNpcModelUnavailabilityDoesNotReframeItsNextPose()
    {
        LiveViewState state = state();
        state.updateNpc(pose(120,0x00ff00),7);
        LiveViewState.RenderRequest first = state.begin();
        state.finish(first);
        state.updateNpc(null,7);
        assertFalse(state.hasNpc());
        LiveViewState.RenderRequest unavailable = state.begin();
        state.finish(unavailable);
        state.updateNpc(pose(220,0x00ff00),7);
        assertSame(first.npcFraming,state.begin().npcFraming);
    }

    @Test public void disabledNpcCannotBeRestoredByAnInFlightCapture()
    {
        LiveViewState state = state();
        state.updateNpc(pose(120,0x00ff00),7);
        LiveViewState.RenderRequest old = state.begin();
        state.setNpcEnabled(false);
        state.updateNpc(pose(120,0x00ff00),7);
        assertFalse(state.hasNpc());
        assertFalse(state.finish(old));
        BufferedImage image = new ModelRenderer().renderScene(state.begin());
        assertEquals(512,image.getWidth());
    }

    @Test public void closeOrStopClearsNpcAndInvalidatesBusySceneWithoutQueueingMoreWork()
    {
        LiveViewState state = state();
        state.updateNpc(pose(120,0x00ff00),7);
        LiveViewState.RenderRequest busy = state.begin();
        state.clear();
        assertFalse(state.hasNpc());
        assertNull(state.begin());
        assertFalse(state.finish(busy));
        assertNull(state.begin());
    }
}
