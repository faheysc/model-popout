package com.modelpopout;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class NativeInspectionWindowTest
{
    @Test public void realInspectionWindowsHaveIndependentPositionsAndNativeCloseCleanup() throws Exception
    {
        Assume.assumeTrue("Enable with desktopPresentationTest",Boolean.getBoolean("modelpopout.desktopTests"));
        Assume.assumeFalse("Native window checks need a display",GraphicsEnvironment.isHeadless());
        ModelPopoutConfig config=new ModelPopoutConfig() {};
        Worker worker=new Worker();InspectionSession[] sessions=new InspectionSession[2];ViewerWindow[] frames=new ViewerWindow[2];
        try
        {
            SwingUtilities.invokeAndWait(() -> {
                InspectionRenderScheduler scheduler=new InspectionRenderScheduler(worker,SwingUtilities::invokeLater);
                for (int i=0;i<2;i++)
                {
                    sessions[i]=new InspectionSession(config,scheduler,() -> {},value -> {});
                    sessions[i].open("desktop inspection test "+i);
                    for (Window window:Window.getWindows()) if (window instanceof ViewerWindow
                        && ((ViewerWindow)window).getTitle().equals("Popout — desktop inspection test "+i)) frames[i]=(ViewerWindow)window;
                    assertNotNull(frames[i]);frames[i].setLocation(100+i*100,100+i*100);
                    frames[i].applyBackgroundOpacity(50);
                    assertFalse(frames[i].getRootPane().isOpaque());assertFalse(frames[i].getLayeredPane().isOpaque());
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                assertNotEquals(frames[0].getLocation(),frames[1].getLocation());
                frames[0].dispatchEvent(new WindowEvent(frames[0],WindowEvent.WINDOW_CLOSING));
            });
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(sessions[0].closed());assertFalse(sessions[1].closed());assertTrue(frames[1].isDisplayable());
            });
        }
        finally { SwingUtilities.invokeAndWait(() -> { for (InspectionSession session:sessions) if (session!=null) session.close(); }); }
    }
}
