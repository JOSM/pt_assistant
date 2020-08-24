package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.drew.lang.annotations.Nullable;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The top panel that is added via {@link MapFrame#addTopPanel(Component)}.
 * This class should just handle the display and input. The state of the routing helper should be handled in {@link RoutingHelperAction}.
 */
public class RoutingHelperPanel extends JPanel {

    private final JLabel wayLabel = new JLabel("Way");

    private final RoutingHelperAction routingHelperAction;

    public RoutingHelperPanel(final RoutingHelperAction routingHelperAction) {
        this.routingHelperAction = routingHelperAction;


        // Style main label
        wayLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton closeButton = new JButton(ImageProvider.get("misc", "black_x"));
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText(I18n.tr("Close the routing helper"));
        closeButton.addActionListener(e ->
            Optional.ofNullable(MainApplication.getMap())
                .ifPresent(map -> {
                    map.removeTopPanel(RoutingHelperPanel.class);
                    routingHelperAction.updateEnabledState();
                })
        );

        final JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.add(wayLabel, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.EAST);

        // build the left and right button panels
        final JPanel buttonLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonLeftPanel.setOpaque(false);
        final JButton prevGapButton = new JButton("« to previous gap");
        prevGapButton.addActionListener(e -> routingHelperAction.goToPreviousGap());
        buttonLeftPanel.add(prevGapButton);
        final JButton prevButton = new JButton("‹ to previous way");
        prevButton.addActionListener(e -> routingHelperAction.goToPreviousWay());
        buttonLeftPanel.add(prevButton);

        final JPanel buttonRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRightPanel.setOpaque(false);
        final JButton nextButton = new JButton("to next way ›");
        nextButton.addActionListener(e -> routingHelperAction.goToNextWay());
        buttonRightPanel.add(nextButton);
        final JButton nextgapButton = new JButton("to next gap »");
        nextgapButton.addActionListener(e -> routingHelperAction.goToNextGap());
        buttonRightPanel.add(nextgapButton);

        // Combine both button panels into one
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(buttonLeftPanel, BorderLayout.WEST);
        buttonPanel.add(buttonRightPanel, BorderLayout.EAST);

        // put everything together
        setBackground(new Color(0xFF9966));
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void onCurrentWayChange(@Nullable final Way way) {
        if (way == null) {
            wayLabel.setText(I18n.tr("No way found in relation"));
        } else {
            wayLabel.setText(I18n.tr("Active way: {0} ({1} nodes)", way.getId(), way.getNodesCount()));
        }
    }
}
