package com.modelpopout;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowTransparencyTest
{
    /** Models the public JDK background setter's equality and alpha-transition conditions. */
    private static final class NativeWindow
    {
        Color background = new Color(0,0,0,0);
        boolean capable, reject;
        final List<Integer> alphas = new ArrayList<>();
        void setBackground(Color next)
        {
            Color previous=background;background=next;alphas.add(next.getAlpha());
            if(previous.equals(next)) return;
            if(previous.getAlpha()==255 && next.getAlpha()<255)
            {
                if(reject) throw new UnsupportedOperationException("Desktop rejected translucency");
                capable=true;
            }
        }
        boolean apply(boolean available)
        {return WindowTransparency.apply(available,() -> capable,() -> background,this::setBackground);}
    }
    @Test public void transparentColorWithLostAlphaConfigurationMustRenegotiateInsteadOfRepeatingNoOp()
    {
        NativeWindow nativeWindow=new NativeWindow();
        assertTrue(nativeWindow.apply(true));
        assertEquals(List.of(255,0),nativeWindow.alphas);assertTrue(nativeWindow.capable);
        assertEquals(0,nativeWindow.background.getAlpha());
        nativeWindow.alphas.clear();nativeWindow.apply(true);
        assertEquals("Healthy configuration must never toggle native opacity",List.of(0),nativeWindow.alphas);
    }
    @Test public void opaqueBackgroundNegotiatesDirectlyAndConfigurationLossCanRecoverRepeatedly()
    {
        NativeWindow nativeWindow=new NativeWindow();nativeWindow.background=Color.BLACK;
        assertTrue(nativeWindow.apply(true));assertEquals(List.of(0),nativeWindow.alphas);
        for(int i=0;i<3;i++)
        {
            nativeWindow.capable=false;nativeWindow.alphas.clear();
            assertTrue(nativeWindow.apply(true));assertEquals(List.of(255,0),nativeWindow.alphas);
        }
    }
    @Test public void unsupportedOrRejectedDesktopFallsBackWithoutClaimingTransparency()
    {
        NativeWindow unavailable=new NativeWindow();
        assertFalse(unavailable.apply(false));assertEquals(List.of(255),unavailable.alphas);
        NativeWindow rejected=new NativeWindow();rejected.reject=true;
        assertFalse(rejected.apply(true));assertEquals(255,rejected.background.getAlpha());
        assertFalse(rejected.capable);
    }
    @Test public void advertisedSupportWithoutAUsableConfigurationMustNotSilentlyLoseAlpha()
    {
        Color[] background={Color.BLACK};
        boolean supported=WindowTransparency.apply(true,() -> false,() -> background[0],next -> background[0]=next);
        assertFalse(supported);assertEquals(255,background[0].getAlpha());
    }
}
