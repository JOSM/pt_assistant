// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.*;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Abstract class that is implemented to mend the relations by going through each way,
 * sorting them and proposing fixes for the gaps that are found
 *
 * @author sudhanshu2
 */
public abstract class AbstractMendRelationAction extends AbstractRelationEditorAction {
    protected static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();
    protected final String[] RESTRICTIONS = new String[] { "restriction", "restriction:bus", "restriction:trolleybus",
        "restriction:tram", "restriction:subway", "restriction:light_rail", "restriction:rail",
        "restriction:train", "restriction:trolleybus" };
    protected final String[] ACCEPTED_TAGS = { "no_right_turn", "no_left_turn",
        "no_u_turn", "no_straight_on", "no_entry", "no_exit" };

    protected GenericRelationEditor editor;
    protected Relation relation;
    protected MemberTableModel memberTableModel;
    protected List<RelationMember> members;
    protected Node currentNode;
    protected Way previousWay;
    protected List<Integer> extraWaysToBeDeleted;
    protected AbstractMapViewPaintable temporaryLayer;
    protected HashMap<Way, Integer> waysAlreadyPresent;
    protected Way nextWay;
    protected Way currentWay;
    protected String notice;
    protected Node previousCurrentNode;
    protected List<Node> backNodes;
    protected HashMap<Way, Character> wayColoring;
    protected HashMap<Character, List<Way>> wayListColoring;
    protected Node splitNode;

    protected boolean nextIndex = true;
    protected boolean abort = false;
    protected boolean noLinkToPreviousWay = true;
    protected boolean halt = false;
    protected boolean aroundGaps = false;
    protected boolean aroundStops = false;
    protected boolean showOption0 = false;
    protected boolean onFly = false;
    protected boolean shorterRoutes = false;
    protected boolean setEnable = true;

    protected int currentIndex;
    protected int downloadCounter;

