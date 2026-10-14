package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;

/** Independent 2D model/HUD layer: no UI-opacity multiplier and no 3D raster work. */
final class HudPainter
{
    static void paint(Graphics2D graphics,HudSnapshot snapshot,HudAnchor playerAnchor,HudAnchor npcAnchor,
        CameraView camera,int width,int height,boolean npcEnabled,boolean mirrorNpc,long imagePlayerKey,long imageNpcKey,
        boolean showHitsplats,boolean showBars)
    {
        if(snapshot==null || width<=0 || height<=0) return;
        Graphics2D g=(Graphics2D)graphics.create();
        try
        {
            g.setComposite(AlphaComposite.SrcOver);
            if(playerAnchor!=null && snapshot.player.key==imagePlayerKey)
                actor(g,snapshot.player,snapshot.cycle,snapshot.skin,playerAnchor.screen(camera,width,height,npcEnabled,0,false),showHitsplats,showBars);
            if(npcEnabled && npcAnchor!=null && imageNpcKey>=0 && snapshot.npcKey==imageNpcKey)
                actor(g,snapshot.npc,snapshot.cycle,snapshot.skin,npcAnchor.screen(camera,width,height,true,1,mirrorNpc),showHitsplats,showBars);
        }
        finally { g.dispose(); }
    }
    private static void actor(Graphics2D g,HudSnapshot.ActorHud actor,int cycle,HudAssets.HealthSkin skin,Point anchor,boolean hits,boolean bars)
    {
        if(bars && actor.health.visible)
        {
            int width=skin.width(),height=skin.height(),x=anchor.x-width/2,y=anchor.y-height;
            int filled=actor.health.fraction<=0?0:Math.max(1,(int)Math.round(width*actor.health.fraction));
            if(skin.back!=null)
            {
                g.drawImage(skin.back,x,y,null);Shape old=g.getClip();g.clipRect(x,y,filled,height);
                g.drawImage(skin.front,x,y,null);g.setClip(old);
            }
            else
            {
                g.setColor(Color.RED);g.fillRect(x,y,width,height);g.setColor(Color.GREEN);g.fillRect(x,y,filled,height);
            }
        }
        if(!hits) return;
        for(HudSnapshot.Hit hit:actor.hits)
        {
            if(!hit.visible(cycle)) continue;
            int dx=hit.slot==2?-18:hit.slot==3?18:0;
            int dy=hit.slot==1?-22:hit.slot>=2?-11:0;
            int x=anchor.x+dx+(int)Math.round(hit.style.offsetX*hit.progress(cycle))-hit.image.getWidth()/2;
            int y=anchor.y-12+dy-(int)Math.round(hit.style.offsetY*hit.progress(cycle))-hit.image.getHeight();
            g.setComposite(AlphaComposite.SrcOver.derive(hit.alpha(cycle)));g.drawImage(hit.image,x,y,null);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }
}
