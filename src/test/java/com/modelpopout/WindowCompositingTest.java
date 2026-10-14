package com.modelpopout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowCompositingTest
{
    @Test public void controlsAndModelsHaveIndependentAlphaAndReplacementClearsOldPixels() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            JLabel title = new JLabel("Title");
            title.setOpaque(true);
            title.setBackground(Color.BLUE);
            surface.add(title,BorderLayout.NORTH);
            surface.setSize(40,40);
            surface.doLayout();
            BufferedImage[] frame = {new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB)};
            frame[0].setRGB(20,30,0xffff0000);
            frame[0].setRGB(21,30,0xff00ff00); // second actor
            surface.modelPainter = g -> g.drawImage(frame[0],0,0,null);
            BufferedImage target = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = target.createGraphics();
            try
            {
                assertTrue(surface.isPaintingOrigin());
                for (int opacity : new int[]{100,50,0})
                {
                    surface.opacity = opacity;
                    for (int i = 0; i < 50; i++)
                    {
                        surface.paint(g);
                        assertEquals(BackgroundOpacity.alpha(opacity),target.getRGB(39,1) >>> 24);
                        assertEquals(0xffff0000,target.getRGB(20,30));
                        assertEquals(0xff00ff00,target.getRGB(21,30));
                    }
                }
                surface.reveal = true;
                surface.paint(g);
                assertEquals(255,target.getRGB(39,1) >>> 24);
                surface.reveal = false;
                // A privately constructed replacement cannot change the published frame.
                BufferedImage replacement = new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);
                replacement.setRGB(25,30,0xff000000);
                surface.paint(g);
                assertEquals(0xffff0000,target.getRGB(20,30));
                frame[0] = replacement;
                surface.paint(g);
                assertEquals(0,target.getRGB(20,30));
                assertEquals(0xff000000,target.getRGB(25,30));
                assertEquals(1,surface.getComponentCount());
            }
            finally { g.dispose(); }
        });
    }

    @Test public void wholeWindowArgbTransitionsPreserveUiAlphaAndOpaqueModels() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface=new ViewerWindow.Surface();surface.setSize(40,40);
            JLabel control=new JLabel("UI");control.setOpaque(true);control.setBackground(Color.BLUE);
            surface.add(control,BorderLayout.NORTH);surface.doLayout();
            surface.modelPainter=g -> {g.setColor(Color.RED);g.fillRect(10,30,1,1);g.setColor(Color.BLACK);g.fillRect(20,30,1,1);};
            WindowPresentation presentation=new WindowPresentation();
            BufferedImage target=new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);Graphics2D g=target.createGraphics();
            try
            {
                for(int opacity:new int[]{100,30,0,50,100,0,30})
                {
                    surface.opacity=opacity;
                    for(int frame=0;frame<20;frame++)
                    {
                        presentation.paint(g,40,40,surface::paint);
                        assertEquals("Background alpha must survive the final presentation blit",BackgroundOpacity.alpha(opacity),target.getRGB(39,25)>>>24);
                        assertEquals("Controls use the UI alpha",BackgroundOpacity.alpha(opacity),target.getRGB(39,1)>>>24);
                        assertEquals(0xffff0000,target.getRGB(10,30));assertEquals(0xff000000,target.getRGB(20,30));
                        assertFalse(surface.isOpaque());
                    }
                }
            }
            finally {g.dispose();}
        });
    }

    @Test public void resizingReallocatesLayersAndPreservesPublishedModel() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            surface.opacity = 0;
            surface.modelPainter = g -> { g.setColor(Color.RED); g.fillRect(surface.getWidth()-1,surface.getHeight()-1,1,1); };
            for (int size : new int[]{20,80,10,40})
            {
                surface.setSize(size,size);
                surface.doLayout();
                BufferedImage target = new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = target.createGraphics();
                try { surface.paint(g); }
                finally { g.dispose(); }
                assertEquals(0,target.getRGB(0,0));
                assertEquals(0xffff0000,target.getRGB(size-1,size-1));
            }
        });
    }
}
