package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;

public class RelationEditorAccessUtils {

    public static List<RelationMember> getRelationMembers(IRelationEditorActionAccess editorAccess) {
        // Same as getMemberTableModel().applyToRelation(relation);
        return RelationEditorAccessUtils.streamMembers(editorAccess)
            .collect(Collectors.toList());
    }

    public static Stream<RelationMember> streamMembers(IRelationEditorActionAccess editorAccess) {
        MemberTableModel membersModel = editorAccess.getMemberTableModel();
        return IntStream.range(0, membersModel.getRowCount())
            .mapToObj(membersModel::getValue)
            .filter(rm -> !rm.getMember().isDeleted() && rm.getMember().getDataSet() != null);
    }
}