    /**
     *
     * @param editorAccess
     */
    protected AbstractMendRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        editor = (GenericRelationEditor) editorAccess.getEditor();
        memberTableModel = editorAccess.getMemberTableModel();
        relation = editor.getRelation();
        editor.addWindowListener(new AbstractMendRelationAction.WindowEventHandler());
    }

    /**
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(relation != null && setEnable && ((relation.hasTag("route", "bus")
            && relation.hasTag("public_transport:version", "2")) || (RouteUtils.isPTRoute(relation) && !relation.hasTag("route", "bus"))));
    }

    /**
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (relation.hasIncompleteMembers()) {
            downloadIncompleteRelations();
            new Notification(tr("Downloading incomplete relation members. Kindly wait till download gets over."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3600).show();
        } else {
            initialise();
        }
    }

    /**
     *
     */
    private void downloadIncompleteRelations() {
        List<Relation> parents = Collections.singletonList(relation);
        Future<?> future = MainApplication.worker.submit(new DownloadRelationMemberTask(parents,
                Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
                    .buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
                MainApplication.getLayerManager().getEditLayer()));

        MainApplication.worker.submit(() -> {
            try {
                NotificationUtils.downloadWithNotifications(future, tr("Incomplete relations"));
                initialise();
            } catch (InterruptedException | ExecutionException e1) {
                Logging.error(e1);
            }
        });
    }

    /**
     *
     * @param typeRoute
     * @return
     */
    protected String getQuery(String typeRoute) {
        StringBuilder timeoutString = new StringBuilder("[timeout:100];\n(\n");
        String wayFormatterString = "   way(%.6f,%.6f,%.6f,%.6f)\n";
        List<Node> nodeList = aroundGaps ? getBrokenNodes() : new ArrayList<>();

        if (aroundStops) {
            nodeList.addAll(members.stream().filter(RelationMember::isNode).map(RelationMember::getNode).collect(Collectors.toList()));
        }

        for (Node n : nodeList) {
            double maxLatitude = n.getBBox().getTopLeftLat() + 0.001;
            double minLatitude = n.getBBox().getBottomRightLat() - 0.001;

            double maxLongitude = n.getBBox().getBottomRightLon() + 0.001;
            double minLongitude = n.getBBox().getTopLeftLon() - 0.001;

            timeoutString.append(String.format(wayFormatterString, minLatitude, minLongitude, maxLatitude, maxLongitude)).append(typeRoute);
        }

        return timeoutString.append(");\n(._;<;);\n(._;>;);\nout meta;").toString();
    }

    /**
     *
     * @return
     */
    private List<Node> getBrokenNodes() {
        List<Node> brokenNodes = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                int j = getNextWayIndex(i);
                if (j < members.size()) {
                    Way w = members.get(i).getWay();
                    Way v = members.get(j).getWay();
                    if (findNumberOfCommonNodes(w, v) != 1) {
                        brokenNodes.add(w.firstNode());
                        brokenNodes.add(w.lastNode());
                    }
                }
            }
        }
        return brokenNodes;
    }

    /**
     *
     * @param index
     * @return
     */
    protected int getNextWayIndex(int index) {
        int j = index + 1;
        for (; j < members.size(); j++) {
            if (members.get(j).isWay())
                break;
        }
        return j;
    }

    /**
     *
     * @param currentWay
     * @param previousWay
     * @return
     */
    protected int findNumberOfCommonNodes(Way currentWay, Way previousWay) {
        int count = 0;
        List<Node> nodes = currentWay.getNodes();
        List<Node> previousWayNodes = previousWay.getNodes();
        for (Node currentNode : nodes) {
            for (Node previousNode : previousWayNodes) {
                if (currentNode.equals(previousNode))
                    count = count + 1;
            }
        }
        return count;
    }

    /**
     *
     */
    public void stop() {
        defaultStates();
        nextIndex = false;
        abort = true;
        removeTemporaryLayers();
    }

    /**
     *
     */
    protected void defaultStates() {
        currentIndex = 0;
        currentNode = null;
        previousWay = null;
        noLinkToPreviousWay = true;
        nextIndex = true;
        extraWaysToBeDeleted = new ArrayList<>();
        halt = false;
    }

    /**
     *
     */
    protected void removeTemporaryLayers() {
        List<MapViewPaintable> tempLayers = MainApplication.getMap().mapView.getTemporaryLayers();
        for (MapViewPaintable tempLayer : tempLayers) {
            MainApplication.getMap().mapView.removeTemporaryLayer(tempLayer);
        }
    }

    /**
     *
     */
    protected void save() {
        editor.apply();
    }

    /**
     *
     * @param members
     */
    void sortBelow(List<RelationMember> members) {
        RelationSorter relationSorter = new RelationSorter();
        final List<RelationMember> subList = members.subList(0, members.size());
        final List<RelationMember> sorted = relationSorter.sortMembers(subList);
        subList.clear();
        subList.addAll(sorted);
        memberTableModel.fireTableDataChanged();
    }

    /**
     *
     * @return
     */
    protected List<Way> getListOfAllWays() {
        List<Way> ways = new ArrayList<>();
        for (RelationMember member : members) {
            if (member.isWay()) {
                waysAlreadyPresent.put(member.getWay(), 1);
                ways.add(member.getWay());
            }
        }
        return ways;
    }

    /**
     *
     */
    void deleteExtraWays() {
        int[] ints = extraWaysToBeDeleted.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ints);
        setEnable = true;
        setEnabled(true);
        halt = false;
    }

    /**
     *
     * @param way
     * @param nodes
     */
    protected void findNextWayBeforeDownload(Way way, Node... nodes) {
        nextIndex = false;
        AutoScaleAction.zoomTo(Collections.singletonList(way));
        if (nodes.length == 1) {
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();
            ds.setSelected(way);
            //downloadAreaAroundWay(way, nodes[0], null);
        } else if (nodes.length == 2) {
            //downloadAreaAroundWay(way, nodes[0], nodes[1]);
        } else {
            Logging.error("Unexpected arguments");
        }
    }

    /**
     *
     * @param relation
     * @param current
     * @param next
     * @param reversed is true if the ways need to be searched in reverse order
     * @return
     */
    protected List<Way> searchWayFromOtherRelations(Relation relation, Way current, Way next, boolean reversed) {
        List<Way> listOfWays = new ArrayList<>();
        boolean canAdd = false;
        Way previousWay = null;

        for (RelationMember relationMember : relation.getMembers()) {
            if (relationMember.isWay()) {
                Way currentWay = relationMember.getWay();
                boolean optionalComparison;
                boolean emptyListComparison;
                boolean returnListComparison;

                if (reversed) {
                    optionalComparison = isOneWayOrRoundabout(current);
                    emptyListComparison = currentWay.equals(next);
                    returnListComparison = currentWay.equals(current);
                } else {
                    optionalComparison = true;
                    emptyListComparison = currentWay.equals(current);
                    returnListComparison = currentWay.equals(next);
                }

                if (emptyListComparison) {
                    listOfWays.clear();
                    canAdd = true;
                    previousWay = currentWay;
                } else if (returnListComparison && listOfWays.size() > 0) {
                    if (reversed) {
                        Collections.reverse(listOfWays);
                    }
                    return listOfWays;
                } else if (canAdd) {
                    if (findNumberOfCommonNodes(currentWay, previousWay) != 0 && optionalComparison) {
                        listOfWays.add(currentWay);
                        previousWay = currentWay;
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param way
     * @param currentNode
     * @return
     */
    protected Node getOtherNode(Way way, Node currentNode) {
        if (way.firstNode().equals(currentNode))
            return way.lastNode();
        else
            return way.firstNode();
    }

    /**
     *
     * @param way
     * @return
     */
    protected boolean isSplitRoundabout(Way way) {
        return way.hasTag("junction", "roundabout") && !way.firstNode().equals(way.lastNode());
    }

    /**
     *
     * @param way
     * @return
     */
    public static boolean isNonSplitRoundabout(final Way way) {
        return way.hasTag("junction", "roundabout") && way.firstNode().equals(way.lastNode());
    }

    /**
     *
     * @param way
     * @param node
     * @return
     */
    protected List<Way> findNextWay(Way way, Node node) {
        List<Way> parentWays = node.getParentWays();
        parentWays = removeInvalidWaysFromParentWays(parentWays, node, way);

        // if the way is a roundabout but it has not find any suitable option for next
        // way, look at parents of all nodes
        if (way.hasTag("junction", "roundabout") && parentWays.size() == 0) {
            for (Node n : way.getNodes()) {
                parentWays.addAll(removeInvalidWaysFromParentWaysOfRoundabouts(n.getParentWays(), n, way));
            }
        }

        // put the most possible answer in front
        Way frontWay = parentWays.stream().filter(it -> checkIfWayConnectsToNextWay(it, 0, node)).findFirst()
            .orElse(null);

        if (frontWay == null && parentWays.size() > 0) {
            Way minWay = parentWays.get(0);
            double minLength = findDistance(minWay, nextWay, node);
            for (int i = 1; i < parentWays.size(); i++) {
                double length = findDistance(parentWays.get(i), nextWay, node);
                if (minLength > length) {
                    minLength = length;
                    minWay = parentWays.get(i);
                }
            }
            frontWay = minWay;
        }

        if (frontWay != null) {
            parentWays.remove(frontWay);
            parentWays.add(0, frontWay);
        }

        return parentWays;
    }

    /**
     *
     * @param parents
     * @param node
     * @param way
     * @return
     */
    protected List<Way> removeInvalidWaysFromParentWaysOfRoundabouts (List<Way> parents, Node node, Way way) {
        parents.remove(way);

        if (abort) {
            return null;
        }

        List<Way> waysToBeRemoved = new ArrayList<>();

        /* One way direction do not match */

        for (Way w : parents) {
            if (w.isOneway() != 0) {
                if (!checkOneWaySatisfiability(w, node)) {
                    waysToBeRemoved.add(w);
                }
            }
        }

        /* Check if any of them belong to roundabout, if yes then show ways accordingly */
        for (Way w : parents) {
            if (w.hasTag("junction", "roundabout")) {
                if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) == 1) {
                    if (w.lastNode().equals(node)) {
                        waysToBeRemoved.add(w);
                    }
                }
            }
        }

        /* Check mode of transport, also check if there is no loop */
        for (Way w : parents) {
            if (!WayUtils.isSuitableForBuses(w)) {
                waysToBeRemoved.add(w);
            }

            if (w.equals(previousWay)) {
                waysToBeRemoved.add(w);
            }
        }

        parents.removeAll(waysToBeRemoved);
        return parents;
    }

    /**
     *
     * @param way
     * @param count
     * @param node
     * @return
     */
    boolean checkIfWayConnectsToNextWay(Way way, int count, Node node) {

        if (count < 50) {
            if (way.equals(nextWay)) {
                return true;
            }

            /* Check if way's intermediate node is next way's first or last node */
            if (way.getNodes().contains(nextWay.firstNode()) || way.getNodes().contains(nextWay.lastNode())) {
                return true;
            }

            node = getOtherNode(way, node);
            List<Way> parents = node.getParentWays();

            if (parents.size() != 1) {
                return false;
            } else {
                way = parents.get(0);
            }

            count += 1;

            if (checkIfWayConnectsToNextWay(way, count, node)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param way
     * @param nextWay
     * @param node
     * @return
     */
    double findDistance(Way way, Way nextWay, Node node) {
        Node otherNode = getOtherNode(way, node);
        double Lat = (nextWay.firstNode().lat() + nextWay.lastNode().lat()) / 2;
        double Lon = (nextWay.firstNode().lon() + nextWay.lastNode().lon()) / 2;
        return (otherNode.lat() - Lat) * (otherNode.lat() - Lat) + (otherNode.lon() - Lon) * (otherNode.lon() - Lon);
    }

    /**
     *
     * @param previousWay
     * @param commonNode
     * @return
     */
    protected boolean isRestricted(Way previousWay, Node commonNode) {
        Set<Relation> parentSet = OsmPrimitive.getParentRelations(previousWay.getNodes());

        if (parentSet == null || parentSet.isEmpty()) {
            return false;
        }

        List<Relation> parentRelation = new ArrayList<>(parentSet);

        parentRelation.removeIf(relation -> {
            if (relation.hasKey("except")) {
                String[] val = relation.get("except").split(";");
                for (String s : val) {
                    if (relation.hasTag("route", s))
                        return true;
                }
            }

            if (!relation.hasTag("type", RESTRICTIONS))
                return true;
            else if (relation.hasTag("type", "restriction") && relation.hasKey("restriction"))
                return false;
            else {
                boolean remove = true;
                String routeValue = relation.get("route");
                for (String s : RESTRICTIONS) {
                    String sub = s.substring(12);
                    if (routeValue.equals(sub) && relation.hasTag("type", s))
                        remove = false;
                    else if (routeValue.equals(sub) && relation.hasKey("restriction:" + sub))
                        remove = false;
                }
                return remove;
            }
        });

        /* check for "only" kind of restrictions */
        for (Relation relation : parentRelation) {
            Collection<RelationMember> prevMemberList = relation.getMembersFor(Collections.singletonList(previousWay));
            Collection<RelationMember> commonNodeList = relation.getMembersFor(Collections.singletonList(commonNode));
            Collection<RelationMember> curMemberList = relation.getMembersFor(Collections.singletonList(currentWay));

            /* if commonNode is not the node involved in the restriction relation then just continue */
            if (prevMemberList.isEmpty() || commonNodeList.isEmpty()) {
                continue;
            }

            String curRole = curMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);
            String prevRole = prevMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);

            if (prevRole.equals("from")) {
                for (String s : RESTRICTIONS) {
                    /* if we have any "only" type restrictions then the current way should be in the relation else it is restricted */
                    if (relation.hasTag(s, ACCEPTED_TAGS)) {
                        if (relation.getMembersFor(Collections.singletonList(currentWay)).isEmpty()) {
                            for (String str : ACCEPTED_TAGS) {
                                if (relation.hasTag(s, str))
                                    notice = str + " restriction violated";
                            }
                            return true;
                        }
                    }
                }
            }

            if ("to".equals(curRole) && "from".equals(prevRole)) {
                for (String s : RESTRICTIONS) {
                    if (relation.hasTag(s, ACCEPTED_TAGS)) {
                        for (String str : ACCEPTED_TAGS) {
                            if (relation.hasTag(s, str))
                                notice = str + " restriction violated";
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     *
     * @param j
     */
    protected void removeWay(int j) {
        List<Integer> initial = new ArrayList<>();
        List<Way> list = new ArrayList<>();
        initial.add(j);
        list.add(members.get(j).getWay());

        /* if the way at members.get(j) is one way then check if the next ways are on
        a way, if so then remove them as well */
        if (WayUtils.isOneWay(members.get(j).getWay())) {
            while (true) {
                int k = getNextWayIndex(j);
                if (k == -1 || k >= members.size())
                    break;

                if (!WayUtils.isOneWay(members.get(k).getWay()))
                    break;
                if (WayUtils.findNumberOfCommonFirstLastNodes(members.get(k).getWay(), members.get(j).getWay()) == 0)
                    break;

                j = k;
                initial.add(j);
                list.add(members.get(j).getWay());
            }
        }

        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        ds.setSelected(list);
        downloadAreaBeforeRemovalOption(list, initial);
    }

    /**
     *
     * @param wayList
     * @param Int
     */
    void downloadAreaBeforeRemovalOption(List<Way> wayList, List<Integer> Int) {
        if (abort) {
            return;
        }

        if (!onFly) {
            displayWaysToRemove(Int);
        }

        downloadCounter = 0;

        DownloadOsmTask task = new DownloadOsmTask();

        BoundsUtils.createBoundsWithPadding(wayList, .4).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area before removal"));
                    displayWaysToRemove(Int);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    }

    /**
     *
     * @param wayIndices
     */
    void displayWaysToRemove(List<Integer> wayIndices) {

        // find the letters of the fix variants:
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        if (numeric)
            alphabet = '1';
        wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();

        if (numeric) {
            allowedCharacters.add('1');
            allowedCharacters.add('2');
            allowedCharacters.add('3');
        } else {
            allowedCharacters.add('A');
            allowedCharacters.add('B');
            allowedCharacters.add('R');
        }

        for (int i = 0; i < 5 && i < wayIndices.size(); i++) {
            wayColoring.put(members.get(wayIndices.get(i)).getWay(), alphabet);
        }

        if (notice.equals("vehicle travels against oneway restriction")) {
            if (numeric) {
                allowedCharacters.add('4');
            } else {
                allowedCharacters.add('C');
            }
        }

        // remove any existing temporary layer
        removeTemporaryLayers();

        if (abort)
            return;

        // zoom to problem:
        final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

        for (Integer i : wayIndices) {
            waysToZoom.add(members.get(i).getWay());
        }

        AutoScaleAction.zoomTo(waysToZoom);

        // display the fix variants:
        temporaryLayer = new PTMendRelationSupportClasses.PTMendRelationRemoveLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    return;
                }
                char typedKey = e.getKeyChar();
                char typedKeyUpperCase = Character.toString(typedKey).toUpperCase().toCharArray()[0];

                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    nextIndex = true;
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    Logging.debug(String.valueOf(typedKeyUpperCase));
                    if (typedKeyUpperCase == 'R' || typedKeyUpperCase == '3') {
                        wayIndices.add(0, currentIndex);
                    }
                    removeWayAfterSelection(wayIndices, typedKeyUpperCase);
                }

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    Logging.debug("ESC");
                    nextIndex = false;
                     = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
            }
        });
    }

    /**
     *
     */
    void downloadEntireArea() {
        if (abort) {
            return;
        }

        DownloadOsmTask task = new DownloadOsmTask();
        List<Way> wayList = getListOfAllWays();

        if (wayList.isEmpty()) {
            callNextWay(currentIndex);
            return;
        }

        String typeRoute = "   [\"highway\"][\"highway\"!=\"footway\"][\"highway\"!=\"path\"][\"highway\"!=\"cycleway\"];\n";
        String query = getQuery(typeRoute);
        Logging.debug(query);

        if (aroundStops || aroundGaps) {
            BoundsUtils.createBoundsWithPadding(wayList, .1).ifPresent(area -> {
                final Future<?> future = task.download(
                    new OverpassDownloadReader(area, OverpassDownloadReader.OVERPASS_SERVER.get(), query),
                    DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Entire area"));
                        callNextWay(currentIndex);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            callNextWay(currentIndex);
        }
    }

    /**
     *
     * @param index
     * @return
     */
    protected int getPreviousWayIndex(int index) {
        for (int j = index - 1; j >= 0; j--) {
            if (members.get(j).isWay()) {
                return j;
            }
        }
        return -1;
    }

    /**
     *
     */
    protected class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            editor.cancel();
            Logging.debug("close");
            stop();
        }
    }

    /**
     *
     */
    private class TempStrategy implements SplitWayCommand.Strategy {
        @Override
        public Way determineWayToKeep(Iterable<Way> wayChunks) {
            for (Way way : wayChunks) {
                if (!way.containsNode(previousCurrentNode)) {
                    return way;
                }
            }
            return null;
        }
    }

    /**
     *
     */
    private class TempStrategyRoundabout implements SplitWayCommand.Strategy {
        @Override
        public Way determineWayToKeep(Iterable<Way> wayChunks) {
            for (Way way : wayChunks) {
                if (way.containsNode(splitNode)) {
                    return way;
                }
            }
            return null;
        }
    }

    /**
     *
     */
    protected class MendRelationAddMultipleLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            PTMendRelationSupportClasses.PTMendRelationPaintVisitor paintVisitor = new PTMendRelationSupportClasses.PTMendRelationPaintVisitor(g, mv);
            paintVisitor.drawMultipleVariants(wayListColoring);
        }
    }
}
