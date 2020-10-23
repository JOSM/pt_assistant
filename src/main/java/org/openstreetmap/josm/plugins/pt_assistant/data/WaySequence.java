package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;

import java.util.List;

import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;

/**
 * This class keeps track of up to 4 consecutive ways
 * that are most likely members in a route relation
 */
public final class WaySequence {
    public boolean hasGap = false;
    public int traversalSense;
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

    public int getTraversalSense() {
        if (traversalSense == 0) {
            if (currentWay != null) {
                if (previousWay != null || nextWay != null) {
                    if (currentWay.firstNode().equals(WayUtils.findCommonFirstLastNode(previousWay, currentWay).orElse(null))
                      && currentWay.lastNode().equals(WayUtils.findCommonFirstLastNode(currentWay, nextWay).orElse(null))) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        } else {
            return traversalSense;
        }
        return 0;
    }

    public boolean isItineraryInSameDirection(WaySequence parent_ws) {
        assert currentWay == parent_ws.currentWay :
            "this only works when comparing two equivalent way sequences" ;

        // if all ways are present, try the simple solution first
        if (previousWay != null
            && nextWay != null
            && parent_ws.previousWay != null
            && parent_ws.nextWay != null
            && (previousWay == parent_ws.previousWay
                ||  nextWay == parent_ws.nextWay)
        ) {
            return (!previousWay.equals(parent_ws.nextWay) &&
                    !nextWay.    equals(parent_ws.previousWay));
        }

        // if not, compare on the nodes
        Node firstNodeCurrentWay = null;
        if (previousWay != null) {
            firstNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                previousWay, currentWay).orElse(null);
        }
        Node lastNodeCurrentWay = null;
        if (nextWay != null) {
            lastNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                currentWay, nextWay).orElse(null);
        }
        Node firstNodeWayOfParent = null;
        if (parent_ws.previousWay != null) {
            firstNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                parent_ws.previousWay, parent_ws.currentWay).orElse(null);
        }
        Node lastNodeWayOfParent = null;
        if (parent_ws.nextWay != null) {
            lastNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                parent_ws.currentWay, parent_ws.nextWay).orElse(null);
        }

        return (firstNodeCurrentWay != null && firstNodeCurrentWay.equals(firstNodeWayOfParent)
                ||
                lastNodeCurrentWay != null && lastNodeCurrentWay.equals(lastNodeWayOfParent));
    }
}
