package org.openstreetmap.josm.plugins.pt_assistant.actions.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class AddStopAreaAction extends JosmAction {
    private static final List<String> TAGS_TO_COPY = Arrays.asList(
        OSMTags.NAME_TAG,
        OSMTags.REF_TAG,
        "uic_ref",
        "uic_name",
        OSMTags.OPERATOR_TAG,
        OSMTags.NETWORK_TAG
    );

    public AddStopAreaAction() {
        super(tr("Create stop area"), null, tr("Create a stop area from the selected elements"),
            null, false);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection.size() > 0
            && selection.stream().noneMatch(it -> null != StopUtils.findContainingStopArea(it)));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation areaRelation = new Relation();
        areaRelation.setModified(true);

        // Tags
        areaRelation.put(OSMTags.KEY_RELATION_TYPE, OSMTags.PUBLIC_TRANSPORT_TAG);
        areaRelation.put(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_AREA_TAG_VALUE);
        HashSet<String> duplicateDetector = new HashSet<>();
        getLayerManager().getActiveDataSet()
            .getSelected()
            .stream()
            .flatMap(it -> it.getKeys().getTags().stream())
            .distinct()
            .filter(tag -> TAGS_TO_COPY.contains(tag.getKey()))
            .filter(tag -> duplicateDetector.add(tag.getKey()))
            .forEach(areaRelation::put);

        // Members
        getLayerManager().getActiveDataSet()
            .getSelected()
            .stream()
            .map(selected -> new RelationMember(suggestRole(selected), selected))
            .forEach(areaRelation::addMember);

        DialogUtils.showRelationEditor(RelationEditor.getEditor(
            MainApplication.getLayerManager().getEditLayer(),
            areaRelation,
            null /* no selected members */
        ));
    }

    private String suggestRole(OsmPrimitive selected) {
        if (StopUtils.isPlatform(selected)) {
            return OSMTags.PLATFORM_ROLE;
        } else if (selected.hasTag(OSMTags.HIGHWAY_TAG, OSMTags.BUS_STOP_TAG_VALUE)
            || selected.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_POSITION_TAG_VALUE)) {
            return OSMTags.STOP_ROLE;
        } else {
            return "";
        }
    }
}
