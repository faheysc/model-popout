package com.modelpopout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Ordinary Swing children: every repaint ascends through Surface and WindowPresentation. */
final class WindowTitleBar extends JPanel
{
    interface Actions
    {
        void minimize();
        void maximize();
        void close();
        Rectangle bounds();
        boolean maximized();
        void move(Rectangle bounds);
    }

    final JLabel title = new JLabel("Model Popout");
    final JButton minimize, maximize, close;

    WindowTitleBar(Actions actions)
    {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(38,38,38));
        title.setForeground(new Color(230,230,230));
        title.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
        add(title,BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,0,0));
        buttons.setOpaque(false);
        minimize = button("−","Minimize",actions::minimize);
        maximize = button("□","Maximize / restore",actions::maximize);
        close = button("×","Close",actions::close);
        buttons.add(minimize);buttons.add(maximize);buttons.add(close);
        add(buttons,BorderLayout.EAST);
        MouseAdapter drag = new MouseAdapter()
        {
            private Point pointer;
            private Rectangle start;
            @Override public void mousePressed(MouseEvent e)
            {
                pointer=null;
                if (!SwingUtilities.isLeftMouseButton(e) || actions.maximized()) return;
                pointer=e.getLocationOnScreen();start=actions.bounds();
            }
            @Override public void mouseDragged(MouseEvent e)
            {
                if (pointer!=null && !actions.maximized())
                    actions.move(WindowGeometry.move(start,pointer,e.getLocationOnScreen()));
            }
            @Override public void mouseReleased(MouseEvent e) { pointer=null; }
            @Override public void mouseClicked(MouseEvent e)
            { if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount()==2) actions.maximize(); }
        };
        // Buttons deliberately do not receive the drag listener.
        addMouseListener(drag);addMouseMotionListener(drag);
        title.addMouseListener(drag);title.addMouseMotionListener(drag);
    }

    private static JButton button(String text,String name,Runnable action)
    {
        JButton button=new JButton(text);
        button.setPreferredSize(new Dimension(32,26));
        button.setMargin(new java.awt.Insets(0,0,0,0));
        button.setToolTipText(name);button.getAccessibleContext().setAccessibleName(name);
        button.addActionListener(e -> action.run());
        return button;
    }
}
