package com.modelpopout;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Shared camera input and invalidation only; each viewport retains its own scene/HUD painting. */
abstract class ModelViewport extends JPanel
{
    ModelViewport(LiveViewState state, Runnable requestRender)
    {
        setOpaque(false);
        setDoubleBuffered(false);
        MouseAdapter controls = new MouseAdapter()
        {
            private Point previous;
            @Override public void mousePressed(MouseEvent event)
            { if (SwingUtilities.isLeftMouseButton(event)) previous = event.getPoint(); }
            @Override public void mouseReleased(MouseEvent event)
            { if (SwingUtilities.isLeftMouseButton(event)) previous = null; }
            @Override public void mouseDragged(MouseEvent event)
            {
                if (previous == null || (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) return;
                state.setCamera(state.camera().drag(event.getX()-previous.x,event.getY()-previous.y));
                previous = event.getPoint();
                requestRender.run();
            }
            @Override public void mouseWheelMoved(MouseWheelEvent event)
            { state.setCamera(state.camera().wheel(event.getPreciseWheelRotation())); requestRender.run(); }
        };
        addMouseListener(controls);
        addMouseMotionListener(controls);
        addMouseWheelListener(controls);
    }

    @Override public void repaint(long delay, int x, int y, int width, int height)
    {
        // Child-only native updates bypass the full-surface damage boundary.
        java.awt.Container parent = getParent();
        if (width > 0 && height > 0 && parent instanceof ViewerWindow.Surface) parent.repaint(delay);
        else super.repaint(delay,x,y,width,height);
    }

    @Override protected void paintComponent(Graphics graphics) {}
}
