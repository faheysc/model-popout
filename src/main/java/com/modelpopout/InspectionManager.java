package com.modelpopout;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Separate from the main viewer: client-owned capture sources and EDT-owned windows. */
final class InspectionManager
{
    private static final Logger LOG=LoggerFactory.getLogger(InspectionManager.class);
    static final int CAPTURES_PER_TICK=2;
    static final int TEXTURE_SAMPLES_PER_TICK=2;
    static final long CAPTURE_BUDGET_NANOS=4_000_000L;
    private final Client client;
    private final ClientThread clientThread;
    private final ModelPopoutConfig config;
    private final HudAssets assets;
    private final CopyOnWriteArrayList<Entry> entries=new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final InspectionRenderScheduler scheduler;
    private final java.util.function.BiConsumer<InspectionSession,String> show;
    private volatile boolean running=true;
    private long gameSession;
    private int cursor, textureCursor;

    InspectionManager(Client client, ClientThread clientThread, ModelPopoutConfig config, SpriteManager sprites)
    {
        this(client,clientThread,config,new HudAssets(id -> client.getIndexConfig()==null?null:client.getIndexConfig().loadData(32,id),
            id -> sprites.getSprite(id,0),client::getVarbitValue,client::getVarpValue),newExecutor(),InspectionSession::open);
    }

    InspectionManager(Client client, ClientThread clientThread, ModelPopoutConfig config, HudAssets assets,
        ExecutorService executor, java.util.function.BiConsumer<InspectionSession,String> show)
    {
        this.client=client;this.clientThread=clientThread;this.config=config;
        this.assets=assets;this.executor=executor;this.show=show;
        scheduler=new InspectionRenderScheduler(executor,SwingUtilities::invokeLater);
    }

