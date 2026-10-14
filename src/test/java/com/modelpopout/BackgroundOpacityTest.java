package com.modelpopout;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundOpacityTest
{
    @Test public void opacityLimitsAndUnsupportedFallbackAreSafe()
    {
        assertEquals(0,BackgroundOpacity.clamp(-100));
        assertEquals(100,BackgroundOpacity.clamp(200));
        assertEquals(0,BackgroundOpacity.alpha(0));
        assertEquals(128,BackgroundOpacity.alpha(50));
        assertEquals(255,BackgroundOpacity.alpha(100));
        assertEquals(100,BackgroundOpacity.effective(0,false));
        assertEquals(0,BackgroundOpacity.effective(0,true));
    }

    @Test public void backgroundAlphaNeverFadesModelsOrErasesBlackDetails()
    {
        BufferedImage models = new BufferedImage(3,1,BufferedImage.TYPE_INT_ARGB);
        models.setRGB(0,0,0xffff0000);
        models.setRGB(1,0,0xff000000);
        for(int percent : new int[]{0,25,50,100})
        {
            BufferedImage composed = BackgroundOpacity.compose(models,percent);
            assertEquals(0xffff0000,composed.getRGB(0,0));
            assertEquals(0xff000000,composed.getRGB(1,0));
            assertEquals(BackgroundOpacity.alpha(percent),composed.getRGB(2,0) >>> 24);
        }
    }

    @Test public void decreasingOpacityClearsOldPixelsInsteadOfAccumulatingOpaqueBackground() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            surface.setSize(10,10);
            BufferedImage image = new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try
            {
                surface.opacity = 100; surface.paint(g);
                assertEquals(255,image.getRGB(5,5) >>> 24);
                surface.opacity = 50; surface.paint(g);
                assertEquals(128,image.getRGB(5,5) >>> 24);
                surface.opacity = 0; surface.paint(g);
                assertEquals(0,image.getRGB(5,5) >>> 24);
            }
            finally { g.dispose(); }
        });
    }

    @Test public void repeatedFrozenFramePaintingKeepsModelOpaqueInBothBackgroundModes() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage frozen = new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
            frozen.setRGB(5,5,0xffff0000);
            ViewerWindow.Surface surface = new ViewerWindow.Surface();
            surface.setSize(10,10);
            surface.modelPainter = g -> g.drawImage(frozen,0,0,null);
            for (int opacity : new int[]{100,50,0})
            {
                surface.opacity = opacity;
                BufferedImage target = new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = target.createGraphics();
                try
                {
                    for (int i = 0; i < 100; i++)
                    {
                        surface.paint(g);
                        assertEquals(0xffff0000,target.getRGB(5,5));
                        assertEquals(BackgroundOpacity.alpha(opacity),target.getRGB(1,1) >>> 24);
                    }
                }
                finally { g.dispose(); }
            }
        });
    }

    @Test public void settingsSliderUsesSavedKeyAndSurvivesPanelRecreationWithoutFeedbackWrites() throws Exception
    {
        Map<String,Object> saved = new HashMap<>();
        ModelPopoutConfig config = new ModelPopoutConfig()
        {
            @Override public int backgroundOpacity() { return (int) saved.getOrDefault(OPACITY_KEY,100); }
            @Override public boolean renderNpc() { return (boolean) saved.getOrDefault(NPC_KEY,true); }
        };
        SwingUtilities.invokeAndWait(() -> {
            int[] writes = {0};
            ViewerSettingsPanel first = new ViewerSettingsPanel(config,(key,value) -> { saved.put(key,value); writes[0]++; },() -> {});
            assertEquals(0,writes[0]);
            first.opacity.setValue(37);
            assertEquals(37,saved.get(ModelPopoutConfig.OPACITY_KEY));
            ViewerSettingsPanel reopened = new ViewerSettingsPanel(config,(key,value) -> { saved.put(key,value); writes[0]++; },() -> {});
            assertEquals(37,reopened.opacity.getValue());
            saved.put(ModelPopoutConfig.OPACITY_KEY,0);
            reopened.sync(config);
            assertEquals(0,reopened.opacity.getValue());
            assertEquals(1,writes[0]);
            reopened.npcs.doClick();
            assertEquals(false,saved.get(ModelPopoutConfig.NPC_KEY));
        });
    }
    @Test public void renderTargetPersistsIndependentlyWithoutFeedbackWrites() throws Exception
    {
        Map<String,Object> saved = new HashMap<>();
        ModelPopoutConfig config = new ModelPopoutConfig()
        {
            @Override public RenderTarget renderTarget() { return (RenderTarget)saved.getOrDefault("renderTarget",RenderTarget.FPS_60); }
        };
        SwingUtilities.invokeAndWait(() -> {
            int[] writes = {0};
            ViewerSettingsPanel first = new ViewerSettingsPanel(config,(key,value) -> { saved.put(key,value); writes[0]++; },() -> {});
            assertEquals(0,writes[0]);
            first.renderTarget.setSelectedItem(RenderTarget.FPS_165);
            assertEquals(RenderTarget.FPS_165,saved.get("renderTarget"));
            ViewerSettingsPanel reopened = new ViewerSettingsPanel(config,(key,value) -> { saved.put(key,value); writes[0]++; },() -> {});
            assertEquals(RenderTarget.FPS_165,reopened.renderTarget.getSelectedItem());
            assertEquals(20,config.refreshRate());
            assertEquals(1,writes[0]);
        });
    }
}
