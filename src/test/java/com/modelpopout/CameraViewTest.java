package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraViewTest
{
    @Test public void yawWrapsThroughFullTurnsInBothDirections()
    {
        assertEquals(.25, new CameraView(Math.PI * 4 + .25, 0, 1).yaw, 1e-12);
        assertEquals(Math.PI * 2 - .25, new CameraView(-.25, 0, 1).yaw, 1e-12);
        assertEquals(0, new CameraView(Math.PI * 2, 0, 1).yaw, 1e-12);
        assertEquals(.5, new CameraView(0, 0, 1).drag(50, 0).yaw, 1e-12);
    }

    @Test public void verticalDragClampsWithoutLimitingYaw()
    {
        CameraView up = CameraView.DEFAULT.drag(1000, -10000);
        CameraView down = CameraView.DEFAULT.drag(-1000, 10000);
        assertEquals(CameraView.MIN_PITCH, up.pitch, 0);
        assertEquals(CameraView.MAX_PITCH, down.pitch, 0);
        assertTrue(up.yaw >= 0 && up.yaw < Math.PI * 2);
        assertTrue(down.yaw >= 0 && down.yaw < Math.PI * 2);
    }

    @Test public void verticalDragIsInvertedWhileHorizontalSensitivityAndZoomStayUnchanged()
    {
        CameraView moved = CameraView.DEFAULT.drag(20,15);
        assertEquals(CameraView.DEFAULT.pitch + .15,moved.pitch,1e-12);
        assertEquals(CameraView.DEFAULT.yaw + .2,moved.yaw,1e-12);
        assertEquals(CameraView.DEFAULT.zoom,moved.zoom,0);
        CameraView back = moved.drag(-20,-15);
        assertEquals(CameraView.DEFAULT.pitch,back.pitch,1e-12);
        assertEquals(CameraView.DEFAULT.yaw,back.yaw,1e-12);
    }

    @Test public void wheelZoomIsReversibleAndBounded()
    {
        CameraView zoomed = CameraView.DEFAULT.wheel(-2);
        assertTrue(zoomed.zoom > 1);
        assertEquals(1, zoomed.wheel(2).zoom, 1e-12);
        assertEquals(CameraView.MIN_ZOOM, CameraView.DEFAULT.wheel(10000).zoom, 0);
        assertEquals(CameraView.MAX_ZOOM, CameraView.DEFAULT.wheel(-10000).zoom, 0);
        assertEquals(CameraView.DEFAULT.yaw, zoomed.yaw, 0);
        assertEquals(CameraView.DEFAULT.pitch, zoomed.pitch, 0);
        assertEquals(1, CameraView.DEFAULT.zoom, 0); // Input never mutates the reset value.
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonFiniteCameraIsRejected()
    {
        new CameraView(Double.NaN, 0, 1);
    }

    /** Colored surfaces exercise front/back/side visibility, not an in-game character fixture. */
    private ModelSnapshot box()
    {
        float[] x = {-40, 40, 40, -40, -40, 40, 40, -40};
        float[] y = {-100, -100, 100, 100, -100, -100, 100, 100};
        float[] z = {-20, -20, -20, -20, 20, 20, 20, 20};
        int[] a = {0, 0, 4, 4, 0, 0, 1, 1, 0, 0, 3, 3};
        int[] b = {1, 2, 6, 7, 3, 7, 5, 6, 4, 5, 2, 6};
        int[] c = {2, 3, 5, 6, 7, 4, 6, 2, 5, 1, 6, 7};
        int[] colors = {0xff0000, 0xff0000, 0x0000ff, 0x0000ff,
            0x00ff00, 0x00ff00, 0xffff00, 0xffff00, 0xff00ff, 0xff00ff, 0x00ffff, 0x00ffff};
        return new ModelSnapshot(x, y, z, a, c, b, colors, colors, colors, new byte[12]);
    }

    @Test public void orbitShowsFrontBackAndBothSideSurfaces()
    {
        ModelRenderer renderer = new ModelRenderer();
        ModelSnapshot mesh = box();
        int[] expected = {0xff0000, 0xffff00, 0x0000ff, 0x00ff00};
        for (int i = 0; i < 4; i++)
        {
            BufferedImage image = renderer.renderStatic(mesh, new CameraView(i * Math.PI / 2, 0, 1));
            assertColor(expected[i], image.getRGB(256, 256));
            assertTrue(visiblePixels(image) > 1000);
        }
        assertColor(0xff0000, renderer.renderStatic(mesh, CameraView.DEFAULT).getRGB(256, 256));
    }

    @Test public void pitchExposesTopAndBottomSurfaces()
    {
        ModelRenderer renderer = new ModelRenderer();
        ModelSnapshot mesh = box();
        assertTrue(colorPixels(renderer.renderStatic(mesh, new CameraView(0, CameraView.MAX_PITCH, 1)), 0xff00ff) > 100);
        assertTrue(colorPixels(renderer.renderStatic(mesh, new CameraView(0, CameraView.MIN_PITCH, 1)), 0x00ffff) > 100);
    }

    @Test public void zoomChangesProjectedSizeWithoutChangingMeshOrCamera()
    {
        ModelSnapshot mesh = box();
        float[] originalX = mesh.x.clone(), originalY = mesh.y.clone(), originalZ = mesh.z.clone();
        int[] originalFaces = mesh.a.clone();
        CameraView view = new CameraView(Math.PI / 2, .2, 1);
        ModelRenderer renderer = new ModelRenderer();
        int small = visiblePixels(renderer.renderStatic(mesh, new CameraView(view.yaw, view.pitch, .5)));
        int large = visiblePixels(renderer.renderStatic(mesh, view));
        assertTrue(large > small * 3);
        assertArrayEquals(originalX, mesh.x, 0);
        assertArrayEquals(originalY, mesh.y, 0);
        assertArrayEquals(originalZ, mesh.z, 0);
        assertArrayEquals(originalFaces, mesh.a);
        // Rendering a fresh capture uses the selected camera without resetting it.
        assertArrayEquals(pixels(renderer.renderStatic(mesh, view)), pixels(renderer.renderStatic(box(), view)));
        assertEquals(Math.PI / 2, view.yaw, 1e-12);
        assertEquals(.2, view.pitch, 0);
    }

    @Test public void fullTurnReturnsSameImageAndProjectionStaysCentered()
    {
        ModelRenderer renderer = new ModelRenderer();
        ModelSnapshot mesh = box();
        assertArrayEquals(pixels(renderer.renderStatic(mesh, new CameraView(0, 0, 1))),
            pixels(renderer.renderStatic(mesh, new CameraView(Math.PI * 2, 0, 1))));
        for (int i = 0; i < 8; i++)
        {
            BufferedImage image = renderer.renderStatic(mesh, new CameraView(i * Math.PI / 4, .3, 1));
            int minX = 512, minY = 512, maxX = -1, maxY = -1;
            for (int y = 0; y < 512; y++) for (int x = 0; x < 512; x++)
            {
                if ((image.getRGB(x, y) & 0xffffff) == ModelRenderer.BACKGROUND) continue;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
            assertEquals(511, minX + maxX, 1);
            assertEquals(511, minY + maxY, 1);
        }
    }

    @Test public void protrudingEquipmentGeometryRemainsVisibleAcrossOrbit()
    {
        ModelSnapshot body = box();
        float[] x = Arrays.copyOf(body.x, 17), y = Arrays.copyOf(body.y, 17), z = Arrays.copyOf(body.z, 17);
        float[] gearX = {-90, -90, -50, 55, 100, 55, -45, 45, 0};
        float[] gearY = {-90, 100, 0, -60, 0, 60, -90, -90, 115};
        float[] gearZ = {-10, -10, -10, -35, -35, -35, 50, 50, 50};
        System.arraycopy(gearX, 0, x, 8, 9);
        System.arraycopy(gearY, 0, y, 8, 9);
        System.arraycopy(gearZ, 0, z, 8, 9);
        int[] a = Arrays.copyOf(body.a, 15), b = Arrays.copyOf(body.b, 15), c = Arrays.copyOf(body.c, 15);
        int[] colors = Arrays.copyOf(body.colorA, 15);
        int[] gearColors = {0xaaaaaa, 0x8844cc, 0x44aa88};
        for (int i = 0; i < 3; i++)
        {
            a[12 + i] = 8 + i * 3; b[12 + i] = 9 + i * 3; c[12 + i] = 10 + i * 3;
            colors[12 + i] = gearColors[i];
        }
        // Stand-ins for protruding weapon, shield, and rear cape geometry, used only in this test.
        ModelSnapshot equipped = new ModelSnapshot(x, y, z, a, b, c, colors, colors, colors, new byte[15]);
        boolean[] seen = new boolean[3];
        ModelRenderer renderer = new ModelRenderer();
        for (int i = 0; i < 16; i++)
        {
            BufferedImage image = renderer.renderStatic(equipped, new CameraView(i * Math.PI / 8, .2, 1));
            for (int gear = 0; gear < 3; gear++) seen[gear] |= colorPixels(image, gearColors[gear]) > 100;
            for (int edge = 0; edge < 512; edge++)
            {
                assertEquals(ModelRenderer.BACKGROUND, image.getRGB(edge, 0) & 0xffffff);
                assertEquals(ModelRenderer.BACKGROUND, image.getRGB(edge, 511) & 0xffffff);
                assertEquals(ModelRenderer.BACKGROUND, image.getRGB(0, edge) & 0xffffff);
                assertEquals(ModelRenderer.BACKGROUND, image.getRGB(511, edge) & 0xffffff);
            }
        }
        assertTrue("Weapon geometry visible", seen[0]);
        assertTrue("Shield geometry visible", seen[1]);
        assertTrue("Cape geometry visible", seen[2]);
    }

    private static int[] pixels(BufferedImage image)
    {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static int visiblePixels(BufferedImage image)
    {
        return (int) Arrays.stream(pixels(image)).filter(p -> (p & 0xffffff) != ModelRenderer.BACKGROUND).count();
    }

    private static long colorPixels(BufferedImage image, int color)
    {
        return Arrays.stream(pixels(image)).filter(p -> (p & 0xffffff) == color).count();
    }

    private static void assertColor(int expected, int actual)
    {
        for (int shift = 0; shift <= 16; shift += 8)
        {
            assertEquals((expected >> shift) & 255, (actual >> shift) & 255, 1);
        }
    }
}
