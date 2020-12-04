package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.IncompleteMembersWarningPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class StopAreaGroupPanel extends AbstractVicinityPanel<AreaGroupDerivedDataSet> {
    public StopAreaGroupPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoomSaver) {
        super(new AreaGroupDerivedDataSet(editorAccess), editorAccess, zoomSaver);

        if (RelationAccess.of(editorAccess)
            .getMembers()
            .stream()
            .anyMatch(it ->
                it.getMember().isIncomplete()
                    || it.isRelation() && it.getRelation().getMembers().stream().anyMatch(r -> r.getMember().isIncomplete())
            )) {
            add(new IncompleteMembersWarningPanel(), BorderLayout.NORTH);
        }

    }

    @Override
    protected Set<OsmPrimitive> getToHighlightFor(Point point) {
        OsmPrimitive primitive = getPrimitiveAt(point);
        if (primitive == null) {
            return Collections.emptySet();
        }
        Relation area = StopUtils.findContainingStopArea(primitive);
        if (area == null) {
            return Collections.emptySet();
        } else {
            return area
                .getMembers()
                .stream()
                .map(RelationMember::getMember)
                .collect(Collectors.toSet());
        }
    }

    @Override
    protected List<String> getStylePath() {
        return Arrays.asList(
            "org/openstreetmap/josm/plugins/pt_assistant/gui/stoparea/ptbackground.mapcss",
            "org/openstreetmap/josm/plugins/pt_assistant/gui/stoparea/stopareagroup.mapcss");
    }

    @Override
    protected OsmPrimitive getPrimitiveAt(Point point) {
        return getOsmPrimitiveAt(point, it -> StopUtils.findContainingStopArea(it) != null);
    }

    @Override
    protected void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
        Relation area = StopUtils.findContainingStopArea(originalPrimitive);
        if (area == null) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        Relation group = StopUtils.findParentStopGroup(area);
        if (group != null && !group.equals(editorAccess.getEditor().getRelation())) {
            // Cannot add → already in group
            menu.add(new UnBoldLabel(tr("This stop area is already a member of a different group")));
        } else if (RelationAccess.of(editorAccess).getMembers().stream().anyMatch(
            member -> member.getMember().equals(area)
        )) {
            menu.add(new JMenuItem(new JosmAction(tr("Remove from this relation"),
                null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editorAccess.getMemberTableModel().removeMembersReferringTo(Arrays.asList(area));
                }
            }));
        } else {
            menu.add(new JMenuItem(new JosmAction(tr("Add to this relation"),
                null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editorAccess.getMemberTableModel().addMembersAtEnd(Arrays.asList(area));
                }
            }));
        }
        menu.add(new JMenuItem(new JosmAction(tr("Open area relation"), null, null, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogUtils.showRelationEditor(RelationEditor.getEditor(
                    editorAccess.getEditor().getLayer(),
                    area,
                    Collections.emptyList()
                ));
            }
        }));
        menu.show(mapView, point.x, point.y);
    }

    @Override
    protected JComponent generateActionButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);

        panel.add(generateZoomToButton(tr("Zoom to"), tr("Zoom to all areas contained in this group.")));
        return panel;
    }

}
