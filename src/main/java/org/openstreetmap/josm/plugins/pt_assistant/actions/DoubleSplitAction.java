// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IWaySegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NodeUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PrimitiveUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Shortcut;

import jakarta.annotation.Nonnull;

/**
 * The DoubleSplitAction is a mapmode that allows users to add a bus_bay,a
 * bridge or a tunnel .
 *
 * @author Biswesh
 */
public class DoubleSplitAction extends MapMode implements KeyListener {
    private static final String BOTH = "both";
    private static final String BRIDGE = "bridge";
    private static final String BUILDING_PASSAGE = "building_passage";
    private static final String BUS_BAY = "bus_bay";
    private static final String CULVERT = "culvert";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String TRAFFIC_CALMING = "traffic_calming";
    private static final String TUNNEL = "tunnel";
    private static final String WATERWAY = "waterway";
    private static final String LAYER = "layer";
    private static final String TABLE = "table";

    private final transient Set<OsmPrimitive> newHighlights = new HashSet<>();
    private final transient Set<OsmPrimitive> oldHighlights = new HashSet<>();
    private final List<Node> atNodes = new ArrayList<>();
    private final transient DoubleSplitLayer temporaryLayer = new DoubleSplitLayer();
    ILatLon Pos1;
    ILatLon Pos2;
    Way SegWay1;
    Way SegWay2;
    Way affected;
    Way previousAffectedWay;

    private final Cursor cursorJoinNode;
    private final Cursor cursorJoinWay;

    /**
     * Creates a new DoubleSplitAction
     */
    public DoubleSplitAction() {
        super(tr("Double Split"), "double_split", tr("Double Split"),
            Shortcut.registerShortcut("pt_assistant:double_split", tr("PT Assistant: Double Split"),
                KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), getCursor());
        cursorJoinNode = ImageProvider.getCursor("crosshair", "joinnode");
        cursorJoinWay = ImageProvider.getCursor("crosshair", "joinway");
    }

