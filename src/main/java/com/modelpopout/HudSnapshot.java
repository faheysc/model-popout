package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.List;

/** Completed HUD data only: no Actor/Model/HitSplat or writable client arrays cross the handoff. */
final class HudSnapshot
{
    final long token,npcKey;
    final int cycle;
    final ActorHud player,npc;
    final HudAssets.HealthSkin skin;
    HudSnapshot(long token,int cycle,long npcKey,ActorHud player,ActorHud npc,HudAssets.HealthSkin skin)
    { this.token=token;this.cycle=cycle;this.npcKey=npcKey;this.player=player;this.npc=npc;this.skin=skin; }

    static final class ActorHud
    {
        final long key;
        final HudHealth health;
        final List<Hit> hits;
        ActorHud(long key,HudHealth health,List<Hit> hits)
        { this.key=key;this.health=health;this.hits=List.copyOf(hits); }
    }

    static final class Hit
    {
        final int type,amount,expires,slot;
        final HitsplatStyle style;
        final BufferedImage image;
        Hit(int type,int amount,int expires,int slot,HitsplatStyle style,BufferedImage image)
        { this.type=type;this.amount=amount;this.expires=expires;this.slot=slot;this.style=style;this.image=image; }
        boolean active(int cycle) { return expires-cycle>0; }
        boolean visible(int cycle) { int remaining=expires-cycle;return remaining>0 && remaining<=style.duration; }
        double progress(int cycle) { return Math.max(0,Math.min(1,1-(expires-cycle)/(double)style.duration)); }
        float alpha(int cycle)
        {
            if(!visible(cycle)) return 0;
            return style.fadeStart<0 || style.fadeStart>=style.duration?1:
                (float)Math.min(1,(expires-cycle)/(double)(style.duration-style.fadeStart));
        }
    }
}
