package com.modelpopout;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development entry point: registers Model Popout before starting RuneLite. */
public final class ModelPopoutLauncher
{
    @SuppressWarnings("unchecked") // RuneLite 1.13.1 exposes a generic varargs registration method.
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ModelPopoutPlugin.class);
        RuneLite.main(args);
    }
}
