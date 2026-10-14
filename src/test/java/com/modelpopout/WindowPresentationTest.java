package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowPresentationTest
{
    @Test public void surfaceAndButtonDirtyRegionsReachTheNativeWindow() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            // A native-parent stand-in observes the real JComponent.paintImmediately routing.
            NativeParent parent = new NativeParent();
            parent.setSize(100,100);
            JRootPane root = new JRootPane();
            root.setOpaque(false);
            root.getLayeredPane().setOpaque(false);
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            JButton button = new JButton("Move");
            surface.add(button);
            root.setContentPane(surface);
            parent.add(root);
            root.setSize(100,100);
            root.doLayout();
            surface.setSize(100,100);
            surface.doLayout();
            root.addNotify();
            try
            {
                parent.repaints = 0;
                surface.paintImmediately(0,0,100,100);
                assertEquals(1,parent.repaints);
                parent.repaints = 0;
                button.paintImmediately(0,0,20,20);
                assertEquals(1,parent.repaints);
            }
            finally { root.removeNotify(); }
        });
    }

    @Test public void topLevelClearMustStayPrivateUntilHierarchyFinishes() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage screen = new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
            screen.setRGB(6,6,0xffff0000);
            Graphics2D target = screen.createGraphics();
            int[] observedDuringPaint = {0};
            Consumer<Graphics2D> hierarchy = g -> {
                // The installed JDK's Window.paint starts with this operation for !isOpaque().
                Graphics2D clear = (Graphics2D) g.create();
                try
                {
                    clear.setComposite(AlphaComposite.Src);
                    clear.setColor(new Color(0,0,0,0));
                    clear.fillRect(0,0,12,12);
                }
                finally { clear.dispose(); }
                observedDuringPaint[0] = screen.getRGB(6,6);
                g.setColor(Color.GREEN);
                g.fillRect(6,6,1,1);
            };
            try
            {
                // Reproduce the old path: inner surface buffering cannot undo the earlier clear.
                hierarchy.accept(target);
                assertEquals(0,observedDuringPaint[0]);
                assertEquals(0xff00ff00,screen.getRGB(6,6));
                screen.setRGB(6,6,0xffff0000);
                new WindowPresentation().paint(target,12,12,hierarchy);
                assertEquals("Previous completed frame must remain visible during painting",
                    0xffff0000,observedDuringPaint[0]);
                assertEquals(0xff00ff00,screen.getRGB(6,6));
                assertEquals(0,screen.getRGB(5,5));
            }
            finally { target.dispose(); }
        });
    }

    @Test public void fixedFramesAndNewPublicationsNeverExposeAnIntermediateClear() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            surface.setSize(20,20);
            JButton header = new JButton("Close");
            surface.add(header);
            surface.doLayout();
            BufferedImage[] completed = {model(5,Color.RED)};
            surface.modelPainter = g -> g.drawImage(completed[0],0,0,null);
            WindowPresentation presentation = new WindowPresentation();
            BufferedImage screen = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
            Graphics2D target = screen.createGraphics();
            try
            {
                for (boolean nativeOpaque : new boolean[]{false,true})
                    for (int opacity : new int[]{100,50,0})
                    {
                        surface.opacity = opacity;
                        completed[0] = model(5,Color.RED);
                        Consumer<Graphics2D> hierarchy = g -> {
                            if (!nativeOpaque)
                            {
                                g.setComposite(AlphaComposite.Src);
                                g.setColor(new Color(0,0,0,0));
                                g.fillRect(0,0,20,20);
                                g.setComposite(AlphaComposite.SrcOver);
                            }
                            surface.paint(g);
                        };
                        presentation.paint(target,20,20,hierarchy);
                        BufferedImage previous = copy(screen);
                        for (int i = 0; i < 100; i++)
                        {
                            if (i >= 50) completed[0] = model(5 + i % 2,i % 2 == 0 ? Color.RED : Color.GREEN);
                            BufferedImage last = previous;
                            presentation.paint(target,20,20,g -> {
                                // A private clear must not alter any already-presented model/UI pixel.
                                g.setComposite(AlphaComposite.Clear);
                                g.fillRect(0,0,20,20);
                                g.setComposite(AlphaComposite.SrcOver);
                                hierarchy.accept(g);
                                assertPixelsEqual(last,screen);
                            });
                            if (i < 50) assertPixelsEqual(previous,screen);
                            int modelX = i < 50 ? 5 : 5 + i % 2;
                            assertEquals(255,screen.getRGB(modelX,15) >>> 24);
                            previous = copy(screen);
                        }
                    }
            }
            finally { target.dispose(); }
        });
    }

    @Test public void partialDamageResizeAndFailedPaintRetainCompletedPixels() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            WindowPresentation presentation = new WindowPresentation();
            BufferedImage screen = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);
            Graphics2D target = screen.createGraphics();
            try
            {
                presentation.paint(target,40,40,g -> { g.setColor(Color.RED); g.fillRect(0,0,40,40); });
                target.setClip(10,10,10,10);
                presentation.paint(target,40,40,g -> { g.setColor(Color.GREEN); g.fillRect(0,0,40,40); });
                assertEquals(0xffff0000,screen.getRGB(0,0));
                assertEquals(0xff00ff00,screen.getRGB(15,15));
                BufferedImage previous = copy(screen);
                try
                {
                    presentation.paint(target,40,40,g -> { g.fillRect(0,0,40,40); throw new IllegalStateException("paint failed"); });
                    fail("Expected paint failure");
                }
                catch (IllegalStateException expected) { assertEquals("paint failed",expected.getMessage()); }
                assertPixelsEqual(previous,screen);
                target.setClip(null);
                for (int size : new int[]{20,40,10,40})
                {
                    presentation.paint(target,size,size,g -> { g.setColor(Color.BLUE); g.fillRect(size-1,size-1,1,1); });
                    assertEquals(0,screen.getRGB(0,0));
                    assertEquals(0xff0000ff,screen.getRGB(size-1,size-1));
                }
            }
            finally { target.dispose(); }
        });
    }

    private static BufferedImage model(int x, Color color)
    {
        BufferedImage image = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(x,15,color.getRGB());
        image.setRGB(x+5,15,0xff000000); // NPC/black model detail must also retain full alpha.
        return image;
    }

    private static BufferedImage copy(BufferedImage source)
    {
        BufferedImage image = new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,source.getWidth(),source.getHeight(),source.getRGB(0,0,source.getWidth(),source.getHeight(),null,0,source.getWidth()),0,source.getWidth());
        return image;
    }

    private static void assertPixelsEqual(BufferedImage expected, BufferedImage actual)
    {
        assertArrayEquals(expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth()),
            actual.getRGB(0,0,actual.getWidth(),actual.getHeight(),null,0,actual.getWidth()));
    }

    private static final class NativeParent extends Container
    {
        int repaints;
        @Override public boolean isShowing() { return true; }
        @Override public void repaint(long delay, int x, int y, int width, int height) { repaints++; }
    }
}
