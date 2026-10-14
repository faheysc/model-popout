package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** EDT-owned buffer at the native Window.paint boundary, including AWT's background clear. */
final class WindowPresentation
{
    private BufferedImage buffer;
    private final boolean diagnostics = Boolean.getBoolean("modelpopout.paintDiagnostics");
    private String destinationFormat = "unpainted";

    // Read-only trace of the outer border pixel, away from model/HUD content.
    String backgroundArgb()
    {
        return buffer == null ? "unpainted" : String.format("%08x",buffer.getRGB(0,0));
    }

    String destinationFormat() { return destinationFormat; }

    void paint(Graphics graphics, int width, int height, Consumer<Graphics2D> hierarchy)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (width <= 0 || height <= 0) return;
        if (diagnostics && graphics instanceof Graphics2D)
        {
            java.awt.image.ColorModel format = ((Graphics2D)graphics).getDeviceConfiguration().getColorModel();
            destinationFormat = "bits="+format.getPixelSize()+",alpha="+format.hasAlpha();
        }
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height)
            buffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

        Graphics2D frame = buffer.createGraphics();
        try
        {
            frame.setComposite(AlphaComposite.Clear);
            frame.fillRect(0,0,width,height);
            frame.setComposite(AlphaComposite.SrcOver);
            // Window.paint may clear again. Both that clear and all child paints stay private.
            hierarchy.accept(frame);
        }
        finally { frame.dispose(); }

        Graphics2D target = (Graphics2D) graphics.create();
        try
        {
            target.setComposite(AlphaComposite.Src);
            target.drawImage(buffer,0,0,null);
        }
        finally { target.dispose(); }
    }
}
