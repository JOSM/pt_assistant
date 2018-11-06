// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ptl;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Computes and displays the distance between stops.
 */
public class DistanceBetweenStops extends JosmAction {

    public DistanceBetweenStops() {
        super(tr("Distance between stops"), null, null, null, false);
    }

    static String calculateDistanceBetweenStops(final Relation route) {
        CheckParameterUtil.ensureThat(isRouteSupported(route), "A valid public_transport:version=2 route is required");

        final List<Node> stopNodes = new ArrayList<>();
        final List<RelationMember> routeSegments = new ArrayList<>();
        final List<Node> routeNodes = new ArrayList<>();
        for (final RelationMember member : route.getMembers()) {
            if (member.hasRole("stop", "stop_exit_only", "stop_entry_only") && OsmPrimitiveType.NODE.equals(member.getType())) {
                stopNodes.add(member.getNode());
            } else if (member.hasRole("") && OsmPrimitiveType.WAY.equals(member.getType())) {
                routeSegments.add(member);
            }
        }

        final WayConnectionTypeCalculator connectionTypeCalculator = new WayConnectionTypeCalculator();
        final List<WayConnectionType> links = connectionTypeCalculator.updateLinks(routeSegments);
        for (int i = 0; i < links.size(); i++) {
            final WayConnectionType link = links.get(i);
            final List<Node> nodes = routeSegments.get(i).getWay().getNodes();
            if (link.direction == WayConnectionType.Direction.BACKWARD) {
                Collections.reverse(nodes);
            }
            if (link.direction == WayConnectionType.Direction.BACKWARD || link.direction == WayConnectionType.Direction.FORWARD) {
                routeNodes.addAll(link.linkPrev ? nodes.subList(1, nodes.size()) : nodes);
            }
        }

        final StringBuilder sb = new StringBuilder();
        Node lastN = null;
        List<Node> remainingRouteNodes = routeNodes;
        double totalLength = 0.0;
        int lengthN = 0;
        final boolean onlyLowerUnit = Config.getPref().getBoolean("system_of_measurement.use_only_lower_unit", false);
        Config.getPref().putBoolean("system_of_measurement.use_only_lower_unit", true);
        try {
            for (Node n : stopNodes) {
                final double length;
                if (lastN != null) {
                    if (remainingRouteNodes.indexOf(lastN) > 0) {
                        remainingRouteNodes = remainingRouteNodes.subList(remainingRouteNodes.indexOf(lastN), remainingRouteNodes.size());
                    }
                    if (remainingRouteNodes.indexOf(n) > 0) {
                        final List<Node> segmentBetweenStops = remainingRouteNodes.subList(0, remainingRouteNodes.indexOf(n) + 1);
                        length = getLength(segmentBetweenStops);
                        totalLength += length;
                        lengthN++;
                    } else {
                        length = Double.NaN;
                    }
                } else {
                    length = 0.0;
                }
                sb.append(SystemOfMeasurement.getSystemOfMeasurement().getDistText(length, new DecimalFormat("0"), -1))
                    .append('\t')
                    .append(n.getDisplayName(DefaultNameFormatter.getInstance()))
                    .append('\n');
                lastN = n;
            }
            sb.insert(0, '\n')
                .insert(0, route.getDisplayName(DefaultNameFormatter.getInstance()))
                .insert(0, '\t')
                .insert(0, SystemOfMeasurement.getSystemOfMeasurement().getDistText(totalLength / lengthN, new DecimalFormat("0"), -1));
        } finally {
            Config.getPref().putBoolean("system_of_measurement.use_only_lower_unit", onlyLowerUnit);
        }

        return sb.toString();
    }

    private static boolean isRouteSupported(Relation route) {
        return !route.hasIncompleteMembers()
                && route.hasTag("type", "route")
                && route.hasKey("route")
                && route.hasTag("public_transport:version", "2");
    }

    private static double getLength(Iterable<Node> nodes) {
        double length = 0;
        Node lastN = null;
        for (Node n : nodes) {
            if (lastN != null) {
                final LatLon lastNcoor = lastN.getCoor();
                final LatLon coor = n.getCoor();
                if (lastNcoor != null && coor != null) {
                    length += coor.greatCircleDistance(lastNcoor);
                }
            }
            lastN = n;
        }
        return length;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getLayerManager().getEditDataSet() == null) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (Relation relation : getLayerManager().getEditDataSet().getSelectedRelations()) {
            if (isRouteSupported(relation)) {
                sb.append('\n').append(calculateDistanceBetweenStops(relation)).append('\n');
            } else {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "<html>" + tr("A valid public_transport:version=2 route is required") +
                    Utils.joinAsHtmlUnorderedList(Collections.singleton(relation.getDisplayName(DefaultNameFormatter.getInstance()))),
                    tr("Invalid selection"),
                    JOptionPane.WARNING_MESSAGE
                );
            }
        }

        new ExtendedDialog(MainApplication.getMainFrame(), getValue(NAME).toString(), tr("Close")) {
            {
                setButtonIcons("ok.png");
                final JosmTextArea jte = new JosmTextArea();
                jte.setFont(GuiHelper.getMonospacedFont(jte));
                jte.setEditable(false);
                jte.append(sb.toString());
                jte.setSelectionStart(0);
                jte.setSelectionEnd(0);
                setContent(jte);
            }

        }.showDialog();
    }
}
