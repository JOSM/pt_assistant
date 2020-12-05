package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;

/**
 * A class to store the relation member indexes along with the member
 * <br/>
 * A relation may contain the same member with the same role multiple times.
 * <br/>
 * Some times, we mix relations (e.g. superroutes). Therefore, we store the relation the member originated from, too.
 */
public class IndexedRelationMember {
    private final RelationMember member;
    private final int index;
    private final RelationAccess relation;

    public IndexedRelationMember(RelationMember member, int index, RelationAccess relation) {
        this.member = Objects.requireNonNull(member, "member");
        this.index = index;
        this.relation = Objects.requireNonNull(relation, "relation");
        if (index < 0 || index >= relation.getMembers().size()) {
            throw new IllegalArgumentException("Index out of bounds: " + index + " for " + member);
        }
        if (relation.getMembers().get(index) != member) {
            throw new IllegalArgumentException("Relation member index wrong: " + index + " for " + member);
        }
    }

    public RelationAccess getRelation() {
        return relation;
    }

    public RelationMember getMember() {
        return member;
    }

    public int getIndex() {
        return index;
    }


    @Override
    public String toString() {
        return "IndexedRelationMember{" +
            "member=" + member +
            ", index=" + index +
            ", relation=" + relation +
            '}';
    }
}
