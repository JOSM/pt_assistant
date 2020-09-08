package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

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

        // save the current state, otherwise accidents happen
        memberTableModel.applyToRelation(originalRelation);
        // todo unfortunately there are still index out of bounds exceptions happening

        final Collection<RelationMember> selectedMembers = memberTableModel.getSelectedMembers();

        List<Command> commands = new ArrayList<>();

        String title = I18n.tr("Extract part from relation?");
        String question = I18n.tr(
            "Do you want to create a new relation with the {0} selected members?",
            selectedMembers.size()
        );
        final JCheckBox cbConvertToSuperroute = new JCheckBox(I18n.tr("Convert to superroute"));
        final JCheckBox cbProposed = new JCheckBox(I18n.tr("subrelation is proposed"));
        final JCheckBox cbDeviation = new JCheckBox(I18n.tr("subrelation is deviation"));
        final JTextField tfNameTag = new JTextField(getEditor().getRelation().get("name"));
        Object[] params = {question, tfNameTag, cbConvertToSuperroute, cbProposed, cbDeviation};
        if (
            JOptionPane.showConfirmDialog(
                getMemberTable(),
                params,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            ) == JOptionPane.OK_OPTION
        ) {
            final Relation clonedRelation = new Relation(originalRelation);
            final Relation extractedRelation = extractMembersToRouteRelationAndSubstituteThem(clonedRelation, memberTableModel);
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
                UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Extract ways to relation"), commands));
                RelationEditor.getEditor(getEditor().getLayer(), extractedRelation, Collections.emptyList()).toFront();
                try {
                    editor.reloadDataFromRelation();
                    // todo this often give index out of bounds exception
                    // I can't figure out why.
                    // catching it here causes the exception to occur when
                    // the user presses the update button, so not really a solution
                    // The update button appears if I remove 1 or 2 members before extracting
                    // so when the relation editor is 'dirty'.
                    // using memberTableModel.applyToRelation(originalRelation);
                    // at the beginning doesn't seem to actually save the relation
                } catch  (Exception e) {
                    Logging.error(e);
                }
            }
        }
    }

    public Relation extractMembersToRouteRelationAndSubstituteThem(Relation clonedRelation,
                                                                   MemberTableModel memberTableModel) {
        int[] selectedIndices = memberTableModel.getSelectedIndices();
        return extractMembersForIndicesAndSubstitute(clonedRelation, selectedIndices);
    }

    public Relation extractMembersForIndicesAndSubstitute(Relation clonedRelation,
                                                          int[] selectedIndices) {
        final Relation extractedRelation = new Relation();
        extractedRelation.setKeys(clonedRelation.getKeys());
        extractedRelation.put("type", "route");

        int index = 0;
        boolean atLeast1MemberAddedToExtractedRelation = false;
        for (int i = selectedIndices.length-1; i >= 0; i--) {
            atLeast1MemberAddedToExtractedRelation = true;
            index = selectedIndices[i];
            RelationMember relationMember = clonedRelation.removeMember(index);
            extractedRelation.addMember(0, relationMember);
        }

        if (atLeast1MemberAddedToExtractedRelation) {
            // replace removed members with the extracted relation
            clonedRelation.addMember(index,
                new RelationMember("", extractedRelation));
            return extractedRelation;
        } else {
            return null;
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

        putValue(SHORT_DESCRIPTION, (
            newEnabledState
                ? I18n.tr("Extract part of the route into new relation")
                : I18n.tr("Extract into new relation (needs type=route tag and at least one selected relation member)")
        ));
        setEnabled(newEnabledState);
    }
}
