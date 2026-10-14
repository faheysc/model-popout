package com.modelpopout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class MinimalWindowTest
{
    @Test public void transparentSurfaceHasNoDuplicateControlsOrFooter() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            assertEquals(0,buttons(surface).size());
            assertNoLabels(surface);
            java.awt.BorderLayout layout = (java.awt.BorderLayout) surface.getLayout();
            assertNull(layout.getLayoutComponent(java.awt.BorderLayout.NORTH));
            assertNull(layout.getLayoutComponent(java.awt.BorderLayout.SOUTH));
        });
    }

    @Test public void backgroundFadesWithUiWhileModelsStayOpaqueAndZeroOpacityHoverRecoversControls() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            surface.setSize(120,80);
            surface.doLayout();
            surface.modelPainter = g -> { g.setColor(java.awt.Color.RED); g.fillRect(60,60,1,1); };
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(120,80,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = image.createGraphics();
            try
            {
                for (int opacity : new int[]{100,50,0})
                {
                    surface.opacity = opacity;
                    surface.paint(g);
                    assertEquals(BackgroundOpacity.alpha(opacity),image.getRGB(106,14) >>> 24); // UI background
                    assertEquals(0xffff0000,image.getRGB(60,60));
                }
                surface.reveal = true;
                surface.paint(g);
                assertEquals(255,image.getRGB(106,14) >>> 24);
                assertNull(((java.awt.BorderLayout) surface.getLayout()).getLayoutComponent(java.awt.BorderLayout.SOUTH));
            }
            finally { g.dispose(); }
        });
    }

    @Test public void edgesAndCornersResizeWithoutMovingTheOppositeAnchorOrBreakingMinimumSize()
    {
        Rectangle start = new Rectangle(100,200,600,400);
        Dimension min = new Dimension(480,300);
        for (int edges : new int[]{1,2,4,8,3,9,6,12})
        {
            assertNotEquals(java.awt.Cursor.DEFAULT_CURSOR,WindowGeometry.cursor(edges));
            Rectangle resized = WindowGeometry.resize(start,edges,50,40,min);
            if ((edges & WindowGeometry.WEST) != 0) assertEquals(start.x+start.width,resized.x+resized.width);
            else assertEquals(start.x,resized.x);
            if ((edges & WindowGeometry.NORTH) != 0) assertEquals(start.y+start.height,resized.y+resized.height);
            else assertEquals(start.y,resized.y);
            Rectangle clamped = WindowGeometry.resize(start,edges,
                (edges & WindowGeometry.WEST) != 0 ? 10000 : -10000,
                (edges & WindowGeometry.NORTH) != 0 ? 10000 : -10000,min);
            assertTrue(clamped.width >= min.width);
            assertTrue(clamped.height >= min.height);
        }
        assertEquals(WindowGeometry.NORTH | WindowGeometry.WEST,WindowGeometry.edges(0,0,600,400));
        assertEquals(WindowGeometry.SOUTH | WindowGeometry.EAST,WindowGeometry.edges(599,399,600,400));
        assertEquals(WindowGeometry.WEST,WindowGeometry.edges(2,200,600,400));
        assertEquals(WindowGeometry.EAST,WindowGeometry.edges(598,200,600,400));
        assertEquals(0,WindowGeometry.edges(300,200,600,400));
    }

    private static List<JButton> buttons(Container root)
    {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents())
        {
            if (child instanceof JButton) result.add((JButton) child);
            else if (child instanceof Container) result.addAll(buttons((Container) child));
        }
        return result;
    }

    private static void assertNoLabels(Container root)
    {
        for (Component child : root.getComponents())
        {
            assertFalse(child instanceof JLabel);
            if (child instanceof Container) assertNoLabels((Container) child);
        }
    }
}
