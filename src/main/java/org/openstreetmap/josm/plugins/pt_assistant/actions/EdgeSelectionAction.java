// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.plugins.pt_assistant.gui.PTAssistantLayer;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The action allows to select a set of consecutive ways at once in order to
 * speed up the mapper. The selected ways are going to be coherent to the
 * current route the mapper is working on.
 *
 * @author giacomo
 */
public class EdgeSelectionAction extends MapMode {

    private static final long serialVersionUID = 2414977774504904238L;

    private static final Cursor SELECTION_CURSOR = ImageProvider.getCursor("normal", "selection");
    private static final Cursor WAY_SELECT_CURSOR = ImageProvider.getCursor("normal", "select_way");

    private transient Set<Way> highlighted;

    private List<Way> edgeList = new ArrayList<>();
    private String modeOfTravel = null;

    public EdgeSelectionAction() {
        // CHECKSTYLE.OFF: LineLength
        super(
            tr("Edge Selection"),
            "edgeSelection",
            tr("Edge Selection"),
            Shortcut.registerShortcut("mapmode:edge_selection", tr("Mode: {0}", tr("Edge Selection")), KeyEvent.VK_K, Shortcut.CTRL),
            SELECTION_CURSOR
        );
        // CHECKSTYLE.ON: LineLength
        highlighted = new HashSet<>();
    }

    /*
     * given a way, it looks at both directions for good candidates to be added to
     * the edge
     */
    private List<Way> getEdgeFromWay(Way initial, String modeOfTravel) {
        List<Way> edge1 = new ArrayList<>();
        if (!isWaySuitableForMode(initial, modeOfTravel))
            return edge1;

        Way curr = initial;
        while (true) {
            final List<Way> options = curr.firstNode(true).getParentWays();
            if (StopUtils.isStopPosition(curr.firstNode())) {
                break;
            }
            options.remove(curr);
            curr = chooseBestWay(options, modeOfTravel);
            if (curr == null || edge1.contains(curr))
                break;
            edge1.add(curr);
        }

        List<Way> edge2 = new ArrayList<>();
        curr = initial;
        while (true) {
            final List<Way> options = curr.lastNode(true).getParentWays();
            if (StopUtils.isStopPosition(curr.lastNode())) {
                break;
            }
            options.remove(curr);
            curr = chooseBestWay(options, modeOfTravel);
            if (curr == null || edge2.contains(curr))
                break;
            edge2.add(curr);
        }

        Collections.reverse(edge1);

        edge1.add(initial);
        edge1.addAll(edge2);
        edge1 = sortEdgeWays(edge1);
        return edge1;
    }

    private static List<Way> sortEdgeWays(List<Way> edge) {
        final List<RelationMember> members = edge.stream().map(w -> new RelationMember("", w)).collect(Collectors.toList());
        final List<RelationMember> sorted = new RelationSorter().sortMembers(members);
        return sorted.stream().map(RelationMember::getWay).collect(Collectors.toList());
    }

    private static Boolean isWaySuitableForMode(Way way, String modeOfTravel) {
        if ("bus".equals(modeOfTravel))
            return RouteUtils.isWaySuitableForBuses(way);

        if ("bicycle".equals(modeOfTravel)) {
            return way.hasTag("bicycle", "yes")
                || (
                    !way.hasTag("bicycle", "no", "side_path")
                    && !way.hasTag("highway", "motorway", "trunk", "footway", "pedestrian")
                );
        }

        if ("foot".equals(modeOfTravel)) {
            return way.hasTag("highway", "footway")
                || (
                    !way.hasKey("highway", "motorway")
                    && !way.hasKey("foot", "no", "use_sidepath")
                );
        }

        // if ("hiking".equals(modeOfTravel))
        // return RouteUtils.isWaySuitableForBuses(toCheck);
        //
        if ("horse".equals(modeOfTravel))
            return true;

        if ("light_rail".equals(modeOfTravel))
            return way.hasTag("railway", "light_rail");

        if ("railway".equals(modeOfTravel))
            return way.hasKey("railway");

        if ("subway".equals(modeOfTravel))
            return way.hasTag("railway", "subway");

        if ("train".equals(modeOfTravel))
            return way.hasTag("railway", "rail");

        if ("tram".equals(modeOfTravel))
            return way.hasTag("railway", "tram");

        if ("trolleybus".equals(modeOfTravel)) {
            return way.hasTag("trolley_wire", "yes");
        }

        return RouteUtils.isWaySuitableForPublicTransport(way);
    }

