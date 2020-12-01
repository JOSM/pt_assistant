package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
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
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class StopAreaGroupPanel extends AbstractVicinityPanel {
    public StopAreaGroupPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoomSaver) {
        super(new DerivedDataSet(editorAccess.getEditor().getLayer().getDataSet()) {
            @Override
            protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
                // We need to add all members of the current relation and flag them with a special tag
                // This is because JOSM cannot handle super relations in MapCSS
                RelationAccess.of(editorAccess)
                    .getMembers()
                    .stream()
                    .map(RelationMember::getMember)
                    .forEach(child -> {
                        if (child instanceof Relation) {
                            Relation copy = new Relation((Relation) child);
                            copy.put("childOfActiveAreaGroup", "1");
                            addOrGetDerived(copy);
                        }
                    });
                // No need to add our parent relation => we flagged all children, that should be enough
            }

            @Override
            protected boolean isIncluded(OsmPrimitive primitive) {
                // Members are automatically included recursively
                return primitive instanceof Relation
                    && !primitive.getPrimitiveId().equals(editorAccess.getEditor().getRelation())
                    && (StopUtils.isStopArea((Relation) primitive) || RouteUtils.isPTRoute((Relation) primitive));
            }
        }, editorAccess, zoomSaver);
    }

    @Override
    protected void doInitialZoom() {
        zoomToEditorRelation();
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
    protected void doAction(Point point, OsmPrimitive originalPrimitive) {
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
