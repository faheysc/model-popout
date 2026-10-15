package com.modelpopout;

import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutomaticWindowControlsTest
{
    @Test public void automaticUsesKnownSwingControlsAndFallsBackOnUnknownDesktops()
    {
        for (boolean inherited:new boolean[]{true,false}) for (boolean supported:new boolean[]{true,false})
            assertEquals(inherited && supported,ViewerWindow.useSwingDecorations(inherited,supported));
    }

    @Test public void obsoleteModeIsNotExposed()
    {
        for (java.lang.reflect.Method method:ModelPopoutConfig.class.getMethods())
        {
            assertNotEquals("windowControlMode",method.getName());
            ConfigItem item=method.getAnnotation(ConfigItem.class);
            if (item!=null) assertNotEquals("windowControlMode",item.keyName());
        }
    }

    @Test public void rootPaintingOriginIncludesLookAndFeelTitleSiblings() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.PresentationRoot root=new ViewerWindow.PresentationRoot();
            ViewerWindow.keepPresentationNonOpaque(root);
            assertTrue(root.isPaintingOrigin());assertFalse(root.isOpaque());
            assertFalse(root.getLayeredPane().isOpaque());assertFalse(root.getContentPane().isOpaque());
        });
    }
}
