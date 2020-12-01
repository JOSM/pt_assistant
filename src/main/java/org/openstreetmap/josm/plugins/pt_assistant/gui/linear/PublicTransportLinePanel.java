package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel.safeHtml;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRefKey;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRefKeyEmpty;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRelation;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRelationsProvider;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops.FoundStop;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops.StopCollector;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This panel displays a public transport line and it's stops
 */
public class PublicTransportLinePanel extends JPanel {

    public static final Color HIGHLIGHT = new Color(0x6989FF);

    public PublicTransportLinePanel(LineRelationsProvider p) {
        List<LineRelation> relations = Objects.requireNonNull(p.getRelations(), "p.getRelations()");

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        add(p.createHeadlinePanel());

        add(Box.createRigidArea(new Dimension(0, 5)));

        if (relations.isEmpty()) {
            add(new UnBoldLabel(tr("No public transport v2 routes are currently used for this relation.")));
            add(Box.createVerticalGlue());
        } else {
            renderRelationGrid(p, sortByRefKeys(relations));
        }
    }

    private List<LineRelation> sortByRefKeys(List<LineRelation> relations) {
        // Don't just use the ref. This compares to LineRefKey, since we may have multiple with same ref.
        List<LineRefKey> allRefs = relations
            .stream()
            .map(LineRelation::getLineRefKey)
            .sorted(Comparator.comparing(LineRefKey::getRef))
            .distinct()
            .collect(Collectors.toList());

        Comparator<LineRelation> byRefKey = Comparator.comparing(it -> allRefs.indexOf(it.getLineRefKey()));
        return relations
            .stream()
            .sorted(byRefKey.thenComparing(it -> it.getRelation().get("ref")))
            .collect(Collectors.toList());
    }

    private void renderRelationGrid(LineRelationsProvider p, List<LineRelation> relations) {
        int actionColumn = relations.size();
        int labelColumn = relations.size() + 1;
        JPanel gridArea = new JPanel(new GridBagLayout());
        int stopGridOffset;
        BitSet stopsToHighlight = new BitSet();

        StopCollector collector = new StopCollector(relations);
        List<FoundStop> stops = collector.getAllStops();

        // HEADER LINES
        if (relations.size() <= 6 || stops.isEmpty()) {
            // Horizontal headers to the right of all columns
            stopGridOffset = relations.size();
            for (int i = 0; i < relations.size(); i++) {
                gridArea.add(new LineGridHorizontalColumnArrow(),
                    GBC.std(i, i).span(relations.size() - i).fill().weight(0, 0));
                gridArea.add(new LineGridRouteActions(p.getLayer(), relations.get(i)),
                    GBC.std(actionColumn, i));
                gridArea.add(new LineGridRouteHeader(relations.get(i)),
                    GBC.std(labelColumn, i).fill(GridBagConstraints.HORIZONTAL).weight(1, 0));
            }
        } else {
            // Diagonal headers above each column
            stopGridOffset = 2;
            for (int i = 0; i < relations.size(); i++) {
                gridArea.add(new Rotate90DegreesPanel(new LineGridRouteHeader(relations.get(i))),
                    GBC.std(i, 0).fill(GridBagConstraints.VERTICAL).weight(0, 0));
                gridArea.add(new LineGridRouteActions(p.getLayer(), relations.get(i)),
                    GBC.std(i, 1));
            }
        }

        // Line ref keys (e.g. for stop areas it indicates which platform the train stops at)
        if (!relations.stream().allMatch(it -> it.getLineRefKey().equals(new LineRefKeyEmpty()))) {
            // We have line refs => add them to the top of each line(s)
            for (int i = 0; i < relations.size(); i++) {
                LineRefKey lineRefKey = relations.get(i).getLineRefKey();
                int start = i;
                while (i + 1 < relations.size()
                    && relations.get(i + 1).getLineRefKey().equals(lineRefKey)) {
                    i++;
                }
                gridArea.add(new LineRefKeyPanel(lineRefKey),
                    GBC.std(start, stopGridOffset).span(i - start + 1).fill(GridBagConstraints.HORIZONTAL).weight(0, 0));
            }
            stopGridOffset++;
        }

        // CONTENT LINES
        for (int i = 0; i < stops.size(); i++) {
            FoundStop stop = stops.get(i);
            boolean shouldHighlightStop = p.shouldHighlightStop(stop);
            if (stop.isIncomplete() || !stop.getNameAndInfos().isEmpty()) {
                String incomplete = stop.isIncomplete() ? " <font color=\"red\">" + tr("Incomplete") + "</font>" : "";
                UnBoldLabel label = new UnBoldLabel(MessageFormat.format("<html>{0}{1}</html>", safeHtml(stop.getNameAndInfos()), incomplete));
                if (shouldHighlightStop) {
                    label.setBackground(HIGHLIGHT);
                }
                gridArea.add(label, GBC.std(labelColumn, stopGridOffset + i));
            }
            Component actions = stop.createActionButtons();
            if (shouldHighlightStop) {
                actions.setBackground(HIGHLIGHT);
            }
            if (actions != null) {
                gridArea.add(actions, GBC.std(actionColumn, stopGridOffset + i));
            }
            stopsToHighlight.set(i, shouldHighlightStop);
        }
        List<List<LineGridCell>> gridColumns = collector.getLineGrid();
        for (int column = 0; column < gridColumns.size(); column++) {
            List<LineGridCell> lineCells = gridColumns.get(column);
            for (int row = 0; row < lineCells.size(); row++) {
                LineGridCell comp = lineCells.get(row);
                if (stopsToHighlight.get(row)) {
                    comp.setBackground(HIGHLIGHT);
                }
                gridArea.add(comp, GBC.std(column, stopGridOffset + row).fill().weight(0, 0));
            }
        }
        // SPACING LINES (below all others, make sure that each column has a min width and remaining space is filled)
        int spacingGridY = relations.size() + stops.size();
        for (int i = 0; i < relations.size(); i++) {
            gridArea.add(new JPanel(), GBC.std(i, spacingGridY).fill(GridBagConstraints.VERTICAL).insets(20, 0, 0, 0).weight(0, 1));
        }
        add(gridArea);
    }

    public static JPanel createActions(JButton... actions) {
        JPanel panel = new JPanel();
        for (JButton action: actions) {
            panel.add(action);
        }
        return panel;
    }

    public static JButton createAction(String label, ImageProvider icon, Runnable action) {
        JButton button = new JButton(new AbstractAction() {
            {
                // putValue(NAME, label);
                putValue(SHORT_DESCRIPTION, label);
                icon.getResource().attachImageIcon(this, true);
                setEnabled(action != null);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    public static boolean isRouteMaster(OsmPrimitive relation) {
        return relation.getType() == OsmPrimitiveType.RELATION && relation.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.VALUE_TYPE_ROUTE_MASTER);
    }

}