    private static Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return isEditableDataLayer(l);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }

    @Override
    public void enterMode() {
        if (!isEnabled())
            return;
        super.enterMode();
        MainApplication.getMap().mapView.addMouseListener(this);
        MainApplication.getMap().mapView.addMouseMotionListener(this);
        MainApplication.getMap().mapView.addKeyListener(this);
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);
    }

    @Override
    public void exitMode() {
        reset();
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
        MainApplication.getMap().mapView.removeMouseMotionListener(this);
        MainApplication.getMap().mapView.removeKeyListener(this);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
        resetLayer();
    }

    private void reset() {
        try {
            atNodes.clear();
            updateHighlights();
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    private void resetLayer() {
        Pos1 = null;
        Pos2 = null;
        SegWay1 = null;
        SegWay2 = null;
        temporaryLayer.invalidate();
    }

    // check if both the nodes are starting and ending nodes of same way
    private boolean startEndPoints(List<Command> commandList) {

        try {
            for (Way way : atNodes.get(0).getParentWays()) {
                if (atNodes.get(1).getParentWays().contains(way) && way.isFirstLastNode(atNodes.get(0)) && way.isFirstLastNode(atNodes.get(1))) {
                    newHighlights.add(way);
                    dialogBox(SelectFromOptionDialog.TYPE_NODES_ARE_ENDS_OF_SAME_WAY, null, way, way, commandList);
                    return true;
                }
            }
        } catch (Exception e) {
            Logging.error(e);
        }

        return false;
    }

    private static boolean firstNodeIsConnectionNode(Node node, Way affected) {
        if (node.isConnectionNode()) {
            return !node.getParentWays().contains(affected);
        }
        return false;
    }

    private static boolean secondNodeIsConnectionNode(Node node, Way previousAffectedWay) {
        if (node.isConnectionNode()) {
            return !node.getParentWays().contains(previousAffectedWay);
        }
        return false;
    }

    private void removeFirstNode() {

        atNodes.get(0).setDeleted(true);
        atNodes.get(1).setDeleted(true);
        Pos1 = Pos2;
        Pos2 = null;
        SegWay1 = SegWay2;
        SegWay2 = null;
        reset();
    }

    private void action() {

        List<Command> commandList = new ArrayList<>();

        // check if the user has selected an existing node, or a new one
        Point curP1 = MainApplication.getMap().mapView.getPoint(Pos1);
        ILatLon p1 = new LatLon(curP1.getX(), curP1.getY());
        Node node1 = createNode(p1, commandList);

        Point curP2 = MainApplication.getMap().mapView.getPoint(Pos2);
        ILatLon p2 = new LatLon(curP2.getX(), curP2.getY());
        Node node2 = createNode(p2, commandList);

        if (node1.equals(node2)) {
            resetLayer();
            return;
        }

        atNodes.add(node1);
        atNodes.add(node2);

        previousAffectedWay = SegWay1;
        affected = SegWay2;

        if (affected == null || previousAffectedWay == null) {
            node1.setDeleted(true);
            node2.setDeleted(true);
            resetLayer();
            return;
        }

        // if both the nodes are starting and ending points of the same way
        // we don't split the way, just add new key-value to the way
        boolean areStartEndPoints = startEndPoints(commandList);
        if (areStartEndPoints) {
            resetLayer();
            return;
        }

        // if first node is a connection node
        boolean isConnectionNode = firstNodeIsConnectionNode(atNodes.get(0), affected);
        if (isConnectionNode) {
            resetLayer();
            return;
        }

        // if second node is a connection node
        isConnectionNode = secondNodeIsConnectionNode(atNodes.get(1), previousAffectedWay);
        if (isConnectionNode) {
            resetLayer();
            return;
        } else {
            if (atNodes.get(1).isConnectionNode()) {
                affected = previousAffectedWay;
            }
        }

        // if both the nodes are not on same way and don't have any common node then
        // make second node as first node
        if (previousAffectedWay != affected) {
            final Optional<Node> firstCommonNode = WayUtils.findFirstCommonNode(affected, previousAffectedWay);
            if (firstCommonNode.isPresent()) {
                dialogBox(SelectFromOptionDialog.TYPE_NODES_ON_ADJACENT_WAYS, firstCommonNode.get(), affected, previousAffectedWay, commandList);
            } else {
                removeFirstNode();
            }
        } else {
            dialogBox(SelectFromOptionDialog.TYPE_NODES_ON_SAME_WAY, null, affected, previousAffectedWay, commandList);
        }
    }

    // calls the dialog box
    private void dialogBox(int type, Node commonNode, Way affected, Way previousAffectedWay,
            List<Command> commandList) {

        final ExtendedDialog dialog = new SelectFromOptionDialog(type, commonNode, affected, previousAffectedWay,
                commandList);
        dialog.setModal(false);
        dialog.showDialog();
        // splitting is performed in SegmentToKeepSelectionDialog.buttonAction()

    }

    // create a node in the position that the user has marked
    @Nonnull
    private Node createNode(ILatLon pos, List<Command> commandList) {
        boolean newNode = false;
        Node newStopPos;

        Point p = new Point();
        p.setLocation(pos.lat(), pos.lon());

        Node n = MainApplication.getMap().mapView.getNearestNode(p, OsmPrimitive::isUsable);
        if (n == null) {
            newNode = true;
            newStopPos = new Node(MainApplication.getMap().mapView.getLatLon(pos.lat(), pos.lon()));
        } else {
            newStopPos = new Node(n);
        }

        if (newNode) {
            commandList.add(new AddCommand(getLayerManager().getEditDataSet(), newStopPos));
        } else {
            commandList.add(new ChangeCommand(n, newStopPos));
            MainApplication.getLayerManager().getEditLayer().data.setSelected(newStopPos);
            newStopPos = n;
        }

        return newStopPos;
    }

    /*
     * the function works if we have both nodes on a single way. It adds the nodes
     * in the undoRedo, splits the ways if required, collects the existing keys of
     * the ways and sends to addTags function
     */
    private void addKeys(Way affected, List<Command> commandList, JComboBox<String> keys, JComboBox<String> values) {
        List<TagMap> affectedKeysList = new ArrayList<>();
        Way selectedWay = null;

        UndoRedoHandler.getInstance().add(new SequenceCommand("Add Nodes", commandList));
        commandList.clear();

        NodeUtils.moveOntoNearestWay(atNodes.get(0));
        NodeUtils.moveOntoNearestWay(atNodes.get(1));

        SplitWayCommand result = SplitWayCommand.split(affected, atNodes, Collections.emptyList());
        if (result == null) {
            UndoRedoHandler.getInstance().undo();
            resetLayer();
            return;
        }

        commandList.add(result);
        UndoRedoHandler.getInstance().add(new SequenceCommand("Split Way", commandList));

        // Find the middle way after split
        List<Way> affectedWayList = result.getNewWays();
        affectedWayList.add(result.getOriginalWay());

        for (Way way : affectedWayList) {
            if (atNodes.contains(way.firstNode()) && atNodes.contains(way.lastNode())) {
                selectedWay = way;
                break;
            }
        }

        // add the existing keys of the selected way to affectedKeysList
        if (selectedWay != null) {
            affectedKeysList.add(affected.getKeys());
            addTags(affectedKeysList, Collections.singletonList(selectedWay), keys, values);
        } else {
            UndoRedoHandler.getInstance().undo();
            UndoRedoHandler.getInstance().undo();
            resetLayer();
        }
    }

    // this function is called when both nodes are in 2 adjacent ways
    private void addKeysOnBothWays(Node commonNode, Way affected, Way previousAffectedWay, List<Command> commandList,
            JComboBox<String> keys, JComboBox<String> values) {
        List<TagMap> affectedKeysList = new ArrayList<>();

        UndoRedoHandler.getInstance().add(new SequenceCommand("Add Nodes", commandList));
        commandList.clear();

        // join newly created nodes to parent ways
        NodeUtils.moveOntoNearestWay(atNodes.get(0));
        NodeUtils.moveOntoNearestWay(atNodes.get(1));

        List<Node> nodelist1 = Arrays.asList(atNodes.get(0), commonNode);
        List<Node> nodelist2 = Arrays.asList(atNodes.get(1), commonNode);

        // required to be added to newly split way
        affectedKeysList.add(previousAffectedWay.getKeys());
        affectedKeysList.add(affected.getKeys());

        // split both the ways separately
        SplitWayCommand result1 = SplitWayCommand.split(previousAffectedWay, nodelist1, Collections.emptyList());
        SplitWayCommand result2 = SplitWayCommand.split(affected, nodelist2, Collections.emptyList());

        if (result1 != null)
            commandList.add(result1);
        else {
            UndoRedoHandler.getInstance().undo();
            resetLayer();
            return;
        }
        if (result2 != null)
            commandList.add(result2);
        else {
            UndoRedoHandler.getInstance().undo();
            resetLayer();
            return;
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Split way"), commandList));

        // add newly split way to relations
        final List<Relation> referrers1 = WayUtils.findPTRouteParents(previousAffectedWay);
        final Map<Relation, List<Integer>> indices1 = PrimitiveUtils.findIndicesOfPrimitiveInRelations(previousAffectedWay, referrers1);

        final List<Relation> referrers2 = WayUtils.findPTRouteParents(affected);
        final Map<Relation, List<Integer>> indices2 = PrimitiveUtils.findIndicesOfPrimitiveInRelations(affected, referrers2);

        Way way1 = null;
        Way way2 = null;

        // Find middle way which is a part of both the ways, so find the 2 ways which
        // together would form middle way
        boolean isOriginalWay = true; // we check both the original way and new ways
        for (Way way : result1.getNewWays()) {
            checkMembership(way, referrers1, indices1);
            if (way.containsNode(commonNode) && way.containsNode(atNodes.get(0))) {
                way1 = way;
                isOriginalWay = false;
                break;
            }
        }

        checkMembership(result1.getOriginalWay(), referrers1, indices1);

        if (isOriginalWay) {
            Way way = result1.getOriginalWay();
            if (way.containsNode(commonNode) && way.containsNode(atNodes.get(0))) {
                way1 = way;
            }
        }

        // now do for 2nd way
        isOriginalWay = true;

        for (Way way : result2.getNewWays()) {
            checkMembership(way, referrers2, indices2);
            if (way.containsNode(commonNode) && way.containsNode(atNodes.get(1))) {
                way2 = way;
                isOriginalWay = false;
                break;
            }
        }

        checkMembership(result2.getOriginalWay(), referrers2, indices2);

        if (isOriginalWay) {
            Way way = result2.getOriginalWay();
            if (way.containsNode(commonNode) && way.containsNode(atNodes.get(1))) {
                way2 = way;
            }
        }

        if (way1 != null && way2 != null) {
            List<Way> selectedWays = Arrays.asList(way1, way2);
            addTags(affectedKeysList, selectedWays, keys, values);
        } else {
            UndoRedoHandler.getInstance().undo();
            UndoRedoHandler.getInstance().undo();
            resetLayer();
        }
    }

    // this function is called when both nodes are starting and ending points of
    // same way, we dont split anything here
    private void addKeysWhenStartEndPoint(Way affected, JComboBox<String> keys, JComboBox<String> values) {
        NodeUtils.moveOntoNearestWay(atNodes.get(0));
        NodeUtils.moveOntoNearestWay(atNodes.get(1));

        if (affected == null) {
            resetLayer();
        } else {
            addTags(Collections.singletonList(affected.getKeys()), Collections.singletonList(affected), keys, values);
        }
    }

    // check if a way is present in a relation, if not then add it
    private static void checkMembership(Way way, List<Relation> referrers, Map<Relation, List<Integer>> index) {
        for (Relation r : referrers) {
            boolean isMember = false;
            for (RelationMember rm : r.getMembers()) {
                if (rm.getType() == OsmPrimitiveType.WAY && rm.getWay().equals(way)) {
                    isMember = true;
                }
            }
            if (!isMember) {
                for (int i = 0; i < index.get(r).size(); i++) {
                    r.addMember(index.get(r).get(i), new RelationMember("", way));
                }
            }
        }
    }

    // take key value pair from the dialog box and add it to the existing ways
    private void addTags(List<TagMap> affectedKeysList, List<Way> selectedWay, JComboBox<String> keys,
            JComboBox<String> values) {

        MainApplication.getLayerManager().getEditDataSet().setSelected(selectedWay);

        TagMap newKeys1 = affectedKeysList.get(0);
        String prevValue = null;

        if (BRIDGE.equals(keys.getSelectedItem())) {
            newKeys1.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
            newKeys1.put(LAYER, "1");
        } else if (TUNNEL.equals(keys.getSelectedItem())) {
            newKeys1.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
            if (!values.getSelectedItem().toString().equals(BUILDING_PASSAGE))
                newKeys1.put(LAYER, "-1");
        } else if (keys.getSelectedItem() == TRAFFIC_CALMING) {
            newKeys1.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
            newKeys1.put("maxspeed", "30");
        } else if (keys.getSelectedItem() != "none") {
            if (newKeys1.containsKey(BUS_BAY)) {
                prevValue = newKeys1.get(BUS_BAY);
                newKeys1.put(Objects.requireNonNull(keys.getSelectedItem()).toString(), BOTH);
            } else {
                newKeys1.put(Objects.requireNonNull(keys.getSelectedItem()).toString(),
                    Objects.requireNonNull(values.getSelectedItem()).toString());
            }
        }

        if (affectedKeysList.size() == 2) {
            TagMap newKeys2 = affectedKeysList.get(1);

            if (keys.getSelectedItem() == BRIDGE) {
                newKeys2.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
                newKeys2.put(LAYER, "1");
            } else if (keys.getSelectedItem() == TUNNEL) {
                newKeys2.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
                if (!values.getSelectedItem().toString().equals(BUILDING_PASSAGE))
                    newKeys2.put(LAYER, "-1");
            } else if (keys.getSelectedItem() == TRAFFIC_CALMING) {
                newKeys2.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
                newKeys2.put("maxspeed", "30");
            } else if (keys.getSelectedItem() != "none") {
                if (newKeys2.containsKey(BUS_BAY)) {
                    prevValue = newKeys2.get(BUS_BAY);
                    newKeys2.put(keys.getSelectedItem().toString(), BOTH);
                    if (Objects.equals(values.getSelectedItem(), LEFT) && prevValue.equals(LEFT))
                        newKeys1.put(BUS_BAY, RIGHT);
                    else if (Objects.equals(values.getSelectedItem(), RIGHT) && prevValue.equals(RIGHT))
                        newKeys1.put(BUS_BAY, LEFT);
                } else {
                    if (newKeys1.get(BUS_BAY).equals(BOTH)) {
                        if (Objects.equals(values.getSelectedItem(), LEFT) && Objects.equals(prevValue, LEFT))
                            newKeys2.put(BUS_BAY, RIGHT);
                        else if (Objects.equals(values.getSelectedItem(), RIGHT) && Objects.equals(prevValue, RIGHT))
                            newKeys2.put(BUS_BAY, LEFT);
                        else
                            newKeys2.put(keys.getSelectedItem().toString(), values.getSelectedItem().toString());
                    } else {
                        newKeys2.put(keys.getSelectedItem().toString(), Objects.requireNonNull(values.getSelectedItem()).toString());
                    }
                }
            }
            UndoRedoHandler.getInstance().add(
                new ChangePropertyCommand(Collections.singleton(selectedWay.get(1)), newKeys2)
            );
        }
        UndoRedoHandler.getInstance().add(new ChangePropertyCommand(Collections.singleton(selectedWay.get(0)), newKeys1));
        resetLayer();
    }

    // to find if there is any highway, railway, waterway crossing the way
    private void findIntersection(Set<Way> newWays) {
        try {
            DataSet ds = getLayerManager().getEditDataSet();
            addWaysIntersectingWays(ds.getWays(), Arrays.asList(previousAffectedWay, affected), newWays);
            Node n1 = previousAffectedWay.firstNode();
            Node n2 = previousAffectedWay.lastNode();
            Node n3 = affected.firstNode();
            Node n4 = affected.lastNode();
            List<Way> waysToBeRemoved = new ArrayList<>();
            for (Way way : newWays) {
                int count = 0;
                if (way.containsNode(n1))
                    count++;
                if (way.containsNode(n2))
                    count++;
                if (!previousAffectedWay.equals(affected)) {
                    if (way.containsNode(n3))
                        count++;
                    if (way.containsNode(n4))
                        count++;
                }
                if (count == 1) {
                    waysToBeRemoved.add(way);
                } else {
                    if (!way.hasKey("highway") && !way.hasKey(WATERWAY) && !way.hasKey("railway")) {
                        waysToBeRemoved.add(way);
                    }
                }
            }
            newWays.removeAll(waysToBeRemoved);
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    void addWaysIntersectingWay(Collection<Way> ways, Way w, Set<Way> newWays) {
        List<Pair<Node, Node>> nodePairs = w.getNodePairs(false);
        for (Way anyway : ways) {
            if (Objects.equals(anyway, w))
                continue;
            if (newWays.contains(anyway))
                continue;
            List<Pair<Node, Node>> nodePairs2 = anyway.getNodePairs(false);
            loop: for (Pair<Node, Node> p1 : nodePairs) {
                for (Pair<Node, Node> p2 : nodePairs2) {
                    if (null != Geometry.getSegmentSegmentIntersection(p1.a.getEastNorth(), p1.b.getEastNorth(),
                            p2.a.getEastNorth(), p2.b.getEastNorth())) {
                        newWays.add(anyway);
                        break loop;
                    }
                }
            }
        }
    }

    void addWaysIntersectingWays(Collection<Way> allWays, Collection<Way> initWays, Set<Way> newWays) {
        for (Way w : initWays) {
            addWaysIntersectingWay(allWays, w, newWays);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        // while the mouse is moving, surroundings are checked
        // if anything is found, it will be highlighted.
        // priority is given to nodes
        Cursor newCurs = getCursor();

        Node n = MainApplication.getMap().mapView.getNearestNode(e.getPoint(), OsmPrimitive::isUsable);
        if (n != null) {
            newHighlights.add(n);
            newCurs = cursorJoinNode;
        } else {
            List<WaySegment> wss = MainApplication.getMap().mapView.getNearestWaySegments(e.getPoint(),
                    OsmPrimitive::isSelectable);

            if (!wss.isEmpty()) {
                for (IWaySegment<?, Way> ws : wss) {
                    newHighlights.add(ws.getWay());
                }
                newCurs = cursorJoinWay;
            }
        }

        MainApplication.getMap().mapView.setCursor(newCurs);
        updateHighlights();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (Pos1 == null) {
            SegWay1 = MainApplication.getMap().mapView.getNearestWay(e.getPoint(), OsmPrimitive::isSelectable);
            if (SegWay1 != null) {
                Pos1 = MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY());
            }
        } else if (Pos2 == null) {
            LatLon tempPos = MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY());
            if (Pos1.lat() != tempPos.lat() || Pos1.lon() != tempPos.lon()) {
                SegWay2 = MainApplication.getMap().mapView.getNearestWay(e.getPoint(), OsmPrimitive::isSelectable);
                if (SegWay2 != null) {
                    Pos2 = MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY());
                }
            }
        }

        if (Pos2 != null) {
            reset();
            action();
        }
    }

    // turn off what has been highlighted on last mouse move and highlight what has
    // to be highlighted now
    private void updateHighlights() {
        if ((oldHighlights == null || oldHighlights.isEmpty()) && (newHighlights == null || newHighlights.isEmpty())) {
            return;
        }

        if (oldHighlights != null) {
            for (OsmPrimitive osm : oldHighlights) {
                osm.setHighlighted(false);
            }
        }


        for (OsmPrimitive osm : newHighlights) {
            osm.setHighlighted(true);
        }

        MainApplication.getLayerManager().getEditLayer().invalidate();

        assert oldHighlights != null;
        oldHighlights.clear();
        oldHighlights.addAll(newHighlights);
        newHighlights.clear();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        boolean z = e.getKeyCode() == KeyEvent.VK_Z;
        updateKeyModifiers(e);
        if (z && Pos1 != null && Pos2 == null) {
            Pos1 = null;
            SegWay1 = null;
            temporaryLayer.invalidate();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Do nothing
    }

    // A dialogBox to query whether to select bus_bay, tunnel or bridge.

    private class SelectFromOptionDialog extends ExtendedDialog {
        private static final int TYPE_NODES_ON_ADJACENT_WAYS = 1;
        private static final int TYPE_NODES_ON_SAME_WAY = 2;
        private static final int TYPE_NODES_ARE_ENDS_OF_SAME_WAY = 3;

        Way affected;
        Way previousAffectedWay;
        private final JComboBox<String> keys;
        private final JComboBox<String> values;
        private final int type;
        private final List<Command> commandList;
        private final Node commonNode;
        private boolean rightHandTraffic;

        SelectFromOptionDialog(int type, Node commonNode, Way affected, Way previousAffectedWay,
                List<Command> commandList) {
            super(
                MainApplication.getMainFrame(),
                tr("What do you want the segment to be?"),
                new String[] {tr("Ok"), tr("Cancel") },
                true
            );
            this.affected = affected;
            this.previousAffectedWay = previousAffectedWay;
            this.commandList = commandList;
            this.type = type;
            this.commonNode = commonNode;

            rightHandTraffic = true;
            for (Node n : atNodes) {
                if (!RightAndLefthandTraffic.isRightHandTraffic(n.getCoor())) {
                    rightHandTraffic = false;
                    break;
                }
            }

            setButtonIcons("ok", "cancel");
            setCancelButton(2);
            configureContextsensitiveHelp("/Dialog/AddValue", true /* show help button */);

            final JPanel pane = new JPanel(new GridBagLayout());
            pane.add(new JLabel("Select the appropriate option"), GBC.eol().fill(GridBagConstraints.HORIZONTAL));

            keys = new JComboBox<>();
            values = new JComboBox<>();
            keys.setEditable(true);
            Set<Way> newWays = new HashSet<>();
            findIntersection(newWays);

            if (previousAffectedWay.hasKey(WATERWAY) || affected.hasKey(WATERWAY)) {
                setOptionsWithTunnel();
            } else if (previousAffectedWay.hasKey(BUS_BAY) || affected.hasKey(BUS_BAY)) {
                setOptionsWithBusBay();
            } else if (!newWays.isEmpty()) {
                setOptionsWithBridge();
            } else {
                setOptionsWithBusBay();
            }

            pane.add(keys, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
            pane.add(values, GBC.eop().fill(GridBagConstraints.HORIZONTAL));

            setContent(pane, false);
            setDefaultCloseOperation(HIDE_ON_CLOSE);
        }

        private void setOptionsWithBusBay() {
            keys.setModel(
                    new DefaultComboBoxModel<>(new String[] {BUS_BAY, BRIDGE, TUNNEL, TRAFFIC_CALMING, "none" }));

            if (affected.hasTag(BUS_BAY, RIGHT) || previousAffectedWay.hasTag(BUS_BAY, RIGHT)) {
                values.setModel(new DefaultComboBoxModel<>(new String[] {LEFT, RIGHT, BOTH }));
            } else if (affected.hasTag(BUS_BAY, LEFT) || previousAffectedWay.hasTag(BUS_BAY, LEFT)) {
                values.setModel(new DefaultComboBoxModel<>(new String[] {RIGHT, LEFT, BOTH }));
            } else if (rightHandTraffic) {
                values.setModel(new DefaultComboBoxModel<>(new String[] {RIGHT, LEFT, BOTH }));
            } else {
                values.setModel(new DefaultComboBoxModel<>(new String[] {LEFT, RIGHT, BOTH }));
            }

            // below code changes the list in values on the basis of key
            keys.addActionListener(e -> {
                if (BUS_BAY.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {BOTH, RIGHT, LEFT}));
                    if (rightHandTraffic)
                        values.setModel(new DefaultComboBoxModel<>(new String[] {RIGHT, LEFT, BOTH}));
                    else
                        values.setModel(new DefaultComboBoxModel<>(new String[] {LEFT, RIGHT, BOTH}));
                } else if (BRIDGE.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"yes" }));
                } else if (TUNNEL.equals(keys.getSelectedItem())) {
                    if (previousAffectedWay.hasKey(WATERWAY) || affected.hasKey(WATERWAY))
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {CULVERT, "yes", BUILDING_PASSAGE }));
                    else
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {"yes", CULVERT, BUILDING_PASSAGE }));
                } else if (TRAFFIC_CALMING.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {TABLE }));
                } else if ("none".equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"" }));
                }
            });
        }

        private void setOptionsWithTunnel() {
            keys.setModel(
                    new DefaultComboBoxModel<>(new String[] {TUNNEL, BRIDGE, BUS_BAY, TRAFFIC_CALMING, "none" }));

            if (previousAffectedWay.hasKey(WATERWAY) || affected.hasKey(WATERWAY))
                values.setModel(new DefaultComboBoxModel<>(new String[] {CULVERT, "yes", BUILDING_PASSAGE }));
            else
                values.setModel(new DefaultComboBoxModel<>(new String[] {"yes", CULVERT, BUILDING_PASSAGE }));

            // below code changes the list in values on the basis of key
            keys.addActionListener(e -> {
                if (TUNNEL.equals(keys.getSelectedItem())) {
                    if (previousAffectedWay.hasKey(WATERWAY) || affected.hasKey(WATERWAY))
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {CULVERT, "yes", BUILDING_PASSAGE }));
                    else
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {"yes", CULVERT, BUILDING_PASSAGE }));
                } else if (BUS_BAY.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {BOTH, RIGHT, LEFT }));
                    if (rightHandTraffic)
                        values.setModel(new DefaultComboBoxModel<>(new String[] {RIGHT, LEFT, BOTH }));
                    else
                        values.setModel(new DefaultComboBoxModel<>(new String[] {LEFT, RIGHT, BOTH }));
                } else if (BRIDGE.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"yes" }));
                } else if (TRAFFIC_CALMING.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {TABLE }));
                } else if ("none".equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"" }));
                }
            });
        }

        private void setOptionsWithBridge() {
            keys.setModel(
                    new DefaultComboBoxModel<>(new String[] {BRIDGE, BUS_BAY, TUNNEL, TRAFFIC_CALMING, "none" }));

            values.setModel(new DefaultComboBoxModel<>(new String[] {"yes" }));

            // below code changes the list in values on the basis of key
            keys.addActionListener(e -> {
                if (TUNNEL.equals(keys.getSelectedItem())) {
                    if (previousAffectedWay.hasKey(WATERWAY) || affected.hasKey(WATERWAY))
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {CULVERT, "yes", BUILDING_PASSAGE }));
                    else
                        values.setModel(
                                new DefaultComboBoxModel<>(new String[] {"yes", CULVERT, BUILDING_PASSAGE }));
                } else if (BUS_BAY.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {BOTH, RIGHT, LEFT }));
                    if (rightHandTraffic)
                        values.setModel(new DefaultComboBoxModel<>(new String[] {RIGHT, LEFT, BOTH }));
                    else
                        values.setModel(new DefaultComboBoxModel<>(new String[] {LEFT, RIGHT, BOTH }));
                } else if (BRIDGE.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"yes" }));
                } else if (TRAFFIC_CALMING.equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {TABLE }));
                } else if ("none".equals(keys.getSelectedItem())) {
                    values.setModel(new DefaultComboBoxModel<>(new String[] {"" }));
                }
            });
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            super.buttonAction(buttonIndex, evt);
            toggleSaveState(); // necessary since #showDialog() does not handle it due to the non-modal dialog

            if (getValue() == 1) {
                if (this.type == TYPE_NODES_ON_ADJACENT_WAYS) {
                    addKeysOnBothWays(this.commonNode, this.affected, this.previousAffectedWay, this.commandList, keys,
                            values);
                } else if (this.type == TYPE_NODES_ON_SAME_WAY) {
                    addKeys(this.affected, this.commandList, keys, values);
                } else if (this.type == TYPE_NODES_ARE_ENDS_OF_SAME_WAY) {
                    addKeysWhenStartEndPoint(this.affected, keys, values);
                }

            } else if (getValue() != 3) {
                resetLayer();
            }
        }
    }

    private class DoubleSplitLayer extends AbstractMapViewPaintable {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            if (Pos1 != null) {
                Point curP1 = MainApplication.getMap().mapView.getPoint(Pos1);
                CheckParameterUtil.ensureParameterNotNull(mv, "mv");
                g.setColor(Color.RED);
                g.fill(new Rectangle2D.Double(curP1.x - 3d, curP1.y - 3d, 6, 6));
            }
            if (Pos2 != null) {
                Point curP2 = MainApplication.getMap().mapView.getPoint(Pos2);
                CheckParameterUtil.ensureParameterNotNull(mv, "mv");
                g.setColor(Color.RED);
                g.fill(new Rectangle2D.Double(curP2.x - 3d, curP2.y - 3d, 6, 6));
            }
        }
    }

}
