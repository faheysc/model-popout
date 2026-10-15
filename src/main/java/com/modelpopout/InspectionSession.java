package com.modelpopout;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** EDT-owned independent camera/presentation. Frozen snapshots remain renderable after logout. */
final class InspectionSession
{
    private static final Logger LOG=LoggerFactory.getLogger(InspectionSession.class);
    final LiveViewState state=new LiveViewState();
    final InspectionPanel panel;
    private final InspectionRenderScheduler scheduler;
    private final ModelPopoutConfig config;
    private final FramePacer pacer=new FramePacer();
    private final TextureClock clock=new TextureClock();
    private final Timer wake=new Timer(1,event -> requestRender());
    private final Runnable onClose;
    private final java.util.function.Consumer<Boolean> visibility;
    private ViewerWindow window;
    private boolean closed, visible=true, live=true;
    private long hudKey=-1;
    private int npcDefinition=-1;

    InspectionSession(ModelPopoutConfig config, InspectionRenderScheduler scheduler,
        Runnable onClose, java.util.function.Consumer<Boolean> visibility)
    {
        assert SwingUtilities.isEventDispatchThread();
        this.config=config;this.scheduler=scheduler;this.onClose=onClose;this.visibility=visibility;
        panel=new InspectionPanel(config,state,this::requestRender);
        wake.setRepeats(false);pacer.setTarget(config.renderTarget());
    }

    void open(String name)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (closed) return;
        ViewerWindow frame=ViewerWindow.create();window=frame;
        frame.setTitle("Popout — "+name);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);frame.add(panel,java.awt.BorderLayout.CENTER);
        frame.setModelPainter(g -> {
            Graphics2D child=(Graphics2D)g.create(panel.getX(),panel.getY(),panel.getWidth(),panel.getHeight());
            try { panel.paintModels(child); }
            finally { child.dispose(); }
        });
        frame.addWindowListener(new WindowAdapter()
        { @Override public void windowClosed(WindowEvent event) { close(); } });
        frame.addWindowStateListener(event -> setVisible((event.getNewState() & Frame.ICONIFIED)==0));
        frame.installHoverControls();frame.applyBackgroundOpacity(config.backgroundOpacity());
        frame.applyAlwaysOnTop(config.alwaysOnTop());frame.setMinimumSize(new Dimension(300,300));
        frame.pack();frame.setLocationByPlatform(true);frame.setVisible(true);
    }

    void accept(InspectionCapture.Packet packet)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (closed) return;
        live=packet.live;
        boolean hudIdentityChanged=packet.hud!=null && packet.hud.player.key!=hudKey;
        if (packet.model!=null)
        {
            if (npcDefinition>=0 && packet.npcDefinition>=0 && npcDefinition!=packet.npcDefinition)
            { state.reframeModel(packet.model);panel.anchor=null; }
            else state.updateModel(packet.model,hudIdentityChanged);
            npcDefinition=packet.npcDefinition;
        }
        state.updateTextures(packet.textures);
        if (packet.model!=null) clock.observe(packet.cycle,packet.observedAt);
        if (!live && packet.model!=null) state.setTexturePhase(packet.cycle);
        HudSnapshot previousHud=panel.hud;
        String previousStatus=panel.status;
        panel.hud=visible && live?packet.hud:null;
        hudKey=packet.hud==null?-1:packet.hud.player.key;
        panel.status=live?(packet.model==null?"Waiting for the actor's model…":null)
            :packet.model==null?"Actor unavailable — no model captured":"Actor unavailable — last captured model";
        if (!java.util.Objects.equals(previousStatus,panel.status)
            || (previousHud!=panel.hud && (config.showHitsplats() || config.showHpBars()))) panel.repaint();
        requestRender();
    }

    void settingsChanged()
    {
        if (closed) return;
        pacer.setTarget(config.renderTarget());
        if (window!=null)
        { window.applyWindowControls();window.applyBackgroundOpacity(config.backgroundOpacity());window.applyAlwaysOnTop(config.alwaysOnTop()); }
        panel.repaint();requestRender();
    }

    void setVisible(boolean value)
    {
        if (closed || visible==value) return;
        visible=value;panel.hud=null;
        if (!visible) { wake.stop();scheduler.remove(this); }
        visibility.accept(value);
        if (visible) requestRender();
    }

    void requestRender()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (closed || !visible) return;
        long now=System.nanoTime();
        if (live && state.hasAnimatedTextures()) state.setTexturePhase(clock.phase(now));
        if (!state.needsRender())
        {
            if (live && state.hasAnimatedTextures() && clock.advancing(now)) schedule(now);
            return;
        }
        if (pacer.renderDelay(now)>0) { schedule(now);return; }
        scheduler.request(this,this::begin);
    }

    private void schedule(long now)
    { wake.setInitialDelay(FramePacer.delayMillis(pacer.renderDelay(now)));wake.restart(); }

    private InspectionRenderScheduler.Work begin()
    {
        if (closed || !visible) return null;
        LiveViewState.RenderRequest request=state.begin();
        if (request==null) return null;
        pacer.rendered(System.nanoTime());
        long capturedHudKey=hudKey;
        return new InspectionRenderScheduler.Work()
        {
            private BufferedImage image;
            private Throwable failure;
            @Override public void render() { image=new ModelRenderer().renderScene(request); }
            @Override public void failed(Throwable error) { failure=error; }
            @Override public void complete()
            {
                boolean valid=state.finish(request);
                if (closed) return;
                if (failure!=null) LOG.debug("Unable to render inspected model",failure);
                else if (valid)
                {
                    if (panel.anchor==null) panel.anchor=HudAnchor.initial(request.model,request.framing);
                    panel.image=image;panel.imageCamera=request.camera;panel.imageHudKey=capturedHudKey;
                    panel.repaint();pacer.presented(System.nanoTime());
                }
                requestRender();
            }
        };
    }

    void close()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (closed) return;
        closed=true;visible=false;wake.stop();scheduler.remove(this);
        state.clear();clock.clear();panel.clear();
        if (window!=null)
        {
            window.setModelPainter(g -> {});
            window.dispose();window=null;
        }
        onClose.run();
    }

    boolean closed() { return closed; }
}
