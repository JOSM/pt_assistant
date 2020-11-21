// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode.ITransportMode;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.ColorPalette;
import org.openstreetmap.josm.plugins.pt_assistant.utils.GuiUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The top panel that is added via {@link MapFrame#addTopPanel(Component)}.
 * This class should just handle the display and input. The state of the routing helper should be handled in {@link RouteExplorer}.
 */
public class RouteExplorerPanel extends JPanel {

    private final HighlightHelper highlighter = new HighlightHelper();

    private final JLabel relationLabel = GuiUtils.createJLabel();

    private final JLabel activeWayLabel = GuiUtils.createJLabel(true);
    private final JLabel previousWayConnectionLabel = GuiUtils.createJLabel();
    private final JLabel nextWayConnectionLabel = GuiUtils.createJLabel();
    private final JButton openRelationEditorButton = new JButton();

    private final JPanel containerPanel = GuiUtils.createJPanel(new BorderLayout());

    private final JPanel defaultPanel;

    private final JPanel wayTraversalPanel;

    public RouteExplorerPanel(@NotNull final RouteExplorer routingHelperAction) {
        this.defaultPanel = createDefaultPanel(routingHelperAction);
        this.wayTraversalPanel = createWayTraversalPanel(
            routingHelperAction,
            activeWayLabel,
            previousWayConnectionLabel,
            nextWayConnectionLabel,
            openRelationEditorButton
        );

        JButton closeButton = new JButton(ImageProvider.get("misc", "black_x"));
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText(I18n.tr("Close the routing helper"));
        closeButton.addActionListener(__ ->
            Optional.ofNullable(MainApplication.getMap())
                .ifPresent(map -> map.removeTopPanel(RouteExplorerPanel.class))
        );

        final JPanel mainPanel = GuiUtils.createJPanel(new BorderLayout());
        relationLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(relationLabel, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.EAST);

        // put everything together
        setBackground(ColorPalette.ROUTING_HELPER_BACKGROUND);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(containerPanel, BorderLayout.SOUTH);
    }

    public void onRelationChange(@Nullable final Relation relation, @NotNull Optional<ITransportMode> activeTransportMode) {
        relationLabel.setIcon(activeTransportMode.map(it -> it.getIcon().setSize(ImageProvider.ImageSizes.SMALLICON).get()).orElse(null));
        relationLabel.setText(relation == null ? "‹no relation selected›" : "Relation #" + relation.getId());
        onCurrentWayCleared();
    }

    public void onCurrentWayCleared() {
        highlighter.clear();
        activeWayLabel.setText("");
        previousWayConnectionLabel.setText("");
        nextWayConnectionLabel.setText("");
        updateContainerPanel(defaultPanel);
    }

    public void onCurrentWayChange(
        @NotNull final Relation parentRelation,
        @NotNull final RelationMember member,
        @NotNull final ConnectionType previousWayConnection,
        @NotNull final ConnectionType nextWayConnection,
        final int currentWayIndex,
        final int numWaysTotal
    ) {
        highlighter.highlightOnly(member.getMember());
        Optional.ofNullable(MainApplication.getMap()).ifPresent(map -> map.mapView.zoomTo(BoundsUtils.fromBBox(member.getMember().getBBox())));
        activeWayLabel.setText("Way" + (currentWayIndex + 1) + '/' + numWaysTotal + ": #" + member.getMember().getId());
        previousWayConnectionLabel.setIcon(previousWayConnection.icon);
        previousWayConnectionLabel.setText(previousWayConnection.previousWayText);
        nextWayConnectionLabel.setIcon(nextWayConnection.icon);
        nextWayConnectionLabel.setText(nextWayConnection.nextWayText);
        openRelationEditorButton.setAction(new AbstractAction(I18n.tr("Open in relation editor")) {
            @Override
            public void actionPerformed(ActionEvent __) {
                new GenericRelationEditor(MainApplication.getLayerManager().getActiveDataLayer(), parentRelation, Collections.singleton(member)).setVisible(true);
            }
        });
        updateContainerPanel(wayTraversalPanel);
    }

    private void updateContainerPanel(final JPanel newPanel) {
        if (containerPanel.getComponentCount() != 1 || containerPanel.getComponent(0) != newPanel) {
            containerPanel.removeAll();
            containerPanel.add(newPanel, BorderLayout.CENTER);
            containerPanel.revalidate();
            containerPanel.repaint();
        }
    }

    /**
     * Initializes the panel shown when traversing the ways one by one
     */
    private static JPanel createWayTraversalPanel(
        final RouteExplorer routingHelperAction,
        final JLabel activeWayLabel,
        final JLabel previousWayConnectionLabel,
        final JLabel nextWayConnectionLabel,
        final JButton openRelationEditorButton
    ) {
        final JPanel wayTraversalPanel = GuiUtils.createJPanel(new BorderLayout());

        // Left side: buttons to navigate backwards
        wayTraversalPanel.add(
            GuiUtils.createJPanel(
                new FlowLayout(FlowLayout.LEFT),
                GuiUtils.createJButton("‹ to previous way", routingHelperAction::goToPreviousWay)
            ),
            BorderLayout.WEST
        );

        // Center: label for active element
        final JPanel activeWayPanel = new JPanel();
        activeWayPanel.setOpaque(false);
        activeWayPanel.setLayout(new BoxLayout(activeWayPanel, BoxLayout.PAGE_AXIS));

        activeWayPanel.add(activeWayLabel);
        activeWayPanel.add(previousWayConnectionLabel);
        activeWayPanel.add(nextWayConnectionLabel);
        activeWayPanel.add(openRelationEditorButton);
        wayTraversalPanel.add(activeWayPanel, BorderLayout.CENTER);

        // Right side: buttons to navigate forwards
        wayTraversalPanel.add(
            GuiUtils.createJPanel(
                new FlowLayout(FlowLayout.RIGHT),
                GuiUtils.createJButton("to next way ›", routingHelperAction::goToNextWay)
            ),
            BorderLayout.EAST
        );

        return wayTraversalPanel;
    }

    private static JPanel createDefaultPanel(final RouteExplorer routingHelperAction) {
        return GuiUtils.createJPanel(
            new FlowLayout(FlowLayout.CENTER),
            GuiUtils.createJButton(
                I18n.tr("Start way traversal"),
                routingHelperAction::goToFirstWay
            )
        );
    }

    public enum ConnectionType {
        CONNECTED(PTIcons.GREEN_CHECKMARK, I18n.tr("connected with previous way"), I18n.tr("connected with next way")),
        END(PTIcons.BUFFER_STOP, I18n.tr("first way in the relation"), I18n.tr("last way in the relation")),
        NOT_CONNECTED(PTIcons.STOP_SIGN, I18n.tr("not connected with previous way"), I18n.tr("not connected with next way"));

        final Icon icon;
        final String previousWayText;
        final String nextWayText;

        ConnectionType(final ImageProvider imageProvider, final String previousWayText, final String nextWayText) {
            this.icon = imageProvider.setSize(ImageProvider.ImageSizes.SMALLICON).get();
            this.previousWayText = previousWayText;
            this.nextWayText = nextWayText;
        }
    }
}
