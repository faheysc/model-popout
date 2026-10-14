package com.modelpopout;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Opt-in integration checks: use desktopPresentationTest on the actual desktop/JDK. */
public class NativeWindowPresentationTest
{
    @Test public void inheritedAwtPaintExposesItsClearBeforeChildPainting() throws Exception
    {
        requireDesktop();
        boolean[] supported = {false};
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow capability = ViewerWindow.create();
            capability.applyBackgroundOpacity(0);
            supported[0] = capability.effectiveOpacity() == 0;
            capability.dispose();
        });
        Assume.assumeTrue("Desktop does not support per-pixel translucent windows",supported[0]);
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage screen = new BufferedImage(120,120,BufferedImage.TYPE_INT_ARGB);
            screen.setRGB(10,10,Color.RED.getRGB());
            JFrame legacy = new JFrame();
            legacy.setUndecorated(true);
            legacy.setBackground(new Color(0,0,0,0));
            int[] during = {Color.RED.getRGB()};
            legacy.setContentPane(new JPanel()
            {
                @Override protected void paintComponent(Graphics g)
                {
                    during[0] = screen.getRGB(10,10);
                    g.setColor(Color.GREEN);
                    g.fillRect(10,10,1,1);
                }
            });
            legacy.getRootPane().setDoubleBuffered(false);
            legacy.getContentPane().setBackground(new Color(0,0,0,0));
            legacy.setSize(120,120);
            Graphics2D target = screen.createGraphics();
            try
            {
                legacy.setVisible(true);
                legacy.paint(target);
                assertEquals("Real Window.paint clears the destination before its child paints",0,during[0]);
            }
            finally { target.dispose(); legacy.dispose(); }
        });
    }

    @Test public void viewerTopLevelPaintRetainsPreviousFrameDuringRealAwtClear() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow viewer = ViewerWindow.create();
            BufferedImage screen = new BufferedImage(120,120,BufferedImage.TYPE_INT_ARGB);
            viewer.setSize(120,120);
            Graphics2D target = screen.createGraphics();
            try
            {
                viewer.setVisible(true);
                for (int opacity : new int[]{100,50,0})
                {
                    viewer.applyBackgroundOpacity(opacity);
                    for (int i = 0; i < 50; i++)
                    {
                        screen.setRGB(10,10,Color.RED.getRGB());
                        boolean[] called = {false};
                        viewer.setModelPainter(g -> {
                            called[0] = true;
                            assertTrue("Presentation diagnostic must identify the protected top-level path",viewer.isPaintingWindow());
                            assertEquals("Native destination cleared before completed hierarchy",
                                Color.RED.getRGB(),screen.getRGB(10,10));
                            g.setColor(Color.GREEN);
                            g.fillRect(10,10,1,1);
                        });
                        viewer.paint(target);
                        assertFalse(viewer.isPaintingWindow());
                        assertTrue(called[0]);
                        assertEquals(Color.GREEN.getRGB(),screen.getRGB(10,10));
                    }
                }
            }
            finally { viewer.setModelPainter(g -> {}); target.dispose(); viewer.dispose(); }
        });
    }

    @Test public void savedStackingPreferenceCanChangeAndSurviveWindowRecreation() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            for(int opening=0;opening<2;opening++)
            {
                ViewerWindow viewer=ViewerWindow.create();
                try
                {
                    assertFalse(viewer.isAlwaysOnTop());
                    boolean supported=viewer.isAlwaysOnTopSupported();
                    if(viewer.applyAlwaysOnTop(true))
                    {
                        assertTrue(supported);
                        assertTrue(viewer.isAlwaysOnTop());
                    }
                    viewer.applyAlwaysOnTop(false);
                    assertFalse(viewer.isAlwaysOnTop());
                }
                finally { viewer.dispose(); }
            }
        });
    }

    @Test public void realWindowBackgroundTransitionsKeepAllSwingRepaintsAboveTheBuffer() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow viewer=ViewerWindow.create();
            try
            {
                viewer.setSize(120,120);viewer.setVisible(true);
                viewer.applyBackgroundOpacity(0);
                boolean transparent=!viewer.isOpaque();
                // Public Window API triggers the actual JDK setLayersOpaque(true) path.
                viewer.setBackground(Color.BLACK);
                assertTrue(viewer.isOpaque());
                assertFalse(viewer.getRootPane().isOpaque());
                assertFalse(viewer.getLayeredPane().isOpaque());
                assertFalse(viewer.getContentPane().isOpaque());
                final int[] draws={0};viewer.setModelPainter(g -> {
                    draws[0]++;assertTrue("Swing painting bypassed WindowPresentation",viewer.isPaintingWindow());
                });
                javax.swing.JComponent surface=(javax.swing.JComponent)viewer.getContentPane();
                surface.paintImmediately(0,0,120,120);
                assertEquals("Request is queued at Window, not directly painted by Swing",0,draws[0]);
                viewer.applyBackgroundOpacity(50);
                if(transparent) assertFalse(viewer.isOpaque());
                assertFalse(viewer.getRootPane().isOpaque());assertFalse(surface.isOpaque());
            }
            finally {viewer.setModelPainter(g -> {});viewer.dispose();}
        });
    }

    @Test public void nativeOpacityTransitionsRetainCapableConfigurationAndFinalArgb() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow viewer=ViewerWindow.create();
            BufferedImage target=new BufferedImage(120,120,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=target.createGraphics();
            try
            {
                viewer.setSize(120,120);viewer.setVisible(true);viewer.applyBackgroundOpacity(0);
                Assume.assumeTrue("Desktop has no usable per-pixel translucency",viewer.effectiveOpacity()==0);
                viewer.setModelPainter(layer -> {assertTrue(viewer.isPaintingWindow());layer.setColor(Color.RED);layer.fillRect(60,60,1,1);});
                for(int opacity:new int[]{0,30,50,100,30,0})
                {
                    viewer.applyBackgroundOpacity(opacity);viewer.getRootPane().doLayout();viewer.getContentPane().doLayout();
                    assertEquals(0,viewer.getBackground().getAlpha());assertFalse(viewer.isOpaque());
                    assertTrue(viewer.getGraphicsConfiguration().isTranslucencyCapable());
                    assertFalse(viewer.getRootPane().isOpaque());assertFalse(viewer.getLayeredPane().isOpaque());assertFalse(viewer.getContentPane().isOpaque());
                    viewer.paint(g);
                    assertEquals(BackgroundOpacity.alpha(opacity),target.getRGB(80,80)>>>24);
                    assertEquals(0xffff0000,target.getRGB(60,60));
                }
            }
            finally {viewer.setModelPainter(layer -> {});g.dispose();viewer.dispose();}
        });
    }

    private static void requireDesktop()
    {
        Assume.assumeTrue("Enable with desktopPresentationTest",Boolean.getBoolean("modelpopout.desktopTests"));
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
    }
}
