package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.gui.MainApplication.*;

public class ExtractRelationMembersToNewRelationAction extends AbstractRelationEditorAction {
    public ExtractRelationMembersToNewRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE, IRelationEditorUpdateOn.TAG_CHANGE);
        new ImageProvider("bus").getResource().attachImageIcon(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final Collection<RelationMember> selectedMembers = editorAccess.getMemberTableModel().getSelectedMembers();
        final Map<String, String> tags = editorAccess.getTagModel().getTags();
        final Relation originalRelation = editorAccess.getEditor().getRelation();


        if (
            JOptionPane.showConfirmDialog(
                getMemberTable(),
                I18n.tr(
                    "Do you really want to create a new relation from the {0} selected members of relation {1}, copying the {2} tags to the new relation?",
                    selectedMembers.size(),
                    getEditor().getRelation().getId(),
                    tags.size()
                ),
                I18n.tr("Extract part from relation?"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            ) == JOptionPane.OK_OPTION
        ) {
            final Relation extractedRelation = new Relation();
            extractedRelation.setKeys(tags);
            UndoRedoHandler.getInstance().add(new AddCommand(getLayerManager().getActiveDataSet(), extractedRelation));
            final Relation newExtractedRelation = new Relation(extractedRelation);
            boolean flag = true;

            Relation newRelation = new Relation(originalRelation);
            List<RelationMember> members = newRelation.getMembers();
            for (int i = members.size()-1; i >= 0; i--) {
                RelationMember member = members.get(i);
                if (selectedMembers.contains(member)) {
                    newExtractedRelation.addMember(0, member);
                    newRelation.removeMember(i);
                    if (flag) {
                        newRelation.addMember(i, new RelationMember("", extractedRelation));
                        flag = false;
                    }
                }
            }
            newExtractedRelation.put("type", "route");

            UndoRedoHandler.getInstance().add(new ChangeCommand(originalRelation, newRelation));
            UndoRedoHandler.getInstance().add(new ChangeCommand(extractedRelation, newExtractedRelation));
            editorAccess.getEditor().reloadDataFromRelation();
            RelationEditor.getEditor(getEditor().getLayer(), extractedRelation, Collections.emptyList()).setVisible(true);
        }
    }

    @Override
    public boolean isExpertOnly() {
        return true;
    }

    @Override
    protected void updateEnabledState() {
        final boolean newEnabledState = !editorAccess.getSelectionTableModel().getSelection().isEmpty()
            && Optional.ofNullable(editorAccess.getTagModel().get("type")).filter(it -> "route".equals(it.getValue())).isPresent();

        putValue(SHORT_DESCRIPTION, (
            newEnabledState
                ? I18n.tr("Extract part of the route into new relation")
                : I18n.tr("Extract into new relation (needs type=route tag and at least one selected relation member)")
        ));
        setEnabled(newEnabledState);
    }
}
