// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The AddStopPositionAction is a mapmode that allows users to add
 * new stop_positions or to convert already existing nodes.
 *
 * @author giacomo
 */
public class AddStopPositionAction extends MapMode {

    private static final String CROSSHAIR_IMAGE_FILENAME = "crosshair";
    private static final Cursor CURSOR_JOIN_NODE = ImageProvider.getCursor(CROSSHAIR_IMAGE_FILENAME, "joinnode");
    private static final Cursor CURSOR_JOIN_WAY = ImageProvider.getCursor(CROSSHAIR_IMAGE_FILENAME, "joinway");
    private static final Cursor CURSOR_BUS = ImageProvider.getCursor(CROSSHAIR_IMAGE_FILENAME, "bus");

    private transient Set<OsmPrimitive> newHighlights = new HashSet<>();
    private transient Set<OsmPrimitive> oldHighlights = new HashSet<>();


    /**
     * Creates a new AddStopPositionAction
     */
    public AddStopPositionAction() {
        // CHECKSTYLE.OFF: LineLength
        super(
            tr("Add stop position"),
            "bus",
            tr("Add stop position"),
            Shortcut.registerShortcut("mapmode:stop_position", tr("Mode: {0}", tr("Add stop position")), KeyEvent.VK_K, Shortcut.CTRL_SHIFT),
            getCursor()
        );
        // CHECKSTYLE.ON: LineLength
    }

    private static Cursor getCursor() {
        return CURSOR_BUS == null ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : CURSOR_BUS;
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MainApplication.getMap().mapView.addMouseListener(this);
        MainApplication.getMap().mapView.addMouseMotionListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
        MainApplication.getMap().mapView.removeMouseMotionListener(this);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        //while the mouse is moving, surroundings are checked
        //if anything is found, it will be highlighted.
        //priority is given to nodes
        Cursor newCurs = getCursor();

        Node n = MainApplication.getMap().mapView.getNearestNode(e.getPoint(), OsmPrimitive::isUsable);
        if (n != null) {
            newHighlights.add(n);
            newCurs = CURSOR_JOIN_NODE;
        } else {
            List<WaySegment> wss =
                MainApplication.getMap().mapView.getNearestWaySegments(e.getPoint(), OsmPrimitive::isSelectable);

            if (!wss.isEmpty()) {
                for (WaySegment ws : wss) {
                    newHighlights.add(ws.way);
                }
                newCurs = CURSOR_JOIN_WAY;
            }
        }

        MainApplication.getMap().mapView.setCursor(newCurs);
        updateHighlights();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        Boolean newNode = false;
        Node newStopPos;

        //check if the user as selected an existing node, or a new one
        Node n = MainApplication.getMap().mapView.getNearestNode(e.getPoint(), OsmPrimitive::isUsable);
        if (n == null) {
            newNode = true;
            newStopPos = new Node(MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY()));
        } else {
            newStopPos = new Node(n);
        }

        if (newNode) {
            UndoRedoHandler.getInstance().add(new AddCommand(getLayerManager().getEditDataSet(), newStopPos));
        } else {
            UndoRedoHandler.getInstance().add(new ChangeCommand(n, newStopPos));
            newStopPos = n;
        }

        MainApplication.getLayerManager().getEditDataSet().setSelected(newStopPos);

        //join the node to the way only if the node is new
        if (newNode) {
            JoinNodeWayAction joinNodeWayAction = JoinNodeWayAction.createMoveNodeOntoWayAction();
            joinNodeWayAction.actionPerformed(null);
        }

        //add the tags of the stop position
        HashMap<String, String> tagsForNode = new HashMap<>(newStopPos.getKeys());
        if (PTAssistantPluginPreferences.SPLITWAY_1.get()) {
            for (Way w : newStopPos.getParentWays()) {
                for (Relation r : WayUtils.findPTRouteParents(w)) {
                    if (r.hasKey("route")) {
                        tagsForNode.put(r.get("route"), "yes");
                    }
                }
            }
            tagsForNode.put("public_transport", "stop_position");
            UndoRedoHandler.getInstance().add(new ChangePropertyCommand(Collections.singleton(newStopPos), tagsForNode));
        }

