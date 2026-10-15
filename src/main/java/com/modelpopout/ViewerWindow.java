package com.modelpopout;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingUtilities;

/** Buffered per-pixel viewer with shared Swing window controls, edge resizing and opaque fallback. */
final class ViewerWindow extends JFrame
{
    private static final Logger LOG = LoggerFactory.getLogger(ViewerWindow.class);
    private final boolean translucentCreated;
    private final Surface surface = new Surface();
    private final WindowPresentation presentation = new WindowPresentation();
    private int requestedOpacity = 100;
    private boolean supported;
    private boolean paintingWindow;
    private Rectangle restoreBounds;
    private final WindowTitleBar titleBar;
    private final boolean inheritedSwingDecorations;
    private Runnable presented = () -> {};

    static ViewerWindow create()
    {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration capable = null;
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT))
        {
            for (GraphicsConfiguration gc : device.getConfigurations())
            {
                if (gc.isTranslucencyCapable()) { capable = gc; break; }
            }
        }
        ViewerWindow window = new ViewerWindow(capable == null ? device.getDefaultConfiguration() : capable,capable != null);
        if (capable != null)
        {
            try { window.setBackground(new Color(0,0,0,0)); }
            catch (UnsupportedOperationException | java.awt.IllegalComponentStateException failure)
            {
                window.dispose();
                window = new ViewerWindow(device.getDefaultConfiguration(),false);
            }
        }
        return window;
    }

    private ViewerWindow(GraphicsConfiguration configuration, boolean translucent)
    {
        super("Model Popout",configuration);
        inheritedSwingDecorations = getRootPane().getWindowDecorationStyle() == JRootPane.FRAME;
        translucentCreated = translucent;
        supported = translucent;
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        titleBar = new WindowTitleBar(new WindowTitleBar.Actions()
        {
            public void minimize() { setExtendedState(getExtendedState() | Frame.ICONIFIED); }
            public void maximize() { toggleMaximize(); }
            public void close() { dispatchEvent(new java.awt.event.WindowEvent(ViewerWindow.this,java.awt.event.WindowEvent.WINDOW_CLOSING)); }
            public Rectangle bounds() { return getBounds(); }
            public boolean maximized() { return restoreBounds != null; }
            public void move(Rectangle bounds) { setBounds(bounds); }
        });
        surface.titleBar = titleBar;
        surface.add(titleBar,BorderLayout.NORTH);
        addPropertyChangeListener("title",e -> titleBar.title.setText(getTitle()));
        setContentPane(surface);
        keepPresentationNonOpaque(getRootPane());
        getRootPane().setDoubleBuffered(false);
        surface.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        installResize(surface);
        applyWindowControls();
        addPropertyChangeListener("graphicsConfiguration",e -> SwingUtilities.invokeLater(() -> {
            if (Boolean.getBoolean("modelpopout.paintDiagnostics"))
                LOG.debug("Model Popout graphics configuration changed: old={} new={} displayable={}",e.getOldValue(),e.getNewValue(),isDisplayable());
            if (isDisplayable()) applyBackgroundOpacity(requestedOpacity);
        }));
    }

    void applyWindowControls()
    {
        assert SwingUtilities.isEventDispatchThread();
        // The global JFrame default may install a Swing title bar even on an undecorated frame.
        // Never change that global default: it also controls RuneLite and unrelated plugins.
        boolean swing = useSwingDecorations(inheritedSwingDecorations,
            javax.swing.UIManager.getLookAndFeel().getSupportsWindowDecorations());
        int style = swing ? JRootPane.FRAME : JRootPane.NONE;
        if (getRootPane().getWindowDecorationStyle() != style) getRootPane().setWindowDecorationStyle(style);
        boolean builtIn = !swing;
        if (builtIn)
        {
            surface.titleBar = titleBar;
            if (titleBar.getParent() != surface) surface.add(titleBar,BorderLayout.NORTH);
        }
        else
        {
            surface.remove(titleBar);
            surface.titleBar = null;
        }
        keepPresentationNonOpaque(getRootPane());
        getRootPane().revalidate();
        // Repaint through the existing top-level composition boundary after relayout.
        repaint();
    }

    static boolean useSwingDecorations(boolean inherited, boolean supported) { return inherited && supported; }

    boolean hasBuiltInControls() { return surface.titleBar != null; }

    void applyBackgroundOpacity(int requested)
    {
        requestedOpacity = BackgroundOpacity.clamp(requested);
        // A moved window may report the device's ordinary configuration. Window.setBackground
        // negotiates a capable configuration when needed; do not mistake that for lost support.
        supported = translucentCreated && getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (Boolean.getBoolean("modelpopout.paintDiagnostics"))
            LOG.debug("Model Popout background policy: requested={} supported={} currentConfigurationCapable={}",requestedOpacity,supported,getGraphicsConfiguration().isTranslucencyCapable());
        supported = WindowTransparency.apply(supported,() -> getGraphicsConfiguration().isTranslucencyCapable(),
            this::getBackground,this::setBackground);
        surface.opacity = BackgroundOpacity.effective(requestedOpacity,supported);
        surface.setToolTipText(!supported && requestedOpacity < 100 ? "Desktop transparency unavailable; background stays opaque." : null);
        surface.repaint();
    }

    @Override public void setBackground(Color background)
    {
        Color previous = getBackground();
        try { super.setBackground(background); }
        finally
        {
            // Window.setBackground resets all three Swing layers to opaque on alpha 0 -> 255.
            // Even the native opaque fallback must ascend to ViewerWindow.paint, never paint
            // a Swing subtree directly into the native destination outside its private buffer.
            keepPresentationNonOpaque(getRootPane());
        }
        if (Boolean.getBoolean("modelpopout.paintDiagnostics") && previous != null
            && background != null && previous.getAlpha() != background.getAlpha())
            LOG.debug("Model Popout window background alpha: {} -> {}; presentation layers kept nonopaque",
                previous.getAlpha(),background.getAlpha(),new Throwable("Background transition caller"));
    }

    @Override protected JRootPane createRootPane() { return new PresentationRoot(); }

    // Look-and-feel title bars are siblings of Surface, so their dirty paints also need
    // an origin above all root children before ascending to the buffered Window.paint.
    static final class PresentationRoot extends JRootPane
    {
        @Override protected boolean isPaintingOrigin() { return true; }
    }

    static void keepPresentationNonOpaque(JRootPane root)
    {
        // JFrame's constructor invokes setBackground before ViewerWindow fields initialize.
        if (root == null) return;
        root.setOpaque(false);
        root.getLayeredPane().setOpaque(false);
        if (root.getContentPane() instanceof javax.swing.JComponent)
            ((javax.swing.JComponent)root.getContentPane()).setOpaque(false);
    }

    boolean applyAlwaysOnTop(boolean requested)
    {
        assert SwingUtilities.isEventDispatchThread();
        return WindowStacking.apply(requested,this::isAlwaysOnTopSupported,this::setAlwaysOnTop);
    }

    int effectiveOpacity() { return BackgroundOpacity.effective(requestedOpacity,supported); }

    String transparencyDiagnostics()
    {
        Color background = getBackground();
        return "windowId="+Integer.toHexString(System.identityHashCode(this))+" requested="+requestedOpacity+" effective="+effectiveOpacity()+" supported="+supported
            +" nativeAlpha="+(background==null?-1:background.getAlpha())
            +" configurationCapable="+getGraphicsConfiguration().isTranslucencyCapable()
            +" configurationPixelBits="+getGraphicsConfiguration().getColorModel().getPixelSize()
            +" inheritedSwingDecorations="+inheritedSwingDecorations+" controls=Automatic"+" swingDecorationStyle="+getRootPane().getWindowDecorationStyle()
            +" builtInControls="+hasBuiltInControls()+" undecorated="+isUndecorated()+" surfaceBackground="+surface.backgroundArgb()
            +" finalBufferBackground="+presentation.backgroundArgb()+" destination="+presentation.destinationFormat();
    }


    @Override public void paint(Graphics graphics)
    {
        // Buffer ABOVE Window.paint: it clears a non-opaque window before painting its children.
        // Buffering only Surface.paint leaves that clear visible on the native window.
        paintingWindow = true;
        try
        {
            presentation.paint(graphics,getWidth(),getHeight(),this::paintHierarchy);
            presented.run();
        }
        finally { paintingWindow = false; }
    }

    // Read-only diagnostic: distinguishes top-level composition from a direct Swing subtree paint.
    boolean isPaintingWindow() { return paintingWindow; }

    private void paintHierarchy(Graphics2D graphics)
    {
        Surface.disableBuffering(getRootPane());
        super.paint(graphics);
    }

    private void installResize(Component component)
    {
        MouseAdapter listener = new MouseAdapter()
        {
            private Point pointer;
            private Rectangle bounds;
            private int edges;
            @Override public void mouseMoved(MouseEvent event)
            {
                int hit = (restoreBounds != null || (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) ? 0
                    : WindowGeometry.edges(event.getX(),event.getY(),component.getWidth(),component.getHeight());
                component.setCursor(Cursor.getPredefinedCursor(WindowGeometry.cursor(hit)));
            }
            @Override public void mouseExited(MouseEvent event)
            {
                if (pointer == null) component.setCursor(Cursor.getDefaultCursor());
            }
            @Override public void mousePressed(MouseEvent event)
            {
                if (!SwingUtilities.isLeftMouseButton(event) || (restoreBounds != null || (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0)) return;
                edges = WindowGeometry.edges(event.getX(),event.getY(),component.getWidth(),component.getHeight());
                if (edges == 0) return;
                pointer = event.getLocationOnScreen(); bounds = getBounds();
            }
            @Override public void mouseDragged(MouseEvent event)
            {
                if (pointer == null) return;
                Point now = event.getLocationOnScreen();
                setBounds(WindowGeometry.resize(bounds,edges,now.x-pointer.x,now.y-pointer.y,getMinimumSize()));
            }
            @Override public void mouseReleased(MouseEvent event)
            {
                pointer = null;
                component.setCursor(Cursor.getDefaultCursor());
            }
        };
        component.addMouseListener(listener); component.addMouseMotionListener(listener);
    }

    Rectangle normalBounds() { return new Rectangle(restoreBounds == null ? getBounds() : restoreBounds); }

    void toggleMaximize()
    {
        assert SwingUtilities.isEventDispatchThread();
        if ((getExtendedState() & Frame.MAXIMIZED_BOTH) != 0)
        {
            setExtendedState(getExtendedState() & ~Frame.MAXIMIZED_BOTH);
            return;
        }
        if (restoreBounds == null)
        {
            restoreBounds = getBounds();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            setBounds(WindowGeometry.usable(gc.getBounds(),getToolkit().getScreenInsets(gc)));
        }
        else
        {
            Rectangle restored = restoreBounds;
            restoreBounds = null;
            setBounds(restored);
        }
        titleBar.maximize.setText(restoreBounds == null ? "□" : "❐");
    }

    void revealControls() { surface.reveal = true; surface.repaint(); }

    void setPresentationListener(Runnable listener)
    {
        presented = listener;
        surface.presented = () -> { if (!paintingWindow) presented.run(); };
    }

    void setModelPainter(Consumer<Graphics2D> painter)
    {
        surface.modelPainter = painter;
    }

    // At zero opacity, hovering reveals the background so the window remains discoverable.
    void installHoverControls()
    {
        MouseAdapter hover = new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { surface.reveal = true; surface.repaint(); }
            @Override public void mouseExited(MouseEvent e)
            {
                SwingUtilities.invokeLater(() -> {
                    surface.reveal = surface.getMousePosition(true) != null;
                    surface.repaint();
                });
            }
        };
        installHover(surface, hover);
    }

    private static void installHover(java.awt.Component component, MouseAdapter hover)
    {
        component.addMouseListener(hover);
        if (component instanceof java.awt.Container)
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) installHover(child, hover);
    }

    static final class Surface extends JPanel
    {
        WindowTitleBar titleBar;
        int opacity = 100;
        boolean reveal;
        Consumer<Graphics2D> modelPainter = g -> {};
        private BufferedImage ui, composed;
        Runnable presented = () -> {};
        Surface()
        {
            super(new BorderLayout(8,8));
            setOpaque(false);
            setDoubleBuffered(false);
        }
        // Child dirty regions must repaint through the compositing boundary.
        @Override protected boolean isPaintingOrigin() { return true; }

        @Override public void paint(Graphics graphics)
        {
            int width = getWidth(), height = getHeight();
            if (width <= 0 || height <= 0) return;
            if (ui == null || ui.getWidth() != width || ui.getHeight() != height)
            {
                ui = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                composed = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D layer = ui.createGraphics();
            try
            {
                layer.setComposite(AlphaComposite.Clear);
                layer.fillRect(0,0,width,height);
                layer.setComposite(AlphaComposite.SrcOver);
                // No shared Swing back buffer may bypass this layer.
                disableBuffering(this);
                // paintChildren below omits chrome; it is composited once after the viewport.
                super.paint(layer);
            }
            finally { layer.dispose(); }
            Graphics2D result = composed.createGraphics();
            try
            {
                result.setComposite(AlphaComposite.Clear);
                result.fillRect(0,0,width,height);
                int effective = opacity == 0 && reveal ? 100 : opacity;
                result.setComposite(AlphaComposite.SrcOver.derive(effective / 100f));
                result.drawImage(ui,0,0,null);
                result.setComposite(AlphaComposite.SrcOver);
                Graphics2D models = (Graphics2D) result.create();
                try
                {
                    if (titleBar != null)
                        models.clipRect(WindowGeometry.BORDER,titleBar.getY()+titleBar.getHeight(),
                            Math.max(0,width-2*WindowGeometry.BORDER),
                            Math.max(0,height-titleBar.getY()-titleBar.getHeight()-WindowGeometry.BORDER));
                    modelPainter.accept(models);
                }
                finally { models.dispose(); }
                // Paint chrome at full alpha, into the same private completed frame.
                if (titleBar != null)
                {
                    // Fully transparent native pixels may be click-through (notably on Windows).
                    // Keep the six-pixel resize targets visibly opaque, independently of viewport alpha.
                    result.setColor(new Color(38,38,38));
                    int edge = WindowGeometry.BORDER;
                    result.fillRect(0,0,width,edge);
                    result.fillRect(0,height-edge,width,edge);
                    result.fillRect(0,0,edge,height);
                    result.fillRect(width-edge,0,edge,height);
                    Graphics2D chrome = (Graphics2D) result.create(titleBar.getX(),titleBar.getY(),titleBar.getWidth(),titleBar.getHeight());
                    try { titleBar.paint(chrome); }
                    finally { chrome.dispose(); }
                }
            }
            finally { result.dispose(); }
            Graphics2D target = (Graphics2D) graphics.create();
            try
            {
                target.setComposite(AlphaComposite.Src);
                target.drawImage(composed,0,0,null);
            }
            finally { target.dispose(); }
            presented.run();
        }

        @Override protected void paintChildren(Graphics graphics)
        {
            if (titleBar == null) { super.paintChildren(graphics); return; }
            Graphics2D viewport = (Graphics2D) graphics.create();
            try
            {
                viewport.clipRect(0,titleBar.getY()+titleBar.getHeight(),getWidth(),getHeight());
                super.paintChildren(viewport);
            }
            finally { viewport.dispose(); }
        }

        String backgroundArgb()
        {
            // Inside the six-pixel surface border, outside the model/HUD viewport.
            return composed == null ? "unpainted" : String.format("%08x",composed.getRGB(Math.max(0,composed.getWidth()-2),Math.max(0,composed.getHeight()-2)));
        }

        private static void disableBuffering(java.awt.Component component)
        {
            if (component instanceof javax.swing.JComponent) ((javax.swing.JComponent) component).setDoubleBuffered(false);
            if (component instanceof java.awt.Container)
                for (java.awt.Component child : ((java.awt.Container) component).getComponents()) disableBuffering(child);
        }

        @Override protected void paintComponent(Graphics graphics)
        {
            graphics.setColor(BackgroundOpacity.color(100));
            graphics.fillRect(0,0,getWidth(),getHeight());
        }
    }
}
