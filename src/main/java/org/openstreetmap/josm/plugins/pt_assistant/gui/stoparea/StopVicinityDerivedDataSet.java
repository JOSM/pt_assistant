package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationEditorAccessUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

class StopVicinityDerivedDataSet extends DerivedDataSet {
    private final BBox bBox;
    private final long editedRelationId;
    private final Relation stopRelation;
    private final IRelationEditorActionAccess editorAccess;

    public StopVicinityDerivedDataSet(IRelationEditorActionAccess editorAccess) {
        super(editorAccess.getEditor().getLayer().getDataSet());
        this.stopRelation = editorAccess.getEditor().getRelation();
        this.editorAccess = editorAccess;
        this.editedRelationId = stopRelation == null ? 0 : stopRelation.getId();
        this.bBox = new BBox();
        RelationEditorAccessUtils.streamMembers(editorAccess)
            // Extra space: Something around 200.500m depending on where we are on the map.
            .forEach(p -> bBox.addPrimitive(p.getMember(), 0.005));

    }

    @Override
    protected boolean isIncluded(OsmPrimitive primitive) {
        return primitive.getType() != OsmPrimitiveType.RELATION
            // Normal primitives: all in bbox
            ? bBox.intersects(primitive.getBBox())
            // Relations: all except the one we edit
            // todo: restrict this, e.g. only PT relations + multipolygons in bbox
            : primitive.getId() != editedRelationId;
    }

    @Override
    protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
        // Now apply the relation editor changes
        // Simulate org.openstreetmap.josm.gui.dialogs.relation.actions.SavingAction.applyChanges
        Relation relation = new Relation();
        editorAccess.getTagModel().applyToPrimitive(relation);
        // This is a hack to tag our currently active relation.
        // There is no id selector in MapCSS, so we need a way to uniquely identify our relation
        relation.put("activePtRelation", "1");

        if (stopRelation != null) {
            addTo.addAsCopy(stopRelation, relation);

            // Now we search for all sibling relations.
            // Due to https://josm.openstreetmap.de/ticket/6129#comment:24 we cannot do it in MapCSS
            stopRelation.getReferrers()
                .stream()
                .filter(r -> r instanceof Relation && StopUtils.isStopAreaGroup((Relation) r))
                .flatMap(parent -> ((Relation) parent).getMembers().stream())
                .map(RelationMember::getMember)
                .filter(sibling -> sibling != stopRelation)
                .filter(sibling -> sibling instanceof Relation) // < some group relations contain invalid members
                .forEach(sibling -> {
                    Relation copy = new Relation((Relation) sibling);
                    copy.put("siblingOfActive", "1");
                    // This will add the copy with the fake tag.
                    addOrGetDerived(copy);
                });

        } else {
            addTo.add(relation);
        }

        RelationEditorAccessUtils.getRelationMembers(editorAccess)
            .forEach(m -> relation.addMember(addOrGetDerivedMember(m)));
    }
}
