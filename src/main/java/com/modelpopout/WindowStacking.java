package com.modelpopout;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Apply only to the viewer's Window; a rejected desktop request must not disrupt rendering. */
final class WindowStacking
{
    static boolean apply(boolean requested, BooleanSupplier supported, Consumer<Boolean> setAlwaysOnTop)
    {
        try
        {
            if (requested && !supported.getAsBoolean()) return false;
            setAlwaysOnTop.accept(requested);
            return true;
        }
        catch (UnsupportedOperationException | SecurityException unavailable)
        {
            return false;
        }
    }
}