        if (newStopPos.getParentWays().isEmpty())
            return;

        Way affected = newStopPos.getParentWays().get(0);

        Map<Relation, Boolean> needPostProcess = getAffectedRelation(affected, PTAssistantPluginPreferences.SPLITWAY_2.get());

        if (needPostProcess.isEmpty())
            return;

        SplitWayCommand result = SplitWayCommand.split(
                affected, Collections.singletonList(newStopPos), Collections.emptyList());
        if (result == null) //if the way is already split, return
            return;
        UndoRedoHandler.getInstance().add(result);

        List<Command> cmds = new ArrayList<>();
        for (Entry<Relation, Boolean> route : needPostProcess.entrySet()) {
            Relation r = new Relation(route.getKey());
            if (route.getValue() != null) {
                if (route.getValue()) {
                    deleteFirstWay(r);
                } else {
                    deleteLastWay(r);
                }
                cmds.add(new ChangeCommand(route.getKey(), r));
            }

        }
        UndoRedoHandler.getInstance().add(new SequenceCommand("Update PT Relations", cmds));
    }

    private void deleteLastWay(Relation r) {
        int delete = 0;
        for (int i = r.getMembersCount() - 1; i >= 0; i--) {
            RelationMember rm = r.getMember(i);
            if (rm.getType() == OsmPrimitiveType.WAY &&
                    !PTStop.isPTPlatform(rm)) {
                delete = i;
                break;
            }
        }
        r.removeMember(delete);
    }

    private void deleteFirstWay(Relation r) {
        int delete = 0;
        for (int i = 0; i < r.getMembersCount(); i++) {
            RelationMember rm = r.getMember(i);
            if (rm.getType() == OsmPrimitiveType.WAY &&
                    !PTStop.isPTPlatform(rm)) {
                delete = i;
                break;
            }
        }
        r.removeMember(delete);
    }

    private Map<Relation, Boolean> getAffectedRelation(Way affected, boolean bool) {
        if (bool == false) {
            Map<Relation, Boolean> ret = new HashMap<>();
            for (Relation route : WayUtils.findPTRouteParents(affected)) {
                if (isFirstMember(affected, route)) {
                    ret.put(route, true);
                } else if (isLastMember(affected, route)) {
                    ret.put(route, false);
                }
            }
            return ret;
        } else {
            Map<Relation, Boolean> ret = new HashMap<>();
            for (Relation route : WayUtils.findPTRouteParents(affected)) {
                ret.put(route, null);
            }
            return ret;
        }
    }

    private boolean isFirstMember(Way affected, Relation route) {
        for (int i = 0; i < route.getMembersCount(); i++) {
            RelationMember rm = route.getMember(i);
            if (rm.getMember().equals(affected)) {
                return true;
            } else if (rm.getType() == OsmPrimitiveType.WAY &&
                    !PTStop.isPTPlatform(rm)) {
                return false;
            }
        }
        return true;
    }

    private boolean isLastMember(Way affected, Relation route) {
        for (int i = route.getMembersCount() - 1; i >= 0; i--) {
            RelationMember rm = route.getMember(i);
            if (rm.getMember().equals(affected)) {
                return true;
            } else if (rm.getType() == OsmPrimitiveType.WAY &&
                    !PTStop.isPTPlatform(rm)) {
                return false;
            }
        }

        return true;
    }

    /**
     * turn off what has been highlighted on last mouse move and highlight what has to be highlighted now
     */
    private synchronized void updateHighlights() {
        if (oldHighlights.isEmpty() && newHighlights.isEmpty()) {
            return;
        }
        oldHighlights.forEach(it -> it.setHighlighted(false));
        newHighlights.forEach(it -> it.setHighlighted(true));

        MainApplication.getLayerManager().getEditLayer().invalidate();

        oldHighlights.clear();
        oldHighlights.addAll(newHighlights);
        newHighlights.clear();
    }
}
