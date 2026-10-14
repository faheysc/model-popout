package com.modelpopout;

import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigCompatibilityTest
{
    @Test public void renamedSidebarLoadsAndWritesLegacyProfileKeys() throws Exception
    {
        assertEquals("livemodelviewer",ModelPopoutConfig.GROUP);
        assertFalse(new ModelPopoutConfig() {}.alwaysOnTop());
        Map<String,Object> profile=new HashMap<>();
        profile.put("livemodelviewer.backgroundOpacity",37);
        profile.put("livemodelviewer.alwaysOnTop",true);
        profile.put("livemodelviewer.renderNpc",false);
        profile.put("livemodelviewer.refreshRate",30);
        profile.put("livemodelviewer.renderTarget",RenderTarget.FPS_144);
        ModelPopoutConfig config=new ModelPopoutConfig()
        {
            public boolean alwaysOnTop(){return (Boolean)profile.get("livemodelviewer.alwaysOnTop");}
            public int backgroundOpacity(){return (Integer)profile.get("livemodelviewer.backgroundOpacity");}
            public boolean renderNpc(){return (Boolean)profile.get("livemodelviewer.renderNpc");}
            public int refreshRate(){return (Integer)profile.get("livemodelviewer.refreshRate");}
            public RenderTarget renderTarget(){return (RenderTarget)profile.get("livemodelviewer.renderTarget");}
        };
        SwingUtilities.invokeAndWait(() -> {
            ViewerSettingsPanel panel=new ViewerSettingsPanel(config,
                (key,value) -> profile.put(ModelPopoutConfig.GROUP+"."+key,value),() -> {});
            assertEquals(37,panel.opacity.getValue());
            assertTrue(panel.alwaysOnTop.isSelected());
            panel.alwaysOnTop.doClick();
            assertFalse(panel.npcs.isSelected());
            assertEquals(RenderTarget.FPS_144,panel.renderTarget.getSelectedItem());
            panel.opacity.setValue(50);panel.npcs.doClick();panel.renderTarget.setSelectedItem(RenderTarget.FPS_240);
            ViewerSettingsPanel reopened=new ViewerSettingsPanel(config,(key,value) -> fail("sync must not save defaults"),() -> {});
            assertEquals(50,reopened.opacity.getValue());
            assertFalse(reopened.alwaysOnTop.isSelected());
            assertEquals(false,profile.get("livemodelviewer.alwaysOnTop"));
            assertTrue(reopened.npcs.isSelected());
            assertEquals(RenderTarget.FPS_240,reopened.renderTarget.getSelectedItem());
            assertEquals(30,config.refreshRate());
            assertFalse(profile.containsKey("modelpopout.backgroundOpacity"));
        });
    }
    @Test public void hudSettingsDefaultEnabledApplyIndependentlyAndPersistAcrossSidebarLifecycle() throws Exception
    {
        ModelPopoutConfig defaults=new ModelPopoutConfig() {};
        assertTrue(defaults.showHitsplats());assertTrue(defaults.showHpBars());
        Map<String,Object> profile=new HashMap<>();
        ModelPopoutConfig config=new ModelPopoutConfig()
        {
            public boolean showHitsplats(){return (Boolean)profile.getOrDefault(ModelPopoutConfig.GROUP+".showHitsplats",true);}
            public boolean showHpBars(){return (Boolean)profile.getOrDefault(ModelPopoutConfig.GROUP+".showHpBars",true);}
        };
        SwingUtilities.invokeAndWait(() -> {
            ViewerSettingsPanel panel=new ViewerSettingsPanel(config,(key,value) -> profile.put(ModelPopoutConfig.GROUP+"."+key,value),() -> {});
            assertTrue(panel.hitsplats.isSelected());assertTrue(panel.hpBars.isSelected());
            panel.hitsplats.doClick();assertFalse(config.showHitsplats());assertTrue(config.showHpBars());
            panel.hpBars.doClick();assertFalse(config.showHpBars());
            ViewerSettingsPanel reopened=new ViewerSettingsPanel(config,(key,value) -> fail("opening must not overwrite saved HUD settings"),() -> {});
            assertFalse(reopened.hitsplats.isSelected());assertFalse(reopened.hpBars.isSelected());
            profile.put(ModelPopoutConfig.GROUP+".showHitsplats",true);reopened.sync(config);
            assertTrue(reopened.hitsplats.isSelected());assertFalse(reopened.hpBars.isSelected());
        });
    }
}
