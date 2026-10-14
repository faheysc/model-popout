package com.modelpopout;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic triangles test rasterization only; the plugin never displays test fixtures. */
public class ModelRendererTest
{
    private static final CameraView TEST_VIEW = new CameraView(Math.toRadians(165), Math.toRadians(10), 1);

    private ModelSnapshot triangles(int nearColor, int farColor, byte nearAlpha, boolean reverse)
    {
        float[] x = new float[6], y = new float[6], z = new float[6];
        double yaw = Math.toRadians(165), pitch = Math.toRadians(10);
        for (int i = 0; i < 6; i++)
        {
            double sx = i % 3 == 0 ? -100 : i % 3 == 1 ? 100 : 0;
            double sy = i % 3 == 2 ? 100 : -100;
            double sz = i < 3 ? -10 : 10;
            // Inverse of the viewing rotation: overlapping triangles at distinct depths.
            double yy = sy * Math.cos(pitch) + sz * Math.sin(pitch);
            double rz = -sy * Math.sin(pitch) + sz * Math.cos(pitch);
            x[i] = (float) (sx * Math.cos(yaw) - rz * Math.sin(yaw));
            y[i] = (float) yy;
            z[i] = (float) (sx * Math.sin(yaw) + rz * Math.cos(yaw));
        }
        int[] a = {0, 3}, b = reverse ? new int[]{1, 4} : new int[]{2, 5};
        int[] c = reverse ? new int[]{2, 5} : new int[]{1, 4};
        int[] colors = {nearColor, farColor};
        return new ModelSnapshot(x, y, z, a, b, c, colors, colors, colors, new byte[]{nearAlpha, 0});
    }

    @Test public void nearerSurfaceWinsEvenWhenFarFaceIsLast()
    {
        BufferedImage image = new ModelRenderer().renderStatic(triangles(0x00ff00, 0xff0000, (byte) 0, false), TEST_VIEW);
        assertEquals(0x00ff00, image.getRGB(256, 256) & 0xffffff);
        assertEquals(ModelRenderer.BACKGROUND, image.getRGB(0, 0) & 0xffffff);
    }

    @Test public void backFacesAreCulledLikeTheNativeSoftwareRenderer()
    {
        BufferedImage image = new ModelRenderer().renderStatic(triangles(0x00ff00, 0xff0000, (byte) 0, true), TEST_VIEW);
        assertEquals(ModelRenderer.BACKGROUND, image.getRGB(256, 256) & 0xffffff);
    }

    @Test public void translucentNearSurfaceBlendsOverFarSurface()
    {
        BufferedImage image = new ModelRenderer().renderStatic(triangles(0x00ff00, 0xff0000, (byte) 128, false), TEST_VIEW);
        assertEquals(0x807f00, image.getRGB(256, 256) & 0xffffff); // Native GPU alpha uses transparency / 255.
    }

    @Test public void fullyTransparentAndHiddenFacesDoNotOcclude()
    {
        ModelRenderer renderer = new ModelRenderer();
        assertEquals(0xff0000, renderer.renderStatic(triangles(0x00ff00, 0xff0000, (byte) 255, false), TEST_VIEW)
            .getRGB(256, 256) & 0xffffff);
        assertEquals(0xff0000, renderer.renderStatic(triangles(-1, 0xff0000, (byte) 0, false), TEST_VIEW)
            .getRGB(256, 256) & 0xffffff);
    }

    @Test(expected = IllegalArgumentException.class)
    public void excessiveOverdrawIsRejected()
    {
        ModelSnapshot source = triangles(0x00ff00, 0xff0000, (byte) 0, false);
        int[] a = new int[100], b = new int[100], c = new int[100], colors = new int[100];
        java.util.Arrays.fill(b, 2);
        java.util.Arrays.fill(c, 1);
        java.util.Arrays.fill(colors, 0x00ff00);
        new ModelRenderer().renderStatic(new ModelSnapshot(source.x, source.y, source.z,
            a, b, c, colors, colors, colors, new byte[100]),
            new CameraView(TEST_VIEW.yaw, TEST_VIEW.pitch, 3));
    }

    @Test public void degenerateGeometryProducesBackground()
    {
        ModelSnapshot point = new ModelSnapshot(new float[]{0}, new float[]{0}, new float[]{0},
            new int[]{0}, new int[]{0}, new int[]{0}, new int[]{0xff0000}, new int[]{0xff0000},
            new int[]{0xff0000}, new byte[]{0});
        assertEquals(ModelRenderer.BACKGROUND, new ModelRenderer().renderStatic(point).getRGB(256, 256) & 0xffffff);
    }
}
