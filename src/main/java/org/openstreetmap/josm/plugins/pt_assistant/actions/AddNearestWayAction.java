package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;


/**
 * For all nodes with role "stop" in the relation, finds all ways that contain at least one of those nodes.
 * All found ways are then added to the end of the member list.
 */
public class AddNearestWayAction extends AbstractRelationEditorAction {

    private static final List<String> POSSIBLE_ROUTE_TYPES = Arrays.asList("bus", "railway");

    public AddNearestWayAction(IRelationEditorActionAccess editorAccess){
        super(
            editorAccess,
            IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE,
            IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION,
            IRelationEditorUpdateOn.TAG_CHANGE
        );
        putValue(SHORT_DESCRIPTION, tr("Add ways connected to stop positions"));
        new ImageProvider("dialogs/relation", "routing_assistance.svg").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        final String routeType = editorAccess.getTagModel().get("route").getValue();

        setEnabled(routeType != null && POSSIBLE_ROUTE_TYPES.contains(routeType));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final Set<RelationMember> prevMembers = IntStream.range(0, editorAccess.getMemberTableModel().getRowCount())
            .mapToObj(i -> editorAccess.getMemberTableModel().getValue(i))
            .collect(Collectors.toSet());

        final List<Way> newMembers = prevMembers.stream()
            .filter(member -> member.getType() == OsmPrimitiveType.NODE && "stop".equals(member.getRole()))
            .flatMap(member ->
                member.getNode().getDataSet().getWays().stream()
                    .filter(w -> w.containsNode(member.getNode()) && prevMembers.stream().noneMatch(it -> w.equals(it.getMember())))
            )
            .distinct()
            .collect(Collectors.toList());

        editorAccess.getMemberTableModel().addMembersAtEnd(newMembers);
        new Notification(I18n.trn("Adding {0} new way to the relation", "Adding {0} new ways to the relation", newMembers.size())).show();
    }
}
