package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;

public final class WaySequence {
    public Way previousWay;
    public Way currentWay;
    public Way nextWay;
    public Way wayAfterNextWay;

    public WaySequence() {
        this(null, null, null, null);
    }

    public WaySequence(Relation relation, int index) {
        final RelationMember currentMember = relation.getMember(index);
        if (isPTWay(currentMember)) {
            if (currentMember.isRelation()) {
                final Relation subRelation = currentMember.getRelation();
                if (subRelation.getMembersCount() == 1) {
                    final RelationMember subRelationMember = subRelation.getMember(0);
                    if (subRelationMember.isWay()) {
                        currentWay = subRelationMember.getWay();
                    } else {
                        // if there is a relation with a single way at this position, no problem
                        // But if that sub relation has more than a single member, there is no
                        // possibility to decide which way is meant
                        currentWay = null;
                        return;
                    }
                }
            } else if (currentMember.isWay()) {
                currentWay = currentMember.getWay();
            }
        } else {
            // the member was probably a stop or platform
            currentWay = null;
            return;
        }
        if (index > 1) setPreviousPtWay(relation.getMember(index - 1));
        final int membersCount = relation.getMembersCount();
        if (index < membersCount -1) {
            setNextPtWay(relation.getMember(index + 1));
            if (index < membersCount - 2)
                setAfterNextPtWay(relation.getMember(index + 2));
        }
    }

    public WaySequence(Way previousWay, Way currentWay, Way nextWay)
    {
        this(previousWay, currentWay, nextWay, null);
    }

    public WaySequence(Way previousWay, Way currentWay, Way nextWay, Way wayAfterNextWay) {
        this.previousWay = previousWay;
        this.currentWay = currentWay;
        this.nextWay = nextWay;
        this.wayAfterNextWay =  wayAfterNextWay;
    }

    public void setPreviousPtWay(RelationMember previousMember) {
        if (isPTWay(previousMember)) {
            if (previousMember.isWay()) {
                previousWay = previousMember.getWay();
            } else if (previousMember.isRelation()) {
               List<RelationMember> subRelationMembers = previousMember.getRelation().getMembers();
               previousWay = subRelationMembers.get(subRelationMembers.size() - 1).getWay();
            }
        } else {
            previousWay = null;
        }
    }

    public void setNextPtWay(RelationMember nextMember) {
        if (isPTWay(nextMember)) {
            if (nextMember.isWay()) {
            nextWay = nextMember.getWay();
        } else if (nextMember.isRelation()) {
                nextWay = nextMember.getRelation().getMember(0).getWay();
            }
        } else {
            nextWay = null;
        }
    }

    public void setAfterNextPtWay(RelationMember afterNextMember) {
        if (isPTWay(afterNextMember)) {
            if (afterNextMember.isWay()) {
                wayAfterNextWay = afterNextMember.getWay();
            } else if (afterNextMember.isRelation()) {
                wayAfterNextWay = afterNextMember.getRelation().getMember(0).getWay();
            }
        } else {
            wayAfterNextWay = null;
        }
    }
}
