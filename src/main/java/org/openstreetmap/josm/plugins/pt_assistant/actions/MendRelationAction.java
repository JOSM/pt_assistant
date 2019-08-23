// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.command.SplitWayCommand.Strategy;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
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
import org.openstreetmap.josm.gui.layer.validation.PaintVisitor;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mend the relations by going through each way, sorting them and proposing
 * fixes for the gaps that are found
 *
 * @author Biswesh
 */
public class MendRelationAction extends AbstractRelationEditorAction {
    private static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();

    private static final Color[] FIVE_COLOR_PALETTE = { new Color(0, 255, 0, 150), new Color(255, 0, 0, 150),
            new Color(0, 0, 255, 150), new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

    private static final Map<Character, Color> CHARACTER_COLOR_MAP = new HashMap<>();
    static {
        CHARACTER_COLOR_MAP.put('A', new Color(0, 255, 0, 200));
        CHARACTER_COLOR_MAP.put('B', new Color(255, 0, 0, 200));
        CHARACTER_COLOR_MAP.put('C', new Color(0, 0, 255, 200));
        CHARACTER_COLOR_MAP.put('D', new Color(255, 255, 0, 200));
        CHARACTER_COLOR_MAP.put('E', new Color(0, 255, 255, 200));
        CHARACTER_COLOR_MAP.put('1', new Color(0, 255, 0, 200));
        CHARACTER_COLOR_MAP.put('2', new Color(255, 0, 0, 200));
        CHARACTER_COLOR_MAP.put('3', new Color(0, 0, 255, 200));
        CHARACTER_COLOR_MAP.put('4', new Color(255, 255, 0, 200));
        CHARACTER_COLOR_MAP.put('5', new Color(0, 255, 255, 200));
    }

    private static final Color CURRENT_WAY_COLOR = new Color(255, 255, 255, 190);
    private static final Color NEXT_WAY_COLOR = new Color(169, 169, 169, 210);

    String I18N_ADD_ONEWAY_VEHICLE_NO_TO_WAY = I18n.marktr("Add oneway:bus=no to way");
    private static final String I18N_CLOSE_OPTIONS = I18n.marktr("Close the options");
    private static final String I18N_NOT_REMOVE_WAYS = I18n.marktr("Do not remove ways");
    private static final String I18N_REMOVE_CURRENT_EDGE = I18n.marktr("Remove current edge (white)");
    private static final String I18N_REMOVE_WAYS = I18n.marktr("Remove ways");
    private static final String I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY = I18n.marktr("Remove ways along with previous way");
    private static final String I18N_SKIP = I18n.marktr("Skip");
    private static final String I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS = I18n
            .marktr("solutions based on other route relations");
    private static final String I18N_TURN_BY_TURN_NEXT_INTERSECTION = I18n.marktr("turn-by-turn at next intersection");
    private static final String I18N_BACKTRACK_WHITE_EDGE = I18n.marktr("Split white edge");
    Relation relation = null;
    MemberTableModel memberTableModel = null;
    GenericRelationEditor editor = null;
    HashMap<Way, Integer> waysAlreadyPresent = null;
    List<RelationMember> members = null;
    Way previousWay;
    Way currentWay;
    Way nextWay;
    List<Integer> extraWaysToBeDeleted = null;
    Node currentNode = null;
    boolean noLinkToPreviousWay = true;
    int currentIndex;
    int downloadCounter;
    boolean nextIndex = true;
    boolean setEnable = true;
    boolean firstCall = true;
    boolean halt = false;
    boolean abort = false;
    boolean shorterRoutes = false;
    boolean showOption0 = false;
    boolean onFly = false;
    boolean aroundGaps = false;
    boolean aroundStops = false;
    Node prevCurrenNode = null;
    Node splitNode = null;
    HashMap<Way, Character> wayColoring;
    HashMap<Character, List<Way>> wayListColoring;
    int nodeIdx = 0;
    AbstractMapViewPaintable temporaryLayer = null;
    String notice = null;
    List<Node> backnodes = new ArrayList<>();

    public MendRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        editor = (GenericRelationEditor) editorAccess.getEditor();
        memberTableModel = editorAccess.getMemberTableModel();
        relation = editor.getRelation();
        editor.addWindowListener(new WindowEventHandler());
    }

    String getQuery() {
        final StringBuilder str = new StringBuilder("[timeout:100];\n(\n");
        final String wayFormatterString = "   way(%.6f,%.6f,%.6f,%.6f)\n";
        final String str3 = "   [\"highway\"][\"highway\"!=\"footway\"][\"highway\"!=\"path\"][\"highway\"!=\"cycleway\"];\n";

        final List<Node> nodeList = aroundGaps ? getBrokenNodes() : new ArrayList<>();
        if (aroundStops) {
            nodeList.addAll(members.stream().filter(RelationMember::isNode).map(RelationMember::getNode)
                    .collect(Collectors.toList()));
        }

        for (final Node n : nodeList) {
            final double maxLat = n.getBBox().getTopLeftLat() + 0.001;
            final double minLat = n.getBBox().getBottomRightLat() - 0.001;
            final double maxLon = n.getBBox().getBottomRightLon() + 0.001;
            final double minLon = n.getBBox().getTopLeftLon() - 0.001;
            str.append(String.format(wayFormatterString, minLat, minLon, maxLat, maxLon)).append(str3);

        }

        return str.append(");\n(._;<;);\n(._;>;);\nout meta;").toString();
    }

