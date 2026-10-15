package com.modelpopout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Own-window drawing only, using the same Window.paint and full-surface repaint boundaries. */
final class InspectionPanel extends ModelViewport
{
    BufferedImage image;
    HudSnapshot hud;
    HudAnchor anchor;
    CameraView imageCamera = CameraView.DEFAULT;
    long imageHudKey = -1;
    String status = "Waiting for the actor's model…";
    private final ModelPopoutConfig config;

    InspectionPanel(ModelPopoutConfig config, LiveViewState state, Runnable requestRender)
    {
        super(state,requestRender);
        this.config=config;
        setPreferredSize(new Dimension(ModelRenderer.SIZE,ModelRenderer.SIZE));
    }

    void paintModels(Graphics2D graphics)
    {
        ModelScenePainter.paint(graphics,image,false,false,getWidth(),getHeight());
        if (image!=null)
            HudPainter.paint(graphics,hud,anchor,null,imageCamera,getWidth(),getHeight(),false,false,
                imageHudKey,-1,config.showHitsplats(),config.showHpBars());
        if (status!=null)
        {
            Graphics2D g=(Graphics2D)graphics.create();
            try
            {
                g.setColor(new Color(0,0,0,180));
                g.fillRect(8,getHeight()-32,Math.min(getWidth()-16,g.getFontMetrics().stringWidth(status)+16),24);
                g.setColor(Color.WHITE);g.drawString(status,16,getHeight()-15);
            }
            finally { g.dispose(); }
        }
    }

    void clear() { image=null;hud=null;anchor=null; }
}
