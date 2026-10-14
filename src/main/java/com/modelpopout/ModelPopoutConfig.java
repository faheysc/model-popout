package com.modelpopout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(ModelPopoutConfig.GROUP)
public interface ModelPopoutConfig extends Config
{
    // Keep the original persisted group so the rename does not reset existing user settings.
    String GROUP = "livemodelviewer";
    String OPACITY_KEY = "backgroundOpacity";
    String NPC_KEY = "renderNpc";
    String HITSPLATS_KEY = "showHitsplats";
    String HP_BARS_KEY = "showHpBars";
    String ALWAYS_ON_TOP_KEY = "alwaysOnTop";

    @Range(min = 1, max = 50)
    @ConfigItem(keyName = "refreshRate", name = "Model capture FPS", position = 0,
        description = "Target live model sampling rate. Actual updates depend on client ticks, pose changes, and rendering speed.")
    default int refreshRate()
    {
        return 20;
    }

    @ConfigItem(keyName = NPC_KEY, name = "Render interacting NPC", position = 1,
        description = "Show your current NPC target, or an NPC attacking you, beside your character.")
    default boolean renderNpc() { return true; }

    @Range(min = 0, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = OPACITY_KEY, name = "Background opacity", position = 2,
        description = "Window content: 0% reveals the desktop, 100% is opaque. Models keep intrinsic alpha. Native KDE title bars remain desktop-controlled. Unsupported desktops use an opaque background.")
    default int backgroundOpacity() { return 100; }
    @ConfigItem(keyName = "renderTarget", name = "Rendering target", position = 3,
        description = "Independent viewer render/presentation target. Latest pose is held; this does not create skeletal animation frames or change RuneLite timing.")
    default RenderTarget renderTarget() { return RenderTarget.FPS_60; }

    @ConfigItem(keyName = ALWAYS_ON_TOP_KEY, name = "Always on Top", position = 4,
        description = "Keep only the Model Popout window above other application windows where the desktop supports it.")
    default boolean alwaysOnTop() { return false; }
    @ConfigItem(keyName = HITSPLATS_KEY, name = "Show Hitsplats", position = 5,
        description = "Display observed hitsplats for your character and the displayed NPC.")
    default boolean showHitsplats() { return true; }

    @ConfigItem(keyName = HP_BARS_KEY, name = "Show HP Bars", position = 6,
        description = "Display native-visible health bars. NPC health is scaled data, never an invented exact HP value.")
    default boolean showHpBars() { return true; }
}
