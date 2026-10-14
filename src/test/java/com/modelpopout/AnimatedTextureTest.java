package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnimatedTextureTest
{
    @Test public void nativeClockDoesNotRunAwayWhenClientLosesFocusOrStopsAdvancing()
    {
        TextureClock clock = new TextureClock();
        clock.observe(100,1000000000L);
        assertEquals(100,clock.phase(1000000000L),0);
        assertEquals(100.5,clock.phase(1010000000L),0);
        assertEquals(101,clock.phase(2000000000L),0);
        assertFalse(clock.advancing(2000000000L));
        clock.observe(100,2000000000L); // Repeated events at the same cycle do not restart free-running motion.
        assertEquals(101,clock.phase(2000000000L),0);
        clock.observe(110,2000000000L);
        assertEquals(110,clock.phase(2000000000L),0);
        clock.clear();
        assertFalse(clock.advancing(3000000000L));
    }

    @Test public void allDirectionsSpeedWrapAndCpuAlreadyScrolledPixelsUseCorrectOffsets()
    {
        int[] pixels=new int[16];
        for(int i=0;i<pixels.length;i++) pixels[i]=0x100000+i+1;
        for(int direction=1;direction<=4;direction++)
        {
            TextureSet.Pixels texture=new TextureSet.Pixels(pixels,direction,1,0,0);
            TextureSet cpu=new TextureSet(Collections.singletonMap(0,texture),50,false);
            assertEquals(0xff000000|pixels[0],texture.sampleArgb(.125,.125,50,cpu));
            int index=direction==1?12:direction==3?4:direction==2?3:1;
            int moved=texture.sampleArgb(.125,.125,51,cpu);
            assertEquals(0xff000000|pixels[index],moved);
            assertEquals(0xff000000|pixels[0],texture.sampleArgb(.125,.125,54,cpu));
            TextureSet gpu=new TextureSet(cpu.textures,50,true);
            assertEquals(moved,texture.sampleArgb(.125,.125,32,gpu)); // One texel of a 4px fixture in GPU's 128-unit clock.
        }
        TextureSet.Pixels fast=new TextureSet.Pixels(pixels,4,2,0,0);
        TextureSet cpu=new TextureSet(Collections.singletonMap(0,fast),0,false);
        assertEquals(0xff000000|pixels[1],fast.sampleArgb(.125,.125,.5,cpu));
    }

    @Test public void fractionalAnimationFiltersRealTexelsAndRetainsCutoutAlpha()
    {
        TextureSet.Pixels texture=new TextureSet.Pixels(new int[]{0xff0000,0,0xff0000,0},4,1,0,0);
        TextureSet set=new TextureSet(Collections.singletonMap(0,texture),0,false);
        int halfway=texture.sampleArgb(.25,.25,.5,set);
        assertEquals(128,halfway>>>24);
        assertEquals(0xff0000,halfway&0xffffff);
        assertEquals(0,texture.sampleArgb(.25,.25,1,set));
    }

    @Test public void uniformTexturesDoNotScheduleDuplicateAnimationFrames()
    {
        TextureSet set = new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(new int[]{0xff0000},1,1,0,0)),0,true);
        assertFalse(set.animated());
    }

    @Test public void cachedAndUncachedRasterizationAgreeIncludingCutoutsAndCoincidentFaces()
    {
        TextureRenderingTest.TexturedModel nativeModel = new TextureRenderingTest.TexturedModel()
        {
            @Override public int getFaceCount() { return 2; }
            @Override public int[] getFaceIndices1() { return new int[]{0,0}; }
            @Override public int[] getFaceIndices2() { return new int[]{2,2}; }
            @Override public int[] getFaceIndices3() { return new int[]{1,1}; }
            @Override public int[] getFaceColors1() { return new int[]{127,20000}; }
            @Override public int[] getFaceColors2() { return getFaceColors1(); }
            @Override public int[] getFaceColors3() { return getFaceColors1(); }
            @Override public short[] getFaceTextures() { return new short[]{0,-1}; }
            @Override public byte[] getFaceTransparencies() { return new byte[]{0,0}; }
            @Override public byte[] getTextureFaces() { return new byte[]{-1,-1}; }
        };
        ModelSnapshot mesh = ModelSnapshot.capture(nativeModel,null).toRgb();
        CameraView camera = new CameraView(0,0,1);ModelFrame frame = ModelFrame.initial(mesh);
        ModelProjection projection = mesh.projection(camera,frame);
        TextureRasterPlan plan = projection.rasterPlan(mesh);
        assertNotNull(plan);
        ModelRenderer renderer = new ModelRenderer();
        for (int[] pixels : new int[][]{{0xff0000,0x00ff00,0x0000ff,0xffffff},{0xff0000,0,0,0xffffff}})
        {
            TextureSet set = new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(pixels,2,1,0,0)),0,false);
            for (double phase : new double[]{0,.25,.5,1})
            {
                BufferedImage cached = plan.render(set,phase);
                BufferedImage plain = renderer.rasterize(mesh,projection,set,phase);
                assertArrayEquals(plain.getRGB(0,0,512,512,null,0,512),cached.getRGB(0,0,512,512,null,0,512));
            }
        }
    }

    @Test public void animatedTextureFramesReuseTheCapturedPoseProjectionAndRasterPlan()
    {
        ModelSnapshot mesh=ModelSnapshot.capture(new TextureRenderingTest.TexturedModel(),null);
        CameraView camera=new CameraView(0,0,1);ModelFrame frame=ModelFrame.initial(mesh);
        int[] pixels={0xff0000,0x00ff00,0x0000ff,0xffffff};
        TextureSet set=new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(pixels,2,1,0,0)),0,false);
        ModelRenderer renderer=new ModelRenderer();
        BufferedImage first=renderer.render(mesh,camera,frame,set,0);
        ModelProjection projection=mesh.toRgb().projection(camera,frame);
        TextureRasterPlan plan=projection.rasterPlan(mesh.toRgb());
        assertNotNull(plan);
        assertTrue(plan.fragments() > 0 && plan.fragments() <= TextureRasterPlan.MAX_FRAGMENTS);
        int[] old=first.getRGB(0,0,512,512,null,0,512);
        BufferedImage later=renderer.render(mesh,camera,frame,set,.5);
        assertFalse(Arrays.equals(old,later.getRGB(0,0,512,512,null,0,512)));
        assertSame(projection,mesh.toRgb().projection(camera,frame));
        assertSame(plan,projection.rasterPlan(mesh.toRgb()));
        assertArrayEquals(old,first.getRGB(0,0,512,512,null,0,512));
        assertSame(later,renderer.render(mesh,camera,frame,set,.5));
    }

    @Test public void texturesAdvanceEvenWithoutNewGeometryAndLifecycleClearInvalidatesOldFrames()
    {
        ModelSnapshot mesh=ModelSnapshot.capture(new TextureRenderingTest.TexturedModel(),null);
        TextureSet textures=new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff},1,1,0,0)),0,true);
        LiveViewState state=new LiveViewState();state.updateModel(mesh,false);state.updateTextures(textures);
        state.setTexturePhase(50);LiveViewState.RenderRequest first=state.begin();state.finish(first);
        assertFalse(state.updateModel(mesh,false));state.setTexturePhase(50.5);
        LiveViewState.RenderRequest next=state.begin();
        assertSame(first.model,next.model);assertSame(first.framing,next.framing);
        assertEquals(50.5,next.texturePhase,0);
        state.clear();assertFalse(state.finish(next));assertFalse(state.hasAnimatedTextures());assertNull(state.begin());
    }
}
