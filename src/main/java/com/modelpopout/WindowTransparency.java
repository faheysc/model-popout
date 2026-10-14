package com.modelpopout;

import java.awt.Color;
import java.awt.IllegalComponentStateException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Native background/configuration recovery only; never applies whole-window opacity. */
final class WindowTransparency
{
    private static final Color TRANSPARENT = new Color(0,0,0,0);

    static boolean apply(boolean available, BooleanSupplier capable, Supplier<Color> background,
        Consumer<Color> setBackground)
    {
        if (available)
        {
            try
            {
                Color current = background.get();
                // AWT negotiates an alpha-capable configuration only on opaque -> translucent.
                // Repeating an already-transparent color is a no-op after a configuration change.
                if (!capable.getAsBoolean() && current != null && current.getAlpha() < 255)
                    setBackground.accept(BackgroundOpacity.color(100));
                setBackground.accept(TRANSPARENT);
                Color applied = background.get();
                if (capable.getAsBoolean() && applied != null && applied.getAlpha() == 0) return true;
            }
            catch (UnsupportedOperationException | IllegalComponentStateException unavailable)
            {
                // A desktop/device can reject translucency despite its advertised capability.
            }
        }
        setBackground.accept(BackgroundOpacity.color(100));
        return false;
    }
}
