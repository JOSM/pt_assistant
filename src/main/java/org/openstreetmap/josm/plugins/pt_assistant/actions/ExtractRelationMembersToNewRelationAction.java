package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTWay;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.*;

import static java.util.Collections.*;
import static org.openstreetmap.josm.gui.MainApplication.*;

/*
Extracts selected members to a new route relation
and substitutes them with this new route relation
in the original relation

 * @author Florian, Polyglot
 */
public class ExtractRelationMembersToNewRelationAction extends AbstractRelationEditorAction {
    public ExtractRelationMembersToNewRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION,
            IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE,
            IRelationEditorUpdateOn.TAG_CHANGE);
        new ImageProvider("bus").getResource().attachImageIcon(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final MemberTableModel memberTableModel = editorAccess.getMemberTableModel();
        IRelationEditor editor = editorAccess.getEditor();
        final Relation originalRelation = editor.getRelation();
        Relation editedRelation = new Relation(originalRelation);
        // save the current state, otherwise accidents happen
        memberTableModel.applyToRelation(editedRelation);
        editorAccess.getTagModel().applyToPrimitive(editedRelation);
        UndoRedoHandler.getInstance().add(new ChangeCommand(originalRelation, editedRelation));

        final Collection<RelationMember> selectedMembers = memberTableModel.getSelectedMembers();

        List<Command> commands = new ArrayList<>();

        String title = I18n.tr("Extract part from relation?");
        String question = I18n.tr(
            "Do you want to create a new relation with the {0} selected members?",
            selectedMembers.size()
        );
        List<Relation> parentRelations = new ArrayList<>(Utils.filteredCollection(originalRelation.getReferrers(), Relation.class));
        parentRelations.removeIf(parentRelation -> !Objects.equals(parentRelation.get("type"), "superroute"));
        final int numberOfParentRelations = parentRelations.size();
        final JCheckBox cbReplaceInSuperrouteRelations = new JCheckBox(I18n.tr(
            "This route is a member of {0} superroute {1}, add the extracted relation there instead of in this relation?",
            numberOfParentRelations,
            I18n.trn("relation", "relations", numberOfParentRelations)));
        final JCheckBox cbConvertToSuperroute = new JCheckBox(I18n.tr("Convert this relation to superroute"));
        final JCheckBox cbProposed = new JCheckBox(I18n.tr("subrelation is proposed (wenslijn)"));
        final JCheckBox cbDeviation = new JCheckBox(I18n.tr("subrelation is deviation (omleiding)"));
        final JCheckBox cbFindAllSegmentsAutomatically = new JCheckBox(I18n.tr("Process complete route relation for segments and extract into multiple sub route relations"));
        final JTextField tfNameTag = new JTextField(getEditor().getRelation().get("name"));
        ArrayList<Object> paramsList = new ArrayList<>();
        paramsList.add(question);
        paramsList.add(tfNameTag);
        // Only show cbReplaceInSuperrouteRelations when it's relevant
        if (numberOfParentRelations > 0) {
            // check the check box
            cbReplaceInSuperrouteRelations.setSelected(true);
            paramsList.add(cbReplaceInSuperrouteRelations);
        }
        if (originalRelation.hasTag("cycle_network", "cycle_highway")) {
            paramsList.add(cbDeviation);
            paramsList.add(cbProposed);
        }
        paramsList.add(cbConvertToSuperroute);
        if (RouteUtils.isVersionTwoPTRoute(originalRelation)) {
            cbConvertToSuperroute.setSelected(true);
            cbFindAllSegmentsAutomatically.setSelected(true);
            paramsList.add(cbFindAllSegmentsAutomatically);
        }
        paramsList.add(cbConvertToSuperroute);
        Object[] params = paramsList.toArray(new Object[paramsList.size()]);
        if (
            JOptionPane.showConfirmDialog(
                getMemberTable(),
                params,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            ) == JOptionPane.OK_OPTION
        ) {
            if (!cbFindAllSegmentsAutomatically.isSelected()) {
                final Relation clonedRelation = new Relation(originalRelation);
                boolean substituteWaysWithRelation = !cbReplaceInSuperrouteRelations.isSelected();
                List<Integer> selectedIndices = Arrays.stream(memberTableModel.getSelectedIndices())
                    .boxed().collect(Collectors.toList());
                final Relation extractedRelation = extractMembersForIndicesAndSubstitute(
                    clonedRelation, selectedIndices, substituteWaysWithRelation);
                if (extractedRelation != null) {
                    if (cbConvertToSuperroute.isSelected()) {
                        clonedRelation.put("type", "superroute");
                    }
                    extractedRelation.put("name", tfNameTag.getText());
                    if (cbProposed.isSelected()) {
                        extractedRelation.put("state", "proposed");
                        extractedRelation.put("name", tfNameTag.getText() + " (wenslijn)");
                    }
                    if (cbDeviation.isSelected()) {
                        extractedRelation.put("name", tfNameTag.getText() + " (omleiding)");
                    }
                    DataSet activeDataSet = getLayerManager().getActiveDataSet();
                    commands.add(new AddCommand(activeDataSet, extractedRelation));
                    commands.add(new ChangeCommand(originalRelation, clonedRelation));
                    addExtractedRelationToParentSuperrouteRelations(originalRelation, commands, parentRelations, selectedIndices, extractedRelation);
                    UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Extract ways to relation"), commands));
                    RelationEditor extraEditor = RelationEditor.getEditor(getEditor().getLayer(), extractedRelation, emptyList());
                    extraEditor.setVisible(true);
                    extraEditor.setAlwaysOnTop(true);

                    try {
                        editor.reloadDataFromRelation();
                        // todo this often gives index out of bounds exception
                        // I can't figure out why.
                        // catching it here causes the exception to occur when
                        // the user presses the update button, so not really a solution
                        // The update button appears if I remove 1 or 2 members before extracting
                        // so when the relation editor is 'dirty'.
                        // using memberTableModel.applyToRelation(originalRelation);
                        // at the beginning doesn't seem to actually save the relation
                    } catch (Exception e) {
                        Logging.error(e);
                    }
                }
            } else {
                final Relation clonedRelation = new Relation(originalRelation);
                DataSet activeDataSet = getLayerManager().getActiveDataSet();
                String previousLineIdentifiersSignature = "";
                List<Integer> selectedIndices = new ArrayList<>();
                List<RelationMember> members = clonedRelation.getMembers();
                List<String> streetNamesList = new ArrayList<>();
                for (int i = members.size()-1; i>=0; i--) {
                    RelationMember rm = members.get(i);
                    if (rm.getType().equals(OsmPrimitiveType.WAY)) {
                        final Way rmWay = rm.getWay();
                        List<Relation> parentRouteRelations = new ArrayList<>(Utils.filteredCollection(rmWay.getReferrers(), Relation.class));
                        List<String> lineIdentifiersList = new ArrayList<>();
                        for (Relation pr : parentRouteRelations) {
                            final String ref = pr.get("ref");
                            if (ref != null && !lineIdentifiersList.contains(ref)) {
                                lineIdentifiersList.add(ref);
                            }
                        }
                        sort(lineIdentifiersList);
                        String lineIdentifiersSignature = String.join(";", lineIdentifiersList);

                        String streetName = rmWay.get("name");
                        if (streetName == null) {
                            streetName = rmWay.get("ref");
                        }
                        // check if we need to start a new segment
                        if (!lineIdentifiersSignature.equals(previousLineIdentifiersSignature)) {
                            if (!"".equals(previousLineIdentifiersSignature)) {
                                // todo check if a similar sub route relation already exists
                                String streetNamesSignature = String.join(";", streetNamesList);
                                Relation extractedRelation = extractMembersForIndicesAndSubstitute(
                                    clonedRelation, selectedIndices, true);
                                if (extractedRelation != null) {
                                    if (!streetNamesSignature.isEmpty()) {
                                        extractedRelation.put("street_names", streetNamesSignature);
                                    }
                                    String fromToStreet = "";
                                    if (streetNamesList.size() > 0) {
                                        fromToStreet = streetNamesList.get(0).concat(" ");
                                        if (streetNamesList.size() > 1) {
                                            fromToStreet = String.format("%s- %s ", fromToStreet, streetNamesList.get(streetNamesList.size() - 1));
                                        }
                                    }
                                    String name = String.format("%s(%s)",fromToStreet, previousLineIdentifiersSignature);
                                    extractedRelation.put("name", name);
                                    extractedRelation.put("route_ref", previousLineIdentifiersSignature);
                                    extractedRelation.remove("bus");
                                    extractedRelation.remove("colour");
                                    extractedRelation.remove("from");
                                    extractedRelation.remove("network");
                                    extractedRelation.remove("network:wikidata");
                                    extractedRelation.remove("operator");
                                    extractedRelation.remove("operator:wikidata");
                                    extractedRelation.remove("ref");
                                    extractedRelation.remove("to");
                                    extractedRelation.remove("via");
                                    extractedRelation.remove("wikidata");
                                    commands.add(new AddCommand(activeDataSet, extractedRelation));
                                    RelationEditor extraEditor = RelationEditor.getEditor(getEditor().getLayer(), extractedRelation, emptyList());
                                    extraEditor.setVisible(true);
                                    // extraEditor.setAlwaysOnTop(true);
                                }
                                streetNamesList = new ArrayList<>();
                                selectedIndices = new ArrayList<>();
                            }
                            previousLineIdentifiersSignature = lineIdentifiersSignature;
                        }
                        if (streetName != null && !streetNamesList.contains(streetName)) {
                            streetNamesList.add(0,streetName);
                        }
                        selectedIndices.add(0,i);
                    }
                }
                commands.add(new ChangeCommand(originalRelation, clonedRelation));
                UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Extract ways to relation"), commands));
            }
        }
    }

    public Relation extractMembersForIndicesAndSubstitute(Relation clonedRelation,
                                                          List<Integer> selectedIndices,
                                                          boolean substituteWaysWithRelation) {
        final Relation extractedRelation = new Relation();
        extractedRelation.setKeys(clonedRelation.getKeys());
        extractedRelation.put("type", "route");

        int index = 0;
        boolean atLeast1MemberAddedToExtractedRelation = false;
        for (int i = selectedIndices.size() - 1; i >= 0; i--) {
            atLeast1MemberAddedToExtractedRelation = true;
            index = selectedIndices.get(i);
            RelationMember relationMember = clonedRelation.removeMember(index);
            extractedRelation.addMember(0, relationMember);
        }

        if (atLeast1MemberAddedToExtractedRelation) {
            if (substituteWaysWithRelation) {
                // replace removed members with the extracted relation
                if (index >= clonedRelation.getMembersCount()) {
                    index = clonedRelation.getMembersCount() - 1;
                }
                clonedRelation.addMember(index, new RelationMember("", extractedRelation));
                }
            } else {
                return null;
            }
        return extractedRelation;
    }

    private void addExtractedRelationToParentSuperrouteRelations(Relation originalRelation, List<Command> commands, List<Relation> parentRelations, List<Integer> selectedIndices, Relation extractedRelation) {
        for (Relation superroute : parentRelations) {
            int index = 0;
            for (RelationMember member : superroute.getMembers()) {
                if (member.getMember().getId() == originalRelation.getId()) {
                    Relation clonedParentRelation = new Relation(superroute);
                    if (selectedIndices.get(0) != 0) {
                        // if the user selected a block that didn't start with
                        // the first member, add the extracted relation after
                        // the position where this relation was in the parent
                        index++;
                    }
                    if (index >= superroute.getMembersCount()) {
                        index = superroute.getMembersCount()-1;
                    }
                    clonedParentRelation.addMember(index, new RelationMember("", extractedRelation));
                    commands.add(new ChangeCommand(superroute, clonedParentRelation));
                    break;
                }
                index++;
            }
        }
    }

    @Override
    public boolean isExpertOnly() {
        return true;
    }

    @Override
    protected void updateEnabledState() {
        final boolean newEnabledState = !editorAccess.getSelectionTableModel().getSelection().isEmpty()
            && Optional.ofNullable(editorAccess.getTagModel().get("type")).filter(it -> it.getValue().matches("route|superroute")).isPresent();
            // && !editorAccess.getEditor().isDirtyRelation(); // This seems to cause a problem

        if (newEnabledState) {
            putValue(SHORT_DESCRIPTION, I18n.tr("Extract selected part of the route into new relation"));
        } else {
            putValue(SHORT_DESCRIPTION, I18n.tr("Extract into new relation (needs type=route/superroute tag and at least one selected relation member)"));
        }
        setEnabled(newEnabledState);
    }
}