    private static Way chooseBestWay(final List<Way> ways, final String modeOfTravel) {
        final List<Way> suitable = ways.stream()
            .filter(w -> isWaySuitableForMode(w, modeOfTravel))
            .collect(Collectors.toList());
        return suitable.size() == 1 ? suitable.get(0) : null;
    }

    private static String getModeOfTravel() {
        // find a way to get the currently opened relation editor and get the
        // from there the current type of route
        return MainApplication.getLayerManager().getLayers().stream()
            .filter(it -> it instanceof PTAssistantLayer)
            .map(it -> ((PTAssistantLayer) it).getModeOfTravel())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("bus");
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        final Way initial = MainApplication.getMap().mapView.getNearestWay(e.getPoint(), OsmPrimitive::isUsable);

        updateKeyModifiers(e);

        if (!shift && !ctrl) {
            /*
             * remove all previous selection and just add the latest selection
             */
            edgeList.clear();
            ds.clearSelection();
            if (initial != null) {
                modeOfTravel = getModeOfTravel();
                if ("mtb".equals(modeOfTravel))
                    modeOfTravel = "bicycle";
                List<Way> edge = getEdgeFromWay(initial, modeOfTravel);
                for (Way way : edge) {
                    if (!edgeList.contains(way))
                        edgeList.add(way);
                }
                edgeList.addAll(edge);
                // set for a more accurate count
                Set<Way> edgeSet = new HashSet<>(edgeList);
                edgeList = sortEdgeWays(new ArrayList<>(edgeSet));
                new Notification(
                        tr("Mode of Travel -> {0} \n total ways selected -> {1}", modeOfTravel, edgeList.size()))
                                .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(4800).show();
                ds.setSelected(edgeList);
            }

        } else if (!shift && ctrl && initial != null) {
            /*
             * toggle mode where we can individually select and deselect the edges
             */
            if (edgeList.isEmpty() || modeOfTravel == null) {
                modeOfTravel = getModeOfTravel();
                if ("mtb".equals(modeOfTravel))
                    modeOfTravel = "bicycle";
            }

            final List<Way> edge = getEdgeFromWay(initial, modeOfTravel);
            final List<Way> newEdges = new ArrayList<>();
            if (edgeList.containsAll(edge)) {
                edgeList.removeAll(edge);
            } else {
                for (Way way : edge) {
                    if (!edgeList.contains(way)) {
                        edgeList.add(way);
                        newEdges.addAll(findNewEdges(way, edge, edgeList));
                    }
                }
                final List<Way> waysToBeRemoved = waysToBeRemoved(newEdges);
                if (waysToBeRemoved != null) {
                    newEdges.removeAll(waysToBeRemoved);
                }
                edgeList.addAll(newEdges);
            }
            ds.clearSelection();
            Set<Way> edgeSet = new HashSet<>(edgeList);
            edgeList = sortEdgeWays(new ArrayList<>(edgeSet));
            new Notification(tr("Mode of Travel -> {0} \n total ways selected -> {1}", modeOfTravel, edgeList.size()))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(900).show();
            ds.setSelected(edgeList);

        } else if (shift && !ctrl && initial != null) {
            /*
             * add new selection to existing edges
             */
            if (edgeList.isEmpty() || modeOfTravel == null) {
                modeOfTravel = getModeOfTravel();
                if ("mtb".equals(modeOfTravel))
                    modeOfTravel = "bicycle";
            }
            List<Way> edge = getEdgeFromWay(initial, modeOfTravel);
            final List<Way> newEdges = new ArrayList<>();

            for (Way way : edge) {
                if (!edgeList.contains(way)) {
                    edgeList.add(way);
                    newEdges.addAll(findNewEdges(way, edge, edgeList));
                }
            }

            List<Way> waysToBeRemoved = waysToBeRemoved(newEdges);
            if (waysToBeRemoved != null) {
                newEdges.removeAll(waysToBeRemoved);
            }
            edgeList.addAll(newEdges);

            Set<Way> edgeSet = new HashSet<>(edgeList);
            edgeList = sortEdgeWays(new ArrayList<>(edgeSet));
            new Notification(
                    tr("Mode of Travel: {0} \n total ways selected: {1}", modeOfTravel, edgeList.size()))
                            .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(900).show();
            ds.setSelected(edgeList);
        }
    }

