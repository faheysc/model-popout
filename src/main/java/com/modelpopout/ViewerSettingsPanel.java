package com.modelpopout;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.client.ui.PluginPanel;

/** Supported RuneLite sidebar UI; the slider writes the same persisted ConfigManager key. */
final class ViewerSettingsPanel extends PluginPanel
{
    final JSlider opacity = new JSlider(0,100);
    final JCheckBox hitsplats = new JCheckBox("Show Hitsplats");
    final JCheckBox hpBars = new JCheckBox("Show HP Bars");
    final JCheckBox alwaysOnTop = new JCheckBox("Always on Top");
    final JCheckBox npcs = new JCheckBox("Render interacting NPC");
    private final JSpinner rate = new JSpinner(new SpinnerNumberModel(20,1,50,1));
    final JComboBox<RenderTarget> renderTarget = new JComboBox<>(RenderTarget.values());
    private final JLabel performance = new JLabel();
    private final JLabel opacityLabel = new JLabel();
    private boolean updating;

    ViewerSettingsPanel(ModelPopoutConfig config, BiConsumer<String,Object> save, Runnable open)
    {
        setLayout(new BorderLayout(8,8));
        JPanel controls = new JPanel(new GridLayout(0,1,4,8));
        controls.add(new JLabel("Model Popout settings"));
        JButton launch = new JButton("Open Model Popout");
        launch.addActionListener(e -> open.run());
        controls.add(launch);
        controls.add(npcs);
        controls.add(alwaysOnTop);
        controls.add(hitsplats);
        controls.add(hpBars);
        controls.add(new JLabel("Model capture FPS"));
        controls.add(rate);
        controls.add(new JLabel("Rendering target"));
        controls.add(renderTarget);
        controls.add(performance);
        controls.add(opacityLabel);
        opacity.setMajorTickSpacing(25);
        opacity.setPaintTicks(true);
        opacity.setPaintLabels(true);
        controls.add(opacity);
        controls.add(new JLabel("<html>Native title bar opacity:<br>controlled by the desktop.</html>"));
        add(controls,BorderLayout.NORTH);
        opacity.addChangeListener(e -> {
            opacityLabel.setText("Background opacity: " + opacity.getValue() + "%");
            if (!updating) save.accept(ModelPopoutConfig.OPACITY_KEY,opacity.getValue());
        });
        hitsplats.addActionListener(e -> { if (!updating) save.accept(ModelPopoutConfig.HITSPLATS_KEY,hitsplats.isSelected()); });
        hpBars.addActionListener(e -> { if (!updating) save.accept(ModelPopoutConfig.HP_BARS_KEY,hpBars.isSelected()); });
        alwaysOnTop.addActionListener(e -> { if (!updating) save.accept(ModelPopoutConfig.ALWAYS_ON_TOP_KEY,alwaysOnTop.isSelected()); });
        npcs.addActionListener(e -> { if (!updating) save.accept(ModelPopoutConfig.NPC_KEY,npcs.isSelected()); });
        rate.addChangeListener(e -> { if (!updating) save.accept("refreshRate",rate.getValue()); });
        renderTarget.addActionListener(e -> { if (!updating) save.accept("renderTarget",renderTarget.getSelectedItem()); });
        sync(config);
    }

    void sync(ModelPopoutConfig config)
    {
        updating = true;
        try
        {
            opacity.setValue(BackgroundOpacity.clamp(config.backgroundOpacity()));
            opacityLabel.setText("Background opacity: " + opacity.getValue() + "%");
            hitsplats.setSelected(config.showHitsplats());
            hpBars.setSelected(config.showHpBars());
            alwaysOnTop.setSelected(config.alwaysOnTop());
            npcs.setSelected(config.renderNpc());
            renderTarget.setSelectedItem(config.renderTarget());
            rate.setValue(Math.max(1,Math.min(50,config.refreshRate())));
        }
        finally { updating = false; }
    }
    void showPerformance(String text)
    {
        performance.setText("<html>" + text.replace(" · ","<br>") + "</html>");
    }
}
