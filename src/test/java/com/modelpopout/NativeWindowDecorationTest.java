package com.modelpopout;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.*;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

/** Reproduces RuneLite's global decoration setting rather than testing only a plain JFrame environment. */
public class NativeWindowDecorationTest
{
    @Test public void inheritedRuneLiteAndMetalBarsAreUsedWithoutAnExtraCustomBar() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original=UIManager.getLookAndFeel();boolean decorated=JFrame.isDefaultLookAndFeelDecorated();
            try
            {
                for (LookAndFeel laf:new LookAndFeel[]{new javax.swing.plaf.metal.MetalLookAndFeel(),new net.runelite.client.ui.laf.RuneLiteLAF()})
                {
                    UIManager.setLookAndFeel(laf);JFrame.setDefaultLookAndFeelDecorated(true);
                    JFrame inherited=new JFrame("inherited decoration reproduction");
                    try
                    {
                        // This is the missing case in the original tests: undecorated does not mean no Swing bar.
                        assertTrue(inherited.isUndecorated());assertEquals(JRootPane.FRAME,inherited.getRootPane().getWindowDecorationStyle());
                        inherited.add(new WindowTitleBar(new WindowTitleBarTest.Actions()),BorderLayout.NORTH);
                        assertEquals("Legacy duplication occurs inside one frame",2,titlePanes(inherited));
                    }
                    finally { inherited.dispose(); }
                    ViewerWindow frame=ViewerWindow.create();
                    try
                    {
                        frame.setSize(480,360);frame.setVisible(true);
                        assertEquals(JRootPane.FRAME,frame.getRootPane().getWindowDecorationStyle());assertFalse(frame.hasBuiltInControls());
                        assertEquals(1,titlePanes(frame));
                        assertTrue("Inherited bar must provide minimize/maximize/close buttons",titleButtons(frame)>=3);
                        assertTrue(JFrame.isDefaultLookAndFeelDecorated());
                        frame.applyWindowControls();frame.validate();
                        assertFalse(frame.hasBuiltInControls());
                        assertEquals(JRootPane.FRAME,frame.getRootPane().getWindowDecorationStyle());
                        assertEquals(1,titlePanes(frame));
                        assertTrue(frame.isDisplayable());assertTrue(frame.isUndecorated());
                        assertFalse(frame.getRootPane().isOpaque());assertFalse(frame.getLayeredPane().isOpaque());assertFalse(frame.getContentPane().isOpaque());
                        verifyRoutingAndAlpha(frame);
                    }
                    finally { frame.dispose(); }
                }
            }
            catch (UnsupportedLookAndFeelException failure) { throw new AssertionError(failure); }
            finally
            {
                JFrame.setDefaultLookAndFeelDecorated(decorated);
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException failure) { throw new AssertionError(failure); }
            }
        });
    }

    @Test public void absentInheritedDecorationsAlwaysGetBuiltInControls() throws Exception
    {
        requireDesktop();
        SwingUtilities.invokeAndWait(() -> {
            boolean decorated=JFrame.isDefaultLookAndFeelDecorated();
            JFrame.setDefaultLookAndFeelDecorated(false);
            ViewerWindow frame=ViewerWindow.create();
            try
            {
                frame.setSize(480,360);frame.setVisible(true);
                frame.applyWindowControls();frame.validate();
                assertTrue(frame.hasBuiltInControls());assertEquals(JRootPane.NONE,frame.getRootPane().getWindowDecorationStyle());
                assertEquals(1,titlePanes(frame));verifyRoutingAndAlpha(frame);
            }
            finally { frame.dispose();JFrame.setDefaultLookAndFeelDecorated(decorated); }
        });
    }

    private static void verifyRoutingAndAlpha(ViewerWindow frame)
    {
        AtomicInteger paints=new AtomicInteger();
        JComponent probe=new JComponent()
        { @Override protected void paintComponent(Graphics g) { paints.incrementAndGet();assertTrue(frame.isPaintingWindow()); } };
        probe.setOpaque(true);probe.setBounds(2,2,10,10);frame.getLayeredPane().add(probe,JLayeredPane.POPUP_LAYER);
        try
        {
            probe.paintImmediately(0,0,10,10);
            assertEquals("Opaque siblings must ascend to the native presentation boundary",0,paints.get());
            for (int opacity:new int[]{0,30,50,100})
            {
                frame.applyBackgroundOpacity(opacity);frame.validate();
                BufferedImage image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=image.createGraphics();try { frame.paint(g); } finally { g.dispose(); }
                Point viewport=SwingUtilities.convertPoint(frame.getContentPane(),80,80,frame);
                assertEquals(BackgroundOpacity.alpha(frame.effectiveOpacity()),image.getRGB(viewport.x,viewport.y)>>>24);
                Container title=titlePane(frame);
                if (title!=null)
                {
                    Point chrome=SwingUtilities.convertPoint(title,title.getWidth()/2,title.getHeight()/2,frame);
                    assertEquals("Title bar must remain opaque at every viewport opacity",255,image.getRGB(chrome.x,chrome.y)>>>24);
                }
            }
            assertEquals(4,paints.get());
        }
        finally { frame.getLayeredPane().remove(probe); }
    }

    @Test public void automaticAppliesToIndependentInspectionWindowsAndSurvivesSettingsChanges() throws Exception
    {
        requireDesktop();
        ModelPopoutConfig config=new ModelPopoutConfig() {};
        SwingUtilities.invokeAndWait(() -> {
            InspectionRenderScheduler scheduler=new InspectionRenderScheduler(new Worker(),SwingUtilities::invokeLater);
            InspectionSession[] sessions=new InspectionSession[2];ViewerWindow[] frames=new ViewerWindow[2];
            try
            {
                for (int i=0;i<2;i++)
                {
                    sessions[i]=new InspectionSession(config,scheduler,() -> {},v -> {});sessions[i].open("decoration test "+i);
                    for (Window window:Window.getWindows()) if (window instanceof ViewerWindow
                        && ((ViewerWindow)window).getTitle().equals("Popout — decoration test "+i)) frames[i]=(ViewerWindow)window;
                    assertNotNull(frames[i]);assertEquals(1,titlePanes(frames[i]));
                }
                for (int i=0;i<2;i++)
                {
                    boolean builtIn=frames[i].hasBuiltInControls();
                    sessions[i].settingsChanged();
                    assertEquals(builtIn,frames[i].hasBuiltInControls());assertEquals(1,titlePanes(frames[i]));
                    assertFalse(sessions[i].closed());assertTrue(frames[i].isDisplayable());
                }
                frames[0].dispatchEvent(new java.awt.event.WindowEvent(frames[0],java.awt.event.WindowEvent.WINDOW_CLOSING));
                assertTrue(frames[1].isDisplayable());
            }
            finally { for (InspectionSession session:sessions) if (session!=null) session.close(); }
        });
    }

    static Container titlePane(Container container)
    {
        if (container instanceof WindowTitleBar || container.getClass().getSimpleName().endsWith("TitlePane")) return container;
        for (Component child:container.getComponents()) if (child instanceof Container)
        {
            Container title=titlePane((Container)child);
            if (title!=null) return title;
        }
        return null;
    }

    static int titleButtons(Container container)
    {
        int buttons=0;
        for (Component child:container.getComponents())
        {
            if (child instanceof Container && child.getClass().getSimpleName().endsWith("TitlePane"))
                buttons+=buttons((Container)child);
            else if (child instanceof Container) buttons+=titleButtons((Container)child);
        }
        return buttons;
    }

    private static int buttons(Container container)
    {
        int count=0;
        for (Component child:container.getComponents())
        {
            if (child instanceof JButton) count++;
            else if (child instanceof Container) count+=buttons((Container)child);
        }
        return count;
    }

    static int titlePanes(Container container)
    {
        int count=container instanceof WindowTitleBar || container.getClass().getSimpleName().endsWith("TitlePane") ? 1 : 0;
        for (Component child:container.getComponents()) if (child instanceof Container) count+=titlePanes((Container)child);
        return count;
    }

    static void requireDesktop()
    {
        Assume.assumeTrue(Boolean.getBoolean("modelpopout.desktopTests"));Assume.assumeFalse(GraphicsEnvironment.isHeadless());
    }
}
