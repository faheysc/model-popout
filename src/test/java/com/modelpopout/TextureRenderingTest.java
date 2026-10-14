package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextureRenderingTest
{
    static class TexturedModel extends TestModel
    {
        final float[] x={-80,80,-80}, y={-80,-80,80}, z={0,0,0};
        final short[] textures={0};
        final byte[] alpha={0}, mapping={-1};
        // Match native screen-space winding; the UV basis follows face vertex order.
        final int[] ia={0},ib={2},ic={1};
        @Override public int getVerticesCount(){return 3;}
        @Override public float[] getVerticesX(){return x;}
        @Override public float[] getVerticesY(){return y;}
        @Override public float[] getVerticesZ(){return z;}
        @Override public int getFaceCount(){return 1;}
        @Override public int[] getFaceIndices1(){return ia;}
        @Override public int[] getFaceIndices2(){return ib;}
        @Override public int[] getFaceIndices3(){return ic;}
        @Override public int[] getFaceColors1(){return new int[]{127};}
        @Override public int[] getFaceColors2(){return new int[]{127};}
        @Override public int[] getFaceColors3(){return new int[]{127};}
        @Override public byte[] getFaceTransparencies(){return alpha;}
        @Override public short[] getFaceTextures(){return textures;}
        @Override public byte[] getTextureFaces(){return mapping;}
        @Override public int[] getTexIndices1(){return new int[]{0};}
        @Override public int[] getTexIndices2(){return new int[]{2};}
        @Override public int[] getTexIndices3(){return new int[]{1};}
    }
    static TextureSet texture(int[] pixels)
    {
        return new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(pixels,0,0,0,0)),0,false);
    }
    static TextureProvider provider(Texture[] metadata,int[] pixels)
    {
        return new TextureProvider()
        {
            @Override public double getBrightness(){return .8;}
            @Override public void setBrightness(double value){throw new AssertionError("Must never change native brightness");}
            @Override public Texture[] getTextures(){return metadata;}
            @Override public int[] load(int id){assertEquals(0,id);return pixels;}
            @Override public int getDefaultColor(int id){return 20000;}
        };
    }
    @Test public void referencedTextureCaptureCopiesOnlyOnceAndNeverMutatesProviderPixels()
    {
        int[] source = {0xff0000,0x00ff00,0x0000ff,0xffffff};
        TestTexture metadata = new TestTexture(source,1,2);
        TextureProvider provider = provider(new Texture[]{metadata},source);
        ModelSnapshot mesh = ModelSnapshot.capture(new TexturedModel(),provider);
        TextureSet copied = TextureSet.capture(provider,mesh,mesh,100,true);
        assertEquals(1,copied.textures.size());
        assertEquals(0xff0000,copied.textures.get(0).rgb[0]);
        source[0] = 0;
        assertEquals(0xff0000,copied.textures.get(0).rgb[0]);
        assertEquals(1,copied.textures.get(0).direction);
        assertEquals(2,copied.textures.get(0).speed);
        assertTrue(copied.gpu);
    }

    @Test public void temporaryTextureCacheMissRetainsOwnedPixelsAndDeduplicatesFailedLoads()
    {
        int[] pixels = {0xff0000,0x00ff00,0x0000ff,0xffffff};
        TestTexture metadata = new TestTexture(pixels,1,1);
        TextureProvider loaded = provider(new Texture[]{metadata},pixels);
        ModelSnapshot mesh = ModelSnapshot.capture(new TexturedModel(),loaded);
        TextureSet old = TextureSet.capture(loaded,mesh,mesh,10,true);
        int[] attempts = {0};
        TextureProvider missing = new TextureProvider()
        {
            @Override public double getBrightness(){return .8;}
            @Override public void setBrightness(double b){fail();}
            @Override public Texture[] getTextures(){return new Texture[]{metadata};}
            @Override public int[] load(int id){attempts[0]++;throw new IllegalStateException("temporarily unavailable");}
            @Override public int getDefaultColor(int id){return 0;}
        };
        TextureSet retained = TextureSet.capture(missing,mesh,mesh,11,true,old);
        assertEquals(1,attempts[0]);
        assertSame(old.texture(0),retained.texture(0));
        assertEquals(0xff0000,retained.texture(0).rgb[0]);
        TextureSet cpuOld=TextureSet.capture(loaded,mesh,mesh,10,false);
        TextureSet cpuRetained=TextureSet.capture(missing,mesh,mesh,11,false,cpuOld);
        assertSame(cpuOld.texture(0),cpuRetained.texture(0));
        assertEquals(0xff0000ff,cpuRetained.texture(0).sampleArgb(.25,.25,11,cpuRetained)); // Keeps the old bitmap's own phase origin.

    }

    @Test public void explicitAndImplicitMappingAgreeAndOwnedArraysSurviveNativeMutation()
    {
        TexturedModel nativeModel=new TexturedModel();
        ModelSnapshot implicit=ModelSnapshot.capture(nativeModel,null);
        assertArrayEquals(new double[]{0,0,1,0,0,1},implicit.uvs(),1e-9);
        nativeModel.mapping[0]=0;
        ModelSnapshot explicit=ModelSnapshot.capture(nativeModel,null);
        assertArrayEquals(implicit.uvs(),explicit.uvs(),1e-9);
        nativeModel.mapping[0]=127;nativeModel.x[0]=999;nativeModel.textures[0]=5;
        assertEquals(0,explicit.textureIds[0]);
        assertEquals(-80,explicit.x[0],0);
        assertEquals(0,explicit.textureFaces[0]);
    }
    @Test public void realTexturePixelsReplaceFallbackColorsAndZeroPixelsCutOut()
    {
        ModelSnapshot model=ModelSnapshot.capture(new TexturedModel(),null);
        ModelRenderer renderer=new ModelRenderer();CameraView camera=new CameraView(0,0,1);ModelFrame frame=ModelFrame.fit(model);
        BufferedImage filled=renderer.render(model,camera,frame,texture(new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff}),0);
        assertEquals(0xffff0000,filled.getRGB(140,140));
        assertEquals(0xff0000ff,filled.getRGB(350,100));
        assertEquals(0xff00ff00,filled.getRGB(100,350));
        BufferedImage cutout=renderer.render(model,camera,frame,texture(new int[]{0}),0);
        assertEquals(0,cutout.getRGB(140,140));
        assertEquals(0xffff0000,filled.getRGB(140,140)); // New textures never mutate a published frame.
        assertSame(model.toRgb().projection(camera,frame),model.toRgb().projection(camera,frame));
    }
    @Test public void textureSamplingWrapsAndDoesNotTreatBlackModelDetailsAsCutouts()
    {
        TextureSet.Pixels pixels=new TextureSet.Pixels(new int[]{1,2,3,4},0,0,0,0);
        assertEquals(1,pixels.sample(0,0));
        assertEquals(1,pixels.sample(1,1));
        assertEquals(4,pixels.sample(-.1,-.1));
        assertEquals(2,pixels.sample(.75,.1));
    }
    @Test public void textureIdsAndLightingParticipateInAppearanceComparison()
    {
        TexturedModel nativeModel=new TexturedModel();ModelSnapshot first=ModelSnapshot.capture(nativeModel,null);
        nativeModel.textures[0]=1;
        assertFalse(first.sameAppearance(ModelSnapshot.capture(nativeModel,null)));
        nativeModel.textures[0]=0;nativeModel.mapping[0]=0;
        assertFalse(first.sameAppearance(ModelSnapshot.capture(nativeModel,null)));
    }
    @Test public void priorityTenInterleavesByDepthAndTintDoesNotOverrideTexturedLighting()
    {
        TexturedModel nativeModel = new TexturedModel()
        {
            @Override public int getVerticesCount(){return 9;}
            @Override public float[] getVerticesX(){return new float[9];}
            @Override public float[] getVerticesY(){return new float[9];}
            @Override public float[] getVerticesZ(){return new float[9];}
            @Override public int getFaceCount(){return 3;}
            @Override public int[] getFaceIndices1(){return new int[]{0,3,6};}
            @Override public int[] getFaceIndices2(){return new int[]{1,4,7};}
            @Override public int[] getFaceIndices3(){return new int[]{2,5,8};}
            @Override public int[] getFaceColors1(){return new int[]{127,127,127};}
            @Override public int[] getFaceColors2(){return getFaceColors1();}
            @Override public int[] getFaceColors3(){return getFaceColors1();}
            @Override public short[] getFaceTextures(){return new short[]{0,0,0};}
            @Override public byte[] getFaceTransparencies(){return new byte[3];}
            @Override public byte[] getTextureFaces(){return new byte[]{-1,-1,-1};}
            @Override public byte[] getFaceRenderPriorities(){return new byte[]{10,1,9};}
            @Override public byte getOverrideAmount(){return 64;}
            @Override public byte getOverrideHue(){return 20;}
        };
        ModelSnapshot snapshot = ModelSnapshot.capture(nativeModel,null);
        assertEquals(127,snapshot.colorA[0]); // Textured intensity is not an HSL tint target, even without a provider.
        assertArrayEquals(new int[]{0,1,2},FaceOrdering.order(snapshot,new double[]{10,10,10,0,0,0,-10,-10,-10}));
        assertArrayEquals(new int[]{1,2,0},FaceOrdering.order(snapshot,new double[]{-20,-20,-20,0,0,0,-10,-10,-10}));
    }

    @Test public void brightnessAndModelTransparencyUsePublicMetadata()
    {
        TexturedModel model=new TexturedModel()
        {
            @Override public byte getTransparency(){return (byte)128;}
            @Override public byte[] getFaceRenderPriorities(){return new byte[]{10};}
        };
        model.textures[0]=-1;
        ModelSnapshot snapshot=ModelSnapshot.capture(model,provider(null,null));
        assertEquals(126,snapshot.alpha[0]&255);
        assertEquals(10,snapshot.priorities[0]);
        assertNotEquals(ModelSnapshot.hslToRgb(10000,.6),ModelSnapshot.hslToRgb(10000,.9));
    }
    @Test public void degenerateMappingUsesDefaultColorInsteadOfInventingUvs()
    {
        TexturedModel model=new TexturedModel(){@Override public int[] getTexIndices3(){return new int[]{2};}};
        model.mapping[0]=0;
        ModelSnapshot snapshot=ModelSnapshot.capture(model,null);
        assertTrue(Double.isNaN(snapshot.uvs()[0]));
        BufferedImage image=new ModelRenderer().render(snapshot,new CameraView(0,0,1),ModelFrame.fit(snapshot),texture(new int[]{0xff0000}),0);
        assertNotEquals(0xffff0000,image.getRGB(140,140));
    }
}