    private static ExecutorService newExecutor()
    {
        return new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),task -> {
            Thread thread=new Thread(task,"ModelPopout-inspectors");thread.setDaemon(true);return thread;
        });
    }

    void menuOpened()
    {
        assert client.isClientThread();
        if (!running || client.getGameState()!=GameState.LOGGED_IN) return;
        long session=gameSession;
        new InspectionMenu(() -> running && config.rightClickInspection(),client::getWorldView,binding -> {
            if (session==gameSession) open(binding);
            else binding.release();
        }).add(client.getMenu());
    }

    private void open(InspectionActor binding)
    {
        assert client.isClientThread();
        if (!running) { binding.release();return; }
        Entry entry=new Entry(binding);
        entries.add(entry);
        // Queue construction before any mailbox delivery, including the immediate initial capture.
        SwingUtilities.invokeLater(() -> {
            if (!running || entry.closed) return;
            try
            {
                entry.window=new InspectionSession(config,scheduler,() -> close(entry),value -> {
                    entry.visible=value;
                    clientThread.invokeLater(() -> {
                        if (!entry.closed)
                        {
                            entry.capture.resetTiming();
                            entry.capture.clearHud();
                            if (value) sampleHud(entry);
                        }
                    });
                });
                show.accept(entry.window,binding.name);
            }
            catch (RuntimeException failure)
            {
                LOG.warn("Unable to open actor inspector",failure);
                if (entry.window!=null) entry.window.close();
                else close(entry);
            }
        });
        Actor actor=client.getGameState()==GameState.LOGGED_IN?resolve(entry):null;
        if (actor!=null) capture(entry,actor);
        sampleHud(entry);
    }

    void tick()
    {
        assert client.isClientThread();
        if (!running || entries.isEmpty() || client.getGameState()!=GameState.LOGGED_IN) return;
        // Identity and HUD updates are cheap and independent of model capture throttling.
        for (Entry entry:entries) sampleHud(entry);
        Entry[] round=entries.toArray(new Entry[0]);
        long started=System.nanoTime();
        int captured=0;
        for (int visited=0;visited<round.length;visited++)
        {
            Entry entry=round[Math.floorMod(cursor++,round.length)];
            if (entry.closed || !entry.visible || !entry.capture.due(System.nanoTime())) continue;
            Actor actor=resolve(entry);
            if (actor!=null) { capture(entry,actor);sampleHud(entry);captured++; }
            if (captured>=CAPTURES_PER_TICK || System.nanoTime()-started>=CAPTURE_BUDGET_NANOS) break;
        }
        int sampled=0;
        for (int visited=0;visited<round.length && System.nanoTime()-started<CAPTURE_BUDGET_NANOS;visited++)
        {
            Entry entry=round[Math.floorMod(textureCursor++,round.length)];
            int cycle=client.getGameCycle();
            if (entry.closed || !entry.visible || !entry.capture.textureDue(cycle) || resolve(entry)==null) continue;
            try { entry.capture.sampleTextures(client.getTextureProvider(),client.isGpu(),cycle,System.nanoTime()); }
            catch (RuntimeException failure) { LOG.debug("Unable to sample inspector textures; retaining owned material data",failure); }
            sampleHud(entry);
            if (++sampled>=TEXTURE_SAMPLES_PER_TICK) break;
        }
    }

    private Actor resolve(Entry entry)
    {
        if (entry.closed || !entry.capture.live()) return null;
        Actor actor=entry.capture.binding.resolve(client::getWorldView);
        if (actor==null) entry.capture.detach();
        return actor;
    }

    private void capture(Entry entry, Actor actor)
    {
        try
        {
            entry.capture.capture(actor,client.getTextureProvider(),client.isGpu(),client.getGameCycle(),
                System.nanoTime(),config.refreshRate());
        }
        catch (RuntimeException failure) { LOG.debug("Unable to capture inspected actor; retaining last model",failure); }
    }

    private void sampleHud(Entry entry)
    {
        if (entry.closed) return;
        if (!entry.capture.live() && entry.published!=null && !entry.published.live) return;
        if (entry.capture.live() && client.getGameState()!=GameState.LOGGED_IN) return;
        try
        {
            Actor actor=resolve(entry); // Validate even while minimized, so a reused slot never revives a binding.
            if (!entry.visible && entry.capture.live()) return;
            int cycle=client.getGameCycle();
            InspectionCapture.Packet packet=entry.capture.packet(actor,cycle,
                actor==null?HudAssets.HealthSkin.FALLBACK:assets.healthSkin(cycle));
            if (packet!=entry.published) { entry.published=packet;entry.mailbox.offer(packet); }
        }
        catch (RuntimeException failure) { LOG.debug("Unable to sample inspected actor HUD",failure); }
    }

    void hitsplat(HitsplatApplied event)
    {
        assert client.isClientThread();
        if (!running || client.getGameState()!=GameState.LOGGED_IN) return;
        for (Entry entry:entries)
        {
            if (entry.closed || !entry.visible || !entry.capture.binding.matches(event.getActor())) continue;
            try
            {
                Actor actor=resolve(entry);
                if (actor!=null) entry.capture.hit(actor,event.getHitsplat(),client.getGameCycle(),assets);
            }
            catch (RuntimeException failure) { LOG.debug("Unable to capture inspected actor hitsplat",failure); }
        }
    }

    void despawned(Actor actor)
    {
        for (Entry entry:entries) if (!entry.closed && entry.capture.binding.matches(actor))
        { entry.capture.detach();sampleHud(entry); }
    }

    void gameStateChanged(GameState state)
    {
        assert client.isClientThread();
        if (state==GameState.LOGGED_IN) return;
        if (state==GameState.LOADING)
        {
            for (Entry entry:entries) if (!entry.closed && entry.capture.live())
            {
                entry.capture.clearHud();
                InspectionCapture.Packet packet=entry.capture.packet(null,client.getGameCycle(),HudAssets.HealthSkin.FALLBACK);
                entry.published=packet;entry.mailbox.offer(packet);
            }
            return;
        }
        gameSession++;
        for (Entry entry:entries) if (!entry.closed)
        { entry.capture.detach();sampleHud(entry); }
    }

    void settingsChanged()
    {
        clientThread.invokeLater(() -> {
            if (!running) return;
            if (!config.rightClickInspection()) InspectionMenu.remove(client.getMenu());
            for (Entry entry:entries) if (!entry.closed) entry.capture.resetTiming();
        });
        SwingUtilities.invokeLater(() -> {
            if (running) for (Entry entry:entries) if (entry.window!=null) entry.window.settingsChanged();
        });
    }

    private void close(Entry entry)
    {
        entry.closed=true;entry.visible=false;entry.mailbox.close();entries.remove(entry);
        clientThread.invokeLater(() -> { entry.published=null;entry.capture.close(); });
    }

    void stop()
    {
        running=false;
        Entry[] closing=entries.toArray(new Entry[0]);
        for (Entry entry:closing) close(entry);
        SwingUtilities.invokeLater(() -> {
            scheduler.close();
            for (Entry entry:closing) if (entry.window!=null) entry.window.close();
        });
        executor.shutdown(); // No interrupt/sleep; any current raster finishes without publishing to a closed session.
    }

    int size() { return entries.size(); }

    private static final class Entry
    {
        final InspectionCapture capture;
        final InspectionMailbox mailbox;
        volatile boolean closed, visible=true;
        InspectionSession window; // EDT only.
        InspectionCapture.Packet published; // Client thread only.
        Entry(InspectionActor binding)
        {
            capture=new InspectionCapture(binding);
            mailbox=new InspectionMailbox(SwingUtilities::invokeLater,packet -> {
                if (!closed && window!=null) window.accept(packet);
            });
        }
    }
}