    private static List<Way> waysToBeRemoved(List<Way> newEdges) {

        final List<Way> waysToBeRemoved = new ArrayList<>();

        for (int i = 0; i < newEdges.size(); i++) {
            Node node1 = newEdges.get(i).firstNode();
            Node node2 = newEdges.get(i).lastNode();
            for (int j = i + 1; j < newEdges.size(); j++) {
                if (newEdges.get(i).equals(newEdges.get(j)))
                    continue;
                Node node3 = newEdges.get(j).firstNode();
                Node node4 = newEdges.get(j).lastNode();

                if ((node1.equals(node3) && node2.equals(node4)) || (node1.equals(node4) && node2.equals(node3))) {
                    if (!waysToBeRemoved.contains(newEdges.get(i)))
                        waysToBeRemoved.add(newEdges.get(i));
                    if (!waysToBeRemoved.contains(newEdges.get(j)))
                        waysToBeRemoved.add(newEdges.get(j));
                }
            }
        }
        return waysToBeRemoved;
    }

    private static List<Way> findNewEdges(Way way, List<Way> edge, List<Way> edgeList) {
        List<Way> newEdges = new ArrayList<>();

        Node firstNode = way.firstNode();
        Node lastNode = way.lastNode();

        List<Way> parentWayList1 = firstNode.getParentWays();
        parentWayList1.removeAll(edgeList);
        parentWayList1.removeAll(edge);

        List<Way> parentWayList2 = lastNode.getParentWays();
        parentWayList2.removeAll(edgeList);
        parentWayList2.removeAll(edge);

        parentWayList1.addAll(parentWayList2);

        for (Way parentWay : parentWayList1) {
            if (edge.contains(parentWay) || edgeList.contains(parentWay))
                continue;

            Node node1 = parentWay.firstNode();
            Node node2 = parentWay.lastNode();
            for (Way oldWay : edgeList) {
                if (!oldWay.equals(way)) {
                    if ((oldWay.containsNode(node1) && !way.containsNode(node1))
                            || (oldWay.containsNode(node2) && !way.containsNode(node2))) {
                        newEdges.add(parentWay);
                    }

                }
            }
        }
        return newEdges;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);

        highlighted.forEach(it -> it.setHighlighted(false));
        highlighted.clear();

        Way initial = MainApplication.getMap().mapView.getNearestWay(e.getPoint(), OsmPrimitive::isUsable);
        if (initial == null) {
            MainApplication.getMap().mapView.setCursor(SELECTION_CURSOR);
        } else {
            MainApplication.getMap().mapView.setCursor(WAY_SELECT_CURSOR);
            highlighted.addAll(getEdgeFromWay(initial, modeOfTravel));
        }

        highlighted.forEach(it -> it.setHighlighted(true));
    }

    @Override
    public synchronized void enterMode() {
        super.enterMode();
        MainApplication.getMap().mapView.addMouseListener(this);
        MainApplication.getMap().mapView.addMouseMotionListener(this);
    }

    @Override
    public synchronized void exitMode() {
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
        MainApplication.getMap().mapView.removeMouseMotionListener(this);
    }
}
