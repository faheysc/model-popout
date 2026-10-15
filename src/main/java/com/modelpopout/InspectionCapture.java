package com.modelpopout;

import net.runelite.api.Actor;
import net.runelite.api.Model;
import net.runelite.api.TextureProvider;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/** Client-thread source. Packets contain owned data only, never native actors or models. */
final class InspectionCapture
{
    static final class Packet
    {
        final ModelSnapshot model;
        final TextureSet textures;
        final HudSnapshot hud;
        final int cycle;
        final long observedAt;
        final boolean live;
        final int npcDefinition;
        Packet(ModelSnapshot model, TextureSet textures, HudSnapshot hud, int cycle, long observedAt, boolean live)
        { this(model,textures,hud,cycle,observedAt,live,-1); }
        Packet(ModelSnapshot model, TextureSet textures, HudSnapshot hud, int cycle, long observedAt, boolean live, int npcDefinition)
        {
            this.model=model; this.textures=textures; this.hud=hud; this.cycle=cycle; this.observedAt=observedAt;
            this.live=live;this.npcDefinition=npcDefinition;
        }
    }

    final InspectionActor binding;
    private final HudTracker hud = new HudTracker();
    private ModelSnapshot model;
    private TextureSet textures = TextureSet.EMPTY;
    private int cycle;
    private long observedAt, nextDue;
    private boolean scheduled, live = true;
    private Packet last;
    private int npcDefinition=-1;

    InspectionCapture(InspectionActor binding) { this.binding = binding; }
    boolean live() { return live; }

    boolean due(long now) { return live && (!scheduled || now-nextDue >= 0); }

    boolean textureDue(int gameCycle)
    {
        if (!live || model==null || model.textureIds==null || cycle==gameCycle) return false;
        for (short id:model.textureIds) if (id>=0) return true;
        return false;
    }

    /** Independent material sampling over the retained owned pose; no actor model read. */
    void sampleTextures(TextureProvider provider, boolean gpu, int gameCycle, long now)
    {
        if (!textureDue(gameCycle)) return;
        TextureSet next=TextureSet.capture(provider,model,null,gameCycle,gpu,textures);
        if (!next.samePixels(textures)) textures=next;
        cycle=gameCycle;observedAt=now;
    }

    void capture(Actor actor, TextureProvider provider, boolean gpu, int gameCycle, long now, int fps)
    {
        long period=1_000_000_000L / Math.max(1,Math.min(50,fps));
        // Match the main capture gate's fractional deadlines on a 20ms client cadence.
        nextDue=scheduled?now+period-(now-nextDue)%period:now+period;
        scheduled = true;
        Model nativeModel = actor.getModel();
        if (nativeModel == null) return; // Never discard the last valid model on a temporary cache miss.
        ModelSnapshot next = ModelSnapshot.capture(nativeModel,provider);
        if (actor instanceof NPC)
        {
            NPC npc=(NPC)actor;
            NPCComposition definition=npc.getTransformedComposition();
            npcDefinition=definition==null?npc.getId():definition.getId();
        }
        if (model == null || !next.sameAppearance(model)) model = next;
        cycle = gameCycle;
        observedAt = now;
        // A texture-provider failure must not discard otherwise valid new geometry.
        TextureSet nextTextures = TextureSet.capture(provider,next,null,gameCycle,gpu,textures);
        if (!nextTextures.samePixels(textures)) textures = nextTextures;
    }

    void hit(Actor actor, Hitsplat hit, int cycle, HudAssets assets)
    {
        if (!live || !binding.matches(actor) || hit == null || hit.getHitsplatType() < 0) return;
        HitsplatStyle style = assets.style(hit.getHitsplatType());
        if (style == null) return;
        hud.selectPlayer(actor); // Single actor uses the existing first HUD slot, including NPCs.
        hud.add(actor,hit.getHitsplatType(),hit.getAmount(),hit.getDisappearsOnGameCycle(),cycle,
            style,assets.hitImage(hit.getHitsplatType(),style,hit.getAmount()));
    }

    Packet packet(Actor actor, int gameCycle, HudAssets.HealthSkin skin)
    {
        HudSnapshot nextHud = null;
        if (live && actor != null)
        {
            hud.selectPlayer(actor);
            nextHud = hud.sample(1,gameCycle,HudHealth.scaled(actor.getHealthRatio(),actor.getHealthScale()),HudHealth.UNKNOWN,skin);
        }
        if (last == null || last.model != model || last.textures != textures || last.hud != nextHud
            || last.cycle != cycle || last.observedAt != observedAt || last.live != live || last.npcDefinition!=npcDefinition)
            last = new Packet(model,textures,nextHud,cycle,observedAt,live,npcDefinition);
        return last;
    }

    void detach()
    {
        live = false;
        binding.release();
        hud.clear();
    }

    void resetTiming() { scheduled = false; }
    void clearHud() { hud.clear(); }

    void close()
    {
        detach();
        model = null; textures = TextureSet.EMPTY; last = null;
    }
}
