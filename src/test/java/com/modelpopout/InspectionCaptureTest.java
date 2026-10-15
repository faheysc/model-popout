package com.modelpopout;

import net.runelite.api.Hitsplat;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

public class InspectionCaptureTest
{
    @Test public void animatedMaterialsUpdateBetweenSlowPoseCapturesAndStopWhenFrozen()
    {
        World world=new World(-1);Person actor=new Person(world,1);actor.mesh.texture=0;
        int[] pixels=new int[64*64];java.util.Arrays.fill(pixels,0xff4400);pixels[1]=0xffffff;
        net.runelite.api.TextureProvider provider=(net.runelite.api.TextureProvider)java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.TextureProvider.class.getClassLoader(),new Class<?>[]{net.runelite.api.TextureProvider.class},(proxy,method,args) -> {
                switch (method.getName())
                {
                    case "getBrightness": return .8;
                    case "getDefaultColor": return 5000;
                    case "getTextures": return new net.runelite.api.Texture[]{new TestTexture(pixels,1,1)};
                    case "load": return pixels;
                    default: throw new AssertionError("Unexpected texture mutation/read: "+method.getName());
                }
            });
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        source.capture(actor,provider,false,100,0,1);InspectionCapture.Packet original=packet(source,actor,100);
        assertFalse(source.due(20_000_000L));assertTrue(source.textureDue(101));
        pixels[0]=0x00ff00;source.sampleTextures(provider,false,101,20_000_000L);
        InspectionCapture.Packet material=packet(source,actor,101);
        assertSame(original.model,material.model);assertEquals(1,actor.reads);assertEquals(101,material.cycle);
        assertEquals(0xff4400,original.textures.texture(0).rgb[0]);assertEquals(0x00ff00,material.textures.texture(0).rgb[0]);
        source.detach();assertFalse(source.textureDue(102));source.sampleTextures(provider,false,102,40_000_000L);
        assertSame(material.textures,packet(source,null,102).textures);assertEquals(101,packet(source,null,102).cycle);
    }
    @Test public void failedTextureProviderStillPublishesValidNewGeometry()
    {
        World world=new World(-1);Person actor=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        source.capture(actor,null,false,100,0,20);ModelSnapshot previous=packet(source,actor,100).model;
        actor.mesh.x[0]=-200;
        net.runelite.api.TextureProvider broken=(net.runelite.api.TextureProvider)java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.TextureProvider.class.getClassLoader(),new Class<?>[]{net.runelite.api.TextureProvider.class},(proxy,method,args) -> {
                if (method.getName().equals("getBrightness")) return .8;
                throw new IllegalStateException("Texture cache unavailable");
            });
        try { source.capture(actor,broken,false,101,50_000_000L,20);fail(); }
        catch (IllegalStateException expected) { }
        assertEquals(-80,previous.x[0],0);assertEquals(-200,packet(source,actor,101).model.x[0],0);
    }
    @Test public void defaultRateMaintainsTwentyCapturesOnTheNativeTwentyMillisecondCadence()
    {
        World world=new World(-1);Person actor=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        for (long now=0;now<2_000_000_000L;now+=20_000_000L)
            if (source.due(now)) source.capture(actor,null,false,(int)(now/20_000_000L),now,20);
        assertEquals(40,actor.reads);
    }
    @Test public void capturedTexturesAreOwnedAndRemainAvailableAfterDetaching()
    {
        World world=new World(-1);Person actor=new Person(world,1);actor.mesh.texture=0;
        int[] pixels=new int[64*64];java.util.Arrays.fill(pixels,0xff4400);
        net.runelite.api.TextureProvider provider=(net.runelite.api.TextureProvider)java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.TextureProvider.class.getClassLoader(),new Class<?>[]{net.runelite.api.TextureProvider.class},(proxy,method,args) -> {
                switch (method.getName())
                {
                    case "getBrightness": return .8;
                    case "getDefaultColor": return 5000;
                    case "getTextures": return new net.runelite.api.Texture[]{new TestTexture(pixels,1,1)};
                    case "load": return pixels;
                    default: throw new AssertionError("Unexpected texture mutation/read: "+method.getName());
                }
            });
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        source.capture(actor,provider,false,100,0,20);InspectionCapture.Packet owned=packet(source,actor,100);
        pixels[0]=0x00ff00;source.detach();InspectionCapture.Packet frozen=packet(source,null,101);
        assertSame(owned.textures,frozen.textures);assertEquals(0xff4400,frozen.textures.texture(0).rgb[0]);
    }
    private final HudAssets assets=new HudAssets(id -> null,id -> null,id -> -1,id -> -1);
    private InspectionCapture.Packet packet(InspectionCapture source,net.runelite.api.Actor actor,int cycle)
    { return source.packet(actor,cycle,HudAssets.HealthSkin.FALLBACK); }

    @Test public void copiesLiveEquipmentAndTransformationChangesWhileKeepingEarlierSnapshotsOwned()
    {
        World world=new World(-1);Person player=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(player,id -> world));
        source.capture(player,null,false,100,0,20);InspectionCapture.Packet first=packet(source,player,100);
        player.mesh.x[0]=-120;player.mesh.color=9000;
        source.capture(player,null,false,101,50_000_000L,20);InspectionCapture.Packet equipped=packet(source,player,101);
        assertEquals(-80,first.model.x[0],0);assertEquals(-120,equipped.model.x[0],0);
        assertFalse(first.model.sameAppearance(equipped.model));
        Npc npc=new Npc(world,2);InspectionCapture n=new InspectionCapture(InspectionActor.bind(npc,id -> world));
        n.capture(npc,null,false,100,0,20);ModelSnapshot before=packet(n,npc,100).model;
        npc.id=200;npc.mesh=new Mesh();npc.mesh.y[2]=-300;
        n.capture(npc,null,false,102,50_000_000L,20);
        assertSame(npc,n.binding.resolve(id -> world));assertFalse(before.sameAppearance(packet(n,npc,102).model));
        assertEquals(200,packet(n,npc,102).npcDefinition);
    }

    @Test public void nullModelDetachAndClosePreserveOnlyTheCorrectLastCapture()
    {
        World world=new World(-1);Person actor=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        source.capture(actor,null,false,100,0,20);ModelSnapshot last=packet(source,actor,100).model;
        actor.mesh=null;source.capture(actor,null,false,101,50_000_000L,20);
        assertSame(last,packet(source,actor,101).model);
        source.detach();new Person(world,1);
        InspectionCapture.Packet frozen=packet(source,null,102);
        assertFalse(frozen.live);assertSame(last,frozen.model);assertNull(frozen.hud);
        assertFalse(source.due(Long.MAX_VALUE));assertNull(source.binding.resolve(id -> world));
        source.close();assertNull(packet(source,null,103).model);
    }

    @Test public void disappearingBeforeTheFirstCaptureDoesNotFabricateGeometry()
    {
        World world=new World(-1);Person actor=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        source.detach();InspectionCapture.Packet empty=packet(source,null,100);
        assertNull(empty.model);assertFalse(empty.live);assertNull(empty.hud);
    }

    @Test public void remotePlayersAndNpcsUseOnlyScaledHealthAndTheirOwnObservedHits()
    {
        World world=new World(-1);Person a=new Person(world,1),b=new Person(world,2);
        InspectionCapture one=new InspectionCapture(InspectionActor.bind(a,id -> world));
        InspectionCapture two=new InspectionCapture(InspectionActor.bind(b,id -> world));
        Hitsplat hit=new Hitsplat()
        {
            @Override public int getHitsplatType() { return 16; }
            @Override public int getAmount() { return 7; }
            @Override public int getDisappearsOnGameCycle() { return 150; }
        };
        one.hit(a,hit,100,assets);two.hit(a,hit,100,assets);
        HudSnapshot first=packet(one,a,100).hud;
        assertEquals(.5,first.player.health.fraction,0);assertFalse(first.player.health.exact);
        assertEquals(1,first.player.hits.size());assertTrue(packet(two,b,100).hud.player.hits.isEmpty());
        assertTrue(packet(one,a,150).hud.player.hits.isEmpty());
        a.ratio=-1;assertFalse(packet(one,a,151).hud.player.health.visible);
        Npc npc=new Npc(world,3);InspectionCapture n=new InspectionCapture(InspectionActor.bind(npc,id -> world));
        n.hit(npc,hit,100,assets);HudSnapshot npcHud=packet(n,npc,100).hud;
        assertFalse(npcHud.player.health.exact);assertEquals(.5,npcHud.player.health.fraction,0);
        assertEquals(1,npcHud.player.hits.size());
    }

    @Test public void rateChangesBackpressureAndIdlePacketReuseAvoidCatchupWork()
    {
        World world=new World(-1);Person actor=new Person(world,1);
        InspectionCapture source=new InspectionCapture(InspectionActor.bind(actor,id -> world));
        assertTrue(source.due(0));source.capture(actor,null,false,100,0,20);
        assertFalse(source.due(49_999_999));assertTrue(source.due(50_000_000));
        InspectionCapture.Packet first=packet(source,actor,100);
        assertSame(first,packet(source,actor,101)); // No HUD activity and no new pose/texture observation.
        source.capture(actor,null,false,110,1_000_000_000L,20);
        assertFalse(source.due(1_000_000_001L));assertSame(first.model,packet(source,actor,110).model);
        source.resetTiming();assertTrue(source.due(1_000_000_001L));
    }
}
