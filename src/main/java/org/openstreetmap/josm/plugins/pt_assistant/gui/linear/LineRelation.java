package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.tools.ColorHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;

public class LineRelation {

    private final Relation relation;
    private final boolean primary;

    public LineRelation(Relation relation, boolean primary) {
        this.relation = Objects.requireNonNull(relation, "relation");
        this.primary = primary;
    }

    public Relation getRelation() {
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
}
