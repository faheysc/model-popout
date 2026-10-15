package com.modelpopout;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.modelpopout.InspectionFixtures.*;

/** Uses the real manager and session; substitutes only native window creation and external API reads. */
public class InspectionManagerTest
{
    @Test public void independentTextureSamplingIsBoundedAndFairAcrossSeveralSlowPoseInspectors() throws Exception
    {
        Fixture f=new Fixture();int[] loads=new int[6],pixels=new int[64*64];pixels[0]=0xffffff;pixels[1]=0xff4400;
        net.runelite.api.Texture[] metadata=new net.runelite.api.Texture[6];
        for (int i=0;i<metadata.length;i++) metadata[i]=new TestTexture(pixels,1,1);
        f.materials=(net.runelite.api.TextureProvider)Proxy.newProxyInstance(net.runelite.api.TextureProvider.class.getClassLoader(),
            new Class<?>[]{net.runelite.api.TextureProvider.class},(proxy,method,args) -> {
                switch (method.getName())
                {
                    case "getBrightness": return .8;
                    case "getDefaultColor": return 5000;
                    case "getTextures": return metadata;
                    case "load": loads[(Integer)args[0]]++;return pixels;
                    default: throw new AssertionError("Unexpected texture API read: "+method.getName());
                }
            });
        f.fps=1;List<Person> actors=new ArrayList<>();
        try
        {
            for (int i=0;i<6;i++) { Person actor=new Person(f.world,i);actor.mesh.texture=i;actors.add(actor);f.select(actor); }
            for (int round=0;round<12;round++)
            {
                int beforeLoads=java.util.Arrays.stream(loads).sum(),beforeModels=actors.stream().mapToInt(actor -> actor.reads).sum();
                f.cycle++;f.manager.tick();f.flush();
                int modelReads=actors.stream().mapToInt(actor -> actor.reads).sum()-beforeModels;
                int materialReads=java.util.Arrays.stream(loads).sum()-beforeLoads-modelReads;
                assertTrue(materialReads<=InspectionManager.TEXTURE_SAMPLES_PER_TICK);assertTrue(materialReads>=0);
                assertTrue(f.worker.tasks.size()<=1);
            }
            for (int i=0;i<6;i++) assertTrue("Every texture must receive independent sampling",loads[i]>actors.get(i).reads);
        }
        finally { f.stop(); }
    }
    @Test public void routesHitsplatsOnlyToTheInspectedActorAndClearsHudDuringLoading() throws Exception
    {
        Fixture f=new Fixture();
        try
        {
            Person a=new Person(f.world,1),b=new Person(f.world,2);f.select(a);f.select(b);f.render();
            net.runelite.api.events.HitsplatApplied event=new net.runelite.api.events.HitsplatApplied();
            event.setActor(a);event.setHitsplat(new net.runelite.api.Hitsplat()
            {
                @Override public int getHitsplatType() { return 16; }
                @Override public int getAmount() { return 12; }
                @Override public int getDisappearsOnGameCycle() { return 150; }
            });
            f.manager.hitsplat(event);f.manager.tick();f.flush();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(1,f.windows.get(0).panel.hud.player.hits.size());
                assertTrue(f.windows.get(1).panel.hud.player.hits.isEmpty());
            });
            f.gameState=GameState.LOADING;f.manager.gameStateChanged(f.gameState);f.flush();
            SwingUtilities.invokeAndWait(() -> { assertNull(f.windows.get(0).panel.hud);assertNotNull(f.windows.get(0).panel.image); });
            f.gameState=GameState.LOGGED_IN;f.manager.gameStateChanged(f.gameState);f.manager.tick();f.flush();f.render();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("HUD must match even when the resumed geometry is unchanged",
                    f.windows.get(0).panel.hud.player.key,f.windows.get(0).panel.imageHudKey);
            });
        }
        finally { f.stop(); }
    }
    private static final class ClientQueue extends ClientThread
    {
        private final Queue<Runnable> tasks=new ArrayDeque<>();
        @Override public synchronized void invokeLater(Runnable task) { tasks.add(task); }
        synchronized void flush() { while (!tasks.isEmpty()) tasks.remove().run(); }
    }

    private static final class Fixture
    {
        final World world=new World(-1);
        final Menus menus=new Menus();
        final Worker worker=new Worker();
        final ClientQueue clientQueue=new ClientQueue();
        final List<InspectionSession> windows=new ArrayList<>();
        GameState gameState=GameState.LOGGED_IN;
        boolean enabled=true;
        int cycle=100, fps=20;
        net.runelite.api.TextureProvider materials;
        final ModelPopoutConfig config=new ModelPopoutConfig()
        {
            @Override public boolean rightClickInspection() { return enabled; }
            @Override public RenderTarget renderTarget() { return RenderTarget.UNLIMITED; }
            @Override public int refreshRate() { return fps; }
        };
        final Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args) -> {
            if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("Native client read on EDT: "+method.getName());
            switch (method.getName())
            {
                case "isClientThread": return true;
                case "getGameState": return gameState;
                case "getWorldView": return world;
                case "getGameCycle": return cycle;
                case "getMenu": return menus;
                case "getTextureProvider": return materials;
                case "isGpu": return false;
                default: throw new AssertionError("Unexpected native read: "+method.getName());
            }
        });
        final InspectionManager manager=new InspectionManager(client,clientQueue,config,
            new HudAssets(id -> null,id -> null,id -> -1,id -> -1),worker,(window,name) -> windows.add(window));

        void select(Actor actor) throws Exception
        {
            menus.entries.clear();menus.add("Cancel",null);menus.add("Follow",actor);
            manager.menuOpened();MenuEntry popout=menus.popouts().get(0);popout.onClick().accept(popout);
            flush();
        }
        void flush() throws Exception { SwingUtilities.invokeAndWait(() -> {});clientQueue.flush();SwingUtilities.invokeAndWait(() -> {}); }
        void render() throws Exception
        {
            for (int i=0;i<30 && !worker.tasks.isEmpty();i++) { worker.run();flush(); }
            assertTrue(worker.tasks.isEmpty());
        }
        void stop() throws Exception { manager.stop();flush();render();clientQueue.flush(); }
    }

    @Test public void inspectorsWorkWithoutTheMainWindowAndClosingOneDoesNotStopAnother() throws Exception
    {
        Fixture f=new Fixture();
        try
        {
            Person player=new Person(f.world,1);Npc npc=new Npc(f.world,2);
            f.select(player);f.select(npc);f.render();assertEquals(2,f.manager.size());
            SwingUtilities.invokeAndWait(() -> {
                assertNotNull(f.windows.get(0).panel.image);assertNotNull(f.windows.get(1).panel.image);
                f.windows.get(0).close();
            });
            f.flush();int previous=player.reads;f.manager.settingsChanged();f.flush();f.manager.tick();f.flush();
            assertEquals(previous,player.reads);assertEquals(1,f.manager.size());assertTrue(npc.reads>1);
        }
        finally { f.stop(); }
    }

    @Test public void despawnAndIndexReuseFreezeTheOriginalAndHopDoesNotReattachIt() throws Exception
    {
        Fixture f=new Fixture();
        try
        {
            Person original=new Person(f.world,1);f.select(original);f.render();
            f.manager.despawned(original);Person replacement=new Person(f.world,1);f.flush();
            f.manager.tick();f.flush();f.render();
            assertEquals(0,replacement.reads);
            SwingUtilities.invokeAndWait(() -> {
                assertNotNull(f.windows.get(0).panel.image);assertNull(f.windows.get(0).panel.hud);
                assertTrue(f.windows.get(0).panel.status.contains("last captured"));
            });
            f.select(replacement);f.gameState=GameState.HOPPING;f.manager.gameStateChanged(f.gameState);f.flush();
            int reads=replacement.reads;f.gameState=GameState.LOGGED_IN;f.manager.gameStateChanged(f.gameState);
            f.manager.tick();f.flush();assertEquals(reads,replacement.reads);
        }
        finally { f.stop(); }
    }

    @Test public void actorGoneBetweenMenuAndClickOpensAnUnavailableWindowAndOldWorldMenusAreFenced() throws Exception
    {
        Fixture f=new Fixture();
        try
        {
            Person actor=new Person(f.world,1);f.menus.add("Follow",actor);f.manager.menuOpened();
            MenuEntry popout=f.menus.popouts().get(0);f.world.players.remove(1);popout.onClick().accept(popout);f.flush();
            assertEquals(1,f.windows.size());assertEquals(0,actor.reads);
            SwingUtilities.invokeAndWait(() -> {
                assertNull(f.windows.get(0).panel.image);assertTrue(f.windows.get(0).panel.status.contains("no model captured"));
            });
            f.gameState=GameState.HOPPING;f.manager.gameStateChanged(f.gameState);
            f.gameState=GameState.LOGGED_IN;popout.onClick().accept(popout);f.flush();
            assertEquals(1,f.manager.size());
        }
        finally { f.stop(); }
    }

    @Test public void captureBudgetIsBoundedFairAndMinimizedWindowsStopModelReads() throws Exception
    {
        Fixture f=new Fixture();List<Person> actors=new ArrayList<>();
        try
        {
            for (int i=0;i<6;i++) { Person actor=new Person(f.world,i);actors.add(actor);f.select(actor); }
            f.render();SwingUtilities.invokeAndWait(() -> f.windows.get(0).setVisible(false));f.flush();
            int minimizedReads=actors.get(0).reads;
            for (int round=0;round<10;round++)
            {
                f.manager.settingsChanged();f.flush();
                int before=actors.stream().mapToInt(actor -> actor.reads).sum();
                f.manager.tick();f.flush();
                int after=actors.stream().mapToInt(actor -> actor.reads).sum();
                assertTrue(after-before<=InspectionManager.CAPTURES_PER_TICK);
                assertTrue(f.worker.tasks.size()<=1);
            }
            assertEquals(minimizedReads,actors.get(0).reads);
            for (int i=1;i<actors.size();i++) assertTrue("Every visible inspector must get a capture",actors.get(i).reads>1);
            SwingUtilities.invokeAndWait(() -> f.windows.get(0).setVisible(true));f.flush();
            f.manager.tick();f.flush();
        }
        finally { f.stop(); }
    }

    @Test public void loadingPausesCaptureWithoutDetachingAndDisablingMenusKeepsExistingWindows() throws Exception
    {
        Fixture f=new Fixture();
        try
        {
            Person actor=new Person(f.world,1);f.select(actor);f.render();int before=actor.reads;
            f.gameState=GameState.LOADING;f.manager.gameStateChanged(f.gameState);f.manager.tick();f.flush();
            assertEquals(before,actor.reads);
            f.gameState=GameState.LOGGED_IN;f.manager.gameStateChanged(f.gameState);
            f.manager.settingsChanged();f.flush();f.manager.tick();f.flush();assertTrue(actor.reads>before);
            f.enabled=false;f.manager.settingsChanged();f.flush();assertTrue(f.menus.popouts().isEmpty());
            assertEquals(1,f.manager.size());
        }
        finally { f.stop(); }
    }

    @Test public void shutdownBeforeWindowConstructionDropsAllPendingWork() throws Exception
    {
        Fixture f=new Fixture();Person actor=new Person(f.world,1);
        f.menus.add("Follow",actor);f.manager.menuOpened();MenuEntry entry=f.menus.popouts().get(0);
        // Block the EDT until both the open and stop have been scheduled on the client side.
        java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
        SwingUtilities.invokeLater(() -> { entered.countDown();try { release.await(); } catch (InterruptedException failure) { throw new AssertionError(failure); } });
        assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));
        try { entry.onClick().accept(entry);f.manager.stop(); }
        finally { release.countDown(); }
        f.flush();assertTrue(f.windows.isEmpty());assertEquals(0,f.manager.size());assertTrue(f.worker.tasks.isEmpty());
        assertTrue(f.worker.isShutdown());
    }
}
