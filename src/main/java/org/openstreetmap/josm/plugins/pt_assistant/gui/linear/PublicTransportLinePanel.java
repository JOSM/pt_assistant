package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isVersionTwoPTRoute;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This panel displays a public transport line and it's stops
 */
public class PublicTransportLinePanel extends JPanel {

    public PublicTransportLinePanel(LineRelationsProvider p) {
        Optional<Relation> master = Objects.requireNonNull(p.getMasterRelation(), "p.getMasterRelation()");
        List<LineRelation> relations = Objects.requireNonNull(p.getRelations(), "p.getRelations()");
        String color = master.map(it -> it.get("colour")).filter(Objects::nonNull).orElse("#888888");

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        if (master.isPresent()) {
            Relation m = master.get();
            String headline = MessageFormat.format("<font background=\"{0}\">{1}</font> {2}", safeHtml(color), safeHtml(m.get("ref")), safeHtml(m.get("name")));
            String infos = safeHtml(m.get("operator")) + " " + safeHtml(m.get("network"));
            String routeMasterText = MessageFormat.format("<html><h2>{0}</h2><div>{1}</div></html>", headline, infos);
            add(new UnBoldLabel(routeMasterText));
        } else {
            add(new UnBoldLabel(tr("Route that is not in a parent relation")));
        }

        add(Box.createRigidArea(new Dimension(0, 5)));

        if (relations.isEmpty()) {
            add(new UnBoldLabel(tr("No public transport v2 routes are currently used for this relation.")));
        } else {
            renderRelationGrid(p, relations);
        }
    }

    private void renderRelationGrid(LineRelationsProvider p, List<LineRelation> relations) {
        int actionColumn = relations.size();
        int labelColumn = relations.size() + 1;

        // HEADER LINES
        JPanel gridArea = new JPanel(new GridBagLayout());
        for (int i = 0; i < relations.size(); i++) {
            Relation relation = relations.get(i).getRelation();
            gridArea.add(new LineGridHorizontalColumnArrow(), GBC.std(i, i).span(relations.size() - i).fill().weight(0, 0));
            String name = safeHtml(relation.get("name"));
            String path = Stream.of(relation.get("from"), relation.get("via"), relation.get("to")).filter(Objects::nonNull).collect(Collectors.joining(" → "));
            UnBoldLabel label = new UnBoldLabel(MessageFormat.format("<html><div><font color=\"{2}\">{0}</font></div><div>" + "<font color=\"{2}\">{1}</font></div></html>",
                name, safeHtml(path), relations.get(i).isPrimary() ? "black" : "#888888"));
            gridArea.add(label, GBC.std(labelColumn, i).fill(GridBagConstraints.HORIZONTAL).weight(1, 0));

            gridArea.add(createActions(
                createAction(tr("Open route relation"), new ImageProvider("dialogs", "edit"),
                    relation.equals(p.getCurrentRelation()) ? null : () -> EditRelationAction.launchEditor(relation))
            ), GBC.std(actionColumn, i));
        }

        // CONTENT LINES
        StopCollector collector = new StopCollector(relations);
        List<StopCollector.FoundStop> stops = collector.getAllStops();
        int stopGridOffset = relations.size();
        for (int i = 0; i < stops.size(); i++) {
            StopCollector.FoundStop stop = stops.get(i);
            if (stop.isIncomplete() || !stop.getNameAndInfos().isEmpty()) {
                String incomplete = stop.isIncomplete() ? " <font color=\"red\">" + tr("Incomplete") + "</font>" : "";
                UnBoldLabel label = new UnBoldLabel(MessageFormat.format("<html>{0}{1}</html>", safeHtml(stop.getNameAndInfos()), incomplete));
                gridArea.add(label, GBC.std(labelColumn, stopGridOffset + i));
            }
        }
        List<List<LineGridCell>> gridColumns = collector.getLineGrid();
        for (int column = 0; column < gridColumns.size(); column++) {
            List<LineGridCell> lineCells = gridColumns.get(column);
            for (int row = 0; row < lineCells.size(); row++) {
                gridArea.add(lineCells.get(row), GBC.std(column, stopGridOffset + row).fill().weight(0, 0));
            }
        }
        // SPACING LINES (below all others, make sure that each column has a min width and remaining space is filled)
        int spacingGridY = relations.size() + stops.size();
        for (int i = 0; i < relations.size(); i++) {
            gridArea.add(new JPanel(), GBC.std(i, spacingGridY).fill(GridBagConstraints.VERTICAL).insets(20, 0, 0, 0).weight(0, 1));
        }
        add(gridArea);
    }

    private JPanel createActions(JButton... actions) {
        JPanel panel = new JPanel();
        for (JButton action: actions) {
            panel.add(action);
        }
        return panel;
    }

    private JButton createAction(String label, ImageProvider icon, Runnable action) {
        return new JButton(new AbstractAction() {
            {
                // putValue(NAME, label);
                putValue(SHORT_DESCRIPTION, label);
                icon.getResource()
                    .attachImageIcon(this, true);
                setEnabled(action != null);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private String safeHtml(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static boolean isRouteMaster(OsmPrimitive relation) {
        return relation.getType() == OsmPrimitiveType.RELATION && relation.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.VALUE_TYPE_ROUTE_MASTER);
    }

    private static class UnBoldLabel extends JLabel {
        public UnBoldLabel(String text) {
            super(text);
            setHorizontalAlignment(LEFT);
            setFont(getFont().deriveFont(0));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, super.getMaximumSize().height);
        }
    }

}
