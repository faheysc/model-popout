package com.modelpopout;

import net.runelite.client.ui.NavigationButton;
import org.junit.Test;
import static org.junit.Assert.*;

public class LaunchNavigationTest
{
    @Test public void onlyOriginalLaunchNavigationRemainsWithoutASettingsPanel()
    {
        ModelPopoutPlugin plugin=new ModelPopoutPlugin();NavigationButton button=plugin.createLaunchButton();
        assertEquals("Open Model Popout",button.getTooltip());assertEquals(10,button.getPriority());
        assertNotNull(button.getOnClick());assertNull(button.getPanel());assertEquals(24,button.getIcon().getWidth());
        int navigationFields=0;
        for (java.lang.reflect.Field field:ModelPopoutPlugin.class.getDeclaredFields())
        {
            if (field.getType()==NavigationButton.class) navigationFields++;
            assertFalse(field.getName().startsWith("settings"));
        }
        assertEquals(1,navigationFields);
        assertNull(getClass().getResource("ViewerSettingsPanel.class"));
    }
}
