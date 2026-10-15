package com.modelpopout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.Skill;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.game.SpriteManager;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Model Popout",
    description = "Display your live character and interacting NPC in a movable, resizable window with animated models, camera controls, and adjustable transparency.",
    tags = {"model", "player", "npc", "popout"}
)
public class ModelPopoutPlugin extends Plugin
{
    private static final Logger LOG = LoggerFactory.getLogger(ModelPopoutPlugin.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private ModelPopoutConfig config;
    @Inject private ConfigManager configManager;
    @Inject private SpriteManager spriteManager;
    private final HudTracker hudTracker = new HudTracker();
    private HudAssets hudAssets;
    private volatile HudSnapshot clientHud;
    private final LatestHudMailbox hudMailbox = new LatestHudMailbox(SwingUtilities::invokeLater,this::receiveHud);
    private final NpcTargetTracker npcTargets = new NpcTargetTracker();
    private final LiveCaptureGate captures = new LiveCaptureGate();
    private volatile boolean running;
    private volatile boolean viewerVisible;
    private volatile TextureReferences textureReferences;
    private volatile TextureSet clientTextures = TextureSet.EMPTY;
    private final AtomicBoolean texturePending = new AtomicBoolean();
    private final TextureClock textureClock = new TextureClock();
    private NavigationButton navigation;
    // Everything below is confined to the Swing EDT.
    private ViewerWindow window;
    private ModelPanel panel;
    private RenderJob worker;
    private volatile ExecutorService renderExecutor;
    private final FramePacer pacer = new FramePacer();
    private final Timer renderWake = new Timer(1,e -> requestRender());
    private final Timer presentWake = new Timer(1,e -> presentFrame());
    private final boolean paintDiagnostics = Boolean.getBoolean("modelpopout.paintDiagnostics");
    private final PlayerFrameTrace playerTrace = paintDiagnostics ? new PlayerFrameTrace() : null;
    private final boolean diagnosticFreeze = paintDiagnostics && Boolean.getBoolean("modelpopout.traceFreezeImage");
    private final boolean diagnosticNoHud = paintDiagnostics && Boolean.getBoolean("modelpopout.traceDisableHud");
    private long tracePaintAt;
    private long hudDeliveries, modelPublications, modelDraws, drawsOutsideWindowPaint, playerRebindings;
    private final Timer metricsWake = new Timer(1000,e -> {
        if (playerTrace != null) LOG.debug("Model Popout player trace: freeze={} disableHud={}\n{}",diagnosticFreeze,diagnosticNoHud,playerTrace.drain());
        if (paintDiagnostics && window != null) LOG.debug("Model Popout transparency audit: {}",window.transparencyDiagnostics());
        if (paintDiagnostics && window != null)
            LOG.debug("Model Popout paint audit: HUD packets={} model publications={} model draws={} draws outside Window.paint={} player rebindings={} windowOpaque={} rootOpaque={} layerOpaque={} surfaceOpaque={} hitsplats={} bars={}",
                hudDeliveries,modelPublications,modelDraws,drawsOutsideWindowPaint,playerRebindings,
                window.isOpaque(),window.getRootPane().isOpaque(),window.getLayeredPane().isOpaque(),
                window.getContentPane().isOpaque(),config.showHitsplats(),config.showHpBars());
    });
    private CompletedFrame pendingFrame;
    private final LiveViewState viewState = new LiveViewState();
    private volatile InspectionManager inspectors;

    @Provides
    ModelPopoutConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(ModelPopoutConfig.class);
    }

