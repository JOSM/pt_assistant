// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.routines.RegexValidator;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTProperties;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sorts the stop positions in a PT route according to the assigned ways
 *
 * @author Polyglot
 *
 */
public class CreatePlatformNodeAction extends JosmAction {

    private Node dummy1;
    private Node dummy2;
    private Node dummy3;
    private JCheckBox transferDetails;

    /**
     * Creates a new PlatformAction
     */
    public CreatePlatformNodeAction() {
        super(tr("Transfer details of stop to platform node"), "icons/transfertags", tr("Transfer details of stop to platform node"), null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel panel = new JPanel(new GridBagLayout());
        transferDetailsDialog transferDialog = new transferDetailsDialog();
        transferDialog.setPreferredSize(new Dimension(500, 300));
        transferDialog.toggleEnable("toggle-transfer-details-dialog");
        transferDialog.setButtonIcons("ok", "cancel");
        transferDetails = new JCheckBox(tr("Remove public_transport=platform from platform WAYS when transferring details to platform NODE."));
        panel.add(transferDetails);
        JScrollPane scrollPanel = new JScrollPane(panel);
        transferDialog.setContent(scrollPanel, true);
        if (transferDialog.toggleCheckState()) {
            action();
        } else {
            ExtendedDialog dialog = transferDialog.showDialog();
            switch (dialog.getValue()) {
                case 1:
                    addToPreferences();
                    break;
                default:
                    return; // Do nothing
            }
        }
    }

    private void action() {
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();
        Node platformNode = null;
        Node stopPositionNode = null;
        Way platformWay = null;

        for (OsmPrimitive item : selection) {
            if (item.getType() == OsmPrimitiveType.NODE) {
                if (StopUtils.isStopPosition(item)) {
                    stopPositionNode = (Node) item;
                } else {
                    platformNode = (Node) item;
                }
            } else if (item.getType() == OsmPrimitiveType.WAY && item.hasTag("public_transport", "platform")) {
                platformWay = (Way) item;
            }
        }

        SortedSet<String> refs = new TreeSet<>();
        DataSet ds = getLayerManager().getEditDataSet();

        if (platformWay != null && platformNode != null) {
            List<Command> cmdList = new ArrayList<>();
            if (PTProperties.TRANSFER_PLATFORMWAY_TAG.get()) {
                HashMap<String, String> tagsForWay = new HashMap<>();
                HashMap<String, String> tagsForNode = new HashMap<>(platformNode.getKeys());

                HashMap<String, String> nodeTagsRemove = new HashMap<>(platformNode.getKeys());
                nodeTagsRemove.replaceAll((key, value) -> null);

                HashMap<String, String> wayTagsRemove = new HashMap<>(platformWay.getKeys());
                wayTagsRemove.replaceAll((key, value) -> null);

                tagsForNode.putAll(platformWay.getKeys());
                if (!PTAssistantPluginPreferences.ADD_MODE_OF_TRANSPORT.get()) {
                    tagsForNode.replace("public_transport", "platform");
                } else {
                    tagsForNode.remove("public_transport", "platform");
                    if (tagsForNode.containsKey("bus")) {
                        tagsForNode.remove("bus", "yes");
                        tagsForNode.put("highway", "bus_stop");
                    }
                    if (tagsForNode.containsKey("tram")) {
                        tagsForNode.remove("tram", "yes");
                        tagsForNode.put("railway", "tram_stop");
                    }
                }

                tagsForNode.remove("amenity", "shelter");
                tagsForNode.remove("shelter_type", "public_transport");

                if (platformWay.hasTag("highway", "platform"))
                    tagsForWay.put("highway", "platform");
                if (platformWay.hasTag("railway", "platform"))
                    tagsForWay.put("railway", "platform");
                if (platformWay.hasTag("tactile_paving"))
                    tagsForWay.put("tactile_paving", platformWay.get("tactile_paving"));
                if (platformWay.hasTag("wheelchair"))
                    tagsForWay.put("wheelchair", platformWay.get("wheelchair"));

                // if the user has wants to keep the tag
                if (!PTAssistantPluginPreferences.TRANSFER_DETAILS.get())
                    tagsForWay.put("public_transport", "platform");
                else
                    tagsForWay.remove("public_transport", "platform");

                cmdList.add(new ChangePropertyCommand(Collections.singleton(platformNode), nodeTagsRemove));
                cmdList.add(new ChangePropertyCommand(Collections.singleton(platformWay), wayTagsRemove));
                UndoRedoHandler.getInstance().add(new SequenceCommand("Remove Tags", cmdList));
                cmdList.clear();

                cmdList.add(new ChangePropertyCommand(Collections.singleton(platformNode), tagsForNode));
                cmdList.add(new ChangePropertyCommand(Collections.singleton(platformWay), tagsForWay));
                UndoRedoHandler.getInstance().add(new SequenceCommand("Change Tags", cmdList));
                cmdList.clear();

            }

            // based on the user's response decide whether to transfer the relations or not
            if (PTProperties.SUBSTITUTE_PLATFORMWAY_RELATION.get()) {
                Map<Relation, List<Integer>> savedPositions = getSavedPositions(platformWay);
                List<Relation> parentStopAreaRelation = removeWayFromRelationsCommand(platformWay);
                cmdList.addAll(updateRelation(savedPositions, platformNode, platformWay, parentStopAreaRelation));
                UndoRedoHandler.getInstance().add(new SequenceCommand("Update Relations", cmdList));
            }
        }

        if (platformNode != null && stopPositionNode != null && PTProperties.TRANSFER_STOPPOSITION_TAG.get()) {
            dummy1 = new Node(platformNode.getEastNorth());
            dummy2 = new Node(platformNode.getEastNorth());
            dummy3 = new Node(platformNode.getEastNorth());

            UndoRedoHandler.getInstance().add(new AddCommand(ds, dummy1));
            UndoRedoHandler.getInstance().add(new AddCommand(ds, dummy2));
            UndoRedoHandler.getInstance().add(new AddCommand(ds, dummy3));

            refs.addAll(populateMap(stopPositionNode));
            refs.addAll(populateMap(platformNode));

            stopPositionNode.removeAll();
            stopPositionNode.put("bus", "yes");
            stopPositionNode.put("public_transport", "stop_position");

            platformNode.removeAll();
            platformNode.put("public_transport", "platform");
            platformNode.put("highway", "bus_stop");
            if (!refs.isEmpty()) {
                platformNode.put("route_ref", getRefs(refs));
            }

            if (PTAssistantPluginPreferences.ADD_MODE_OF_TRANSPORT.get()) {
                stopPositionNode.remove("public_transport");
                if (stopPositionNode.hasTag("bus")) {
                    stopPositionNode.remove("bus");
                    stopPositionNode.put("highway", "bus_stop");
                }
                if (stopPositionNode.hasTag("tram")) {
                    stopPositionNode.remove("tram");
                    stopPositionNode.put("railway", "tram_stop");
                }
            }

            List<OsmPrimitive> prims = new ArrayList<>();
            prims.add(platformNode);
            prims.add(dummy1);
            prims.add(dummy2);
            prims.add(dummy3);

            try {
                TagCollection tagColl = TagCollection.unionOfAllPrimitives(prims);
                List<Command> cmds = CombinePrimitiveResolverDialog.launchIfNecessary(tagColl, prims,
                        Collections.singleton(platformNode));
                UndoRedoHandler.getInstance().add(new SequenceCommand("merging", cmds));
            } catch (UserCancelException ex) {
                Logging.trace(ex);
            } finally {
                UndoRedoHandler.getInstance().add(new DeleteCommand(dummy1));
                UndoRedoHandler.getInstance().add(new DeleteCommand(dummy2));
                UndoRedoHandler.getInstance().add(new DeleteCommand(dummy3));
            }
        }
    }

    public List<String> populateMap(OsmPrimitive prim) {
        List<String> unInterestingTags = new ArrayList<>();
        unInterestingTags.add("public_transport");
        unInterestingTags.add("highway");
        unInterestingTags.add("source");

        List<String> refs = new ArrayList<>();
        for (Entry<String, String> tag : prim.getKeys().entrySet()) {
            if ("note".equals(tag.getKey()) || "line".equals(tag.getKey()) || "lines".equals(tag.getKey())
                    || "route_ref".equals(tag.getKey())) {
                refs.addAll(addRefs(tag.getValue()));
                continue;
            }

            if (!unInterestingTags.contains(tag.getKey())) {
                if (dummy1.get(tag.getKey()) == null) {
                    dummy1.put(tag.getKey(), tag.getValue());
                } else if (dummy2.get(tag.getKey()) == null) {
                    dummy2.put(tag.getKey(), tag.getValue());
                } else if (dummy3.get(tag.getKey()) == null) {
                    dummy3.put(tag.getKey(), tag.getValue());
                }
            }
        }
        return refs;
    }

    private static List<String> addRefs(String value) {
        List<String> refs = new ArrayList<>();
        if (new RegexValidator("\\w+([,;].+)*").isValid(value)) {
            for (String ref : value.split("[,;]")) {
                refs.add(ref.trim());
            }
        }
        return refs;
    }

    private static String getRefs(Set<String> refs) {
        StringBuilder sb = new StringBuilder();
        if (refs.isEmpty())
            return sb.toString();

        for (String ref : refs) {
            sb.append(ref).append(';');
        }

        return sb.toString().substring(0, sb.length() - 1);
    }

    private static List<Relation> removeWayFromRelationsCommand(Way way) {
        List<Command> commands = new ArrayList<>();
        List<Relation> referrers = new ArrayList<>(Utils.filteredCollection(way.getReferrers(), Relation.class));
        List<Relation> parentStopAreaRelation = new ArrayList<>();
        referrers.forEach(r -> {
            if (StopUtils.isStopArea(r)) {
                parentStopAreaRelation.add(r);
            }
            Relation c = new Relation(r);
            c.removeMembersFor(way);
            commands.add(new ChangeCommand(r, c));
        });

        UndoRedoHandler.getInstance().add(new SequenceCommand("Remove way from relations", commands));

        return parentStopAreaRelation;
    }

    private static Map<Relation, List<Integer>> getSavedPositions(Way way) {

        Map<Relation, List<Integer>> savedPositions = new HashMap<>();
        List<Relation> referrers = new ArrayList<>(Utils.filteredCollection(way.getReferrers(), Relation.class));

        for (Relation curr : referrers) {
            for (int j = 0; j < curr.getMembersCount(); j++) {
                if (curr.getMember(j).getUniqueId() == way.getUniqueId()) {
                    if (!savedPositions.containsKey(curr))
                        savedPositions.put(curr, new ArrayList<>());
                    List<Integer> positions = savedPositions.get(curr);
                    positions.add(j - positions.size());
                }
            }
        }
        return savedPositions;
    }

    private static List<Command> updateRelation(Map<Relation, List<Integer>> savedPositions, Node platformNode,
            Way platformWay, List<Relation> parentStopAreaRelation) {
        Map<Relation, Relation> changingRelation = new HashMap<>();
        Map<Relation, Integer> memberOffset = new HashMap<>();
        List<Relation> referrers = new ArrayList<>(Utils.filteredCollection(platformNode.getReferrers(), Relation.class));

        savedPositions.forEach((r, positions) -> positions.forEach(i -> {
            if (!changingRelation.containsKey(r))
                changingRelation.put(r, new Relation(r));

            Relation c = changingRelation.get(r);

            if (!memberOffset.containsKey(r))
                memberOffset.put(r, 0);
            int offset = memberOffset.get(r);

            // keep the platformWay in the stop area relation with platform role
            if (parentStopAreaRelation.contains(r) && offset == 0)
                c.addMember(i + offset++, new RelationMember("platform", platformWay));

            if (referrers.contains(r)) {
                for (int j = 0; j < c.getMembers().size(); j++) {
                    if (platformNode.getUniqueId() == c.getMember(j).getUniqueId()) {
                        c.removeMember(j);
                        c.addMember(j, new RelationMember("platform", platformNode));
                    }
                }
            } else {
                c.addMember(i + offset++, new RelationMember("platform", platformNode));
            }

            memberOffset.put(r, offset);
        }));

        List<Command> commands = new ArrayList<>();
        changingRelation.forEach((oldR, newR) -> commands.add(new ChangeCommand(oldR, newR)));

        return commands;
    }

    @Override
    protected void updateEnabledState() {
        super.updateEnabledState();
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(final Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && selection.size() > 1);
    }

    private void addToPreferences() {
        PTAssistantPluginPreferences.TRANSFER_DETAILS.put(this.transferDetails.isSelected());
        action();
    }

    private static class transferDetailsDialog extends ExtendedDialog {

        transferDetailsDialog() {
            super(MainApplication.getMainFrame(), tr("Transferring details to platform NODE."), new String[] {tr("Ok"), tr("Cancel") },
                    true);
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            super.buttonAction(buttonIndex, evt);
        }
    }
}
