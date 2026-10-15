package com.modelpopout;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static com.modelpopout.InspectionFixtures.*;
import static org.junit.Assert.*;

/** Real launcher, config events, manager and native windows; game API reads use fixtures. */
public class NativePluginConfigurationTest
{
    @Rule public TemporaryFolder temporary=new TemporaryFolder();

    private static final class ClientQueue extends ClientThread
    {
        final Queue<Runnable> tasks=new ArrayDeque<>();
        @Override public synchronized void invokeLater(Runnable task) { tasks.add(task); }
        synchronized void flush() { while (!tasks.isEmpty()) tasks.remove().run(); }
    }

    @Test public void launcherAndStandardConfigStillControlOpenMainAndInspectionWindows() throws Exception
    {
        Assume.assumeTrue(Boolean.getBoolean("modelpopout.desktopTests"));
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        java.awt.Robot desktop=new java.awt.Robot();
        ClientQueue queue=new ClientQueue();World world=new World(-1);Menus menus=new Menus();Worker worker=new Worker();
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args) -> {
            assertFalse("Game reads must stay off EDT",SwingUtilities.isEventDispatchThread());
            switch(method.getName())
            {
                case "isClientThread": return true;
                case "getGameState": return GameState.LOGGED_IN;
                case "getWorldView": return world;
                case "getGameCycle": return 100;
                case "getMenu": return menus;
                case "getLocalPlayer": case "getTextureProvider": return null;
                case "isGpu": return false;
                default: throw new AssertionError(method.getName());
            }
        });
        try (ConfigTestSupport profile=new ConfigTestSupport(temporary.newFile(),client))
        {
            ModelPopoutPlugin plugin=new ModelPopoutPlugin();List<InspectionSession> sessions=new ArrayList<>();
            HudAssets assets=new HudAssets(id -> null,id -> null,id -> -1,id -> -1);
            InspectionManager manager=new InspectionManager(client,queue,profile.config,assets,worker,(session,name) -> {
                sessions.add(session);session.open(name);
            });
            ConfigTestSupport.set(plugin,"config",profile.config);ConfigTestSupport.set(plugin,"configManager",profile.manager);
            ConfigTestSupport.set(plugin,"client",client);ConfigTestSupport.set(plugin,"clientThread",queue);
            ConfigTestSupport.set(plugin,"hudAssets",assets);ConfigTestSupport.set(plugin,"inspectors",manager);
            ConfigTestSupport.set(plugin,"running",true);profile.events.register(plugin);
            try
            {
                plugin.createLaunchButton().getOnClick().run();flush(queue);
                ViewerWindow main=ConfigTestSupport.get(plugin,"window");assertNotNull(main);
                Person actor=new Person(world,1);menus.entries.clear();menus.add("Cancel",null);menus.add("Follow",actor);
                manager.menuOpened();menus.popouts().get(0).onClick().accept(menus.popouts().get(0));flush(queue);
                assertEquals(1,sessions.size());ViewerWindow inspector=ConfigTestSupport.get(sessions.get(0),"window");
                for(int opacity:new int[]{0,30,50,100})
                {
                    profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"backgroundOpacity",opacity);flush(queue);
                    SwingUtilities.invokeAndWait(() -> {
                        assertEquals(main.effectiveOpacity(),inspector.effectiveOpacity());
                        try { assertEquals(BackgroundOpacity.effective(opacity,ConfigTestSupport.<Boolean>get(main,"supported")),main.effectiveOpacity()); }
                        catch(Exception failure) { throw new AssertionError(failure); }
                    });
                }
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"alwaysOnTop",true);
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"renderNpc",false);
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"renderTarget",RenderTarget.FPS_144);
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"refreshRate",25);
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"showHitsplats",false);
                profile.manager.setConfiguration(ModelPopoutConfig.GROUP,"showHpBars",false);flush(queue);
                SwingUtilities.invokeAndWait(() -> {
                    try
                    {
                        LiveViewState state=ConfigTestSupport.get(plugin,"viewState");assertFalse(state.npcEnabled());
                        assertEquals(main.isAlwaysOnTop(),inspector.isAlwaysOnTop());
                        if(main.isAlwaysOnTopSupported()) assertTrue(main.isAlwaysOnTop());
                        for(Object owner:new Object[]{plugin,sessions.get(0)})
                        {
                            FramePacer pacer=ConfigTestSupport.get(owner,"pacer");pacer.rendered(0);
                            assertEquals(1_000_000_000L/144,pacer.renderDelay(0));
                        }
                        LiveCaptureGate gate=ConfigTestSupport.get(plugin,"captures");gate.setLoggedIn(true);gate.setRate(25);
                        long token=gate.acquire(0);assertTrue(token>=0);gate.complete(token);
                        assertEquals(-1,gate.acquire(39_999_999));assertTrue(gate.acquire(40_000_000)>=0);
                        assertFalse(profile.config.showHitsplats());assertFalse(profile.config.showHpBars());
                    }
                    catch(Exception failure) { throw new AssertionError(failure); }
                });
                // Clicking the retained launcher reuses the current window and requests restoration.
                SwingUtilities.invokeAndWait(() -> main.setExtendedState(Frame.ICONIFIED));desktop.waitForIdle();
                plugin.createLaunchButton().getOnClick().run();flush(queue);desktop.waitForIdle();
                SwingUtilities.invokeAndWait(() -> {
                    try { assertSame(main,ConfigTestSupport.get(plugin,"window")); }
                    catch(Exception failure) { throw new AssertionError(failure); }
                    assertEquals(0,main.getExtendedState() & Frame.ICONIFIED);
                    main.dispatchEvent(new WindowEvent(main,WindowEvent.WINDOW_CLOSING));
                });flush(queue);
                assertNull(ConfigTestSupport.get(plugin,"window"));assertFalse(sessions.get(0).closed());
                assertTrue((Boolean)ConfigTestSupport.get(plugin,"running"));
            }
            finally
            {
                profile.events.unregister(plugin);plugin.shutDown();flush(queue);
                for(int i=0;i<20 && !worker.tasks.isEmpty();i++) { worker.run();flush(queue); }
            }
        }
    }

    private static void flush(ClientQueue queue) throws Exception
    { SwingUtilities.invokeAndWait(() -> {});queue.flush();SwingUtilities.invokeAndWait(() -> {}); }
}
