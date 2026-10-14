package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class BackgroundOpacity
{
    static int clamp(int percent) { return Math.max(0, Math.min(100, percent)); }
    static int effective(int requested, boolean supported) { return supported ? clamp(requested) : 100; }
    static int alpha(int percent) { return (clamp(percent) * 255 + 50) / 100; }
    static Color color(int percent)
    {
        int rgb = ModelRenderer.BACKGROUND;
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha(percent));
    }

    /** Reference compositing also used by static renderer tests: background alpha never scales models. */
    static BufferedImage compose(BufferedImage models, int percent)
    {
        BufferedImage image = new BufferedImage(models.getWidth(), models.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setComposite(AlphaComposite.Src);
            g.setColor(color(percent));
            g.fillRect(0,0,image.getWidth(),image.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(models,0,0,null);
        }
        finally { g.dispose(); }
        return image;
    }
}
