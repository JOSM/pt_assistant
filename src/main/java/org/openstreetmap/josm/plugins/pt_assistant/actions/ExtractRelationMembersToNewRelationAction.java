package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class ExtractRelationMembersToNewRelationAction extends AbstractRelationEditorAction {
    public ExtractRelationMembersToNewRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE, IRelationEditorUpdateOn.TAG_CHANGE);
        new ImageProvider("bus").getResource().attachImageIcon(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final Collection<RelationMember> selectedMembers = editorAccess.getMemberTableModel().getSelectedMembers();
        final Map<String, String> tags = editorAccess.getTagModel().getTags();

        final Relation extractedRelation = new Relation();
        extractedRelation.setKeys(tags);
        for (final RelationMember member : selectedMembers) {
            extractedRelation.addMember(member);
        }

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
            getEditor().getLayer().data.addPrimitive(extractedRelation);
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
