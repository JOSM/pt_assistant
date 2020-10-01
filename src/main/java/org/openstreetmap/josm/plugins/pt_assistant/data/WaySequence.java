package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;

public final class WaySequence<previousWay extends Way, currentWay extends Way, nextWay extends Way, wayAfterNextWay extends Way> {
    public Way previousWay;
    public Way currentWay;
    public Way nextWay;
    public Way wayAfterNextWay;

    public WaySequence() {
        this(null, null, null, null);
    }

    public WaySequence(RelationMember currentMember) {
        this(null, null, null, null);
        setCurrentPtWay(currentMember);
    }

    public WaySequence(Relation relation, int index) {
        this(null, null, null, null);
        setCurrentPtWay(relation.getMember(index));
        if (index > 1) setPreviousPtWay(relation.getMember(index - 1));
        final int membersCount = relation.getMembersCount();
        if (index < membersCount -1)
            setNextPtWay(relation.getMember(index + 1));
        if (index < membersCount -2)
            setAfterNextPtWay(relation.getMember(index + 2));
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

    public void setCurrentPtWay(RelationMember currentMember) {
        if (currentMember.isWay() && isPTWay(currentMember)) {
            currentWay = currentMember.getWay();
        } else {
            currentWay = null;
        }
    }

    public void setPreviousPtWay(RelationMember previousMember) {
        if (previousMember.isWay() && isPTWay(previousMember)) {
            previousWay = previousMember.getWay();
        } else {
            previousWay = null;
        }

    }

    public void setNextPtWay(RelationMember nextMember) {
        if (nextMember.isWay() && isPTWay(nextMember)) {
            nextWay = nextMember.getWay();
        } else {
            nextWay = null;
        }
    }

    public void setAfterNextPtWay(RelationMember afterNextMember) {
        if (isPTWay(afterNextMember)) {
            if (afterNextMember.isWay()) {
                wayAfterNextWay = afterNextMember.getWay();
                return;
            } else if (afterNextMember.isRelation()) {
                wayAfterNextWay = afterNextMember.getRelation().getMember(0).getWay();
                return;
            }
        }
        wayAfterNextWay = null;
    }
}
