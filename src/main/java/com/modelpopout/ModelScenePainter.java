package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Display-only transforms of a completed scene; never modifies its pixels or game models. */
final class ModelScenePainter
{
    static boolean shouldMirrorNpc(CameraView camera)
    {
        // Native forward is -Z: its screen X is -sin(yaw). The right-hand NPC should face left.
        return Math.sin(camera.yaw) < -1e-9;
    }

    static void paint(Graphics2D graphics, BufferedImage image, boolean npcEnabled, boolean mirrorNpc,
        int width, int height)
    {
        if (image == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setComposite(AlphaComposite.SrcOver);
            int slots = npcEnabled ? 2 : 1;
            int size = Math.min(width / slots,height);
            int x = (width - size * slots) / 2, y = (height - size) / 2;
            int sourceSize = ModelRenderer.SIZE;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image,x,y,x+size,y+size,0,0,sourceSize,sourceSize,null);
            if (slots == 2 && image.getWidth() >= sourceSize * 2)
                g.drawImage(image,x+size,y,x+size*2,y+size,
                    mirrorNpc ? sourceSize*2 : sourceSize,0,
                    mirrorNpc ? sourceSize : sourceSize*2,sourceSize,null);
        }
        finally { g.dispose(); }
    }
}