    @Override
    protected void startUp()
    {
        hudAssets = new HudAssets(id -> client.getIndexConfig() == null ? null : client.getIndexConfig().loadData(32,id),
            id -> spriteManager.getSprite(id,0),client::getVarbitValue,client::getVarpValue);
        captures.setRate(config.refreshRate());
        renderExecutor = new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),task -> {
            Thread thread = new Thread(task,"ModelPopout-renderer");
            thread.setDaemon(true);
            return thread;
        });
        clientTextures = TextureSet.EMPTY;
        running = true;
        inspectors = new InspectionManager(client,clientThread,config,spriteManager);
        navigation = createLaunchButton();
        toolbar.addNavigation(navigation);
    }

    @Override
    protected void shutDown()
    {
        running = false;
        InspectionManager closingInspectors = inspectors;
        inspectors = null;
        if (closingInspectors != null) closingInspectors.stop();
        hudTracker.clear(); hudMailbox.clear(); clientHud = null;
        textureReferences = null; clientTextures = TextureSet.EMPTY;
        captures.close();
        if (renderExecutor != null) renderExecutor.shutdown();
        npcTargets.clear();
        if (navigation != null) toolbar.removeNavigation(navigation);
        navigation = null;
        SwingUtilities.invokeLater(() -> {
            renderWake.stop(); presentWake.stop(); metricsWake.stop();
            if (window != null) window.dispose();
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!ModelPopoutConfig.GROUP.equals(event.getGroup())) return;
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.settingsChanged();
        if ("refreshRate".equals(event.getKey())) captures.setRate(config.refreshRate());
        if (ModelPopoutConfig.NPC_KEY.equals(event.getKey())) npcTargets.clear();
        SwingUtilities.invokeLater(() -> {
            if ("renderTarget".equals(event.getKey()))
            {
                pacer.setTarget(config.renderTarget());
                requestRender();
            }
            if (window == null) return;
            if (ModelPopoutConfig.HITSPLATS_KEY.equals(event.getKey()) || ModelPopoutConfig.HP_BARS_KEY.equals(event.getKey())) panel.repaint();
            if (ModelPopoutConfig.ALWAYS_ON_TOP_KEY.equals(event.getKey())) applyWindowStacking();
            if (ModelPopoutConfig.OPACITY_KEY.equals(event.getKey())) window.applyBackgroundOpacity(config.backgroundOpacity());
            if (ModelPopoutConfig.NPC_KEY.equals(event.getKey()))
            {
                viewState.setNpcEnabled(config.renderNpc());
                panel.setPreferredSize(new Dimension(ModelRenderer.SIZE * (config.renderNpc() ? 2 : 1),ModelRenderer.SIZE));
                panel.repaint();
                requestRender();
            }
        });
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (running && captures.viewing() && config.renderNpc()) npcTargets.interaction(event.getSource(),event.getTarget());
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        if (running && captures.viewing() && config.renderNpc()) npcTargets.spawned(event.getNpc());
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        if (running && captures.viewing() && config.renderNpc()) npcTargets.spawned(event.getNpc());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.despawned(event.getNpc());
        long key = npcTargets.despawned(event.getNpc());
        if (key < 0) return;
        hudTracker.selectNpc(null,-1);
        long token = captures.epoch();
        SwingUtilities.invokeLater(() -> {
            if (current(token) && viewState.removeNpc(key))
            {
                panel.repaint();
                requestRender();
            }
        });
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        samplePlayer();
        sampleTextures();
        sampleHud();
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.tick();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.gameStateChanged(event.getGameState());
        boolean loggedIn = event.getGameState() == GameState.LOGGED_IN;
        long token = captures.setLoggedIn(loggedIn);
        if (loggedIn) return;
        textureReferences = null; clientTextures = TextureSet.EMPTY;
        npcTargets.clear();
        hudTracker.clear(); hudMailbox.clear(); clientHud = null;
        SwingUtilities.invokeLater(() -> {
            if (current(token))
            {
                clearModel();
            }
        });
    }

    // Interface Styles condenses players at priority 1; inspect existing submenus after ordinary handlers too.
    @Subscribe(priority = -1)
    public void onMenuOpened(MenuOpened event)
    {
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.menuOpened();
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.despawned(event.getPlayer());
    }

    private void openWindow()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (!running) return;
        if (window != null)
        {
            window.revealControls();
            window.setExtendedState(window.getExtendedState() & ~java.awt.Frame.ICONIFIED);
            window.setVisible(true);
            window.toFront();
            return;
        }
        ViewerWindow frame = ViewerWindow.create();
        window = frame;
        viewerVisible = true;
        frame.addWindowStateListener(e -> {
            viewerVisible = (e.getNewState() & java.awt.Frame.ICONIFIED) == 0;
            if (viewerVisible)
            {
                // Refresh the game-cycle HUD before showing hits retained while minimized.
                hudMailbox.clear();
                if (panel != null) panel.hud = null;
                clientHud = null;
                clientThread.invokeLater(this::sampleHud);
                requestRender();
            }
        });
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);
        captures.open();
        pacer.setTarget(config.renderTarget());
        renderWake.setRepeats(false); presentWake.setRepeats(false);
        if (paintDiagnostics) metricsWake.start();
        viewState.resetFraming();
        viewState.setCamera(CameraView.DEFAULT);
        viewState.setNpcEnabled(config.renderNpc());
        panel = new ModelPanel(config);
        frame.add(panel, java.awt.BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosed(WindowEvent event)
            {
                if (window != frame) return;
                configManager.setConfiguration(ModelPopoutConfig.GROUP,"windowBounds",frame.normalBounds());
                captures.close();
                viewerVisible = false; textureReferences = null; clientTextures = TextureSet.EMPTY;
                renderWake.stop(); presentWake.stop(); metricsWake.stop();
                pendingFrame = null;
                npcTargets.clear();
                clearModel();
                window = null; panel = null;
            }
        });
        ModelPanel modelPanel = panel;
        frame.setModelPainter(g -> {
            Graphics2D modelGraphics = (Graphics2D) g.create(modelPanel.getX(),modelPanel.getY(),modelPanel.getWidth(),modelPanel.getHeight());
            try { modelPanel.paintModels(modelGraphics); }
            finally { modelGraphics.dispose(); }
        });
        frame.installHoverControls();
        frame.applyBackgroundOpacity(config.backgroundOpacity());
        applyWindowStacking();
        frame.setMinimumSize(new Dimension(480, 300));
        frame.pack();
        java.awt.Rectangle saved = configManager.getConfiguration(ModelPopoutConfig.GROUP,"windowBounds",java.awt.Rectangle.class);
        if (saved != null && saved.width >= frame.getMinimumSize().width && saved.height >= frame.getMinimumSize().height
            && java.util.Arrays.stream(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                .anyMatch(device -> WindowGeometry.titleReachable(saved,device.getDefaultConfiguration().getBounds())))
            frame.setBounds(saved);
        else frame.setLocationByPlatform(true);
        frame.setVisible(true);
        clientThread.invokeLater(() -> { samplePlayer(); sampleTextures(); sampleHud(); });
    }

    private void applyWindowStacking()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (!window.applyAlwaysOnTop(config.alwaysOnTop()))
            LOG.debug("Desktop did not accept the Model Popout Always on Top setting");
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        InspectionManager currentInspectors = inspectors;
        if (currentInspectors != null) currentInspectors.hitsplat(event);
        if (diagnosticNoHud || !running || !viewerVisible || !captures.active()) return;
        assert client.isClientThread();
        long generation = hudTracker.generation(), token = captures.epoch();
        Player player = client.getLocalPlayer();
        if (player == null) return;
        // A first hit can precede the next throttled model capture for a newly selected target.
        NpcTargetTracker.Selection selected = event.getActor() instanceof NPC && config.renderNpc()
            ? npcTargets.select(player,token) : npcTargets.currentSelection();
        Actor actor = event.getActor();
        if (actor == null || (actor != player && (!config.renderNpc() || actor != selected.npc))) return;
        Hitsplat hit = event.getHitsplat();
        if (hit == null || hit.getHitsplatType() < 0) return;
        try
        {
            HitsplatStyle style = hudAssets.style(hit.getHitsplatType());
            if (style == null) return;
            int type = hit.getHitsplatType(), amount = hit.getAmount(), expiry = hit.getDisappearsOnGameCycle();
            int cycle = client.getGameCycle();
            BufferedImage image = hudAssets.hitImage(type,style,amount);
            hudTracker.inGeneration(generation,() -> {
                if (!running || !captures.active() || !captures.current(token)) return;
                hudTracker.selectPlayer(player);
                hudTracker.selectNpc(config.renderNpc()?selected.npc:null,config.renderNpc()?selected.key:-1);
                hudTracker.add(actor,type,amount,expiry,cycle,style,image);
            });
        }
        catch (RuntimeException unavailable) { LOG.debug("Unable to capture hitsplat visuals",unavailable); }
    }

    /** Independent native-cycle HUD updates: never schedules a 3D rendering job. */
    private void sampleHud()
    {
        if (diagnosticNoHud || !running || !viewerVisible || !captures.active()) return;
        assert client.isClientThread();
        if (client.getGameState() != GameState.LOGGED_IN) return;
        long generation = hudTracker.generation(), token = captures.epoch();
        try
        {
            Player player = client.getLocalPlayer();
            NpcTargetTracker.Selection selected = npcTargets.currentSelection();
            NPC npc = config.renderNpc()?selected.npc:null;
            HudHealth playerHealth = player == null ? HudHealth.UNKNOWN : HudHealth.player(player.getHealthRatio(),player.getHealthScale(),
                client.getBoostedSkillLevel(Skill.HITPOINTS),client.getRealSkillLevel(Skill.HITPOINTS));
            HudHealth npcHealth = npc == null ? HudHealth.UNKNOWN : HudHealth.scaled(npc.getHealthRatio(),npc.getHealthScale());
            int cycle = client.getGameCycle();
            HudAssets.HealthSkin skin = hudAssets.healthSkin(cycle);
            hudTracker.inGeneration(generation,() -> {
                if (!running || !captures.active() || !captures.current(token)) return;
                hudTracker.selectPlayer(player);
                hudTracker.selectNpc(npc,npc==null?-1:selected.key);
                HudSnapshot next = hudTracker.sample(token,cycle,playerHealth,npcHealth,skin);
                if (next != clientHud) { clientHud = next; hudMailbox.offer(next); }
            });
        }
        catch (RuntimeException unavailable) { LOG.debug("Unable to sample combat HUD",unavailable); }
    }

    private void receiveHud(HudSnapshot next)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (!current(next.token) || panel == null) return;
        panel.hud = next;
        if (playerTrace != null) playerTrace.record("hud","cycle="+next.cycle+" token="+next.token+" playerKey="+next.player.key+" npcKey="+next.npcKey);
        if (paintDiagnostics) hudDeliveries++;
        if (config.showHitsplats() || config.showHpBars()) panel.repaint();
    }

    /** Runs only on the client thread, including PostClientTick even when canvas drawing is slow. */
    private void samplePlayer()
    {
        if (!running || !viewerVisible || !captures.viewing()) return;
        assert client.isClientThread();
        boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
        captures.setLoggedIn(loggedIn);
        if (!loggedIn) return;
        long token = captures.acquire(System.nanoTime());
        if (token < 0) return;
        long hudGeneration = hudTracker.generation();
        ModelSnapshot snapshot = null;
        ModelSnapshot npcSnapshot = null;
        long npcKey = -1;
        long playerHudKey = -1;
        try
        {
            Player player = client.getLocalPlayer();
            playerHudKey = hudTracker.selectPlayer(hudGeneration,player,() -> running && captures.current(token));
            Model model = player == null ? null : player.getModel();
            if (model != null)
            {
                snapshot = ModelSnapshot.capture(model, client.getTextureProvider());
                if (config.renderNpc())
                {
                    NpcTargetTracker.Selection selected = npcTargets.select(player,token);
                    NPC npc = selected.npc;
                    if (npc != null)
                    {
                        npcKey = selected.key;
                        try
                        {
                            Model npcModel = npc.getModel();
                            if (npcModel != null) npcSnapshot = ModelSnapshot.capture(npcModel,client.getTextureProvider());
                        }
                        catch (RuntimeException failure)
                        {
                            LOG.debug("Unable to capture NPC model",failure);
                        }
                    }
                }
            }
        }
        catch (RuntimeException failure)
        {
            LOG.debug("Unable to capture live player model", failure);
        }
        if (snapshot != null) textureReferences = new TextureReferences(snapshot,npcSnapshot);
        int sceneCycle = client.getGameCycle();
        long sceneObservedAt = System.nanoTime();
        TextureSet sampledTextures = clientTextures;
        try
        {
            if (snapshot != null) sampledTextures = TextureSet.capture(client.getTextureProvider(),snapshot,npcSnapshot,
                sceneCycle,client.isGpu(),clientTextures);
        }
        catch (RuntimeException failure) { LOG.debug("Unable to copy game textures; using captured default colors",failure); }
        clientTextures = sampledTextures;
        TextureSet textureCopy = sampledTextures;
        ModelSnapshot next = snapshot, nextNpc = npcSnapshot;
        long capturedPlayerHudKey = playerHudKey;
        long selectedNpcKey = npcKey;
        SwingUtilities.invokeLater(() -> {
            // Complete only the matching lifecycle ticket; old callbacks cannot unblock a new session.
            boolean accepted = captures.complete(token) && current(token);
            if (playerTrace != null) playerTrace.record("capture","accepted="+accepted+" token="+token+" playerKey="+capturedPlayerHudKey
                +" pose="+(next==null?"null":System.identityHashCode(next)+" vertices="+next.x.length+" faces="+next.a.length
                +" alphaHash="+java.util.Arrays.hashCode(next.alpha))+" npcKey="+selectedNpcKey);
            if (!accepted) return;
            if (next == null) return;
            // Compare copied geometry/colors, since native Model instances can be shared or reused.
            boolean playerIdentityChanged = panel.sourcePlayerHudKey != capturedPlayerHudKey;
            if (paintDiagnostics && playerIdentityChanged) playerRebindings++;
            panel.sourcePlayerHudKey = capturedPlayerHudKey;
            boolean changed = viewState.updateModel(next, playerIdentityChanged);
            changed |= viewState.updateNpc(nextNpc,selectedNpcKey);
            changed |= viewState.updateTextures(textureCopy);
            textureClock.observe(sceneCycle,sceneObservedAt);
            if (changed)
            {
                requestRender();
            }
        });
    }

    /** Texture clocks/pixels are sampled independently of the geometry capture throttle. */
    private void sampleTextures()
    {
        assert client.isClientThread();
        if (!running || !viewerVisible || !captures.active()) return;
        TextureReferences refs = textureReferences;
        if (refs == null || !refs.textured() || !texturePending.compareAndSet(false,true)) return;
        long token = captures.epoch();
        int observedCycle = client.getGameCycle();
        long observedAt = System.nanoTime();
        TextureSet sampled = clientTextures;
        try
        {
            sampled = TextureSet.capture(client.getTextureProvider(),refs.player,refs.npc,
                observedCycle,client.isGpu(),clientTextures);
        }
        catch (RuntimeException failure) { LOG.debug("Unable to update texture pixels; retaining the previous material data",failure); }
        TextureSet copy = sampled;
        clientTextures = copy;
        SwingUtilities.invokeLater(() -> {
            texturePending.set(false);
            if (!current(token)) return;
            viewState.updateTextures(copy);
            textureClock.observe(observedCycle,observedAt);
            requestRender();
        });
    }

    private static final class TextureReferences
    {
        final ModelSnapshot player,npc;
        TextureReferences(ModelSnapshot player,ModelSnapshot npc) { this.player = player; this.npc = npc; }
        boolean textured()
        {
            for (ModelSnapshot mesh : new ModelSnapshot[]{player,npc})
                if (mesh != null && mesh.textureIds != null) for (short id : mesh.textureIds) if (id >= 0) return true;
            return false;
        }
    }

    private boolean current(long token)
    {
        return running && window != null && captures.current(token);
    }

    private void requestRender()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (!running || !viewerVisible || window == null || !captures.active() || worker != null) return;
        long now = System.nanoTime();
        if (viewState.hasAnimatedTextures()) viewState.setTexturePhase(textureClock.phase(now));
        if (!viewState.needsRender())
        {
            if (viewState.hasAnimatedTextures() && textureClock.advancing(now))
            { renderWake.setInitialDelay(FramePacer.delayMillis(pacer.renderDelay(now))); renderWake.restart(); }
            return;
        }
        long delay = pacer.renderDelay(System.nanoTime());
        if (delay > 0) { renderWake.setInitialDelay(FramePacer.delayMillis(delay)); renderWake.restart(); return; }
        LiveViewState.RenderRequest request = viewState.begin();
        if (request == null) return;
        long token = captures.epoch();
        pacer.rendered(System.nanoTime());
        RenderJob job = new RenderJob(request,token,panel.sourcePlayerHudKey);
        worker = job;
        if (playerTrace != null) playerTrace.record("render-start","job="+System.identityHashCode(job)+" token="+token+" pose="+System.identityHashCode(request.model)+" playerKey="+job.playerHudKey);
        try
        {
            renderExecutor.execute(() -> {
                BufferedImage image = null;
                Throwable failure = null;
                try { image = new ModelRenderer().renderScene(request); }
                catch (Throwable error) { failure = error; }
                if (playerTrace != null) playerTrace.record("render-complete","job="+System.identityHashCode(job)+" error="+(failure!=null)+" "+PlayerFrameTrace.pixels(image));
                BufferedImage completed = image;
                Throwable error = failure;
                // SwingWorker.done batches callbacks at ~33ms; an explicit EDT handoff avoids that cap.
                SwingUtilities.invokeLater(() -> finishRender(job,completed,error));
            });
        }
        catch (RejectedExecutionException failure) { finishRender(job,null,failure); }
    }

    private void finishRender(RenderJob job, BufferedImage image, Throwable error)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (worker != job) return;
        worker = null;
        boolean valid = viewState.finish(job.request);
        if (playerTrace != null) playerTrace.record("render-accept","job="+System.identityHashCode(job)+" valid="+valid+" current="+current(job.token));
        if (error != null) LOG.debug("Unable to render live view",error);
        else
        {
            if (valid && current(job.token))
            {
                pendingFrame = new CompletedFrame(image,job.request,job.token,job.playerHudKey);
                presentFrame();
            }
        }
        requestRender();
    }

    private void presentFrame()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (pendingFrame == null) return;
        if (!current(pendingFrame.token) || !viewState.valid(pendingFrame.request)) { if (playerTrace != null) playerTrace.record("discard","stale completed frame; displayed image retained"); pendingFrame = null; return; }
        long delay = pacer.presentationDelay(System.nanoTime());
        if (delay > 0) { presentWake.setInitialDelay(FramePacer.delayMillis(delay)); presentWake.restart(); return; }
        CompletedFrame next = pendingFrame;
        pendingFrame = null;
        if (diagnosticFreeze && panel.image != null)
        {
            if (playerTrace != null) playerTrace.record("hold","completed image retained for isolation A");
            pacer.presented(System.nanoTime());
            return;
        }
        if (playerTrace != null) playerTrace.record("publish","old="+System.identityHashCode(panel.image)+" next="+System.identityHashCode(next.image)+" token="+next.token+" playerKey="+next.playerHudKey);
        if (panel.imagePlayerHudKey != next.playerHudKey || panel.playerHudAnchor == null)
            panel.playerHudAnchor = HudAnchor.initial(next.request.model,next.request.framing);
        if (next.request.npcModel == null) panel.npcHudAnchor = null;
        else if (panel.imageNpcKey != next.request.npcKey || panel.npcHudAnchor == null)
            panel.npcHudAnchor = HudAnchor.initial(next.request.npcModel,next.request.npcFraming);
        panel.imagePlayerHudKey = next.playerHudKey;
        panel.imageNpcKey = next.request.npcKey;
        panel.imageCamera = next.request.camera;
        panel.image = next.image;
        if (paintDiagnostics) modelPublications++;
        panel.imageNpcEnabled = next.request.npcEnabled;
        panel.mirrorNpc = ModelScenePainter.shouldMirrorNpc(next.request.camera);
        pacer.presented(System.nanoTime());
        window.getContentPane().repaint();
    }

    private static final class RenderJob
    {
        final LiveViewState.RenderRequest request;
        final long token,playerHudKey;
        RenderJob(LiveViewState.RenderRequest request, long token,long playerHudKey)
        { this.request = request; this.token = token; this.playerHudKey = playerHudKey; }
    }

    private static final class CompletedFrame
    {
        final BufferedImage image;
        final LiveViewState.RenderRequest request;
        final long token,playerHudKey;
        CompletedFrame(BufferedImage image, LiveViewState.RenderRequest request, long token,long playerHudKey)
        { this.image = image; this.request = request; this.token = token; this.playerHudKey = playerHudKey; }
    }

    private void clearModel()
    {
        if (playerTrace != null) playerTrace.record("clear","running="+running+" visible="+viewerVisible+" active="+captures.active()+" old="+System.identityHashCode(panel==null?null:panel.image));
        // Invalidate results, but keep a running worker's slot until it actually finishes.
        // SwingWorker.cancel(false) can call done() while its rendering code is still running.
        viewState.clear();
        textureClock.clear();
        hudTracker.clear(); hudMailbox.clear(); clientHud = null;
        pendingFrame = null; presentWake.stop(); renderWake.stop();
        if (panel != null)
        {
            panel.image = null; panel.hud = null;
            panel.playerHudAnchor = null; panel.npcHudAnchor = null;
            panel.sourcePlayerHudKey = panel.imagePlayerHudKey = panel.imageNpcKey = -1;
            panel.repaint();
        }
    }

    NavigationButton createLaunchButton()
    {
        return NavigationButton.builder().tooltip("Open Model Popout")
            .icon(icon()).priority(10).onClick(() -> SwingUtilities.invokeLater(this::openWindow)).build();
    }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setColor(new Color(0xe2c36b));
            g.fillOval(9, 2, 6, 6);
            g.fillRect(7, 9, 10, 8);
            g.fillRect(7, 17, 4, 6);
            g.fillRect(13, 17, 4, 6);
        }
        finally { g.dispose(); }
        return image;
    }

    final class ModelPanel extends ModelViewport
    {
        private BufferedImage image;
        private HudSnapshot hud;
        private HudAnchor playerHudAnchor,npcHudAnchor;
        private CameraView imageCamera = CameraView.DEFAULT;
        private long sourcePlayerHudKey = -1,imagePlayerHudKey = -1,imageNpcKey = -1;
        private boolean imageNpcEnabled;
        private boolean mirrorNpc;
        ModelPanel(ModelPopoutConfig initialConfig)
        {
            super(ModelPopoutPlugin.this.viewState,ModelPopoutPlugin.this::requestRender);
            setPreferredSize(new Dimension(ModelRenderer.SIZE * (initialConfig.renderNpc() ? 2 : 1),ModelRenderer.SIZE));
        }

        private void paintModels(Graphics graphics)
        {
            if (paintDiagnostics)
            {
                modelDraws++;
                if (window != null && !window.isPaintingWindow()) drawsOutsideWindowPaint++;
            }
            boolean tracePaint = playerTrace != null && System.nanoTime()-tracePaintAt >= 250_000_000L;
            String before = tracePaint ? PlayerFrameTrace.pixels(image) : null;
            ModelScenePainter.paint((Graphics2D) graphics,image,imageNpcEnabled,mirrorNpc,getWidth(),getHeight());
            if (image != null && !diagnosticNoHud)
                HudPainter.paint((Graphics2D)graphics,hud,playerHudAnchor,npcHudAnchor,imageCamera,getWidth(),getHeight(),
                    imageNpcEnabled,mirrorNpc,imagePlayerHudKey,viewState.hasNpc()?imageNpcKey:-1,config.showHitsplats(),config.showHpBars());
            if (tracePaint)
            {
                tracePaintAt = System.nanoTime();
                String after = PlayerFrameTrace.pixels(image);
                playerTrace.record("paint","window="+(window!=null&&window.isPaintingWindow())+" playerKey="+imagePlayerHudKey
                    +" sourceKey="+sourcePlayerHudKey+" hudCycle="+(hud==null?-1:hud.cycle)+" viewport="+getWidth()+"x"+getHeight()
                    +" unchanged="+before.equals(after)+" "+after);
            }
        }
    }
}
