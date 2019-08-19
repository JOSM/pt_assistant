// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RemoveNodesCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Extracts node from its ways and adds the public transport tags to it.
 */
public class ExtractPlatformNodeAction extends JosmAction {

    /**
     * Constructs a new {@code ExtractPointAction}.
     */
    public ExtractPlatformNodeAction() {
        // CHECKSTYLE.OFF: LineLength
        super(
            tr("Extract platform node"),
            null,
            tr("Extracts platform node from a node"),
            Shortcut.registerShortcut("tools:extplatformnode", "Tool: Extract platform node", KeyEvent.VK_G, Shortcut.ALT_CTRL),
            false
        );
        putValue("help", ht("/Action/ExtractPlatformNode"));
        MainApplication.registerActionShortcut(
            this,
            Shortcut.registerShortcut("tools:extplatformnode", "Tool: ExtractPlatformNodeAction", KeyEvent.VK_G, Shortcut.ALT_CTRL)
        );
        // CHECKSTYLE.ON: LineLength
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selection = ds.getSelected();
        List<Node> selectedNodes = new ArrayList<>(Utils.filteredCollection(selection, Node.class));
        if (selectedNodes.size() != 1) {
            new Notification(tr("This action requires single node to be selected."))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .show();
            return;
        }
        final Node nd = selectedNodes.get(0);

        if (!StopUtils.isHighwayOrRailwayStopPosition(nd)) {
            new Notification(tr("The selected node is not a stop position for public transport!"))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .show();
            return;
        }

        List<Command> cmds = new LinkedList<>();

        Point p = MainApplication.getMap().mapView.getMousePosition();
        if (p == null)
            return;

        List<OsmPrimitive> refs = nd.getReferrers();
        boolean isFirstLastNode = false;

        for (OsmPrimitive pr : refs) {
            if (pr instanceof Way) {
                Way w = (Way) pr;
                if (w.firstNode().equals(nd) || w.lastNode().equals(nd)) {
                    isFirstLastNode = true;
                }
            }
        }

        if (!isFirstLastNode) {
            cmds.add(new MoveCommand(nd, MainApplication.getMap().mapView.getLatLon(p.x, p.y)));
            for (OsmPrimitive pr : refs) {
                if (pr instanceof Way) {
                    Way w = (Way) pr;
                    UndoRedoHandler.getInstance().add(new RemoveNodesCommand(w, Collections.singleton(nd)));
                }
            }
        } else {
            Node newNode = new Node(MainApplication.getMap().mapView.getLatLon(p.x, p.y));
            UndoRedoHandler.getInstance().add(new AddCommand(getLayerManager().getEditDataSet(), newNode));
            UndoRedoHandler.getInstance()
                    .add(new ChangePropertyCommand(Collections.singleton(newNode), new HashMap<>(nd.getKeys())));
            CreatePlatformNodeThroughReplaceAction cpsa = new CreatePlatformNodeThroughReplaceAction();
            cpsa.modify(newNode, nd);
            return;
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Extract node from line"), cmds));

        if (nd.hasTag("railway")) {
            ArrayList<Command> undoCommands = new ArrayList<>();
            undoCommands.add(new ChangePropertyCommand(nd, "public_transport", "platform"));
            undoCommands.add(new ChangePropertyCommand(nd, "tram", "yes"));
            undoCommands.add(new ChangePropertyCommand(nd, "railway", "tram_stop"));
            UndoRedoHandler.getInstance().add(new SequenceCommand("tag", undoCommands));
        } else if (nd.hasTag("highway")) {
            ArrayList<Command> undoCommands = new ArrayList<>();
            undoCommands.add(new ChangePropertyCommand(nd, "public_transport", "platform"));
            undoCommands.add(new ChangePropertyCommand(nd, "bus", "yes"));
            undoCommands.add(new ChangePropertyCommand(nd, "highway", "bus_stop"));
            UndoRedoHandler.getInstance().add(new SequenceCommand("tag", undoCommands));
        }
    }

}
