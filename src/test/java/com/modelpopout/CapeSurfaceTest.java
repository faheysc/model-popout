package com.modelpopout;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Opposing cape faces must not compete for the same pixels, including on texture-only frames. */
public class CapeSurfaceTest
{
    private ModelSnapshot cape(boolean hideBack, int transparency, byte priority)
    {
        TextureRenderingTest.TexturedModel model = new TextureRenderingTest.TexturedModel()
        {
            @Override public float[] getVerticesZ(){return new float[]{0,5,11};}
            @Override public int getFaceCount(){return 2;}
            @Override public int[] getFaceIndices1(){return new int[]{0,0};}
            @Override public int[] getFaceIndices2(){return new int[]{2,1};}
            @Override public int[] getFaceIndices3(){return new int[]{1,2};}
            @Override public int[] getFaceColors1(){return new int[]{127,910};}
            @Override public int[] getFaceColors2(){return getFaceColors1();}
            @Override public int[] getFaceColors3(){return getFaceColors1();}
            @Override public short[] getFaceTextures(){return new short[]{0,-1};}
            @Override public byte[] getTextureFaces(){return new byte[]{-1,-1};}
            @Override public byte[] getFaceTransparencies(){return new byte[]{(byte)transparency,hideBack?(byte)255:0};}
            @Override public byte[] getFaceRenderPriorities(){return new byte[]{priority,0};}
        };
        return ModelSnapshot.capture(model,null).toRgb();
    }

    private static int[] pixels(BufferedImage image)
    { return image.getRGB(0,0,512,512,null,0,512); }

    @Test public void reverseCapeFacesNeverContaminateLavaInEitherRasterPath()
    {
        for (byte priority : new byte[]{0,10,11}) for (int alpha : new int[]{0,128})
        {
            ModelSnapshot both=cape(false,alpha,priority),front=cape(true,alpha,priority);
            ModelFrame frame=ModelFrame.fit(both);
            TextureSet textures=new TextureSet(Collections.singletonMap(0,
                new TextureSet.Pixels(new int[]{0xf9b535,0x9c3004,0xffcf61,0x541700},1,2,0,0)),0,false);
            for (double yaw : new double[]{0,.35,.9})
            {
                CameraView camera=new CameraView(yaw,.25,1);
                ModelProjection projection=both.projection(camera,frame);
                TextureRasterPlan plan=projection.rasterPlan(both);
                if(alpha==0)
                {
                    assertNotNull(plan);
                    assertTrue(plan.fragments()>0);
                    assertEquals(front.projection(camera,frame).rasterPlan(front).fragments(),plan.fragments());
                }
                else assertNull(plan); // Exercise the general path's original face alpha blending.
                for(double phase:new double[]{0,.25,.5,1,64})
                {
                    ModelRenderer renderer=new ModelRenderer();
                    int[] expected=pixels(renderer.rasterize(front,front.projection(camera,frame),textures,phase));
                    assertArrayEquals(expected,pixels(renderer.rasterize(both,projection,textures,phase)));
                    assertArrayEquals(expected,pixels(renderer.render(both,camera,frame,textures,phase)));
                    assertTrue(Arrays.stream(expected).anyMatch(p -> (p>>>24)>0));
                }
            }
        }
    }

    @Test public void zeroTexelsDoNotRevealTheCulledRedReverseFace()
    {
        ModelSnapshot both=cape(false,0,(byte)10);
        CameraView camera=new CameraView(0,0,1);ModelFrame frame=ModelFrame.fit(both);
        TextureSet zero=TextureRenderingTest.texture(new int[]{0});
        ModelRenderer renderer=new ModelRenderer();
        assertTrue(Arrays.stream(pixels(renderer.rasterize(both,both.projection(camera,frame),zero,0))).allMatch(p -> p==0));
        assertTrue(Arrays.stream(pixels(renderer.render(both,camera,frame,zero,0))).allMatch(p -> p==0));
        // Turning the camera around must show the game's actual reverse material.
        assertTrue(Arrays.stream(pixels(renderer.render(both,new CameraView(Math.PI,0,1),frame,zero,0))).anyMatch(p -> (p>>>24)==255));
    }

    static final class Fixture
    {
        ModelSnapshot mesh;
        int[] pixels;
        int textureId,direction,speed;
    }

    private void actualCape(String name) throws IOException
    {
        String directory=System.getProperty("modelpopout.capeFixtures");
        Assume.assumeTrue("Optional local game-cache fixtures require modelpopout.capeFixtures",directory!=null);
        Path path=Path.of(directory,name+".json");
        Assume.assumeTrue("Optional local game-cache fixture: "+path,Files.isRegularFile(path));
        String source=Files.readString(path);Fixture fixture=CapeFixtureReader.read(source);
        ModelSnapshot mesh=fixture.mesh.toRgb();ModelFrame frame=ModelFrame.fit(mesh);
        TextureSet textures=new TextureSet(Collections.singletonMap(fixture.textureId,
            new TextureSet.Pixels(fixture.pixels,fixture.direction,fixture.speed,0,0)),0,false);
        assertTrue(textures.animated());
        assertFalse(textures.texture(fixture.textureId).cutouts);
        boolean cached=false,changed=false;
        ModelRenderer renderer=new ModelRenderer();
        for(int angle=0;angle<8;angle++)
        {
            CameraView camera=new CameraView(angle*Math.PI/4,.15,1);
            ModelProjection projection=mesh.projection(camera,frame);
            ModelSnapshot reference=CapeFixtureReader.read(source).mesh.toRgb();
            // Independent native visibility oracle: (xA-xB)*(yC-yB)-(yA-yB)*(xC-xB)>0.
            for(int f=0;f<reference.a.length;f++)
            {
                int a=reference.a[f],b=reference.b[f],c=reference.c[f];
                double facing=(projection.x[a]-projection.x[b])*(projection.y[c]-projection.y[b])
                    -(projection.y[a]-projection.y[b])*(projection.x[c]-projection.x[b]);
                if(facing<=1e-8) reference.colorC[f]=-1;
            }
            TextureRasterPlan plan=projection.rasterPlan(mesh);
            cached |= plan!=null && plan.fragments()>0;
            int[] initial=null;
            for(double phase:new double[]{0,.25,.5,1,64})
            {
                int[] expected=pixels(renderer.rasterize(reference,reference.projection(camera,frame),textures,phase));
                int[] general=pixels(renderer.rasterize(mesh,projection,textures,phase));
                assertArrayEquals(name+" native visible surfaces",expected,general);
                assertArrayEquals(name+" cached/general parity",general,pixels(renderer.render(mesh,camera,frame,textures,phase)));
                if(initial==null) initial=general;
                else changed |= !Arrays.equals(initial,general);
            }
        }
        assertTrue("Actually exercised cached texture fragments",cached);
        assertTrue("Actual lava pixels animate with unchanged geometry",changed);
    }

    @Test public void actualFireCapeCacheAssetMatchesVisibleSurfaceOracle() throws IOException { actualCape("fire-cape"); }
    @Test public void actualInfernalCapeCacheAssetMatchesVisibleSurfaceOracle() throws IOException { actualCape("infernal-cape"); }
}
