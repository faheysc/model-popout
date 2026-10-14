package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;

/** Synthetic benchmark only: no game art or claimed KDE/real-model FPS. */
public final class TextureRenderBenchmark
{
    private static volatile int sink;
    public static void main(String[] args)
    {
        TextureRenderingTest.TexturedModel source = new TextureRenderingTest.TexturedModel()
        {
            @Override public int getVerticesCount(){return 6;}
            @Override public float[] getVerticesX(){return new float[]{-80,80,-80,-160,160,-160};}
            @Override public float[] getVerticesY(){return new float[]{-80,-80,80,-160,-160,160};}
            @Override public float[] getVerticesZ(){return new float[]{-10,-10,-10,10,10,10};}
            @Override public int getFaceCount(){return 20;}
            @Override public int[] getFaceIndices1(){int[] a=new int[20];Arrays.fill(a,3);a[0]=0;return a;}
            @Override public int[] getFaceIndices2(){int[] b=new int[20];Arrays.fill(b,5);b[0]=2;return b;}
            @Override public int[] getFaceIndices3(){int[] c=new int[20];Arrays.fill(c,4);c[0]=1;return c;}
            @Override public int[] getFaceColors1(){int[] a=new int[20];Arrays.fill(a,20000);a[0]=127;return a;}
            @Override public int[] getFaceColors2(){return getFaceColors1();}
            @Override public int[] getFaceColors3(){return getFaceColors1();}
            @Override public short[] getFaceTextures(){short[] a=new short[20];Arrays.fill(a,(short)-1);a[0]=0;return a;}
            @Override public byte[] getFaceTransparencies(){return new byte[20];}
            @Override public byte[] getTextureFaces(){byte[] a=new byte[20];Arrays.fill(a,(byte)-1);return a;}
        };
        ModelSnapshot mesh=ModelSnapshot.capture(source,null).toRgb();
        CameraView camera=new CameraView(0,0,1);ModelFrame frame=ModelFrame.initial(mesh);
        ModelProjection projection=mesh.projection(camera,frame);
        TextureRasterPlan plan=projection.rasterPlan(mesh);
        if(plan==null) throw new AssertionError("Fixture did not create a bounded raster plan");
        int[] texels=new int[128*128];
        for(int y=0;y<128;y++) for(int x=0;x<128;x++) texels[y*128+x]=((x+1)<<16)|((y+1)<<8)|1;
        TextureSet textures=new TextureSet(Collections.singletonMap(0,new TextureSet.Pixels(texels,1,2,0,0)),0,true);
        ModelRenderer renderer=new ModelRenderer();
        for(int i=0;i<30;i++) { consume(plan.render(textures,i/4.0));consume(renderer.rasterize(mesh,projection,textures,i/4.0)); }
        int frames=150;long uncached=0,cached=0;
        // Alternate paths to reduce simple warmup/order bias.
        for(int i=0;i<frames;i++)
        {
            long start=System.nanoTime();consume(renderer.rasterize(mesh,projection,textures,i/4.0));uncached+=System.nanoTime()-start;
            start=System.nanoTime();consume(plan.render(textures,i/4.0));cached+=System.nanoTime()-start;
        }
        System.out.printf(java.util.Locale.ROOT,"Synthetic 20-face, 128x128 moving-texture fixture; %d cached fragments.%n",plan.fragments());
        System.out.printf(java.util.Locale.ROOT,"%d frames/path: general raster %.3f ms/frame; cached texture-only %.3f ms/frame; ratio %.2fx.%n",frames,uncached/1e6/frames,cached/1e6/frames,(double)uncached/cached);
        System.out.println("Not capture FPS, not real-model throughput, and not native presentation/display FPS.");
    }
    private static void consume(BufferedImage image){sink^=image.getRGB(256,256);}
}
