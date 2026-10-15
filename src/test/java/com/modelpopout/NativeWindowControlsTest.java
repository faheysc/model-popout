package com.modelpopout;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Run on each target desktop; headless geometry does not establish native compatibility. */
public class NativeWindowControlsTest
{
    private static void resizeMouse(Component component,int id,int x,int y,int screenX,int screenY)
    {
        component.dispatchEvent(new java.awt.event.MouseEvent(component,id,0,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
            x,y,screenX,screenY,1,false,java.awt.event.MouseEvent.BUTTON1));
    }

    @Test public void nativeResizeEdgesAndCorners() throws Exception
    {
        Assume.assumeTrue(Boolean.getBoolean("modelpopout.desktopTests"));
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ViewerWindow[] holder=new ViewerWindow[1];
        Robot desktop=new Robot();
        try
        {
            SwingUtilities.invokeAndWait(() -> {
                holder[0]=ViewerWindow.create();holder[0].setMinimumSize(new Dimension(300,300));
                holder[0].setBounds(100,100,480,360);holder[0].setVisible(true);
            });
            for (int edge:new int[]{1,2,4,8,3,9,6,12})
            {
                SwingUtilities.invokeAndWait(() -> holder[0].setBounds(100,100,480,360));
                desktop.waitForIdle();
                Rectangle[] expected=new Rectangle[1];
                SwingUtilities.invokeAndWait(() -> {
                    ViewerWindow frame=holder[0];frame.validate();Rectangle start=frame.getBounds();
                    Component surface=frame.getContentPane();
                    int x=(edge & WindowGeometry.WEST)!=0 ? 1 : (edge & WindowGeometry.EAST)!=0 ? surface.getWidth()-1 : surface.getWidth()/2;
                    int y=(edge & WindowGeometry.NORTH)!=0 ? 1 : (edge & WindowGeometry.SOUTH)!=0 ? surface.getHeight()-1 : surface.getHeight()/2;
                    resizeMouse(surface,java.awt.event.MouseEvent.MOUSE_PRESSED,x,y,start.x+x,start.y+y);
                    resizeMouse(surface,java.awt.event.MouseEvent.MOUSE_DRAGGED,x,y,start.x+x+20,start.y+y+15);
                    resizeMouse(surface,java.awt.event.MouseEvent.MOUSE_RELEASED,x,y,start.x+x+20,start.y+y+15);
                    expected[0]=WindowGeometry.resize(start,edge,20,15,frame.getMinimumSize());
                });
                desktop.waitForIdle();
                SwingUtilities.invokeAndWait(() -> assertEquals("resize edge "+edge,expected[0],holder[0].getBounds()));
            }
        }
        finally { SwingUtilities.invokeAndWait(() -> { if (holder[0]!=null) holder[0].dispose(); }); }
    }

    @Test public void independentWindowsRestoreBoundsAndCloseThroughNormalLifecycle() throws Exception
    {
        Assume.assumeTrue(Boolean.getBoolean("modelpopout.desktopTests"));
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ViewerWindow[] windows=new ViewerWindow[2];AtomicInteger closed=new AtomicInteger();
        Robot desktop=new Robot();Rectangle[] normalBounds=new Rectangle[1];
        try
        {
            SwingUtilities.invokeAndWait(() -> {
                for (int i=0;i<2;i++)
                {
                    ViewerWindow frame=ViewerWindow.create();windows[i]=frame;
                    frame.setMinimumSize(new Dimension(300,300));frame.setBounds(100+i*80,100+i*80,480,360);
                    frame.addWindowListener(new java.awt.event.WindowAdapter()
                    { @Override public void windowClosed(WindowEvent e) { closed.incrementAndGet(); } });
                    frame.setVisible(true);
                }
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                ViewerWindow frame=windows[0];Rectangle normal=frame.getBounds();normalBounds[0]=normal;
                WindowTitleBar bar=(WindowTitleBar)((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                assertEquals("Model Popout",bar.title.getText());
                WindowTitleBarTest.mouse(bar.title,java.awt.event.MouseEvent.MOUSE_PRESSED,normal.x+30,normal.y+15,1);
                WindowTitleBarTest.mouse(bar.title,java.awt.event.MouseEvent.MOUSE_DRAGGED,normal.x+70,normal.y+45,1);
                WindowTitleBarTest.mouse(bar.title,java.awt.event.MouseEvent.MOUSE_RELEASED,normal.x+70,normal.y+45,1);
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                Rectangle normal=normalBounds[0];
                assertEquals(new Point(normal.x+40,normal.y+30),windows[0].getLocation());
                windows[0].setBounds(normal);
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                WindowTitleBar bar=(WindowTitleBar)((BorderLayout)windows[0].getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                bar.maximize.doClick();assertEquals(normalBounds[0],windows[0].normalBounds());
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                WindowTitleBar bar=(WindowTitleBar)((BorderLayout)windows[0].getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                bar.maximize.doClick();
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                ViewerWindow frame=windows[0];
                WindowTitleBar bar=(WindowTitleBar)((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                assertEquals(normalBounds[0],frame.getBounds());
                // Real title and button dirty paints must never bypass the top-level buffer.
                frame.setModelPainter(g -> assertTrue(frame.isPaintingWindow()));
                bar.close.paintImmediately(0,0,32,26);bar.title.paintImmediately(0,0,80,26);
                bar.minimize.doClick();
            });
            // Allow the window manager to process iconification outside the EDT.
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                assertTrue((windows[0].getExtendedState() & Frame.ICONIFIED)!=0);
                windows[0].setExtendedState(Frame.NORMAL);
            });
            desktop.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0,windows[0].getExtendedState() & Frame.ICONIFIED);
                WindowTitleBar bar=(WindowTitleBar)((BorderLayout)windows[0].getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                bar.close.doClick();assertFalse(windows[0].isDisplayable());assertTrue(windows[1].isDisplayable());
            });
            SwingUtilities.invokeAndWait(() -> assertEquals(1,closed.get()));
        }
        finally { SwingUtilities.invokeAndWait(() -> { for (ViewerWindow frame:windows) if (frame!=null) frame.dispose(); }); }
    }
}
