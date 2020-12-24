package org.openstreetmap.josm.plugins.pt_assistant.gtfs.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.layer.LayerVisibilityAction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

public class GtfsLayerSettingsPanel extends JPanel {
    private final LayerListDialog.LayerListModel layerListModel;
    private final GtfsColorSelector stopColorSelector = new GtfsColorSelector();
    private final GtfsMinServiceSelector minServiceSelector = new GtfsMinServiceSelector();

    public GtfsLayerSettingsPanel(LayerListDialog.LayerListModel layerListModel) {
        super(new GridBagLayout());
        this.layerListModel = layerListModel;
        // This would normally only change once the dialog is opened. But we cannot intercept the open event
        layerListModel.addTableModelListener(e -> update());
        layerListModel.getSelectionModel().addListSelectionListener(e -> update());
    }

    private void update() {
        this.removeAll();
        List<Layer> layers = layerListModel.getSelectedLayers();
        Collection<GtfsLayer> affectedLayers = new SubclassFilteredCollection<>(layers, layer -> layer instanceof GtfsLayer);
        if (!affectedLayers.isEmpty()) {
            this.add(stopColorSelector, GBC.std().grid(0, 0));
            this.add(minServiceSelector, GBC.std().grid(0, 1));
        }
        stopColorSelector.setAffectedLayers(affectedLayers);
        minServiceSelector.setAffectedLayers(affectedLayers);
    }

    private static class GtfsMinServiceSelector extends JPanel {
        private final JSlider slider = new JSlider(0, 70);
        private Collection<GtfsLayer> layers = Collections.emptyList();

        GtfsMinServiceSelector() {
            super(new GridBagLayout());
            add(new JLabel(tr("Services next week")), GBC.std());

            add(slider, GBC.std().grid(1, 0).fill());
            slider.setMajorTickSpacing(20);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.addChangeListener(e -> layers.forEach(layer -> layer.setMinServicesRequired(slider.getValue())));
        }

        public void setAffectedLayers(Collection<GtfsLayer> layers) {
            this.layers = layers;
            slider.setValue((int) layers.stream().mapToInt(GtfsLayer::getMinServicesRequired)
                    .average()
                    .orElse(1)
            );
        }
    }

    private static class GtfsColorSelector extends JPanel {
        private Collection<GtfsLayer> layers = Collections.emptyList();

        public GtfsColorSelector() {
            super(new GridBagLayout());
            add(new JLabel(tr("Text color")), GBC.std());

            for (GtfsLayerTextColor color: GtfsLayerTextColor.values()) {
                add(new GtfsColorButton(color, this::getColor, this::setColor),
                    GBC.std().insets(5, 0, 0, 0));
            }
        }

        private void setColor(GtfsLayerTextColor gtfsLayerTextColor) {
            layers.forEach(layer -> layer.setTextColor(gtfsLayerTextColor));
            // Forces a repaint of all buttons, too.
            // That way, we don't have to listen to text color changes.
            repaint();
        }

        private GtfsLayerTextColor getColor() {
            List<GtfsLayerTextColor> list = layers.stream()
                .map(GtfsLayer::getTextColor)
                .distinct()
                .collect(Collectors.toList());
            return list.size() == 1 ? list.get(0) : null;
        }

        public void setAffectedLayers(Collection<GtfsLayer> layers) {
            this.layers = layers;
            invalidate();
        }
    }

    private static class GtfsColorButton extends JButton {

        private final GtfsLayerTextColor color;
        private final Supplier<GtfsLayerTextColor> current;

        public GtfsColorButton(GtfsLayerTextColor color, Supplier<GtfsLayerTextColor> current, Consumer<GtfsLayerTextColor> callback) {
            super(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    callback.accept(color);
                }
            });
            this.color = color;
            this.current = current;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (color.getColor() == null) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                ((Graphics2D) g).setStroke(new BasicStroke(2));
                g.drawLine(0, 0, getWidth(), getHeight());
                g.drawLine(0, getHeight(), getWidth(), 0);
            } else {
                g.setColor(color.getColor());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            if (color.equals(current.get())) {
                g.setColor(Color.BLACK);
                ((Graphics2D) g).setStroke(new BasicStroke(9));
                g.drawRect(0, 0, getWidth(), getHeight());
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(20, 20);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(20, 20);
        }
    }

    @SuppressWarnings("unchecked")
    public static void hackInto(MapFrame newFrame) {
        LayerListDialog dialog = LayerListDialog.getInstance();
        try {
            Field buttonActionsField = ToggleDialog.class.getDeclaredField("buttonActions");
            buttonActionsField.setAccessible(true);
            List<Action> buttonActions = (List<Action>) buttonActionsField.get(dialog);
            Action visibilityAction = buttonActions
                .stream()
                .filter(it -> it instanceof LayerVisibilityAction)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No LayerVisibilityAction present"));

            Field contentField = LayerVisibilityAction.class.getDeclaredField("content");
            contentField.setAccessible(true);
            JPanel content = (JPanel) contentField.get(visibilityAction);
            content.add(new GtfsLayerSettingsPanel(dialog.getModel()));
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new RuntimeException("Error hacking into layer settings!", e);
        }
    }
}
