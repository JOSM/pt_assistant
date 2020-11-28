package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.tools.ColorHelper;

import java.awt.Color;
import java.util.Objects;
import java.util.stream.Stream;

public class LineRelation {

    private final RelationAccess relation;
    private final boolean primary;

    public LineRelation(RelationAccess relation, boolean primary) {
        this.relation = Objects.requireNonNull(relation, "relation");
        this.primary = primary;
    }

    public RelationAccess getRelation() {
        return relation;
    }

    public boolean isPrimary() {
        return primary;
    }

    @Override
    public String toString() {
        return "LinearRelation{" + "relation=" + relation + ", primary=" + primary + '}';
    }

    public Color getColor() {
        if (isPrimary()) {
            String colour = relation.get("colour");
            Color fromOsm = colour != null ? ColorHelper.html2color(colour) : null;
            return fromOsm != null ? fromOsm : Color.BLACK;
        } else {
            return Color.LIGHT_GRAY;
        }
    }

    /**
     * The stops to display on the panel
     * @return The stops
     */
    public Stream<StopPositionEvent> streamStops() {
        return streamRawStops()
            .map(it -> new StopPositionEvent(it, false, false));
    }

    protected Stream<RelationMember> streamRawStops() {
        return getRelation().getMembers()
            .stream()
            .filter(it -> OSMTags.STOP_ROLES.contains(it.getRole()));
    }

    public static class StopPositionEvent {
        private final RelationMember stop;
        private final boolean skippedBefore;
        private final boolean skippedAfter;

        public StopPositionEvent(RelationMember stop, boolean skippedBefore, boolean skippedAfter) {
            this.stop = stop;
            this.skippedBefore = skippedBefore;
            this.skippedAfter = skippedAfter;
        }

        public RelationMember getStop() {
            return stop;
        }

        public boolean isSkippedAfter() {
            return skippedAfter;
        }

        public boolean isSkippedBefore() {
            return skippedBefore;
        }


        @Override
        public String toString() {
            return "StopPositionEvent{" +
                "stop=" + stop +
                ", skippedBefore=" + skippedBefore +
                ", skippedAfter=" + skippedAfter +
                '}';
        }
    }

}
