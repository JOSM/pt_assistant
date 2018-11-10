// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTRouteDataManager;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTWay;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sorts the members of a PT route. It orders first the ways, then the stops
 * according to the assigned ways
 *
 * @author giacomo, Polyglot
 *
 */
public class SortPTRouteMembersAction extends AbstractRelationEditorAction {

    private static final String ACTION_NAME = "Sort PT Route Members Relation Editor";
    private GenericRelationEditor editor = null;

    /**
     * Creates a new SortPTRouteMembersAction
     *
     * @param editorAccess access to relation editor
     */
    public SortPTRouteMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(ACTION_NAME, tr(ACTION_NAME));
        new ImageProvider("icons", "sortptroutemembers").getResource().attachImageIcon(this, true);

        editor = (GenericRelationEditor) editorAccess.getEditor();
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Relation rel = editor.getRelation();
        editor.apply();

        if (rel.hasIncompleteMembers()) {
            if (
                JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                tr("The relation has incomplete members. Do you want to download them and continue with the sorting?"),
                tr("Incomplete Members"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null)
            ) {

                List<Relation> incomplete = Collections.singletonList(rel);
                Future<?> future = MainApplication.worker.submit(new DownloadRelationMemberTask(
                        incomplete,
                        Utils.filteredCollection(
                                DownloadSelectedIncompleteMembersAction.buildSetOfIncompleteMembers(
                                        Collections.singletonList(rel)), OsmPrimitive.class),
                        MainApplication.getLayerManager().getEditLayer()));

                MainApplication.worker.submit(() -> {
                    try {
                        future.get();
                        continueAfterDownload(rel);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                        return;
                    }
                });
            } else {
                return;
            }
        } else {
            continueAfterDownload(rel);
        }
    }

    private void continueAfterDownload(Relation rel) {

        PTRouteDataManager route_manager = new PTRouteDataManager(rel);
        Relation newRel = new Relation(rel);
        Boolean ask_to_create_return_route_and_routeMaster = false;

        if (!RouteUtils.isVersionTwoPTRoute(newRel)) {
            if (
                JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                tr("This relation is not yet PT v2, would you like to set it to 2?"),
                tr("Not a PT v2 relation"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null)
            ) {
                RouteUtils.setPTRouteVersion(newRel, "2");
                ask_to_create_return_route_and_routeMaster = true;
            } else {
                return;
            }
        }
        sortPTRouteMembers(newRel);

        // determine names of first and last stop by looping over all members
        String fromtag = "from";
        String from = newRel.get(fromtag);
        String totag = "to";
        String to = newRel.get(totag);
        String firstStopName = null;
        String lastStopName = null;

        firstStopName = route_manager.getFirstStop().getName();
        lastStopName = route_manager.getLastStop().getName();

        if (from == null || to == null) {
            if (from == null) {
                if (
                    JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                    tr("This relation doesn't have its from tag set? Set it to " + firstStopName + "?"),
                    tr("Set from tag?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null, null)
                ) {

                    newRel.put(fromtag, firstStopName);
                } else {
                    return;
                }
            }
            if (to == null) {
                if (
                    JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                    tr("This relation doesn't have its to tag set? Set it to " + lastStopName + "?"),
                    tr("Set to tag?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null, null)
                ) {
                    newRel.put(totag, lastStopName);
                } else {
                    return;
                }
            }
        }
        from = newRel.get(fromtag);
        if (from != firstStopName) {
            if (
                JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                tr("from=" + from + ". Set it to " + firstStopName + " instead?"),
                tr("Change from tag?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null)
            ) {
                newRel.put(fromtag, firstStopName);
            } else {
                return;
            }
        }
        to = newRel.get(totag);

        if (to != lastStopName) {
            if (
                JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                tr("to=" + to + ". Set it to " + lastStopName + " instead?"),
                tr("Change to tag?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null)
            ) {
                newRel.put(totag, lastStopName);
            } else {
                return;
            }
        }

        UndoRedoHandler.getInstance().add(new ChangeCommand(rel, newRel));
        editor.reloadDataFromRelation();

        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        Relation routeMaster = null;

        if (ask_to_create_return_route_and_routeMaster) {
            if (
                JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                tr("Create route relation for opposite direction of travel?"),
                tr("Opposite itinerary?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null)
            ) {
                Relation otherDirRel = new Relation(newRel);
                otherDirRel.clearOsmMetadata();
                otherDirRel.put(fromtag, lastStopName);
                otherDirRel.put(totag, firstStopName);

                // Reverse order of members in new route relation
                PTRouteDataManager return_route_manager = new PTRouteDataManager(otherDirRel);

                List<PTStop> stops_reversed = return_route_manager.getPTStops();
                Collections.reverse(stops_reversed);
                stops_reversed.forEach(stopMember -> {
                    otherDirRel.removeMembersFor(stopMember.getMember());
                    otherDirRel.addMember(stopMember);
                });

                List<PTWay> ways_reversed = return_route_manager.getPTWays();
                Collections.reverse(ways_reversed);
                ways_reversed.forEach(wayMember -> {
                    otherDirRel.removeMembersFor(wayMember.getMember());
                    otherDirRel.addMember(wayMember);
                });

                UndoRedoHandler.getInstance()
                        .add(new AddCommand(MainApplication.getLayerManager().getActiveDataSet(), otherDirRel));

                RelationEditor editor = RelationDialogManager.getRelationDialogManager()
                        .getEditorForRelation(layer, otherDirRel);
                if (editor == null) {
                    editor = RelationEditor.getEditor(layer, otherDirRel, null);
                    editor.setVisible(true);
                    return;
                }
                editor.reloadDataFromRelation();

                Relation rmr = null;

                for (OsmPrimitive parent : newRel.getReferrers()) {
                    if (parent.get("type") == "routeMaster") {
                        rmr = (Relation) parent;
                        break;
                    }
                }
                System.out.println("rmr" + rmr);
                if (rmr == null) {
                    System.out.println("Creating new routeMaster");
                    routeMaster = new Relation();
                    routeMaster.put("type", "routeMaster");
                    if (rel.hasKey("route")) {
                        routeMaster.put("routeMaster", rel.get("route"));
                    }

                    List<String> tagslist = Arrays.asList("name", "ref", "operator", "network", "colour");
                    for (String key : tagslist) {
                        if (rel.hasKey(key)) {
                            routeMaster.put(key, rel.get(key));
                        }
                    }

                    routeMaster.addMember(new RelationMember("", rel));
                } else {
                    routeMaster = rmr;
                }
                System.out.println("Adding existing route relation to master");
            routeMaster.addMember(new RelationMember("", otherDirRel));

            UndoRedoHandler.getInstance()
            .add(new AddCommand(MainApplication.getLayerManager().getActiveDataSet(), routeMaster));

            RelationEditor editorRM = RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer,
                    routeMaster);
            if (editorRM == null) {
                editorRM = RelationEditor.getEditor(layer, routeMaster, null);
                editorRM.setVisible(true);
                return;
            }
            editor.reloadDataFromRelation();
            }

        } else {
            return;
        }
    }

    /***
     * Sort the members of the PT route.
     *
     * @param rel route to be sorted
     */
    public static void sortPTRouteMembers(Relation rel) {
        if (!RouteUtils.isVersionTwoPTRoute(rel)) {
            return;
        }

        if (rel.hasTag("fixme:relation", "order members")) {
            rel.remove("fixme:relation");
        }

        // first loop trough all the members and remove the roles
        List<RelationMember> members = new ArrayList<>();
        List<RelationMember> oldMembers = rel.getMembers();
        for (int i = 0; i < oldMembers.size(); i++) {
            RelationMember rm = oldMembers.get(i);
            if (!PTStop.isPTPlatform(rm) && !PTStop.isPTStopPosition(rm))
                members.add(new RelationMember("", rm.getMember()));
            else
                members.add(rm);
            rel.removeMember(0);
        }

        // sort the members with the built in sorting function in order to get
        // the continuity of the ways
        members = new RelationSorter().sortMembers(members);

        // divide stops and ways
        List<RelationMember> stops = new ArrayList<>();
        List<RelationMember> wayMembers = new ArrayList<>();
        List<Way> ways = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RelationMember rm = members.get(i);
            if (PTStop.isPTPlatform(rm) || PTStop.isPTStopPosition(rm))
                stops.add(rm);
            else {
                wayMembers.add(rm);
                if (rm.getType() == OsmPrimitiveType.WAY)
                    ways.add(rm.getWay());
            }
        }

        // couple together stop positions and platforms that are part of the same
        // stop. the only way used to determine whether they are part of the same
        // stop or not is the name. this should be improved by using also the
        // distance
        Map<String, List<PTStop>> stopsByName = new HashMap<>();
        List<PTStop> unnamed = new ArrayList<>();
        stops.forEach(rm -> {
            String name = getStopName(rm.getMember());
            if (name != null) {
                if (!stopsByName.containsKey(name))
                    stopsByName.put(name, new ArrayList<>());
                List<PTStop> ls = stopsByName.get(name);
                if (ls.isEmpty()) {
                    ls.add(new PTStop(rm));
                } else {
                    PTStop stp = ls.get(ls.size() - 1);
                    if (!stp.addStopElement(rm)) {
                        ls.add(new PTStop(rm));
                    }
                }
            } else {
                unnamed.add(new PTStop(rm));
            }
        });

        // assign to each stop the corresponding way. if none is found add already
        // the stop to the relation since it is not possible to reason on the order
        StopToWayAssigner assigner = new StopToWayAssigner(ways);
        List<PTStop> ptstops = new ArrayList<>();
        removeWrongSideStops(ptstops, wayMembers);
        stopsByName.values().forEach(ptstops::addAll);

        Map<Way, List<PTStop>> wayStop = new HashMap<>();
        ptstops.forEach(stop -> {
            Way way = assigner.get(stop);
            if (way == null) {
                addStopToRelation(rel, stop);
            }
            if (!wayStop.containsKey(way))
                wayStop.put(way, new ArrayList<PTStop>());
            wayStop.get(way).add(stop);
        });

        unnamed.forEach(stop -> {
            Way way = assigner.get(stop);
            if (way == null) {
                addStopToRelation(rel, stop);
            }
            if (!wayStop.containsKey(way))
                wayStop.put(way, new ArrayList<PTStop>());
            wayStop.get(way).add(stop);
        });

        // based on the order of the ways, add the stops to the relation
        for (int i = 0; i < wayMembers.size(); i++) {
            RelationMember wm = wayMembers.get(i);
            Way prev = null;
            Way next = null;
            if (i > 0) {
                RelationMember wmp = wayMembers.get(i - 1);
                if (wmp.getType() == OsmPrimitiveType.WAY)
                    prev = wmp.getWay();
            }
            if (i < wayMembers.size() - 1) {
                RelationMember wmn = wayMembers.get(i + 1);
                if (wmn.getType() == OsmPrimitiveType.WAY)
                    next = wmn.getWay();
            }

            if (wm.getType() == OsmPrimitiveType.WAY) {
                Way curr = wm.getWay();
                List<PTStop> stps = wayStop.get(curr);
                if (stps != null) {
                    // if for one way there are more than one stop assigned to it,
                    // another sorting step is needed
                    if (stps.size() > 1)
                        stps = sortSameWayStops(stps, curr, prev, next);
                    stps.forEach(stop -> {
                        if (stop != null) {
                            addStopToRelation(rel, stop);
                        }
                    });
                }
            }
        }

        wayMembers.forEach(rel::addMember);
    }

    private static void addStopToRelation(Relation rel, PTStop stop) {
        if (stop.getStopPositionRM() != null)
            rel.addMember(stop.getStopPositionRM());
        if (stop.getPlatformRM() != null)
            rel.addMember(stop.getPlatformRM());
    }

    // sorts the stops that are assigned to the same way. this is done based on
    // the distance from the previous way.
    private static List<PTStop> sortSameWayStops(List<PTStop> stps, Way way, Way prev, Way next) {
        Map<Node, List<PTStop>> closeNodes = new HashMap<>();
        List<PTStop> noLocationStops = new ArrayList<>();
        List<Node> nodes = way.getNodes();
        for (PTStop stop : stps) {
            Node closest = findClosestNode(stop, nodes);
            if (closest == null) {
                noLocationStops.add(stop);
                continue;
            }
            if (!closeNodes.containsKey(closest)) {
                closeNodes.put(closest, new ArrayList<>());
            }
            closeNodes.get(closest).add(stop);
        }

        boolean reverse = false;

        if (prev != null) {
            reverse = prev.firstNode().equals(way.lastNode())
                    || prev.lastNode().equals(way.lastNode());
        } else if (next != null) {
            reverse = next.firstNode().equals(way.firstNode())
                    || next.lastNode().equals(way.firstNode());
        }

        if (reverse)
            Collections.reverse(nodes);

        List<PTStop> ret = getSortedStops(nodes, closeNodes);
        ret.addAll(noLocationStops);
        return ret;
    }

    private static List<PTStop> getSortedStops(List<Node> nodes, Map<Node, List<PTStop>> closeNodes) {

        List<PTStop> ret = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            Node prevNode = i > 0 ? nodes.get(i - 1) : n;
            List<PTStop> stops = closeNodes.get(n);
            if (stops != null) {
                if (stops.size() > 1) {
                    stops.sort((s1, s2) -> {
                        Double d1 = stopEastNorth(s1).distance(prevNode.getEastNorth());
                        Double d2 = stopEastNorth(s2).distance(prevNode.getEastNorth());
                        return d1.compareTo(d2);
                    });
                }
                stops.forEach(ret::add);
            }
        }

        return ret;
    }

    private static Node findClosestNode(PTStop stop, List<Node> nodes) {
        EastNorth stopEN = stopEastNorth(stop);
        if (stopEN == null)
            return null;
        double minDist = Double.MAX_VALUE;
        Node closest = null;
        for (Node node : nodes) {
            double dist = node.getEastNorth().distance(stopEN);
            if (dist < minDist) {
                minDist = dist;
                closest = node;
            }
        }
        return closest;
    }

    private static EastNorth stopEastNorth(PTStop stop) {
        if (stop.getStopPosition() != null)
            return stop.getStopPosition().getEastNorth();
        OsmPrimitive prim = stop.getPlatform();
        if (prim.getType() == OsmPrimitiveType.WAY)
            return ((Way) prim).firstNode().getEastNorth();
        else if (prim.getType() == OsmPrimitiveType.NODE)
            return ((Node) prim).getEastNorth();
        else
            return null;
    }

    private static String getStopName(OsmPrimitive p) {
        for (Relation ref : Utils.filteredCollection(p.getReferrers(), Relation.class)) {
            if (
                ref.hasTag("type", "public_transport")
                    && StopUtils.isStopArea(ref)
                    && ref.getName() != null
            ) {
                return ref.getName();
            }
        }
        return p.getName();
    }

    private static void removeWrongSideStops(List<PTStop> ptstop, List<RelationMember> wayMembers) {
        for (int i = 0; i < wayMembers.size(); i++) {
            RelationMember wm = wayMembers.get(i);
            Way prev = null;
            Way next = null;
            if (i > 0) {
                RelationMember wmp = wayMembers.get(i - 1);
                if (wmp.getType() == OsmPrimitiveType.WAY)
                    prev = wmp.getWay();
            }
            if (i < wayMembers.size() - 1) {
                RelationMember wmn = wayMembers.get(i + 1);
                if (wmn.getType() == OsmPrimitiveType.WAY)
                    next = wmn.getWay();
            }
            if (wm.getType() == OsmPrimitiveType.WAY) {
                Way curr = wm.getWay();
                Node firstNode = findCommonNode(curr, prev);
                Node lastNode = findCommonNode(curr, next);
                System.out.println(i);
                if (firstNode != null && firstNode.equals(curr.getNode(0))) {
                    System.out.println("Front Way 1 == " + curr.getName());
                } else if (firstNode != null && firstNode.equals(curr.getNode(curr.getNodesCount() - 1))) {
                    System.out.println("Back Way 2 == " + curr.getName());
                } else if (lastNode != null && lastNode.equals(curr.getNode(0))) {
                    System.out.println("Back Way 3 == " + curr.getName());
                } else if (lastNode != null && lastNode.equals(curr.getNode(curr.getNodesCount() - 1))) {
                    System.out.println("Front Way 4 == " + curr.getName());
                }
            }
        }
    }

    private static Node findCommonNode(Way w1, Way w2) {
        if (w1 == null || w2 == null)
            return null;
        for (int i = 0; i < w1.getNodes().size(); i++) {
            for (int j = 0; j < w2.getNodes().size(); j++) {
                if (w1.getNodes().get(i).equals(w2.getNodes().get(j)))
                    return w1.getNodes().get(i);
            }
        }
        return null;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editor != null && RouteUtils.isPTRoute(editor.getRelation()));
    }
}
