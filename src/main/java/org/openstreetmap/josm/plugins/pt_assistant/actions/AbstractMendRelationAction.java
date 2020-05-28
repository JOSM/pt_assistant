// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.actions;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.MainApplication;
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
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();
    protected final Color[] FIVE_COLOR_PALETTE = {
        new Color(0, 255, 0, 150),
        new Color(255, 0, 0, 150),
        new Color(0, 0, 255, 150),
        new Color(255, 255, 0, 150),
        new Color(0, 255, 255, 150)};

    protected final Map<Character, Color> CHARACTER_COLOR_MAP = new HashMap<>();

    protected final Color CURRENT_WAY_COLOR = new Color(255, 255, 255, 190);
    protected final Color NEXT_WAY_COLOR = new Color(169, 169, 169, 210);

    protected final String I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY;
    protected final String I18N_CLOSE_OPTIONS = I18n.marktr("Close the options");
    protected final String I18N_NOT_REMOVE_WAYS = I18n.marktr("Do not remove ways");
    protected final String I18N_REMOVE_CURRENT_EDGE = I18n.marktr("Remove current edge (white)");
    protected final String I18N_REMOVE_WAYS = I18n.marktr("Remove ways");
    protected final String I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY = I18n.marktr("Remove ways along with previous way");
    protected final String I18N_SKIP = I18n.marktr("Skip");
    protected final String I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS = I18n.marktr("solutions based on other route relations");
    protected final String I18N_TURN_BY_TURN_NEXT_INTERSECTION = I18n.marktr("turn-by-turn at next intersection");
    protected final String I18N_BACKTRACK_WHITE_EDGE = I18n.marktr("Split white edge");

    protected GenericRelationEditor editor;
    protected Relation relation;
    protected MemberTableModel memberTableModel;
    protected List<RelationMember> members;
    protected Node currentNode;
    protected Way previousWay;
    protected List<Integer> extraWaysToBeDeleted;
    protected AbstractMapViewPaintable temporaryLayer;
    protected List<Character> allowedCharacters;
    protected HashMap<Way, Integer> waysAlreadyPresent;
    protected Way nextWay;
    protected Way currentWay;

    protected boolean setEnable = true;
    protected boolean nextIndex = true;
    protected boolean abort = false;
    protected boolean noLinkToPreviousWay = true;
    protected boolean halt = false;
    protected boolean aroundGaps = false;
    protected boolean aroundStops = false;
    protected boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
    protected boolean showOption0 = false;
    protected boolean onFly = false;

    protected int currentIndex;
    protected int downloadCounter;

    /**
     *
     * @param editorAccess
     * @param PTorNot is true is PTMendRelation is calling this function, false otherwise
     */
    protected AbstractMendRelationAction(IRelationEditorActionAccess editorAccess, boolean PTorNot) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        editor = (GenericRelationEditor) editorAccess.getEditor();
        memberTableModel = editorAccess.getMemberTableModel();
        relation = editor.getRelation();
        editor.addWindowListener(new AbstractMendRelationAction.WindowEventHandler());
        if (PTorNot) {
            I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY = I18n.marktr("Add oneway:bus=no to way");
        } else {
            I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY = I18n.marktr("Add oneway:bicycle=no to way");
        }


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

    /**
     * Override method with initializing the values for the required mend relation function
     */
    protected abstract void initialise();

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

        for (int i = 0; i < nodeList.size(); i++) {
            Node n = nodeList.get(i);
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
        for (int i = 0; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            for (int j = 0; j < previousWayNodes.size(); j++) {
                Node previousNode = previousWayNodes.get(j);
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
        for (int i = 0; i < tempLayers.size(); i++) {
            MainApplication.getMap().mapView.removeTemporaryLayer(tempLayers.get(i));
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
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                waysAlreadyPresent.put(members.get(i).getWay(), 1);
                ways.add(members.get(i).getWay());
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

    Way findNextWayBeforeDownload(Way way, Node node) {
        nextIndex = false;

        AutoScaleAction.zoomTo(Collections.singletonList(way));

        return null;
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
            downloadAreaAroundWay(way, nodes[0], null);
        } else if (nodes.length == 2) {
            downloadAreaAroundWay(way, nodes[0], nodes[1]);
        } else {
            Logging.error("Unexpected arguments");
        }
    }

    /**
     *
     * @param objects is varible arguments for this function of the following types
     *                1 Argument : {@code Way} (option 'a')
     *                3 Arguments : They can be either {@code Way Way List<Way>} (option 'b') or {@code Way Node Node} (option 'c')
     */
    @SuppressWarnings("unchecked")
    void downloadAreaAroundWay(Object... objects) {
        if (abort) {
            return;
        }

        final DownloadOsmTask task = null;
        char option = 0;
        boolean optionalCheck = false;
        Way way = null;
        Way prevWay = null;
        List<Way> ways = null;
        Node[] node = new Node[2];
        final String notificationText;

        String loggingError = tr("Unexpected arguments");

        if (objects.length == 1) {
            if (objects[0] instanceof Way) {
                option = 'a';
                way = (Way) objects[0];
                notificationText = " (2)";
            } else {
                Logging.error(loggingError);
                return;
            }
        } else if (objects.length == 3) {
            if (objects[0] instanceof Way) {
                way = (Way) objects[0];
                optionalCheck = way.isOutsideDownloadArea() || way.isNew();
                if (objects[1] instanceof Way && objects[2] instanceof List) {
                    option = 'b';
                    prevWay = (Way) objects[1];
                    ways = (List<Way>) objects[2];
                    notificationText = " (3)";
                } else if (objects[1] instanceof Node && objects[2] instanceof Node) {
                    option = 'c';
                    node[0] = (Node) objects[1];
                    node[1] = (Node) objects[2];
                    notificationText = " (1)";
                } else {
                    Logging.error(loggingError);
                    return;
                }
            } else {
                Logging.error(loggingError);
                return;
            }
        }

        if ((downloadCounter > 160 || optionalCheck) && onFly) {
            downloadCounter = 0;
            task = new DownloadOsmTask();
        } else {
            if (option == 'a') {
                if (currentIndex >= members.size() - 1) {
                    deleteExtraWays();
                } else {
                    currentIndex += 1;
                    callNextWay(currentIndex);
                }
            } else if (option == 'b') {
                goToNextWays(way, prevWay, ways);
            } else if (option == 'c') {
                findNextWayAfterDownload(way, node[0], node[1]);
            } else {
                Logging.error(loggingError);
                return;
            }
        }

        BoundsUtils.createBoundsWithPadding(way.getBBox(), .1).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);
            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area around way") + notificationText);
                    if (option == 'a') {
                        if (currentIndex >= members.size() - 1) {
                            deleteExtraWays();
                        } else {
                            callNextWay(++currentIndex);
                        }
                    } else if (option == 'b') {
                        goToNextWays(way, prevWay, ways);
                    } else if (option == 'c') {
                        findNextWayAfterDownload(way, node[0], node[1]);
                    }

                } catch (InterruptedException | ExecutionException e) {
                    Logging.error(e);
                }
            });
        });
    }

    /**
     *
     * @param currentIndex
     */
    protected abstract void callNextWay(int currentIndex);

    /**
     *
     * @param way
     * @param node1
     * @param node2
     */
    protected abstract void findNextWayAfterDownload(Way way, Node node1, Node node2);

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            editor.cancel();
            Logging.debug("close");
            stop();
        }
    }
}
