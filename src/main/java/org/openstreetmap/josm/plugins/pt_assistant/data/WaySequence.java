package org.openstreetmap.josm.plugins.pt_assistant.data;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;

import java.util.List;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;

/**
 * This class keeps track of up to 4 consecutive ways
 * that are most likely members in a route relation
 */
public final class WaySequence {
    public boolean hasGap = false;
    public int traversalSense = 0;
    public Way previousWay;
    public Way currentWay;
    public Way nextWay;
    public Way wayAfterNextWay;

    /**
     * Constructor to fetch the ways from a route relation
     * The caller needs to verify that hasGap remains false
     *
     * @param relation the route relation
     * @param index the position of currentWay in the route relation
     */
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
            // the member was probably a stop or a platform
            currentWay = null;
        }
        if (index > 1) setPreviousPtWay(relation.getMember(index - 1));
        final int membersCount = relation.getMembersCount();
        if (index < membersCount -1) {
            setNextPtWay(relation.getMember(index + 1));
            if (index < membersCount - 2)
                setAfterNextPtWay(relation.getMember(index + 2));
        }
    }

    /**
     * Constructor if only 3 ways are needed/known
     * @param previousWay the way before currentWay
     * @param currentWay the current way
     * @param nextWay the way after currentWay
     */
    public WaySequence(Way previousWay, Way currentWay, Way nextWay)
    {
        this(previousWay, currentWay, nextWay, null);
    }

    /**
     * Constructor if all 4 ways are known
     * @param previousWay the way before currentWay
     * @param currentWay the way this way sequence was created for
     * @param nextWay the way after currentWay
     * @param wayAfterNextWay the way after nextWay
     */
    public WaySequence(Way previousWay, Way currentWay, Way nextWay, Way wayAfterNextWay) {
        this.currentWay = currentWay;
        setPreviousWay(previousWay);
        setNextWay(nextWay);
        setAfterNextWay(wayAfterNextWay);
    }

    /**
     * @param way the way that comes before currentWay
     *            a check is performed to make sure they actually connect
     */
    public void setPreviousWay(Way way) {
        if (way == null || RouteUtils.waysTouch(way, currentWay)) {
            previousWay = way;
        } else {
            previousWay = null;
            hasGap = true;
        }
        getTraversalSense();
    }

    /**
     * @param way the way that comes after currentWay
     *            a check is performed to make sure they actually connect
     */
    public void setNextWay(Way way) {
        if (way == null || currentWay == null || RouteUtils.waysTouch(currentWay, way)) {
            nextWay = way;
        } else {
            nextWay = null;
            hasGap = true;
        }
        getTraversalSense();
    }

    /**
     * @param way the way that comes after nextWay
     *            a check is performed to make sure they actually connect
     */
    public void setAfterNextWay(Way way) {
        if (way == null || RouteUtils.waysTouch(nextWay, way)) {
            wayAfterNextWay = way;
        } else {
            wayAfterNextWay = null;
            hasGap = true;
        }
    }

    /**
     * @param previousMember the relation member that comes before currentWay in the route relation
     *                       in case this member is also a relation, the last way in that relation
     *                       is used
     */
    public void setPreviousPtWay(RelationMember previousMember) {
        if (isPTWay(previousMember)) {
            if (previousMember.isWay()) {
                setPreviousWay(previousMember.getWay());
            } else if (previousMember.isRelation()) {
               List<RelationMember> subRelationMembers = previousMember.getRelation().getMembers();
                setPreviousWay(subRelationMembers.get(subRelationMembers.size() - 1).getWay());
            }
        } else {
            previousWay = null;
        }
    }

    /**
     * @param nextMember the relation member that comes after currentWay in the route relation
     *                   in case this member is also a relation, the first way in that relation
     *                   is used.
     *                   If that relation has more than 1 member, afterNextWay is also set already
     */
    public void setNextPtWay(RelationMember nextMember) {
        if (isPTWay(nextMember)) {
            if (nextMember.isWay()) {
                setNextWay(nextMember.getWay());
            } else if (nextMember.isRelation()) {
                final Relation subRelation = nextMember.getRelation();
                setNextWay(subRelation.getMember(0).getWay());
                if (subRelation.getMembersCount() > 1) {
                    setAfterNextWay(subRelation.getMember(1).getWay());
                }
            }
        } else {
            nextWay = null;
            hasGap = true;
        }
    }

    /**
     * @param afterNextMember the relation member that comes after nextWay in the route relation
     *                        in case this member is also a relation, the last way in that relation
     *                        is used.
     *                        If wayAfterNextWay already contains a way, this is not performed anymore
     *                        Also if there was no nextWay, hasGap will be true
     */
    public void setAfterNextPtWay(RelationMember afterNextMember) {
        if (!hasGap && wayAfterNextWay == null && isPTWay(afterNextMember)) {
            if (afterNextMember.isWay()) {
                setAfterNextWay(afterNextMember.getWay());
            } else if (afterNextMember.isRelation()) {
                setAfterNextWay(afterNextMember.getRelation().getMember(0).getWay());
            }
        } else {
            wayAfterNextWay = null;
        }
    }

    /**
     * @return sense currentWay is traversed
     * 1 forward traversal
     * -1 backward traversal
     * 0 traversal can't be determined yet
     * it needs either currentWay and previousWay to be defined
     * of currentWay and nextWay to be defined already
     */
    public int getTraversalSense() {
        if (traversalSense == 0) {
            if (previousWay == null && nextWay == null) return 0;
            if (currentWay != null) {
                if (previousWay != null && currentWay.firstNode()
                    .equals(WayUtils.findCommonFirstLastNode(previousWay, currentWay).orElse(null))) {
                    traversalSense = 1;
                } else {
                    if (nextWay != null && currentWay.lastNode()
                        .equals(WayUtils.findCommonFirstLastNode(currentWay, nextWay).orElse(null))) {
                        traversalSense = 1;
                    } else {
                        traversalSense = -1;
                    }
                }
            }
        }
        return traversalSense;
    }

    /**
     * compares the traversalSense of this WaySequence with the WaySequence in ws
     * @param ws WaySequence to compare with
     * @return true if currentWay is traversed in the same direction/sense
     */
    public boolean compareTraversal(WaySequence ws) {
        assert currentWay == ws.currentWay :
            "this only works when comparing two equivalent way sequences" ;

        return getTraversalSense() == ws.getTraversalSense();
    }
}
