// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
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
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTRouteDataManager;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTWay;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sorts the members of a PT route. It orders first the ways, then the stops
 * according to the assigned ways
 *
 * @author giacomo, Polyglot
 *
 */
public class SortPTRouteMembersAction extends AbstractRelationEditorAction {

    private static final long serialVersionUID = 1L;

    private GenericRelationEditor editor = null;
    public static boolean zooming = true;
    public static HashMap<Long, String> stopOrderMap = new HashMap<>();
    public static LatLon locat = null;
    public static boolean rightSide = true;

    /**
     * Creates a new SortPTRouteMembersAction
     *
     * @param editorAccess access to relation editor
     */
    public SortPTRouteMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(SHORT_DESCRIPTION, tr("Sort PT Route Members Relation Editor"));
        new ImageProvider("icons", "sortptroutemembers").getResource().attachImageIcon(this, true);
        editor = (GenericRelationEditor) editorAccess.getEditor();
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Relation rel = editor.getRelation();
        editor.apply();

        if (rel.hasIncompleteMembers()) {
            if (DialogUtils.showYesNoQuestion(tr("Not all members are downloaded"), tr(
                    "The relation has incomplete members.\nDo you want to download them and continue with the sorting?"))) {

                List<Relation> incomplete = Collections.singletonList(rel);
                Future<?> future = MainApplication.worker.submit(new DownloadRelationMemberTask(incomplete,
                        Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
                                .buildSetOfIncompleteMembers(Collections.singletonList(rel)), OsmPrimitive.class),
                        MainApplication.getLayerManager().getEditLayer()));

                MainApplication.worker.submit(() -> {
                    try {
                        future.get();
                        continueAfterDownload(rel);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            }
        } else {
            continueAfterDownload(rel);
        }
    }

    private void continueAfterDownload(Relation rel) {
        Relation newRel = new Relation(rel);
        Boolean ask_to_create_return_route_and_routeMaster = false;

        if (!RouteUtils.isVersionTwoPTRoute(newRel)) {
            if (DialogUtils.showYesNoQuestion(tr("This is not a PT v2 relation"),
                    tr("This relation is not PT version 2. Sorting its stops wouldn't make sense.\n"
                            + "Would you like to set ''public_transport:version=2''?\n\n"
                            + "There will be some extra work needed after c,\n\n"
                            + "but PT_Assistant can help prepare the relations."))) {
                RouteUtils.setPTRouteVersion(newRel, "2");
                ask_to_create_return_route_and_routeMaster = true;
            } else {
                return;
            }
        }
        for (RelationMember rm : newRel.getMembers()) {
            if (rm.getType() == OsmPrimitiveType.WAY) {
                locat = rm.getWay().firstNode().getCoor();
                break;
            }
        }
        RightAndLefthandTraffic.initialize();
        rightSide = RightAndLefthandTraffic.isRightHandTraffic(locat);

        sortPTRouteMembers(newRel);
        UndoRedoHandler.getInstance().add(new ChangeCommand(rel, newRel));
        editor.reloadDataFromRelation();

        final PTRouteDataManager route_manager = new PTRouteDataManager(rel);

        final String from = route_manager.get("from");
        final Optional<String> firstStopName = Optional.ofNullable(route_manager.getFirstStop()).map(PTStop::getName)
                .filter(name -> !name.isEmpty()).map(name -> {
                    if ((from.isEmpty() && DialogUtils.showYesNoQuestion(tr("Set from tag?"),
                            tr("''from'' tag not set. Set it to\n{0} ?", name)))
                            || (!name.equals(from) && DialogUtils.showYesNoQuestion(tr("Change from tag?"),
                                    tr("''from''={0}.\nChange it to\n''{1}''\n instead?", from, name)))) {
                        route_manager.set("from", name);
                    }
                    return name;
                });

        final Optional<String> lastStopName = Optional.ofNullable(route_manager.getLastStop()).map(PTStop::getName)
                .filter(name -> !name.isEmpty()).map(name -> {
                    final String to = route_manager.get("to");
                    if ((to.isEmpty() && DialogUtils.showYesNoQuestion(tr("Set to tag?"),
                            tr("''to'' tag not set. Set it to\n{0} ?", name)))
                            || (!name.equals(to) && DialogUtils.showYesNoQuestion(tr("Change to tag?"),
                                    tr("''to''={0}.\nChange it to\n''{1}''\n instead?", to, name)))) {
                        route_manager.set("to", name);
                    }
                    return name;
                });

        String proposedRelname = route_manager.getComposedName();
        if (!Objects.equals(proposedRelname, route_manager.get("name"))) {
            if (DialogUtils.showYesNoQuestion(tr("Change name tag?"),
                    tr("Change name to\n''{0}''\n?", proposedRelname))) {
                route_manager.set("name", proposedRelname);
            }
            route_manager.writeTagsToRelation();
            editor.reloadDataFromRelation();

            OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
            Relation routeMaster = null;

            if (ask_to_create_return_route_and_routeMaster && DialogUtils.showYesNoQuestion(tr("Opposite itinerary?"),
                    tr("Create ''route'' relation for opposite direction of travel?"))) {
                Relation otherDirRel = new Relation(newRel);
                otherDirRel.clearOsmMetadata();
                lastStopName.ifPresent(name -> otherDirRel.put("from", name));
                firstStopName.ifPresent(name -> otherDirRel.put("to", name));

                // Reverse order of members in new route relation
                final PTRouteDataManager return_route_manager = new PTRouteDataManager(otherDirRel);

                final List<PTStop> stops_reversed = return_route_manager.getPTStops();
                Collections.reverse(stops_reversed);
                stops_reversed.forEach(stopMember -> {
                    otherDirRel.removeMembersFor(stopMember.getMember());
                    otherDirRel.addMember(stopMember);
                });

                final List<PTWay> ways_reversed = return_route_manager.getPTWays();
                Collections.reverse(ways_reversed);
                ways_reversed.forEach(wayMember -> {
                    otherDirRel.removeMembersFor(wayMember.getMember());
                    otherDirRel.addMember(wayMember);
                });

                UndoRedoHandler.getInstance()
                        .add(new AddCommand(MainApplication.getLayerManager().getActiveDataSet(), otherDirRel));

                RelationEditor editor = RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer,
                        otherDirRel);
                if (editor == null) {
                    editor = RelationEditor.getEditor(layer, otherDirRel, null);
                    editor.setVisible(true);
                }
                editor.reloadDataFromRelation();

                final Optional<Relation> rmr = rel.getReferrers().stream()
                        .map(it -> it instanceof Relation ? (Relation) it : null)
                        .filter(it -> it != null
                                && OSMTags.VALUE_TYPE_ROUTE_MASTER.equals(it.get(OSMTags.KEY_RELATION_TYPE)))
                        .findFirst();

                if (rmr.isPresent()) {
                    routeMaster = rmr.get();
                } else if (DialogUtils.showYesNoQuestion(tr("Create a route_master?"),
                        tr("Create ''route_master'' relation and add route relations to it?"))) {
                    routeMaster = new Relation();
                    routeMaster.put(OSMTags.KEY_RELATION_TYPE, OSMTags.VALUE_TYPE_ROUTE_MASTER);
                    if (rel.hasKey(OSMTags.KEY_ROUTE)) {
                        routeMaster.put(OSMTags.KEY_ROUTE_MASTER, rel.get(OSMTags.KEY_ROUTE));

                        List<String> tagslist = Arrays.asList("name", "ref", "operator", "network", "colour");
                        for (String key : tagslist) {
                            if (rel.hasKey(key)) {
                                routeMaster.put(key, rel.get(key));
                            }
                        }
                    }

                    routeMaster.addMember(new RelationMember("", rel));
                    UndoRedoHandler.getInstance()
                            .add(new AddCommand(MainApplication.getLayerManager().getActiveDataSet(), routeMaster));
                } else {
                    return;
                }

                Relation newRouteMaster = new Relation(routeMaster);
                newRouteMaster.addMember(new RelationMember("", otherDirRel));

                UndoRedoHandler.getInstance().add(new ChangeCommand(routeMaster, newRouteMaster));
                editor.reloadDataFromRelation();

                SearchAction.search("(oneway OR junction=roundabout -closed) child new", SearchMode.fromCode('R'));

                RelationEditor editorRM = RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer,
                        routeMaster);
                if (editorRM == null) {
                    editorRM = RelationEditor.getEditor(layer, routeMaster, null);
                    editorRM.setVisible(true);
                    editorRM.toBack();
                    return;
                }
                editor.reloadDataFromRelation();
            }
        }
    }

    public static Node getOtherNode(Way way, Node currentNode) {
        if (way.firstNode().equals(currentNode))
            return way.lastNode();
        else
            return way.firstNode();
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
        computeRefs(rel);
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
        // HashMap<Way,ArrayList<PTStop>> RightSideStops = new RightSideStops();
        // HashMap<Way,ArrayList<PTStop>> LeftSideStops = new LeftSideStops();
        for (int i = 0; i < members.size(); i++) {
            RelationMember rm = members.get(i);
            if (PTStop.isPTPlatform(rm) || PTStop.isPTStopPosition(rm))
                stops.add(rm);
            else {
                wayMembers.add(rm);
                if (rm.getType() == OsmPrimitiveType.WAY) {
                    ways.add(rm.getWay());
                }
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

        removeWrongSideStops(wayMembers);
        stopsByName.values().forEach(ptstops::addAll);

        Map<Way, List<PTStop>> wayStop = new HashMap<>();
        PTRouteDataManager route = new PTRouteDataManager(rel);

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
        HashMap<Way, ArrayList<PTStop>> RightSideStops = new HashMap<>();
        HashMap<Way, ArrayList<PTStop>> LeftSideStops = new HashMap<>();
        HashMap<Way, Integer> wayAlreadyThere = new HashMap<>();
        HashMap<PTStop, Integer> StopHasBeenChecked = new HashMap<>();
        Way prev1 = null;
        Node strt = null;
        Node endn = null;
        Node tempstrt = null;
        Node tempend = null;
        for (int in = 0; in < ways.size(); in++) {
            Way w = ways.get(in);
            wayAlreadyThere.put(w, 0);
            if (prev1 == null) {
                Way nex = ways.get(in + 1);
                if (w.firstNode().equals(nex.firstNode()) || w.firstNode().equals(nex.lastNode())) {
                    strt = w.lastNode();
                    endn = w.firstNode();
                    tempstrt = w.lastNode();
                    tempend = w.firstNode();
                } else {
                    strt = w.firstNode();
                    endn = w.lastNode();
                    tempstrt = w.firstNode();
                    tempend = w.lastNode();
                }
            } else {
                strt = endn;
                endn = getOtherNode(w, strt);
                tempstrt = strt;
                tempend = endn;
            }
            if (wayStop.containsKey(w)) {
                for (PTStop pts : wayStop.get(w)) {
                    Node node3 = pts.getNode();
                    Pair<Node, Node> segment = assigner.calculateNearestSegment(node3, w);
                    Node node1 = segment.a;
                    Node node2 = segment.b;
                    //if the endn(it is not a link at this point) is the starting point of the way nodes
                    if (w.getNodes().get(0).equals(endn)) {
                        for (int i = 0; i < w.getNodes().size() - 1; i++) {
                            if (w.getNodes().get(i) == node1 && w.getNodes().get(i + 1) == node2) {
                                tempend = node1;
                                tempstrt = node2;
                            } else if (w.getNodes().get(i) == node2 && w.getNodes().get(i) == node1) {
                                tempend = node2;
                                tempstrt = node1;
                            }
                        }
                    } else {
                        for (int i = w.getNodes().size() - 1; i > 0; i--) {
                            if (w.getNodes().get(i) == node1 && w.getNodes().get(i - 1) == node2) {
                                tempend = node1;
                                tempstrt = node2;
                            } else if (w.getNodes().get(i) == node2 && w.getNodes().get(i - 1) == node1) {
                                tempend = node2;
                                tempstrt = node1;
                            }
                        }
                    }
                    if (rightSide) {
                        if (route.crossProductValue(tempstrt, tempend, pts) <= 0) {
                            if (!RightSideStops.containsKey(w)) {
                                RightSideStops.put(w, new ArrayList<PTStop>());
                            }
                            if (StopHasBeenChecked.get(pts) == null) {
                                RightSideStops.get(w).add(pts);
                            }
                        } else {
                            if (!LeftSideStops.containsKey(w)) {
                                LeftSideStops.put(w, new ArrayList<PTStop>());
                            }
                            if (StopHasBeenChecked.get(pts) == null) {
                                LeftSideStops.get(w).add(pts);
                            }
                        }
                    } else {
                        if (route.crossProductValue(tempstrt, tempend, pts) >= 0) {
                            if (!RightSideStops.containsKey(w)) {
                                RightSideStops.put(w, new ArrayList<PTStop>());
                            }
                            if (StopHasBeenChecked.get(pts) == null) {
                                RightSideStops.get(w).add(pts);
                            }
                        } else {
                            if (!LeftSideStops.containsKey(w)) {
                                LeftSideStops.put(w, new ArrayList<PTStop>());
                            }
                            if (StopHasBeenChecked.get(pts) == null) {
                                LeftSideStops.get(w).add(pts);
                            }
                        }
                    }
                    StopHasBeenChecked.put(pts, 1);
                }
            }
            prev1 = w;
        }
        // based on the order of the ways, add the stops to the relation
        //my solution
        HashMap<Way, Boolean> checkValidityOfWrongStops = wayCanBeTraversedAgain(wayMembers);
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
                if (wayAlreadyThere.get(curr) == 0) {
                    List<PTStop> stps;
                    stps = RightSideStops.get(curr);
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
                    wayAlreadyThere.put(curr, 1);
                    if (checkValidityOfWrongStops.get(curr) == null && zooming) {
                        List<PTStop> stp;
                        stp = LeftSideStops.get(curr);
                        if (stp != null) {
                            Collection<Node> pt = new ArrayList<>();
                            if (stp.size() > 1)
                                stp = sortSameWayStops(stp, curr, prev, next);
                            stp.forEach(stop -> {
                                if (stop != null) {
                                    Node nod = null;
                                    if (stop.getPlatform() != null) {
                                        LatLon x = stop.getPlatform().getBBox().getCenter();
                                        nod = new Node(x);
                                    } else {
                                        nod = stop.getNode();
                                    }
                                    pt.add(nod);
                                    if (pt.size() > 0) {
                                        BoundingXYVisitor bboxCalculator = new BoundingXYVisitor();
                                        bboxCalculator.computeBoundingBox(pt);
                                        for (int idx = 0; idx < 4; idx++) {
                                            bboxCalculator.enlargeBoundingBox();
                                        }
                                        MainApplication.getMap().mapView.zoomTo(bboxCalculator);
                                        // AutoScaleAction.zoomTo(pt);
                                    }
                                    final JPanel panel = new JPanel(new GridBagLayout());
                                    panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                                    panel.add(
                                            new JLabel(tr(" " + stopOrderMap.get(stop.getUniqueId())
                                                    + " can not be served in this route relation want to remove it?")),
                                            GBC.eol().fill(GBC.HORIZONTAL));
                                    panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
                                    int n = JOptionPane.showConfirmDialog((Component) null, panel);
                                    if (n == JOptionPane.YES_OPTION) {
                                        tr("This stop has been removed");
                                    } else if (n == JOptionPane.NO_OPTION) {
                                        addStopToRelation(rel, stop);
                                    }
                                }
                            });
                        }
                    }
                } else {
                    List<PTStop> stps;
                    stps = LeftSideStops.get(curr);
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
        }

        wayMembers.forEach(rel::addMember);
    }

    private static void computeRefs(Relation r) {
        // in the end, draw labels of the given route r:
        int stopCount = 1;

        for (RelationMember rm : r.getMembers()) {
            if (PTStop.isPTPlatform(rm) || PTStop.isPTStopPosition(rm)) {

                StringBuilder sb = new StringBuilder();

                final Integer innerCount = stopCount;
                stopOrderMap.computeIfPresent(rm.getUniqueId(),
                        (x, y) -> sb.append(y).append(";").append(innerCount).toString());
                stopOrderMap.computeIfAbsent(rm.getUniqueId(), y -> sb
                        .append(r.hasKey("ref") ? r.get("ref") : (r.hasKey("name") ? r.get("name") : I18n.tr("NA")))
                        .append("-").append(innerCount).toString());

                stopCount++;
            }
        }
    }

    static Node findFirstCommonNode(Way w1, Way w2) {
        if (w1.firstNode().equals(w2.firstNode()) || w1.firstNode().equals(w2.lastNode())) {
            return w1.firstNode();
        } else if (w1.lastNode().equals(w2.firstNode()) || w1.lastNode().equals(w2.lastNode())) {
            return w1.lastNode();
        }
        return null;
    }

    private static void addStopToRelation(Relation rel, PTStop stop) {
        if (stop.getStopPositionRM() != null)
            rel.addMember(stop.getStopPositionRM());
        if (stop.getPlatformRM() != null)
            rel.addMember(stop.getPlatformRM());
    }

    private static HashMap<Way, Boolean> wayCanBeTraversedAgain(List<RelationMember> wayMembers) {
        Way prev = null;
        Way next = null;
        HashMap<Way, Boolean> setWay = new HashMap<>();
        HashMap<Way, Pair<Node, Node>> checkWay = new HashMap<>();
        for (int i = 0; i < wayMembers.size(); i++) {
            Node node1 = null;
            Node node2 = null;
            Way curr = wayMembers.get(i).getWay();
            if (i > 0) {
                prev = wayMembers.get(i - 1).getWay();
                node1 = findFirstCommonNode(curr, prev);
            }
            if (i < wayMembers.size() - 1) {
                RelationMember pk = wayMembers.get(i + 1);
                next = wayMembers.get(i + 1).getWay();
                node2 = findFirstCommonNode(curr, next);
            }
            Pair<Node, Node> par = checkWay.get(curr);
            if (par != null) {
                if (node1 != null) {
                    if (par.b != null) {
                        if (par.b.equals(node1)) {
                            setWay.put(curr, true);
                        }
                    }
                } else if (node2 != null) {
                    if (par.a != null) {
                        if (par.a.equals(node2)) {
                            setWay.put(curr, true);
                        }
                    }
                }
            } else {
                par = new Pair<>(node1, node2);
                checkWay.put(curr, par);
            }
        }
        return setWay;
    }

    // sorts the stops that are assigned to the same way. this is done based on
    // the distance from the previous way.
    public static List<PTStop> sortSameWayStops(List<PTStop> stps, Way way, Way prev, Way next) {
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
            reverse = prev.firstNode().equals(way.lastNode()) || prev.lastNode().equals(way.lastNode());
        } else if (next != null) {
            reverse = next.firstNode().equals(way.firstNode()) || next.lastNode().equals(way.firstNode());
        }

        if (reverse)
            Collections.reverse(nodes);

        List<PTStop> ret = getSortedStops(nodes, closeNodes);
        ret.addAll(noLocationStops);
        return ret;
    }

    static boolean checkAcuteAngles(LatLon a, LatLon b, LatLon c) {
        double x1 = a.getX() - b.getX();
        double y1 = a.getY() - b.getY();

        double x2 = c.getX() - b.getX();
        double y2 = c.getY() - b.getY();

        if (x1 * x2 + y1 * y2 >= 0) {
            return true;
        }
        return false;
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
            if (ref.hasTag("type", "public_transport") && StopUtils.isStopArea(ref) && ref.getName() != null) {
                return ref.getName();
            }
        }
        return p.getName();
    }

    private static void removeWrongSideStops(List<RelationMember> wayMembers) {
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

                final Optional<Node> firstNode = WayUtils.findFirstCommonNode(curr, prev);
                final Optional<Node> lastNode = WayUtils.findFirstCommonNode(curr, next);
                Logging.info(String.valueOf(i));
                if (firstNode.isPresent() && firstNode.get().equals(curr.getNode(0))) {
                    Logging.info("Front Way 1 == " + curr.getName());
                } else if (firstNode.isPresent() && firstNode.get().equals(curr.getNode(curr.getNodesCount() - 1))) {
                    Logging.info("Back Way 2 == " + curr.getName());
                } else if (lastNode.isPresent() && lastNode.get().equals(curr.getNode(0))) {
                    Logging.info("Back Way 3 == " + curr.getName());
                } else if (lastNode.isPresent() && lastNode.get().equals(curr.getNode(curr.getNodesCount() - 1))) {
                    Logging.info("Front Way 4 == " + curr.getName());
                }
            }
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editor != null && RouteUtils.isPTRoute(editor.getRelation()));
    }
}
