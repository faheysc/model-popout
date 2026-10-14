package com.modelpopout;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiveViewStateTest
{
    private ModelSnapshot pose(float y, int color, byte alpha)
    {
        return new ModelSnapshot(new float[]{0, 10, -10}, new float[]{y, 10, 10}, new float[]{0, 0, 0},
            new int[]{0}, new int[]{1}, new int[]{2}, new int[]{color}, new int[]{color}, new int[]{color}, new byte[]{alpha});
    }

    @Test public void identicalAppearanceSkipsRenderingButManualCaptureForcesIt()
    {
        LiveViewState state = new LiveViewState();
        assertTrue(state.updateModel(pose(0, 0xff0000, (byte) 0), false));
        LiveViewState.RenderRequest frame = state.begin();
        assertNotNull(frame);
        state.finish(frame);
        assertFalse(state.updateModel(pose(0, 0xff0000, (byte) 0), false));
        assertNull(state.begin());
        assertTrue(state.updateModel(pose(0, 0xff0000, (byte) 0), true));
        assertNotNull(state.begin());
    }

    @Test public void animationColorAndTransparencyChangesAreDetected()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(0, 0xff0000, (byte) 0), false);
        assertTrue(state.updateModel(pose(1, 0xff0000, (byte) 0), false));
        assertTrue(state.updateModel(pose(1, 0x00ff00, (byte) 0), false));
        assertTrue(state.updateModel(pose(1, 0x00ff00, (byte) 100), false));
    }

    @Test public void fallingBehindKeepsOnlyLatestPoseAndCamera()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(0, 0xff0000, (byte) 0), false);
        LiveViewState.RenderRequest first = state.begin();
        CameraView camera = new CameraView(1.5, .5, 2);
        ModelSnapshot last = null;
        for (int i = 1; i <= 1000; i++)
        {
            last = pose(i, 0xff0000, (byte) 0);
            state.updateModel(last, false);
            state.setCamera(camera);
            assertNull(state.begin());
        }
        assertTrue(state.finish(first));
        LiveViewState.RenderRequest latest = state.begin();
        assertSame(last, latest.model);
        assertSame(camera, latest.camera);
        state.finish(latest);
        assertNull(state.begin());
    }

    @Test public void modelUpdatesAndLogoutPreserveCamera()
    {
        LiveViewState state = new LiveViewState();
        CameraView camera = new CameraView(2, -.5, 1.8);
        state.setCamera(camera);
        state.updateModel(pose(0, 0xff0000, (byte) 0), false);
        state.clear();
        assertNull(state.begin());
        state.updateModel(pose(1, 0x00ff00, (byte) 0), false);
        assertSame(camera, state.begin().camera);
    }

    @Test public void staleCompletionCannotReleaseNewSessionRender()
    {
        LiveViewState state = new LiveViewState();
        state.updateModel(pose(0, 0xff0000, (byte) 0), false);
        LiveViewState.RenderRequest old = state.begin();
        state.clear();
        state.updateModel(pose(1, 0x00ff00, (byte) 0), false);
        assertNull(state.begin()); // Closing/reopening cannot add a second real rendering task.
        assertFalse(state.finish(old)); // Releases the slot, but rejects the invalidated image.
        LiveViewState.RenderRequest fresh = state.begin();
        assertNotNull(fresh);
        assertFalse(state.finish(old)); // Duplicate/stale completions cannot release the new task.
        assertNull(state.begin());
        assertTrue(state.finish(fresh));
    }

    @Test public void completedButNotYetPresentedSceneIsInvalidAfterNpcLayoutOrLogoutChanges()
    {
        LiveViewState state = new LiveViewState();state.setNpcEnabled(true);
        state.updateModel(pose(0,0xff0000,(byte)0),false);
        LiveViewState.RenderRequest completed = state.begin();assertTrue(state.finish(completed));
        assertTrue(state.valid(completed));
        state.setNpcEnabled(false);assertFalse(state.valid(completed));
        LiveViewState.RenderRequest replacement = state.begin();assertTrue(state.finish(replacement));
        assertTrue(state.valid(replacement));state.clear();assertFalse(state.valid(replacement));
    }

    @Test public void equivalentCameraDoesNotCreateDuplicateRenderWork()
    {
        LiveViewState state = new LiveViewState();state.updateModel(pose(0,0xff0000,(byte)0),false);
        state.finish(state.begin());
        state.setCamera(new CameraView(CameraView.DEFAULT.yaw,CameraView.DEFAULT.pitch,CameraView.DEFAULT.zoom));
        assertFalse(state.needsRender());
    }

    @Test public void snapshotConstructorOwnsInputArrays()
    {
        float[] x = {0, 10, -10}, y = {0, 10, 10}, z = {0, 0, 0};
        int[] a = {0}, b = {1}, c = {2}, colors = {0xff0000};
        byte[] alpha = {0};
        ModelSnapshot snapshot = new ModelSnapshot(x, y, z, a, b, c, colors, colors, colors, alpha);
        ModelSnapshot original = pose(0, 0xff0000, (byte) 0);
        x[0] = y[0] = z[0] = 99; a[0] = b[0] = c[0] = 99; colors[0] = 0; alpha[0] = 100;
        assertTrue(snapshot.sameAppearance(original));
    }
}