    List<Node> getBrokenNodes() {
        List<Node> lst = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                int j = getNextWayIndex(i);
                if (j < members.size()) {
                    Way w = members.get(i).getWay();
                    Way v = members.get(j).getWay();
                    if (findNumberOfCommonNode(w, v) != 1) {
                        lst.add(w.firstNode());
                        lst.add(w.lastNode());
                    }
                }
            }
        }
        return lst;
    }

    @Override
    protected void updateEnabledState() {
        final Relation curRel = relation;

        setEnabled(curRel != null && setEnable
                && ((curRel.hasTag("route", "bus") && curRel.hasTag("public_transport:version", "2"))
                        || (RouteUtils.isPTRoute(curRel) && !curRel.hasTag("route", "bus"))));
    }

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

    void initialise() {
        save();
        sortBelow(relation.getMembers(), 0);
        members = editor.getRelation().getMembers();

        // halt is true indicates the action was paused
        if (halt == false) {
            downloadCounter = 0;
            firstCall = false;
            waysAlreadyPresent = new HashMap<>();
            getListOfAllWays();
            extraWaysToBeDeleted = new ArrayList<>();
            setEnable = false;
            previousWay = null;
            currentWay = null;
            nextWay = null;
            noLinkToPreviousWay = true;
            nextIndex = true;
            shorterRoutes = false;
            showOption0 = false;
            currentIndex = 0;

            final JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            final JCheckBox button1 = new JCheckBox("Around Stops");
            final JCheckBox button2 = new JCheckBox("Around Gaps");
            final JCheckBox button3 = new JCheckBox("On the fly");
            button3.setSelected(true);
            panel.add(new JLabel(tr("How would you want the download to take place?")), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button1, GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button2, GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button3, GBC.eol().fill(GBC.HORIZONTAL));

            int i = JOptionPane.showConfirmDialog(null, panel);
            if (i == JOptionPane.OK_OPTION) {
                if (button1.isSelected()) {
                    aroundStops = true;
                } else if (button2.isSelected()) {
                    aroundGaps = true;
                } else if (button3.isSelected()) {
                    onFly = true;
                }
                downloadEntireArea();
            }
        } else {
            halt = false;
            callNextWay(currentIndex);
        }
    }

    void downloadEntireArea() {
        if (abort)
            return;

        DownloadOsmTask task = new DownloadOsmTask();
        List<Way> wayList = getListOfAllWays();

        if (wayList.isEmpty()) {
            callNextWay(currentIndex);
            return;
        }

        String query = getQuery();
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

    void callNextWay(int i) {
        Logging.debug("Index + " + i);
        downloadCounter++;
        if (i < members.size() && members.get(i).isWay()) {
            if (currentNode == null)
                noLinkToPreviousWay = true;

            // find the next index in members which is a way
            int j = getNextWayIndex(i);
            if (j >= members.size()) {
                deleteExtraWays();
                return;
            }

            Way way = members.get(i).getWay();
            this.nextWay = members.get(j).getWay();

            // if some ways are invalid then remove them
            Node node = checkValidityOfWays(way, j);

            // if nextIndex was set as true in checkValidity method then don't move forward
            if (abort || nextIndex) {
                nextIndex = false;
                return;
            } else {
                nextIndex = true;
            }

            if (noLinkToPreviousWay) {
                if (node == null) {
                    // not connected in both ways
                    currentWay = way;
                    nextIndex = false;
                    findNextWayBeforeDownload(way, way.firstNode(), way.lastNode());
                } else {
                    noLinkToPreviousWay = false;
                    currentNode = getOtherNode(nextWay, node);
                    previousWay = way;
                    nextIndex = false;
                    downloadAreaAroundWay(way);
                }
            } else {
                if (node == null) {
                    currentWay = way;
                    nextIndex = false;
                    findNextWayBeforeDownload(way, currentNode);
                } else if (nextWay.lastNode().equals(node)) {
                    currentNode = nextWay.firstNode();
                    previousWay = way;
                    nextIndex = false;
                    downloadAreaAroundWay(way);
                } else {
                    currentNode = nextWay.lastNode();
                    previousWay = way;
                    nextIndex = false;
                    downloadAreaAroundWay(way);
                }
            }
        }

        if (abort)
            return;

        // for members which are not way
        if (i >= members.size() - 1) {
            deleteExtraWays();
        } else if (nextIndex) {
            callNextWay(++currentIndex);
        }
    }

    public static boolean isNonSplitRoundAbout(final Way way) {
        return way.hasTag("junction", "roundabout") && way.firstNode().equals(way.lastNode());
    }

    public static boolean isSplitRoundAbout(final Way way) {
        return way.hasTag("junction", "roundabout") && !way.firstNode().equals(way.lastNode());
    }

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

    void deleteExtraWays() {
        int[] ints = extraWaysToBeDeleted.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ints);
        setEnable = true;
        setEnabled(true);
        halt = false;
    }

    void removeWay(int j) {
        List<Integer> Int = new ArrayList<>();
        List<Way> lst = new ArrayList<>();
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

    List<Way> getListOfAllWays() {
        List<Way> ways = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                waysAlreadyPresent.put(members.get(i).getWay(), 1);
                ways.add(members.get(i).getWay());
            }
        }
        return ways;
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

    void sortBelow(List<RelationMember> members, int index) {
        RelationSorter relationSorter = new RelationSorter();
        final List<RelationMember> subList = members.subList(Math.max(0, index), members.size());
        final List<RelationMember> sorted = relationSorter.sortMembers(subList);
        subList.clear();
        subList.addAll(sorted);
        memberTableModel.fireTableDataChanged();
    }

    private void downloadIncompleteRelations() {

        List<Relation> parents = Collections.singletonList(relation);

        Future<?> future = MainApplication.worker
                .submit(new DownloadRelationMemberTask(parents,
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

    void addNewWays(List<Way> ways, int i) {
        try {
            List<RelationMember> c = new ArrayList<>();
            for (int k = 0; k < ways.size(); k++) {
                c.add(new RelationMember("", ways.get(k)));
                // check if the way that is getting added is already present or not
                if (!waysAlreadyPresent.containsKey(ways.get(k)))
                    waysAlreadyPresent.put(ways.get(k), 1);
                else {
                    deleteWayAfterIndex(ways.get(k), i);
                }
            }
            memberTableModel.addMembersAfterIdx(ways, i);
            members.addAll(i + 1, c);
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    void deleteWayAfterIndex(Way way, int index) {
        for (int i = index + 1; i < members.size(); i++) {
            if (members.get(i).isWay() && members.get(i).getWay().equals(way)) {
                Way prev = null;
                Way next = null;
                boolean del = true;
                if (i > 0 && members.get(i - 1).isWay())
                    prev = members.get(i - 1).getWay();
                if (i < members.size() - 1 && members.get(i + 1).isWay())
                    next = members.get(i + 1).getWay();
                // if the next index where the same way comes is well connected with its prev
                // and next way then don't delete it in that index
                if (prev != null && next != null) {
                    if (WayUtils.findNumberOfCommonFirstLastNodes(prev, way) != 0
                            && WayUtils.findNumberOfCommonFirstLastNodes(way, nextWay) != 0) {
                        del = false;
                    }
                }
                if (del) {
                    int[] x = { i };
                    memberTableModel.remove(x);
                    members.remove(i);
                    break;
                }
            }
        }
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

    Way findNextWayAfterDownload(Way way, Node node1, Node node2) {
        currentWay = way;
        if (abort)
            return null;

        List<Way> parentWays = findNextWay(way, node1);
        if (node2 != null)
            parentWays.addAll(findNextWay(way, node2));

        List<List<Way>> directRoute = getDirectRouteBetweenWays(currentWay, nextWay);
        if (directRoute == null || directRoute.size() == 0)
            showOption0 = false;
        else
            showOption0 = true;

        if (directRoute != null && directRoute.size() > 0 && !shorterRoutes && parentWays.size() > 0
                && notice == null) {
            displayFixVariantsWithOverlappingWays(directRoute);
            return null;
        }

        if (parentWays.size() == 1) {
            goToNextWays(parentWays.get(0), way, new ArrayList<>());
        } else if (parentWays.size() > 1) {
            nextIndex = false;
            displayFixVariants(parentWays);
        } else {
            nextIndex = true;

            if (currentIndex >= members.size() - 1) {
                deleteExtraWays();
            } else {
                callNextWay(++currentIndex);
                return null;
            }
        }
        return null;
    }

    List<List<Way>> getDirectRouteBetweenWays(Way current, Way next) {
        List<List<Way>> list = new ArrayList<>();
        List<Relation> r1;
        List<Relation> r2;
        try {
            r1 = new ArrayList<>(Utils.filteredCollection(current.getReferrers(), Relation.class));
            r2 = new ArrayList<>(Utils.filteredCollection(next.getReferrers(), Relation.class));
        } catch (Exception e) {
            return list;
        }

        List<Relation> rel = new ArrayList<>();
        String value = relation.get("route");

        for (Relation R1 : r1) {
            if (r2.contains(R1) && value.equals(R1.get("route")))
                rel.add(R1);
        }
        rel.remove(relation);

        for (Relation r : rel) {
            List<Way> lst = searchWayFromOtherRelations(r, current, next);
            boolean alreadyPresent = false;
            if (lst != null) {
                for (List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
            lst = searchWayFromOtherRelationsReversed(r, current, next);

            if (lst != null) {
                alreadyPresent = false;
                for (List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
        }

        return list;
    }

    // TODO: Merge with searchWayFromOtherRelationsReversed(Relation r, Way current, Way next)! These seem to do largely the same thing.
    List<Way> searchWayFromOtherRelations(Relation r, Way current, Way next) {
        List<RelationMember> member = r.getMembers();
        List<Way> lst = new ArrayList<>();
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
    List<Way> searchWayFromOtherRelationsReversed(Relation r, Way current, Way next) {
        List<RelationMember> member = r.getMembers();
        List<Way> lst = new ArrayList<>();
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

    void goToNextWays(Way way, Way prevWay, List<Way> wayList) {
        List<List<Way>> lst = new ArrayList<>();
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
        List<Way> parents = node.getParentWays();
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
                List<Way> wl = new ArrayList<>(wayList);
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

    List<Way> findNextWay(Way way, Node node) {
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

    boolean checkIfWayConnectsToNextWay(Way way, int count, Node node) {

        if (count < 50) {
            if (way.equals(nextWay))
                return true;

            // check if way;s intermediate node is next way's first or last node
            if (way.getNodes().contains(nextWay.firstNode()) || way.getNodes().contains(nextWay.lastNode()))
                return true;

            node = getOtherNode(way, node);
            List<Way> parents = node.getParentWays();
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

    List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
        parentWays.remove(way);
        if (abort)
            return null;
        List<Way> waysToBeRemoved = new ArrayList<>();
        // check if any of the way is joining with its intermediate nodes
        List<Way> waysToBeAdded = new ArrayList<>();
        for (Way w : parentWays) {
            if (node != null && !w.isFirstLastNode(node)) {
                Way w1 = new Way();
                Way w2 = new Way();

                List<Node> lst1 = new ArrayList<>();
                List<Node> lst2 = new ArrayList<>();
                boolean firsthalf = true;

                for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
                    if (firsthalf) {
                        lst1.add(nodePair.a);
                        lst1.add(nodePair.b);
                        if (nodePair.b.equals(node))
                            firsthalf = false;
                    } else {
                        lst2.add(nodePair.a);
                        lst2.add(nodePair.b);
                    }
                }

                w1.setNodes(lst1);
                w2.setNodes(lst2);

                w1.setKeys(w.getKeys());
                w2.setKeys(w.getKeys());

                if (!w.hasTag("junction", "roundabout")) {
                    waysToBeRemoved.add(w);
                    waysToBeAdded.add(w1);
                    waysToBeAdded.add(w2);
                }
            }
        }

        // check if one of the way's intermediate node equals the first or last node of next way,
        // if so then break it(finally split in method getNextWayAfterSelection if the way is chosen)
        for (Way w : parentWays) {
            Node nextWayNode = null;
            if (w.getNodes().contains(nextWay.firstNode()) && !w.isFirstLastNode(nextWay.firstNode())) {
                nextWayNode = nextWay.firstNode();
            } else if (w.getNodes().contains(nextWay.lastNode()) && !w.isFirstLastNode(nextWay.lastNode())) {
                nextWayNode = nextWay.lastNode();
            }

            if (nextWayNode != null) {
                Way w1 = new Way();
                Way w2 = new Way();

                List<Node> lst1 = new ArrayList<>();
                List<Node> lst2 = new ArrayList<>();
                boolean firsthalf = true;

                for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
                    if (firsthalf) {
                        lst1.add(nodePair.a);
                        lst1.add(nodePair.b);
                        if (nodePair.b.equals(nextWayNode))
                            firsthalf = false;
                    } else {
                        lst2.add(nodePair.a);
                        lst2.add(nodePair.b);
                    }
                }

                w1.setNodes(lst1);
                w2.setNodes(lst2);

                w1.setKeys(w.getKeys());
                w2.setKeys(w.getKeys());

                if (!w.hasTag("junction", "roundabout")) {
                    waysToBeRemoved.add(w);
                    if (w1.containsNode(node))
                        waysToBeAdded.add(w1);
                    if (w2.containsNode(node))
                        waysToBeAdded.add(w2);
                }
            }
        }
        parentWays.addAll(waysToBeAdded);
        // one way direction doesn't match
        parentWays.stream().filter(it -> WayUtils.isOneWay(it) && !checkOneWaySatisfiability(it, node))
                .forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);
        waysToBeRemoved.clear();

        // check if both nodes of the ways are common, then remove
        for (Way w : parentWays) {
            if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) != 1 && !w.hasTag("junction", "roundabout")) {
                waysToBeRemoved.add(w);
            }
        }

        // check if any of them belong to roundabout, if yes then show ways accordingly
        parentWays.stream()
                .filter(it -> it.hasTag("junction", "roundabout")
                        && WayUtils.findNumberOfCommonFirstLastNodes(way, it) == 1 && it.lastNode().equals(node))
                .forEach(waysToBeRemoved::add);

        // check mode of transport, also check if there is no loop
        if (relation.hasTag("route", "bus")) {
            parentWays.stream().filter(it -> !WayUtils.isSuitableForBuses(it)).forEach(waysToBeRemoved::add);
        } else if (RouteUtils.isPTRoute(relation)) {
            parentWays.stream().filter(it -> !isWaySuitableForOtherModes(it)).forEach(waysToBeRemoved::add);
        }

        parentWays.stream().filter(it -> it.equals(previousWay)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);
        waysToBeRemoved.clear();

        // check restrictions
        parentWays.stream().filter(it -> isRestricted(it, way, node)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);

        return parentWays;
    }

    List<Way> removeInvalidWaysFromParentWaysOfRoundabouts(List<Way> parents, Node node, Way way) {
        List<Way> parentWays = parents;
        parentWays.remove(way);
        if (abort)
            return null;
        List<Way> waysToBeRemoved = new ArrayList<>();

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

    void downloadAreaAroundWay(Way way, Node node1, Node node2) {
        if (abort)
            return;

        if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
            downloadCounter = 0;

            DownloadOsmTask task = new DownloadOsmTask();

            BoundsUtils.createBoundsWithPadding(way.getBBox(), .4).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (1)");
                        findNextWayAfterDownload(way, node1, node2);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            findNextWayAfterDownload(way, node1, node2);
        }
    }

    void downloadAreaAroundWay(Way way) {
        if (abort)
            return;

        if (downloadCounter > 160 && onFly) {
            downloadCounter = 0;

            DownloadOsmTask task = new DownloadOsmTask();
            BoundsUtils.createBoundsWithPadding(way.getBBox(), .1).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (2)");
                        if (currentIndex >= members.size() - 1) {
                            deleteExtraWays();
                        } else {
                            callNextWay(++currentIndex);
                        }
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            if (currentIndex >= members.size() - 1) {
                deleteExtraWays();
            } else {
                callNextWay(++currentIndex);
            }
        }
    }

    void downloadAreaAroundWay(Way way, Way prevWay, List<Way> ways) {
        if (abort)
            return;

        if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
            downloadCounter = 0;

            DownloadOsmTask task = new DownloadOsmTask();

            BoundsUtils.createBoundsWithPadding(way.getBBox(), .2).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (3)");
                        goToNextWays(way, prevWay, ways);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            goToNextWays(way, prevWay, ways);
        }
    }

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

    boolean isRestricted(Way currentWay, Way previousWay, Node commonNode) {
        Set<Relation> parentSet = OsmPrimitive.getParentRelations(previousWay.getNodes());
        if (parentSet == null || parentSet.isEmpty())
            return false;
        List<Relation> parentRelation = new ArrayList<>(parentSet);

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

    boolean checkOneWaySatisfiability(Way way, Node node) {
        String[] acceptedTags = new String[] { "yes", "designated" };

        if ((way.hasTag("oneway:bus", acceptedTags) || way.hasTag("oneway:psv", acceptedTags))
                && way.lastNode().equals(node) && relation.hasTag("route", "bus"))
            return false;

        if (!isNonSplitRoundAbout(way) && way.hasTag("junction", "roundabout")) {
            if (way.lastNode().equals(node))
                return false;
        }

        if (RouteUtils.isOnewayForPublicTransport(way) == 0)
            return true;
        else if (RouteUtils.isOnewayForPublicTransport(way) == 1 && way.lastNode().equals(node))
            return false;
        else if (RouteUtils.isOnewayForPublicTransport(way) == -1 && way.firstNode().equals(node))
            return false;

        return true;
    }

    Node getOtherNode(Way way, Node currentNode) {
        if (way.firstNode().equals(currentNode))
            return way.lastNode();
        else
            return way.firstNode();
    }

    void displayFixVariants(List<Way> fixVariants) {
        // find the letters of the fix variants:
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();
        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporarylayers();

        if (abort)
            return;

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                Character typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(Collections.singletonList(fixVariants.get(idx)));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

    void displayBacktrackFixVariant(List<Way> fixVariants, int idx1) {
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();
        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporarylayers();

        if (abort)
            return;

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                Character typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backTrack(currentWay, idx1 + 1);
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        findWayAfterChunk(currentWay);
                        getNextWayAfterBackTrackSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

    void backtrackCurrentEdge() {
        Way backTrackWay = currentWay;
        Way way = backTrackWay;
        backnodes = way.getNodes();
        if (currentNode == null) {
            currentNode = currentWay.lastNode();
        }
        if (currentNode.equals(way.lastNode())) {
            Collections.reverse(backnodes);
        }
        int idx = 1;
        prevCurrenNode = currentNode;
        backTrack(currentWay, idx);
    }

    public void backTrack(Way way, int idx) {
        if (idx >= backnodes.size() - 1) {
            currentNode = prevCurrenNode;
            callNextWay(currentIndex);
            return;
        }
        Node nod = backnodes.get(idx);
        if (way.isInnerNode(nod)) {
            List<Way> fixVariants = new ArrayList<>();
            List<Way> allWays = nod.getParentWays();
            if (allWays != null) {
                for (Way w : allWays) {
                    if (!w.equals(currentWay)) {
                        if (!WayUtils.isOneWay(w)) {
                            if (relation.hasTag("route", "bus")) {
                                if (WayUtils.isSuitableForBuses(w)) {
                                    fixVariants.add(w);
                                }
                            } else if (relation.hasTag("route", "bicycle")) {
                                if (WayUtils.isSuitableForBicycle(w)) {
                                    fixVariants.add(w);
                                }
                            }
                        } else {
                            if (w.firstNode().equals(nod)) {
                                if (relation.hasTag("route", "bus")) {
                                    if (WayUtils.isSuitableForBuses(w)) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag("route", "bicycle")) {
                                    if (WayUtils.isSuitableForBicycle(w)) {
                                        fixVariants.add(w);
                                    }
                                }
                            } else {
                                if (relation.hasTag("route", "bus")) {
                                    if (RouteUtils.isOnewayForPublicTransport(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag("route", "bicycle")) {
                                    if (RouteUtils.isOnewayForBicycles(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            currentNode = nod;
            if (fixVariants.size() > 0) {
                displayBacktrackFixVariant(fixVariants, idx);
            } else {
                backTrack(way, idx + 1);
            }
        }
    }

    public Way findWayAfterChunk(Way way) {
        Way w1 = null;
        Way wayToKeep = null;
        List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        Strategy strategy = new TempStrategy();
        List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(currentWay, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
            w1 = result.getNewWays().get(0);
            wayToKeep = w1;
        }
        return wayToKeep;
    }

    private void findWayafterchunkRoundabout(Way way) {
        List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        splitNode = way.lastNode();
        Strategy strategy = new TempStrategyRoundabout();
        List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(way, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
        }
    }

    private void removeKeyListenerAndTemporaryLayer(KeyListener keyListener) {
        MainApplication.getMap().mapView.removeKeyListener(keyListener);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
    }

    void displayFixVariantsWithOverlappingWays(List<List<Way>> fixVariants) {
        // find the letters of the fix variants:
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayListColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();

        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayListColoring.put(alphabet, fixVariants.get(i));
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporarylayers();

        if (abort)
            return;

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // display the fix variants:
        temporaryLayer = new MendRelationAddMultipleLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = shorterRoutes ? false : true;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    setEnable = true;
                    shorterRoutes = false;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

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
        removeTemporarylayers();

        if (abort)
            return;

        // zoom to problem:
        final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

        for (Integer i : wayIndices) {
            waysToZoom.add(members.get(i).getWay());
        }

        AutoScaleAction.zoomTo(waysToZoom);

        // display the fix variants:
        temporaryLayer = new MendRelationRemoveLayer();
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
                Character typedKey = e.getKeyChar();
                Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    nextIndex = true;
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                    Logging.debug(String.valueOf(typedKeyUpperCase));
                    if (typedKeyUpperCase == 'R' || typedKeyUpperCase == '3') {
                        wayIndices.add(0, currentIndex);
                    }
                    RemoveWayAfterSelection(wayIndices, typedKeyUpperCase);
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    Logging.debug("ESC");
                    nextIndex = false;
                    setEnable = true;
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

    void RemoveWayAfterSelection(List<Integer> wayIndices, Character chr) {
        if (chr == 'A' || chr == '1') {
            // remove all the ways
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            memberTableModel.remove(lst);
            for (int i = 0; i < lst.length; i++) {
                members.remove(lst[i] - i);
            }
            // OK.actionPerformed(null);
            save();
            if (currentIndex < members.size() - 1) {
                notice = null;
                callNextWay(currentIndex);
            } else {
                notice = null;
                deleteExtraWays();
            }
        } else if (chr == 'B' || chr == '2') {
            if (currentIndex < members.size() - 1) {
                notice = null;
                currentIndex = wayIndices.get(wayIndices.size() - 1);
                callNextWay(currentIndex);
            } else {
                notice = null;
                deleteExtraWays();
            }
        } else if (chr == 'C' || chr == '4') {
            List<Command> cmdlst = new ArrayList<>();
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            for (int i = 0; i < lst.length; i++) {
                Way w = members.get(lst[i]).getWay();
                TagMap newKeys = w.getKeys();
                newKeys.put("oneway", "bus=no");
                cmdlst.add(new ChangePropertyCommand(Collections.singleton(w), newKeys));
            }
            UndoRedoHandler.getInstance().add(new SequenceCommand("Add tags", cmdlst));
            // OK.actionPerformed(null);
            save();
            if (currentIndex < members.size() - 1) {
                notice = null;
                callNextWay(currentIndex);
            } else {
                notice = null;
                deleteExtraWays();
            }
        }
        if (chr == 'R' || chr == '3') {
            // calculate the previous index
            int prevIndex = -1;
            for (int i = currentIndex - 1; i >= 0; i--) {
                if (members.get(i).isWay()) {
                    prevIndex = i;
                    break;
                }
            }
            // remove all the ways
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            memberTableModel.remove(lst);
            for (int i = 0; i < lst.length; i++) {
                members.remove(lst[i] - i);
            }
            // OK.actionPerformed(null);
            save();
            if (prevIndex != -1) {
                notice = null;
                callNextWay(prevIndex);
            } else {
                notice = null;
                deleteExtraWays();
            }
        }
    }

    void getNextWayAfterBackTrackSelection(Way way) {
        save();
        List<Integer> lst = new ArrayList<>();
        lst.add(currentIndex + 1);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ind);
        Way temp = members.get(ind[0]).getWay();
        for (int i = 0; i < ind.length; i++) {
            members.remove(ind[i] - i);
        }
        save();
        List<RelationMember> c = new ArrayList<>();
        List<Way> ways = new ArrayList<>();
        ways.add(temp);
        int p = currentIndex;
        c.add(new RelationMember("", ways.get(0)));
        members.addAll(p + 1, c);
        save();
        int indx = currentIndex;
        addNewWays(Collections.singletonList(way), indx);
        currentNode = getOtherNode(way, currentNode);
        if (currentIndex < members.size() - 1) {
            callNextWay(++currentIndex);
        } else {
            deleteExtraWays();
        }
    }

    void getNextWayAfterSelection(List<Way> ways) {
        if (ways != null) {
            /*
             * check if the selected way is not a complete way but rather a part of a parent
             * way, then split the actual way (the partial way was created in method
             * removeViolatingWaysFromParentWays but here we are finally splitting the
             * actual way and adding to the relation) here there can be 3 cases - 1) if the
             * current node is the node splitting a certain way 2) if next way's first node
             * is splitting the way 3) if next way's last node is splitting the way
             */
            Logging.debug("Number of ways " + ways.size());
            int ind = currentIndex;
            Way prev = currentWay;
            for (int i = 0; i < ways.size(); i++) {
                Way w = ways.get(i);
                Way w1 = null;
                List<Node> breakNode = null;
                boolean brk = false;

                if (w.isNew()) {
                    if (prev != null) {
                        List<Way> par = new ArrayList<>(prev.firstNode().getParentWays());
                        par.addAll(prev.lastNode().getParentWays());
                        for (Way v : par) {
                            if (v.getNodes().containsAll(w.getNodes())) {
                                if (w.equals(v)) {
                                    addNewWays(Collections.singletonList(v), ind);
                                    prev = v;
                                    ind++;
                                    brk = true;
                                    break;
                                } else {
                                    List<Node> temp = new ArrayList<>();
                                    if (!v.isFirstLastNode(w.firstNode()))
                                        temp.add(w.firstNode());
                                    if (!v.isFirstLastNode(w.lastNode()))
                                        temp.add(w.lastNode());
                                    if (temp.size() != 0) {
                                        w1 = v;
                                        breakNode = new ArrayList<>(temp);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // check if the new way is part of one of the parentWay of the nextWay's first
                    // node
                    for (Way v : nextWay.firstNode().getParentWays()) {
                        if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                            if (!w.equals(v) && !v.isFirstLastNode(nextWay.firstNode())) {
                                w1 = v;
                                breakNode = Collections.singletonList(nextWay.firstNode());
                                break;
                            } else if (w.equals(v)) {
                                addNewWays(Collections.singletonList(v), ind);
                                prev = v;
                                ind++;
                                brk = true;
                                break;
                            }
                        }
                    }

                    // check if the new way is part of one of the parentWay of the nextWay's first
                    for (Way v : nextWay.lastNode().getParentWays()) {
                        if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                            if (!w.equals(v) && !v.isFirstLastNode(nextWay.lastNode())) {
                                w1 = v;
                                breakNode = Collections.singletonList(nextWay.lastNode());
                                break;
                            } else if (w.equals(v)) {
                                addNewWays(Collections.singletonList(v), ind);
                                ind++;
                                prev = v;
                                brk = true;
                                break;
                            }
                        }
                    }

                    if (w1 != null && !brk) {
                        SplitWayCommand result = SplitWayCommand.split(w1, breakNode, Collections.emptyList());
                        if (result != null) {
                            UndoRedoHandler.getInstance().add(result);
                            if (result.getOriginalWay().getNodes().contains(w.firstNode())
                                    && result.getOriginalWay().getNodes().contains(w.lastNode()))
                                w = result.getOriginalWay();
                            else
                                w = result.getNewWays().get(0);

                            addNewWays(Collections.singletonList(w), ind);
                            prev = w;
                            ind++;
                        }

                    } else if (!brk) {
                        Logging.debug("none");
                    }
                } else {
                    if (w.isInnerNode(currentNode) && !w.firstNode().equals(w.lastNode())) {
                        findWayafterchunkRoundabout(w);
                    }
                    addNewWays(Collections.singletonList(w), ind);
                    prev = w;
                    ind++;
                }
            }
            Way way = members.get(currentIndex).getWay();
            Way nexWay = members.get(currentIndex + 1).getWay();
            Node n = WayUtils.findCommonFirstLastNode(nexWay, way, currentNode).orElse(null);
            currentNode = getOtherNode(nexWay, n);
            save();
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Logging.error(e);
            }
        } else {
            currentNode = null;
        }
        previousWay = currentWay;
        if (currentIndex < members.size() - 1) {
            callNextWay(++currentIndex);
        } else
            deleteExtraWays();
    }

    List<Way> findCurrentEdge() {
        List<Way> lst = new ArrayList<>();
        lst.add(currentWay);
        int j = currentIndex;
        Way curr = currentWay;
        while (true) {
            int i = getPreviousWayIndex(j);
            if (i == -1)
                break;

            Way prevWay = members.get(i).getWay();

            if (prevWay == null)
                break;

            if (!WayUtils.findCommonFirstLastNode(curr, prevWay).filter(node -> node.getParentWays().size() <= 2)
                    .isPresent()) {
                break;
            }

            lst.add(prevWay);
            curr = prevWay;
            j = i;
        }
        return lst;
    }

    void removeCurrentEdge() {
        List<Integer> lst = new ArrayList<>();
        lst.add(currentIndex);
        int j = currentIndex;
        Way curr = currentWay;
        Node n = getOtherNode(curr, currentNode);

        while (true) {
            int i = getPreviousWayIndex(j);
            if (i == -1)
                break;

            Way prevWay = members.get(i).getWay();

            if (prevWay == null)
                break;

            if (!WayUtils.findCommonFirstLastNode(curr, prevWay).filter(node -> node.getParentWays().size() <= 2)
                    .isPresent()) {
                break;
            }

            lst.add(i);
            curr = prevWay;
            j = i;
        }

        int prevInd = getPreviousWayIndex(j);

        Collections.reverse(lst);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ind);
        for (int i = 0; i < ind.length; i++) {
            members.remove(ind[i] - i);
        }

        save();

        if (prevInd >= 0) {
            currentNode = n;
            currentIndex = prevInd;
            callNextWay(currentIndex);
        } else {
            notice = null;
            deleteExtraWays();
        }
    }

    void removeTemporarylayers() {
        List<MapViewPaintable> tempLayers = MainApplication.getMap().mapView.getTemporaryLayers();
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

    public void save() {
        editor.apply();
    }

    public class TempStrategy implements Strategy {
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

    public class TempStrategyRoundabout implements Strategy {
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

    private class MendRelationAddLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
            paintVisitor.drawVariants();
        }
    }

    private class MendRelationRemoveLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
            paintVisitor.drawOptionsToRemoveWays();
        }
    }

    private class MendRelationAddMultipleLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
            paintVisitor.drawMultipleVariants(wayListColoring);
        }
    }

    class MendRelationPaintVisitor extends PaintVisitor {
        /** The graphics */
        private final Graphics g;
        /** The MapView */
        private final MapView mv;
        private HashMap<Way, List<Character>> waysColoring;

        MendRelationPaintVisitor(Graphics2D g, MapView mv) {
            super(g, mv);
            this.g = g;
            this.mv = mv;
        }

        /*
         * Functions in this class are directly taken from PTAssistantPaintVisitor with
         * some slight modification
         */

        void drawVariants() {
            drawFixVariantsWithParallelLines(true);

            Color[] colors = { new Color(0, 255, 150), new Color(255, 0, 0, 150), new Color(0, 0, 255, 150),
                    new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
            Character chr = 'A';
            if (numeric)
                chr = '1';

            if (showOption0 && numeric) {
                if (!shorterRoutes)
                    drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                            letterY, 25);
                else
                    drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                            letterY, 25);
                letterY = letterY + 60;
            } else if (showOption0) {
                if (!shorterRoutes)
                    drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                            letterY, 25);
                else
                    drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                            letterY, 25);
                letterY = letterY + 60;
            }

            for (int i = 0; i < 5; i++) {
                if (wayColoring.containsValue(chr)) {
                    drawFixVariantLetter(chr.toString(), colors[i], letterX, letterY, 35);
                    letterY = letterY + 60;
                }
                chr++;
            }

            // display the "Esc", "Skip" label:
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);
            letterY = letterY + 60;
            if (numeric) {
                drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("8 : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            } else {
                drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("V : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            }
        }

        void drawOptionsToRemoveWays() {
            drawFixVariantsWithParallelLines(false);
            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            if (notice != null) {
                drawFixVariantLetter("Error:  " + notice, Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
            }
            if (numeric) {
                drawFixVariantLetter("1 : " + tr(I18N_REMOVE_WAYS), FIVE_COLOR_PALETTE[0], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("2 : " + tr(I18N_NOT_REMOVE_WAYS), FIVE_COLOR_PALETTE[1], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("3 : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), FIVE_COLOR_PALETTE[4], letterX,
                        letterY, 25);
                letterY = letterY + 60;
                if (notice.equals("vehicle travels against oneway restriction")) {
                    drawFixVariantLetter("4 : " + tr(I18N_ADD_ONEWAY_VEHICLE_NO_TO_WAY), FIVE_COLOR_PALETTE[3], letterX,
                            letterY, 25);
                }
            } else {
                drawFixVariantLetter("A : " + tr(I18N_REMOVE_WAYS), FIVE_COLOR_PALETTE[0], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("B : " + tr(I18N_NOT_REMOVE_WAYS), FIVE_COLOR_PALETTE[1], letterX, letterY, 25);
                letterY = letterY + 60;
                if (notice.equals("vehicle travels against oneway restriction")) {
                    drawFixVariantLetter("C : " + tr(I18N_ADD_ONEWAY_VEHICLE_NO_TO_WAY), FIVE_COLOR_PALETTE[3], letterX,
                            letterY, 25);
                    letterY = letterY + 60;
                }
                drawFixVariantLetter("R : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), FIVE_COLOR_PALETTE[4], letterX,
                        letterY, 25);
            }

            letterY = letterY + 60;
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 30);
        }

        void drawMultipleVariants(HashMap<Character, List<Way>> fixVariants) {
            waysColoring = new HashMap<>();
            addFixVariants(fixVariants);
            drawFixVariantsWithParallelLines(waysColoring);

            int colorIndex = 0;

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

            if (showOption0 && numeric) {
                if (!shorterRoutes)
                    drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                            letterY, 25);
                else
                    drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                            letterY, 25);
                letterY = letterY + 60;
            } else if (showOption0) {
                if (!shorterRoutes)
                    drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                            letterY, 25);
                else
                    drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                            letterY, 25);
                letterY = letterY + 60;
            }

            for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
                Character c = entry.getKey();
                if (fixVariants.get(c) != null) {
                    drawFixVariantLetter(c.toString(), FIVE_COLOR_PALETTE[colorIndex % 5], letterX, letterY, 35);
                    colorIndex++;
                    letterY = letterY + 60;
                }
            }

            // display the "Esc", "Skip" label:
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);
            letterY = letterY + 60;
            if (numeric) {
                drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("8 : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            } else {
                drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("V : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            }

        }

        protected void drawFixVariantsWithParallelLines(final boolean drawNextWay) {
            wayColoring.entrySet().stream()
                    // Create pairs of a color and an associated pair of nodes
                    .flatMap(entry -> entry.getKey().getNodePairs(false).stream()
                            .map(it -> Pair.create(CHARACTER_COLOR_MAP.get(entry.getValue()), it)))
                    // Grouping by color: groups stream into a map, each map entry has a color and all associated pairs of nodes
                    .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toList())))
                    .forEach((color, nodePairs) -> drawSegmentsWithParallelLines(nodePairs, color));
            drawSegmentsWithParallelLines(currentWay.getNodePairs(false), CURRENT_WAY_COLOR);
            if (drawNextWay) {
                drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);
            }
        }

        protected void drawFixVariantsWithParallelLines(Map<Way, List<Character>> waysColoring) {
            for (final Entry<Way, List<Character>> entry : waysColoring.entrySet()) {
                final List<Color> wayColors = entry.getValue().stream().map(CHARACTER_COLOR_MAP::get)
                        .collect(Collectors.toList());
                for (final Pair<Node, Node> nodePair : entry.getKey().getNodePairs(false)) {
                    drawSegmentWithParallelLines(nodePair.a, nodePair.b, wayColors);
                }
            }

            drawSegmentsWithParallelLines(findCurrentEdge().stream().flatMap(it -> it.getNodePairs(false).stream())
                    .collect(Collectors.toList()), CURRENT_WAY_COLOR);

            drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);

        }

        /**
         * Convenience method for {@link #drawSegmentWithParallelLines(Node, Node, List)}.
         */
        private void drawSegmentsWithParallelLines(List<Pair<Node, Node>> nodePairs, final Color color) {
            final List<Color> colorList = Collections.singletonList(color);
            nodePairs.forEach(it -> drawSegmentWithParallelLines(it.a, it.b, colorList));
        }

        void drawSegmentWithParallelLines(Node n1, Node n2, List<Color> colors) {
            if (!n1.isDrawable() || !n2.isDrawable() || !isSegmentVisible(n1, n2)) {
                return;
            }

            Point p1 = mv.getPoint(n1);
            Point p2 = mv.getPoint(n2);
            double t = Math.atan2((double) p2.x - p1.x, (double) p2.y - p1.y);
            double cosT = 9 * Math.cos(t);
            double sinT = 9 * Math.sin(t);
            double heightCosT = 9 * Math.cos(t);
            double heightSinT = 9 * Math.sin(t);

            double prevPointX = p1.x;
            double prevPointY = p1.y;
            double nextPointX = p1.x + heightSinT;
            double nextPointY = p1.y + heightCosT;

            Color currentColor = colors.get(0);
            int i = 0;
            g.setColor(currentColor);
            g.fillOval(p1.x - 9, p1.y - 9, 18, 18);

            if (colors.size() == 1) {
                int[] xPoints = { (int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
                int[] yPoints = { (int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
                g.setColor(currentColor);
                g.fillPolygon(xPoints, yPoints, 4);
            } else if (colors.size() > 1) {
                boolean iterate = true;
                while (iterate) {
                    currentColor = colors.get(i % colors.size());

                    int[] xPoints = { (int) (prevPointX + cosT), (int) (nextPointX + cosT), (int) (nextPointX - cosT),
                            (int) (prevPointX - cosT) };
                    int[] yPoints = { (int) (prevPointY - sinT), (int) (nextPointY - sinT), (int) (nextPointY + sinT),
                            (int) (prevPointY + sinT) };
                    g.setColor(currentColor);
                    g.fillPolygon(xPoints, yPoints, 4);

                    prevPointX = prevPointX + heightSinT;
                    prevPointY = prevPointY + heightCosT;
                    nextPointX = nextPointX + heightSinT;
                    nextPointY = nextPointY + heightCosT;
                    i++;
                    if ((p1.x < p2.x && nextPointX >= p2.x) || (p1.x >= p2.x && nextPointX <= p2.x)) {
                        iterate = false;
                    }
                }

                int[] lastXPoints = { (int) (prevPointX + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT),
                        (int) (prevPointX - cosT) };
                int[] lastYPoints = { (int) (prevPointY - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT),
                        (int) (prevPointY + sinT) };
                g.setColor(currentColor);
                g.fillPolygon(lastXPoints, lastYPoints, 4);
            }

            g.setColor(currentColor);
            g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
        }

        void addFixVariants(HashMap<Character, List<Way>> fixVariants) {
            for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
                Character currentFixVariantLetter = entry.getKey();
                List<Way> fixVariant = entry.getValue();
                for (Way way : fixVariant) {
                    if (waysColoring.containsKey(way)) {
                        if (!waysColoring.get(way).contains(currentFixVariantLetter)) {
                            waysColoring.get(way).add(currentFixVariantLetter);
                        }
                    } else {
                        List<Character> letterList = new ArrayList<>();
                        letterList.add(currentFixVariantLetter);
                        waysColoring.put(way, letterList);
                    }
                }
            }
        }

        /**
         * Visualizes the letters for each fix variant
         * @param letter letter to draw
         * @param color text color
         * @param letterX X coordinate
         * @param letterY Y coordinate
         * @param size font size
         */
        private void drawFixVariantLetter(String letter, Color color, double letterX, double letterY, int size) {
            g.setColor(color);
            Font stringFont = new Font("SansSerif", Font.PLAIN, size);
            g.setFont(stringFont);
            try {
                g.drawString(letter, (int) letterX, (int) letterY);
                g.drawString(letter, (int) letterX, (int) letterY);
            } catch (NullPointerException ex) {
                // do nothing
                Logging.trace(ex);
            }

        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            editor.cancel();
            Logging.debug("close");
            stop();
        }
    }
}
