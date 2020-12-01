package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.PublicTransportLinePanel;
import org.openstreetmap.josm.tools.ImageProvider;

class FoundStopArea implements FoundStop {
    private final List<RelationMember> stops;
    private final Relation relation;

    FoundStopArea(Relation relation) {
        stops = relation.getMembers()
            .stream()
            .filter(m -> OSMTags.STOP_ROLE.equals(m.getRole()))
            .collect(Collectors.toList());
        this.relation = relation;
    }

    @Override
    public boolean isIncomplete() {
        return relation.isIncomplete();
    }

    @Override
    public boolean matches(OsmPrimitive p) {
        // Many are not marked as stop => we still want to get them.
        return this.relation.equals(p) || this.relation
            .getMembers()
            .stream().anyMatch(s -> s.getMember().equals(p));
    }

    @Override
    public String getNameAndInfos() {
        String name = relation.get("name");
        return (name == null ? tr("- No name -") : name) + " (" + tr("area relation {0}", relation.getId()) + ")";
    }

    @Override
    public Component createActionButtons() {
        return PublicTransportLinePanel.createActions(
            PublicTransportLinePanel.createAction(tr("Zoom to stop area relation"), new ImageProvider("mapmode", "zoom").setMaxSize(12),
                () -> {
                    relation.getDataSet().setSelected(relation);
                    AutoScaleAction.autoScale(AutoScaleAction.AutoScaleMode.SELECTION);
                }),
            PublicTransportLinePanel.createAction(tr("Edit stop area"), new ImageProvider("dialogs", "edit"),
                () -> EditRelationAction.launchEditor(relation))
        );
    }

}
