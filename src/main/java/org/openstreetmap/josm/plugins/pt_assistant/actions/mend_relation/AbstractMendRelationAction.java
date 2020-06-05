// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.plugins.pt_assistant.actions.MendRelationAction;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Abstract class that should be extended for mend relation actions
 *
 * @author sudhanshu2
 */
public abstract class AbstractMendRelationAction extends AbstractRelationEditorAction
    implements DisplayWaysInterface, DownloadAreaInterface, MendRelationPaintVisitorInterface {

    private String downloadAreaString;
    private String addOneWay;

    protected List<RelationMember> members;
    protected Relation relation;
    protected GenericRelationEditor editor;
    protected HashMap<Way, Integer> waysAlreadyPresent;
    protected Way previousWay;
    protected Way currentWay;
    protected Way nextWay;
    protected MemberTableModel memberTableModel;
    protected String notice;
    protected Node currentNode;
    protected List<Integer> extraWaysToBeDeleted;

    protected DownloadArea downloadArea;
    protected DisplayWays displayWays;
    protected MendRelationPaintVisitor paintVisitor;

    protected int downloadCounter;
    protected int currentIndex;

    protected boolean setEnable;
    protected boolean noLinkToPreviousWay;
    protected boolean nextIndex;
    protected boolean shorterRoutes;
    protected boolean showOption0;
    protected boolean onFly;
    protected boolean aroundGaps;
    protected boolean aroundStops;
    protected boolean halt;

    /* Abstract methods */

    protected abstract void initialize();

    /**
     * Constructor for AbstractMendRelationAction class
     * @param editorAccess is the IRelationEditorActionAccess for mend relation action
     * @param addOneWay sets the string which is displayed for one way option in mend relation
     */
    protected AbstractMendRelationAction(IRelationEditorActionAccess editorAccess, String addOneWay, String downloadAreaString) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);

        setEnable = true;
        noLinkToPreviousWay = true;
        nextIndex = true;
        shorterRoutes = false;
        showOption0 = false;
        onFly = false;
        aroundGaps = false;
        aroundStops = false;
        halt = false;

        downloadArea = new DownloadArea(this);
        displayWays = new DisplayWays(this);
        paintVisitor = new MendRelationPaintVisitor(g, mv, this);

        this.addOneWay = addOneWay;
    }

    /**
     * Initializes state variables to null or 0 or empty state
     */
    protected void initializeStates() {
        downloadCounter = 0;
        waysAlreadyPresent = new HashMap<>();
        setEnable = false;
        previousWay = null;
        currentWay = null;
        nextWay = null;
        noLinkToPreviousWay = true;
        nextIndex = true;
        shorterRoutes = false;
        showOption0 = false;
        currentIndex = 0;
    }

    /**
     * Saves the relation editor
     */
    protected void save() {
        editor.apply();
    }

    /**
     * Creates the dialog to select how area should be downloaded
     */
    protected void makePanelToDownloadArea() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JCheckBox aroundStopsBtn = new JCheckBox("Around Stops");
        JCheckBox aroundGapsBtn = new JCheckBox("Around Gaps");
        JCheckBox onFlyBtn = new JCheckBox("On the fly");

        onFlyBtn.setSelected(true);

        panel.add(new JLabel(tr("How would you want the download to take place?")), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(aroundStopsBtn, GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(aroundGapsBtn, GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(onFlyBtn, GBC.eol().fill(GBC.HORIZONTAL));

        int selectedBtn = JOptionPane.showConfirmDialog(null, panel);

        if (selectedBtn == JOptionPane.OK_OPTION) {
            if (aroundStopsBtn.isSelected()) {
                aroundStops = true;
            } else if (aroundGapsBtn.isSelected()) {
                aroundGaps = true;
            } else if (onFlyBtn.isSelected()) {
                onFly = true;
            }
            downloadArea.downloadEntireArea();
        }
    }

    /**
     * Generate string query for downloading area
     * @return string query to download area
     */
    public String getQuery() {
        StringBuilder str = new StringBuilder("[timeout:100];\n(\n");
        String wayFormatterString = "   way(%.6f,%.6f,%.6f,%.6f)\n";

        List<Node> nodeList = aroundGaps ? getBrokenNodes() : new ArrayList<>();

        if (aroundStops) {
            nodeList.addAll(members.stream().filter(RelationMember::isNode).map(RelationMember::getNode)
                .collect(Collectors.toList()));
        }

        for (final Node n : nodeList) {
            double maxLat = n.getBBox().getTopLeftLat() + 0.001;
            double minLat = n.getBBox().getBottomRightLat() - 0.001;
            double maxLon = n.getBBox().getBottomRightLon() + 0.001;
            double minLon = n.getBBox().getTopLeftLon() - 0.001;
            str.append(String.format(wayFormatterString, minLat, minLon, maxLat, maxLon)).append(downloadAreaString);

        }

        return str.append(");\n(._;<;);\n(._;>;);\nout meta;").toString();
    }

    /**
     * Gets a list of broken nodes
     * @return list of broken nodes in the members
     */
    private List<Node> getBrokenNodes() {
        List<Node> brokenNodes = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                int j = getNextWayIndex(i);
                if (j < members.size()) {
                    Way w = members.get(i).getWay();
                    Way v = members.get(j).getWay();
                    if (findNumberOfCommonNode(w, v) != 1) {
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
     * @param way
     * @return
     */
    protected boolean isNonSplitRoundAbout(Way way) {
        return way.hasTag("junction", "roundabout") && way.firstNode().equals(way.lastNode());
    }

    /**
     *
     * @param way
     * @return
     */
    protected boolean isSplitRoundAbout(Way way) {
        return way.hasTag("junction", "roundabout") && !way.firstNode().equals(way.lastNode());
    }

    /**
     *
     */
    @Override
    protected void updateEnabledState() {
        final Relation curRel = relation;

        setEnabled(curRel != null && setEnable
            && ((curRel.hasTag("route", "bus") && curRel.hasTag("public_transport:version", "2"))
            || (RouteUtils.isPTRoute(curRel) && !curRel.hasTag("route", "bus"))));
    }

    /**
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (relation.hasIncompleteMembers()) {
            downloadArea.downloadIncompleteRelations();
            new Notification(tr("Downloading incomplete relation members. Kindly wait till download gets over."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3600).show();
        } else {
            initialise();
        }
    }

    /**
     * Class to determine what happens when mend relation action is closed
     */
    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            editor.cancel();
            Logging.debug("close");
            stop();
        }
    }
























    ///// Public Transport



    Node checkValidityOfWays(Way way, int nextWayIndex) {
        boolean nextWayDelete = false;
        Node node = null;
        nextIndex = false;
        notice = null;

        final NodePair commonEndNodes = WayUtils.findCommonFirstLastNodes(nextWay, way);
        if (commonEndNodes.getA() != null && commonEndNodes.getB() != null) {
            nextWayDelete = true;
            notice = "Multiple common nodes found between current and next way";
        } else if (commonEndNodes.getA() != null) {
            node = commonEndNodes.getA();
        } else if (commonEndNodes.getB() != null) {
            node = commonEndNodes.getB();
        } else {
            // the nodes can be one of the intermediate nodes
            for (Node n : nextWay.getNodes()) {
                if (way.getNodes().contains(n)) {
                    node = n;
                    currentNode = n;
                }
            }
        }

        // check if there is a restricted relation that doesn't allow both the ways together
        if (node != null && isRestricted(nextWay, way, node)) {
            nextWayDelete = true;
        }

        if (isNonSplitRoundAbout(way)) {
            nextWayDelete = false;
            for (Node n : way.getNodes()) {
                if (nextWay.firstNode().equals(n) || nextWay.lastNode().equals(n)) {
                    node = n;
                    currentNode = n;
                }
            }
        }

        if (isNonSplitRoundAbout(nextWay)) {
            nextWayDelete = false;
        }

        if (node != null && !checkOneWaySatisfiability(nextWay, node)) {
            nextWayDelete = true;
            notice = "vehicle travels against oneway restriction";
        }

        if (nextWayDelete) {
            currentWay = way;
            nextIndex = true;
            removeWay(nextWayIndex);
            return null;
        }

        nextIndex = false;
        return node;
    }

    public void deleteExtraWays() {
        int[] ints = extraWaysToBeDeleted.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ints);
        setEnable = true;
        setEnabled(true);
        halt = false;
    }

    void removeWay(int j) {
        java.util.List<Integer> Int = new ArrayList<>();
        java.util.List<Way> lst = new ArrayList<>();
        Int.add(j);
        lst.add(members.get(j).getWay());
        // if the way at members.get(j) is one way then check if the next ways are on
        // way, if so then remove them as well
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
                Int.add(j);
                lst.add(members.get(j).getWay());
            }
        }

        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        ds.setSelected(lst);
        downloadAreaBeforeRemovalOption(lst, Int);
    }

    int getNextWayIndex(int idx) {
        int j = members.size();

        for (j = idx + 1; j < members.size(); j++) {
            if (members.get(j).isWay())
                break;
        }
        return j;
    }

    int getPreviousWayIndex(int idx) {
        int j;

        for (j = idx - 1; j >= 0; j--) {
            if (members.get(j).isWay())
                return j;
        }
        return -1;
    }

    void sortBelow(java.util.List<RelationMember> members, int index) {
        RelationSorter relationSorter = new RelationSorter();
        final java.util.List<RelationMember> subList = members.subList(Math.max(0, index), members.size());
        final java.util.List<RelationMember> sorted = relationSorter.sortMembers(subList);
        subList.clear();
        subList.addAll(sorted);
        memberTableModel.fireTableDataChanged();
    }

    int findNumberOfCommonNode(Way way, Way previousWay) {
        int count = 0;
        for (Node n1 : way.getNodes()) {
            for (Node n2 : previousWay.getNodes()) {
                if (n1.equals(n2))
                    count++;
            }
        }
        return count;
    }

    Way findNextWayBeforeDownload(Way way, Node node) {
        nextIndex = false;
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        ds.setSelected(way);
        AutoScaleAction.zoomTo(Collections.singletonList(way));
        downloadAreaAroundWay(way, node, null);
        return null;
    }

    Way findNextWayBeforeDownload(Way way, Node node1, Node node2) {
        nextIndex = false;
        AutoScaleAction.zoomTo(Collections.singletonList(way));
        downloadAreaAroundWay(way, node1, node2);
        return null;
    }

    // TODO: Merge with searchWayFromOtherRelationsReversed(Relation r, Way current, Way next)! These seem to do largely the same thing.
    java.util.List<Way> searchWayFromOtherRelations(Relation r, Way current, Way next) {
        java.util.List<RelationMember> member = r.getMembers();
        java.util.List<Way> lst = new ArrayList<>();
        boolean canAdd = false;
        Way prev = null;
        for (int i = 0; i < member.size(); i++) {
            if (member.get(i).isWay()) {
                Way w = member.get(i).getWay();
                if (w.equals(current)) {
                    lst.clear();
                    canAdd = true;
                    prev = w;
                } else if (w.equals(next) && lst.size() > 0) {
                    return lst;
                } else if (canAdd) {
                    if (findNumberOfCommonNode(w, prev) != 0) {
                        lst.add(w);
                        prev = w;
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    // TODO: Merge with searchWayFromOtherRelations(Relation r, Way current, Way next)! These seem to do largely the same thing.
    java.util.List<Way> searchWayFromOtherRelationsReversed(Relation r, Way current, Way next) {
        java.util.List<RelationMember> member = r.getMembers();
        java.util.List<Way> lst = new ArrayList<>();
        boolean canAdd = false;
        Way prev = null;
        for (int i = 0; i < member.size(); i++) {
            if (member.get(i).isWay()) {
                Way w = member.get(i).getWay();
                if (w.equals(next)) {
                    lst.clear();
                    canAdd = true;
                    prev = w;
                } else if (w.equals(current) && lst.size() > 0) {
                    Collections.reverse(lst);
                    return lst;
                } else if (canAdd) {
                    // not valid in reverse if it is oneway or part of roundabout
                    if (findNumberOfCommonNode(w, prev) != 0 && w.isOneway() == 0
                        && RouteUtils.isOnewayForPublicTransport(w) == 0 && !isSplitRoundAbout(w)) {
                        lst.add(w);
                        prev = w;
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    void goToNextWays(Way way, Way prevWay, java.util.List<Way> wayList) {
        java.util.List<java.util.List<Way>> lst = new ArrayList<>();
        previousWay = prevWay;
        Node node1 = null;
        for (Node n : way.getNodes()) {
            if (prevWay.getNodes().contains(n)) {
                node1 = n;
                break;
            }
        }

        if (node1 == null) {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }

        // check if the way equals the next way, if so then don't add any new ways to the list
        if (way == nextWay) {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }

        Node node = getOtherNode(way, node1);
        wayList.add(way);
        java.util.List<Way> parents = node.getParentWays();
        parents.remove(way);

        // if the ways directly touch the next way
        if (way.isFirstLastNode(nextWay.firstNode()) || way.isFirstLastNode(nextWay.lastNode())) {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }

        // if next way turns out to be a roundabout
        if (nextWay.containsNode(node) && nextWay.hasTag("junction", "roundabout")) {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }

        // remove all the invalid ways from the parent ways
        parents = removeInvalidWaysFromParentWays(parents, node, way);

        if (parents.size() == 1) {
            // if (already the way exists in the ways to be added
            if (wayList.contains(parents.get(0))) {
                lst.add(wayList);
                displayFixVariantsWithOverlappingWays(lst);
                return;
            }
            downloadAreaAroundWay(parents.get(0), way, wayList);
            return;
        } else if (parents.size() > 1) {
            // keep the most probable option s option A
            Way minWay = parents.get(0);
            double minLength = findDistance(minWay, nextWay, node);
            for (int k = 1; k < parents.size(); k++) {
                double length = findDistance(parents.get(k), nextWay, node);
                if (minLength > length) {
                    minLength = length;
                    minWay = parents.get(k);
                }
            }
            parents.remove(minWay);
            parents.add(0, minWay);

            // add all the list of ways to list of list of ways
            for (int i = 0; i < parents.size(); i++) {
                java.util.List<Way> wl = new ArrayList<>(wayList);
                wl.add(parents.get(i));
                lst.add(wl);
            }

            displayFixVariantsWithOverlappingWays(lst);
            return;
        } else {
            lst.add(wayList);
            displayFixVariantsWithOverlappingWays(lst);
            return;
        }
    }

    java.util.List<Way> findNextWay(Way way, Node node) {
        java.util.List<Way> parentWays = node.getParentWays();
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

    boolean checkIfWayConnectsToNextWay(Way way, int count, Node node) {

        if (count < 50) {
            if (way.equals(nextWay))
                return true;

            // check if way;s intermediate node is next way's first or last node
            if (way.getNodes().contains(nextWay.firstNode()) || way.getNodes().contains(nextWay.lastNode()))
                return true;

            node = getOtherNode(way, node);
            java.util.List<Way> parents = node.getParentWays();
            if (parents.size() != 1)
                return false;
            else
                way = parents.get(0);

            count += 1;
            if (checkIfWayConnectsToNextWay(way, count, node))
                return true;
        }
        return false;
    }

    java.util.List<Way> removeInvalidWaysFromParentWaysOfRoundabouts(java.util.List<Way> parents, Node node, Way way) {
        java.util.List<Way> parentWays = parents;
        parentWays.remove(way);
        if (abort)
            return null;
        java.util.List<Way> waysToBeRemoved = new ArrayList<>();

        // one way direction doesn't match
        for (Way w : parentWays) {
            if (w.isOneway() != 0) {
                if (!checkOneWaySatisfiability(w, node)) {
                    waysToBeRemoved.add(w);
                }
            }
        }

        // check if any of them belong to roundabout, if yes then show ways accordingly
        for (Way w : parentWays) {
            if (w.hasTag("junction", "roundabout")) {
                if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) == 1) {
                    if (w.lastNode().equals(node)) {
                        waysToBeRemoved.add(w);
                    }
                }
            }
        }

        // check mode of transport, also check if there is no loop
        for (Way w : parentWays) {
            if (!WayUtils.isSuitableForBuses(w)) {
                waysToBeRemoved.add(w);
            }

            if (w.equals(previousWay)) {
                waysToBeRemoved.add(w);
            }
        }

        parentWays.removeAll(waysToBeRemoved);
        return parentWays;
    }

    double findDistance(Way way, Way nextWay, Node node) {
        Node otherNode = getOtherNode(way, node);
        double Lat = (nextWay.firstNode().lat() + nextWay.lastNode().lat()) / 2;
        double Lon = (nextWay.firstNode().lon() + nextWay.lastNode().lon()) / 2;
        return (otherNode.lat() - Lat) * (otherNode.lat() - Lat) + (otherNode.lon() - Lon) * (otherNode.lon() - Lon);
    }

    boolean isRestricted(Way currentWay, Way previousWay, Node commonNode) {
        Set<Relation> parentSet = OsmPrimitive.getParentRelations(previousWay.getNodes());
        if (parentSet == null || parentSet.isEmpty())
            return false;
        java.util.List<Relation> parentRelation = new ArrayList<>(parentSet);

        String[] restrictions = new String[] { "restriction", "restriction:bus", "restriction:trolleybus",
            "restriction:tram", "restriction:subway", "restriction:light_rail", "restriction:rail",
            "restriction:train", "restriction:trolleybus" };

        parentRelation.removeIf(rel -> {
            if (rel.hasKey("except")) {
                String[] val = rel.get("except").split(";");
                for (String s : val) {
                    if (relation.hasTag("route", s))
                        return true;
                }
            }

            if (!rel.hasTag("type", restrictions))
                return true;
            else if (rel.hasTag("type", "restriction") && rel.hasKey("restriction"))
                return false;
            else {
                boolean remove = true;
                String routeValue = relation.get("route");
                for (String s : restrictions) {
                    String sub = s.substring(12);
                    if (routeValue.equals(sub) && rel.hasTag("type", s))
                        remove = false;
                    else if (routeValue.equals(sub) && rel.hasKey("restriction:" + sub))
                        remove = false;
                }
                return remove;
            }
        });

        // check for "only" kind of restrictions
        for (Relation r : parentRelation) {
            Collection<RelationMember> prevMemberList = r.getMembersFor(Collections.singletonList(previousWay));
            Collection<RelationMember> commonNodeList = r.getMembersFor(Collections.singletonList(commonNode));
            // commonNode is not the node involved in the restriction relation then just continue
            if (prevMemberList.isEmpty() || commonNodeList.isEmpty())
                continue;

            final String prevRole = prevMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);

            if (prevRole.equals("from")) {
                String[] acceptedTags = { "only_right_turn", "only_left_turn", "only_u_turn", "only_straight_on",
                    "only_entry", "only_exit" };
                for (String s : restrictions) {
                    // if we have any "only" type restrictions then the current way should be in the
                    // relation else it is restricted
                    if (r.hasTag(s, acceptedTags)) {
                        if (r.getMembersFor(Collections.singletonList(currentWay)).isEmpty()) {
                            for (String str : acceptedTags) {
                                if (r.hasTag(s, str))
                                    notice = str + " restriction violated";
                            }
                            return true;
                        }
                    }
                }
            }
        }

        for (Relation r : parentRelation) {
            Collection<RelationMember> curMemberList = r.getMembersFor(Collections.singletonList(currentWay));
            Collection<RelationMember> prevMemberList = r.getMembersFor(Collections.singletonList(previousWay));

            if (curMemberList.isEmpty() || prevMemberList.isEmpty())
                continue;

            final String curRole = curMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);
            final String prevRole = prevMemberList.stream().findFirst().map(RelationMember::getRole).orElse(null);

            if ("to".equals(curRole) && "from".equals(prevRole)) {
                final String[] acceptedTags = { "no_right_turn", "no_left_turn", "no_u_turn", "no_straight_on",
                    "no_entry", "no_exit" };
                for (String s : restrictions) {
                    if (r.hasTag(s, acceptedTags)) {
                        for (String str : acceptedTags) {
                            if (r.hasTag(s, str))
                                notice = str + " restriction violated";
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean isWaySuitableForOtherModes(Way way) {

        if (relation.hasTag("route", "tram"))
            return way.hasTag("railway", "tram");

        if (relation.hasTag("route", "subway"))
            return way.hasTag("railway", "subway");

        if (relation.hasTag("route", "light_rail"))
            return way.hasTag("railway", "light_rail");

        if (relation.hasTag("route", "rail"))
            return way.hasTag("railway", "rail");

        if (relation.hasTag("route", "train"))
            return way.hasTag("railway", "train");

        if (relation.hasTag("route", "trolleybus"))
            return way.hasTag("trolley_wire", "yes");

        return false;
    }

    void addAlreadyExistingWay(Way w) {
        // delete the way if only if we haven't crossed it yet
        if (waysAlreadyPresent.get(w) == 1) {
            deleteWay(w);
        }
    }

    void deleteWay(Way w) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay() && members.get(i).getWay().equals(w)) {
                int[] x = { i };
                memberTableModel.remove(x);
                members.remove(i);
                break;
            }
        }
    }

    Node getOtherNode(Way way, Node currentNode) {
        if (way.firstNode().equals(currentNode))
            return way.lastNode();
        else
            return way.firstNode();
    }

    private void findWayafterchunkRoundabout(Way way) {
        java.util.List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        splitNode = way.lastNode();
        SplitWayCommand.Strategy strategy = new MendRelationAction.TempStrategyRoundabout();
        java.util.List<java.util.List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(way, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
        }
    }

    void removeTemporarylayers() {
        java.util.List<MapViewPaintable> tempLayers = MainApplication.getMap().mapView.getTemporaryLayers();
        for (int i = 0; i < tempLayers.size(); i++) {
            MainApplication.getMap().mapView.removeTemporaryLayer(tempLayers.get(i));
        }
    }

    void defaultStates() {
        currentIndex = 0;
        currentNode = null;
        previousWay = null;
        noLinkToPreviousWay = true;
        nextIndex = true;
        extraWaysToBeDeleted = new ArrayList<>();
        halt = false;
    }

    public void stop() {
        defaultStates();
        nextIndex = false;
        abort = true;
        removeTemporarylayers();
    }

    public class TempStrategy implements SplitWayCommand.Strategy {
        @Override
        public Way determineWayToKeep(Iterable<Way> wayChunks) {
            for (Way way : wayChunks) {
                if (!way.containsNode(prevCurrenNode)) {
                    return way;
                }
            }
            return null;
        }
    }

    public class TempStrategyRoundabout implements SplitWayCommand.Strategy {
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


    ///// Personal Transport


    List<Way> searchWayFromOtherRelations(Relation r, Way current, Way next, boolean reverse) {
        List<RelationMember> member = r.getMembers();
        List<Way> lst = new ArrayList<>();
        boolean canAdd = false;
        Way prev = null;
        for (int i = 0; i < member.size(); i++) {
            if (member.get(i).isWay()) {
                Way w = member.get(i).getWay();
                if (!reverse) {
                    if (w.equals(current)) {
                        lst.clear();
                        canAdd = true;
                        prev = w;
                    } else if (w.equals(next) && lst.size() > 0) {
                        return lst;
                    } else if (canAdd) {
                        if (findNumberOfCommonNode(w, prev) != 0) {
                            lst.add(w);
                            prev = w;
                        } else {
                            break;
                        }
                    }
                } else {
                    if (w.equals(next)) {
                        lst.clear();
                        canAdd = true;
                        prev = w;
                    } else if (w.equals(current) && lst.size() > 0) {
                        Collections.reverse(lst);
                        return lst;
                    } else if (canAdd) {
                        // not valid in reverse if it is oneway or part of roundabout
                        if (findNumberOfCommonNode(w, prev) != 0 && RouteUtils.isOnewayForBicycles(w) == 0
                            && !isSplitRoundAbout(w)) {
                            lst.add(w);
                            prev = w;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Node checkVaildityOfWays(Way way, int nexidx) {
        // TODO Auto-generated method stub
        boolean nexWayDelete = false;
        Node node = null;
        super.nextIndex = false;
        super.notice = null;
        final NodePair commonEndNodes = WayUtils.findCommonFirstLastNodes(super.nextWay, way);
        if (commonEndNodes.getA() != null && commonEndNodes.getB() != null) {
            nexWayDelete = true;
            super.notice = "Multiple common nodes found between current and next way";
        } else if (commonEndNodes.getA() != null) {
            node = commonEndNodes.getA();
        } else if (commonEndNodes.getB() != null) {
            node = commonEndNodes.getB();
        } else {
            // the nodes can be one of the intermediate nodes
            for (Node n : super.nextWay.getNodes()) {
                if (way.getNodes().contains(n)) {
                    node = n;
                    super.currentNode = n;
                }
            }
        }
        if (node != null && isRestricted(super.nextWay, way, node)) {
            nexWayDelete = true;
        }
        if (isNonSplitRoundAbout(way)) {
            nexWayDelete = false;
            for (Node n : way.getNodes()) {
                if (super.nextWay.firstNode().equals(n) || super.nextWay.lastNode().equals(n)) {
                    node = n;
                    super.currentNode = n;
                }
            }
        }

        if (isNonSplitRoundAbout(super.nextWay)) {
            nexWayDelete = false;
        }

        if (node != null && !checkOneWaySatisfiability(super.nextWay, node)) {
            nexWayDelete = true;
            super.notice = "vehicle travels against oneway restriction";
        }
        if (nexWayDelete) {
            super.currentWay = way;
            super.nextIndex = true;
            removeWay(nexidx);
            return null;
        }

        super.nextIndex = false;
        return node;

    }

    String assignRoles(Way w) {
        int flag = 0;
        String s = "";
        if (lastForWay != null && lastBackWay != null) {
            NodePair pair = WayUtils.findCommonFirstLastNodes(lastForWay, lastBackWay);
            if (pair.getA() == null && pair.getB() != null) {
                if (pair.getB().equals(super.currentNode)) {
                    flag = 1;
                }
            } else if (pair.getB() == null && pair.getA() != null) {
                if (pair.getA().equals(super.currentNode)) {
                    flag = 1;
                }
            } else if (pair.getB() != null && pair.getA() != null) {
                if (pair.getA().equals(super.currentNode) || pair.getB().equals(super.currentNode)) {
                    flag = 1;
                }
            }
        }
        // else if(lastForWay==null)

        if (flag == 1) {
            s = "";
        } else if (prelink.isOnewayLoopBackwardPart && super.currentNode.equals(w.firstNode())) {
            s = "backward";
        } else if (prelink.isOnewayLoopBackwardPart && super.currentNode.equals(w.lastNode())) {
            s = "forward";
        } else if (prelink.isOnewayLoopForwardPart && super.currentNode.equals(w.firstNode())) {
            s = "forward";
        } else if (prelink.isOnewayLoopForwardPart && super.currentNode.equals(w.lastNode())) {
            s = "backward";
        } else {
            s = "";
        }
        return s;
    }

    void assignRolesafterloop(Node jointNode) {
        int idx = super.currentIndex;
        int[] idxlst = new int[1];
        Node node1;
        Way w = super.members.get(idx).getWay();
        Node node = null;
        String s = "";
        if (w.firstNode().equals(jointNode)) {
            node = w.lastNode();
            s = "backward";
        } else {
            s = "forward";
            node = w.firstNode();
        }
        idxlst[0] = idx;
        idx--;
        double minLength = findDistance(w, super.nextWay, jointNode);
        super.memberTableModel.updateRole(idxlst, s);
        while (true) {
            w = super.members.get(idx).getWay();
            if (w.firstNode().equals(node)) {
                node = w.lastNode();
                node1 = w.firstNode();
                s = "backward";
                // roles[idx]=s;
            } else {
                node = w.firstNode();
                node1 = w.lastNode();
                s = "forward";
                // roles[idx]=s;
            }
            idxlst[0] = idx;
            double length = findDistance(w, super.nextWay, node1);
            if (minLength > length) {
                minLength = length;
            }
            super.memberTableModel.updateRole(idxlst, s);
            if (w.firstNode().equals(jointNode) || w.lastNode().equals(jointNode)) {
                break;
            }
            idx--;
        }
        super.currentNode = jointNode;
        brokenidx = idx;
        sortBelow(super.relation.getMembers(), 0);
    }

    void fixgapAfterlooping(int idx) {
        Way w = super.members.get(idx).getWay();
        super.currentWay = w;
        Way minWay = w;
        double minLength = findDistance(w, super.nextWay, super.currentNode);
        while (idx <= super.currentIndex) {
            w = super.members.get(idx).getWay();
            List<Way> parentWays = findNextWay(w, w.lastNode());
            if (w.firstNode() != null) {
                parentWays.addAll(findNextWay(w, w.firstNode()));
            }
            for (int i = 0; i < parentWays.size(); i++) {
                if (IsWaythere.get(parentWays.get(i)) == null) {
                    Node node = getOtherNode(parentWays.get(i), null);
                    double dist = findDistance(parentWays.get(i), super.nextWay, node);
                    if (dist < minLength) {
                        minLength = dist;
                        minWay = w;
                    }
                }
            }
            idx++;
        }
        w = minWay;
        downloadAreaAroundWay(w, w.lastNode(), w.firstNode());
    }

    /////////make panel////////////

    void makeNodesZeros() {
        for (int i = 0; i < super.members.size(); i++) {
            Way n = super.members.get(i).getWay();
            Node f = n.firstNode();
            Node l = n.lastNode();
            Isthere.put(f, 0);
            Isthere.put(l, 0);
            IsWaythere.put(n, null);
        }
    }
}
