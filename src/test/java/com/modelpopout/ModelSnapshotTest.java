package com.modelpopout;

import net.runelite.api.Model;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelSnapshotTest
{
    private static final class MutableModel extends TestModel
    {
        final float[] x = {-10, 10, 0}, y = {-10, -10, 10}, z = {0, 0, 0};
        final int[] a = {0}, b = {1}, c = {2}, ca = {10000}, cb = {10000}, cc = {-1};
        final byte[] alpha = {0};
        final short[] textures = {-1};
        @Override public int getVerticesCount() { return 3; }
        @Override public float[] getVerticesX() { return x; }
        @Override public float[] getVerticesY() { return y; }
        @Override public float[] getVerticesZ() { return z; }
        @Override public int getFaceCount() { return 1; }
        @Override public int[] getFaceIndices1() { return a; }
        @Override public int[] getFaceIndices2() { return b; }
        @Override public int[] getFaceIndices3() { return c; }
        @Override public int[] getFaceColors1() { return ca; }
        @Override public int[] getFaceColors2() { return cb; }
        @Override public int[] getFaceColors3() { return cc; }
        @Override public byte[] getFaceTransparencies() { return alpha; }
        @Override public short[] getFaceTextures() { return textures; }
        @Override public Model getModel() { return this; }
    }

    @Test public void sharedNativeModelCanAdvanceWithoutMutatingEarlierSnapshot()
    {
        MutableModel model = new MutableModel();
        ModelSnapshot first = ModelSnapshot.capture(model.getModel(), null);
        model.y[0] = -20;
        model.ca[0] = 20000;
        model.alpha[0] = 127;
        ModelSnapshot second = ModelSnapshot.capture(model.getModel(), null);
        assertFalse(first.sameAppearance(second));
        assertEquals(-10, first.y[0], 0);
        assertEquals(10000, first.colorA[0]);
        assertEquals(0, first.alpha[0]);
        assertEquals(-20, second.y[0], 0);
    }

    @Test public void conversionIsDeferredCachedAndDoesNotModifyCapturedColors()
    {
        MutableModel model = new MutableModel();
        ModelSnapshot snapshot = ModelSnapshot.capture(model, null);
        assertEquals(10000, snapshot.colorA[0]);
        ModelSnapshot rgb = snapshot.toRgb();
        assertEquals(ModelSnapshot.hslToRgb(10000), rgb.colorA[0]);
        assertEquals(rgb.colorA[0], rgb.colorB[0]);
        assertEquals(rgb.colorA[0], rgb.colorC[0]);
        assertSame(rgb, snapshot.toRgb());
        assertEquals(10000, model.ca[0]);
        assertEquals(-1, model.cc[0]);
        assertEquals(10000, snapshot.colorA[0]);
    }

    @Test public void hiddenFacesStayHiddenDuringConversion()
    {
        MutableModel model = new MutableModel();
        model.cc[0] = -2;
        assertEquals(-1, ModelSnapshot.capture(model, null).toRgb().colorC[0]);
    }

    @Test public void textureFallbackLightingIsCopiedWithoutLoadingTexturePixels()
    {
        MutableModel model = new MutableModel();
        model.textures[0] = 0;
        model.ca[0] = 64;
        TextureProvider provider = new TextureProvider()
        {
            @Override public int getDefaultColor(int id) { assertEquals(0, id); return 30000; }
            @Override public double getBrightness() { return .8; }
            @Override public void setBrightness(double value) { throw new UnsupportedOperationException(); }
            @Override public Texture[] getTextures() { throw new UnsupportedOperationException(); }
            @Override public int[] load(int id) { throw new UnsupportedOperationException(); }
        };
        ModelSnapshot captured = ModelSnapshot.capture(model, provider);
        int packed = (30000 & 0xff80) | ((30000 & 127) * 64 / 128);
        assertEquals(packed, captured.colorA[0]);
        assertEquals(ModelSnapshot.hslToRgb(packed), captured.toRgb().colorA[0]);
        assertEquals(64, model.ca[0]);
    }

    @Test public void equalSnapshotContentIsDetectedWithoutModelIdentity()
    {
        MutableModel model = new MutableModel();
        ModelSnapshot first = ModelSnapshot.capture(model, null);
        assertTrue(first.sameAppearance(ModelSnapshot.capture(new MutableModel(), null)));
        model.b[0] = 2;
        assertFalse(first.sameAppearance(ModelSnapshot.capture(model, null)));
    }
    @Test(expected = IllegalArgumentException.class)
    public void shortVertexArraysAreRejectedInsteadOfInventingZeroVertices()
    {
        ModelSnapshot.capture(new TextureRenderingTest.TexturedModel()
        {
            @Override public float[] getVerticesX(){return new float[]{1};}
        },null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFaceColorsAreRejectedExplicitly()
    {
        ModelSnapshot.capture(new TextureRenderingTest.TexturedModel()
        {
            @Override public int[] getFaceColors3(){return null;}
        },null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shortOptionalTextureAssignmentsAreRejectedInsteadOfInventingTextureZero()
    {
        ModelSnapshot.capture(new TextureRenderingTest.TexturedModel()
        {
            @Override public short[] getFaceTextures(){return new short[0];}
        },null);
    }

    @Test public void spareCapacityIsAllowedButNeverRetained()
    {
        ModelSnapshot snapshot=ModelSnapshot.capture(new TextureRenderingTest.TexturedModel()
        {
            @Override public float[] getVerticesX(){return new float[]{-80,80,-80,999};}
        },null);
        assertEquals(3,snapshot.x.length);
    }

}
