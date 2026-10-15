package com.modelpopout;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowTitleBarTest
{
    static final class Actions implements WindowTitleBar.Actions
    {
        int minimized,closed,maximized,moves;
        Rectangle rectangle=new Rectangle(-900,100,500,400);
        boolean max;
        public void minimize() { minimized++; }
        public void maximize() { maximized++;max=!max; }
        public void close() { closed++; }
        public Rectangle bounds() { return new Rectangle(rectangle); }
        public boolean maximized() { return max; }
        public void move(Rectangle bounds) { moves++;rectangle=bounds; }
    }

    @Test public void buttonsDragAndDoubleClickHaveSeparateTargets() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Actions actions=new Actions();WindowTitleBar bar=new WindowTitleBar(actions);
            bar.minimize.doClick();bar.maximize.doClick();bar.maximize.doClick();bar.close.doClick();
            assertEquals(1,actions.minimized);assertEquals(2,actions.maximized);assertEquals(1,actions.closed);
            assertEquals(0,actions.moves);
            mouse(bar.title,MouseEvent.MOUSE_PRESSED,-800,110,1);
            mouse(bar.title,MouseEvent.MOUSE_DRAGGED,-600,160,1);
            mouse(bar.title,MouseEvent.MOUSE_RELEASED,-600,160,1);
            assertEquals(new Rectangle(-700,150,500,400),actions.rectangle);
            mouse(bar.title,MouseEvent.MOUSE_DRAGGED,500,500,1);assertEquals(1,actions.moves);
            mouse(bar.title,MouseEvent.MOUSE_CLICKED,0,0,2);assertTrue(actions.max);
            mouse(bar.title,MouseEvent.MOUSE_PRESSED,0,0,1);
            mouse(bar.title,MouseEvent.MOUSE_DRAGGED,100,100,1);assertEquals(1,actions.moves);
            for (MouseMotionListener listener:bar.close.getMouseMotionListeners())
                assertNotSame(bar.title.getMouseMotionListeners()[0],listener);
        });
    }

    static void mouse(Component target,int id,int screenX,int screenY,int clicks)
    {
        target.dispatchEvent(new MouseEvent(target,id,0,InputEvent.BUTTON1_DOWN_MASK,10,10,screenX,screenY,clicks,false,MouseEvent.BUTTON1));
    }

    @Test public void chromeIsOpaqueAndModelsCannotCoverItAtEveryOpacity() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface=new ViewerWindow.Surface();
            surface.titleBar=new WindowTitleBar(new Actions());surface.add(surface.titleBar,BorderLayout.NORTH);
            surface.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));surface.setSize(480,300);surface.doLayout();
            surface.titleBar.doLayout();
            surface.modelPainter=g -> {g.setColor(Color.RED);g.fillRect(0,0,480,300);};
            WindowPresentation presentation=new WindowPresentation();
            for (int opacity:new int[]{0,30,50,100})
            {
                surface.opacity=opacity;
                BufferedImage image=new BufferedImage(480,300,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=image.createGraphics();presentation.paint(g,480,300,surface::paint);g.dispose();
                assertEquals(new Color(38,38,38).getRGB(),image.getRGB(7,7));
                assertEquals(Color.RED.getRGB(),image.getRGB(100,100));
                assertEquals("Resize hit targets must remain opaque even on click-through desktops",255,image.getRGB(0,0)>>>24);
                assertFalse(surface.isOpaque());
            }
        });
    }

    @Test public void chromeImmediateRepaintsAscendToPresentation() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface=new ViewerWindow.Surface();
            surface.titleBar=new WindowTitleBar(new Actions());surface.add(surface.titleBar,BorderLayout.NORTH);
            JRootPane root=new JRootPane();root.setContentPane(surface);ViewerWindow.keepPresentationNonOpaque(root);
            assertTrue(surface.isPaintingOrigin());
            assertFalse(root.isOpaque());assertFalse(root.getLayeredPane().isOpaque());
            assertTrue(surface.titleBar.isOpaque());
        });
    }

    @Test public void virtualDesktopGeometryUsesLogicalCoordinatesAndInsets()
    {
        Rectangle monitor=new Rectangle(-1536,-200,1536,864);
        assertEquals(new Rectangle(-1536,-170,1536,794),WindowGeometry.usable(monitor,new Insets(30,0,40,0)));
        assertTrue(WindowGeometry.titleReachable(new Rectangle(-1400,-100,500,400),monitor));
        assertFalse(WindowGeometry.titleReachable(new Rectangle(4000,200,500,400),monitor));
        Rectangle start=new Rectangle(-1400,-100,500,400);
        assertEquals(new Rectangle(-1300,0,500,400),WindowGeometry.move(start,new Point(-1200,-50),new Point(-1100,50)));
        assertEquals(new Rectangle(-1400,-100,500,400),start);
    }
}
