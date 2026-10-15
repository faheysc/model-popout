package com.modelpopout;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ConfigCompatibilityTest
{
    @Rule public TemporaryFolder temporary=new TemporaryFolder();

    @Test public void standardConfigurationRetainsAllNamesKeysAndDefaults() throws Exception
    {
        assertEquals("livemodelviewer",ModelPopoutConfig.class.getAnnotation(ConfigGroup.class).value());
        String[][] items={{"refreshRate","Model capture FPS"},{"renderNpc","Render interacting NPC"},
            {"backgroundOpacity","Background opacity"},{"renderTarget","Rendering target"},{"alwaysOnTop","Always on Top"},
            {"showHitsplats","Show Hitsplats"},{"showHpBars","Show HP Bars"},{"rightClickInspection","Enable Right-Click Inspection"}};
        int count=0;
        for (java.lang.reflect.Method method:ModelPopoutConfig.class.getMethods()) if(method.isAnnotationPresent(ConfigItem.class)) count++;
        assertEquals(items.length,count);
        for (String[] item:items)
        {
            ConfigItem annotation=ModelPopoutConfig.class.getMethod(item[0]).getAnnotation(ConfigItem.class);
            assertEquals(item[0],annotation.keyName());assertEquals(item[1],annotation.name());
        }
        try (ConfigTestSupport profile=new ConfigTestSupport(new File(temporary.getRoot(),"defaults.properties"),null))
        {
            ModelPopoutConfig config=profile.config;
            assertEquals(20,config.refreshRate());assertTrue(config.renderNpc());assertEquals(100,config.backgroundOpacity());
            assertEquals(RenderTarget.FPS_60,config.renderTarget());assertFalse(config.alwaysOnTop());
            assertTrue(config.showHitsplats());assertTrue(config.showHpBars());assertTrue(config.rightClickInspection());
            assertTrue(profile.manager.getConfigurationKeys(ModelPopoutConfig.GROUP+".").isEmpty());
        }
    }

    @Test public void legacyProfileLoadsAndStandardConfigChangesPersistWithoutSidebarOrMigration() throws Exception
    {
        File file=temporary.newFile("legacy.properties");
        Files.write(file.toPath(),("livemodelviewer.backgroundOpacity=37\nlivemodelviewer.alwaysOnTop=true\n"
            +"livemodelviewer.renderNpc=false\nlivemodelviewer.refreshRate=30\nlivemodelviewer.renderTarget=FPS_144\n"
            +"livemodelviewer.showHitsplats=false\nlivemodelviewer.showHpBars=false\nlivemodelviewer.rightClickInspection=false\n"
            +"livemodelviewer.windowControlMode=DESKTOP\n").getBytes(StandardCharsets.UTF_8));
        try (ConfigTestSupport profile=new ConfigTestSupport(file,null))
        {
            ModelPopoutConfig config=profile.config;
            assertEquals(37,config.backgroundOpacity());assertTrue(config.alwaysOnTop());assertFalse(config.renderNpc());
            assertEquals(30,config.refreshRate());assertEquals(RenderTarget.FPS_144,config.renderTarget());
            assertFalse(config.showHitsplats());assertFalse(config.showHpBars());assertFalse(config.rightClickInspection());
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"backgroundOpacity",50);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"alwaysOnTop",false);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"renderNpc",true);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"refreshRate",25);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"renderTarget",RenderTarget.FPS_240);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"showHitsplats",true);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"showHpBars",true);
            profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"rightClickInspection",true);
            profile.save();
        }
        try (ConfigTestSupport reopened=new ConfigTestSupport(file,null))
        {
            ModelPopoutConfig config=reopened.config;
            assertEquals(50,config.backgroundOpacity());assertFalse(config.alwaysOnTop());assertTrue(config.renderNpc());
            assertEquals(25,config.refreshRate());assertEquals(RenderTarget.FPS_240,config.renderTarget());
            assertTrue(config.showHitsplats());assertTrue(config.showHpBars());assertTrue(config.rightClickInspection());
            assertEquals("DESKTOP",reopened.manager.getConfiguration(ModelPopoutConfig.GROUP,"windowControlMode"));
            assertTrue(reopened.manager.getConfigurationKeys("modelpopout.").isEmpty());
        }
    }
}
