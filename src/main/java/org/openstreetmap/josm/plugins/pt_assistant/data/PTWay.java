// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Representation of PTWays, which can be of OsmPrimitiveType Way or Relation
 *
 * @author darya
 *
 */
public class PTWay extends RelationMember {

    /*
     * Ways that belong to this PTWay. If the corresponding relation member is
     * OsmPrimitiveType.WAY, this list size is 1. If the corresponding relation
     * member is a nested relation, the list size is >= 1.
     */
    private List<Way> ways = new ArrayList<>();
    public List<PTStop> RightStops = new ArrayList<>();
    public List<PTStop> LeftStops = new ArrayList<>();
    public List<PTStop> allStops = new ArrayList<>();

    /**
     *
     * @param other
     *            the corresponding RelationMember
     * @throws IllegalArgumentException
     *             if the given relation member cannot be a PTWay due to its
     *             OsmPrimitiveType and/or role.
     */
    public PTWay(RelationMember other) throws IllegalArgumentException {

        super(other);

        if (other.getType().equals(OsmPrimitiveType.WAY)) {
            ways.add(other.getWay());
        } else if (other.getType().equals(OsmPrimitiveType.RELATION)) {
            for (RelationMember rm : other.getRelation().getMembers()) {
                if (rm.getType().equals(OsmPrimitiveType.WAY)) {
                    ways.add(rm.getWay());
                } else {
                    throw new IllegalArgumentException(
                            "A route relation member of OsmPrimitiveType.RELATION can only have ways as members");
                }
            }
        } else {
            // the RelationMember other cannot be a OsmPrimitiveType.NODE
            throw new IllegalArgumentException("A node cannot be used to model a public transport way");
        }

    }

    /**
     * Returns the course of this PTWay. In most cases, this list only has 1
     * element. In the case of nested relations in a route, the list can have
     * multiple elements.
     *
     * @return the course of this PTWay
     */

    public List<Way> getWays() {
        return this.ways;
    }

    /**
     * Determines if this PTWay is modeled by an OsmPrimitiveType.WAY
     */
    @Override
    public boolean isWay() {
        if (this.getType().equals(OsmPrimitiveType.WAY)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if this PTWay is modeled by an OsmPrimitieType.RELATION (i.e.
     * this is a nested relation)
     */
    @Override
    public boolean isRelation() {
        if (this.getType().equals(OsmPrimitiveType.RELATION)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the end nodes of this PTWay. If this PTWay is a nested relation,
     * the order of the composing ways is assumed to be correct
     *
     * @return the end nodes of this PTWay
     */

    public Node[] getEndNodes() {
        Node[] endNodes = new Node[2];
        if (this.isWay()) {
            endNodes[0] = this.getWay().firstNode();
            endNodes[1] = this.getWay().lastNode();
            // TODO: if this is a roundabout
        } else { // nested relation:
            Way firstWay = this.getWays().get(0);
            Way secondWay = this.getWays().get(1);
            Way prelastWay = this.getWays().get(this.getWays().size() - 2);
            Way lastWay = this.getWays().get(this.getWays().size() - 1);
            if (firstWay.firstNode() == secondWay.firstNode() || firstWay.firstNode() == secondWay.lastNode()) {
                endNodes[0] = firstWay.lastNode();
            } else {
                endNodes[0] = firstWay.firstNode();
            }
            if (lastWay.firstNode() == prelastWay.firstNode() || lastWay.firstNode() == prelastWay.lastNode()) {
                endNodes[1] = lastWay.lastNode();
            } else {
                endNodes[1] = lastWay.firstNode();
            }
        }

        return endNodes;
    }

    /**
     * Checks if this PTWay contains an unsplit roundabout (i.e. a way that
     * touches itself) among its ways
     *
     * @return {@code true} if this PTWay contains an unsplit roundabout
     */

    public boolean containsUnsplitRoundabout() {

        List<Way> ways = this.getWays();
        for (Way way : ways) {
            if (way.firstNode() == way.lastNode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the first Way of this PTWay is an unsplit roundabout (i.e. a
     * way that touches itself)
     *
     * @return {@code true} if the first Way of this PTWay is an unsplit roundabout
     */

    public boolean startsWithUnsplitRoundabout() {
        if (this.ways.get(0).firstNode() == this.ways.get(0).lastNode()) {
            return true;
        }
        return false;
    }

    public List<PTStop> getAllStops(Way w) {
        Collection<Node> allNodes = w.getDataSet().getNodes();
        List<Node> potentialStops = new ArrayList<>();
        for (Node currentNode : allNodes) {
            String nodeName = currentNode.get("name");
            if (StopUtils.isHighwayOrRailwayStopPosition(currentNode) || StopUtils.isStopPosition(currentNode)
                    || StopUtils.verifyStopAreaPlatform(currentNode)
                    || StopUtils.verifyIfMemberOfStopArea(currentNode)) {
                potentialStops.add(currentNode);
            }
        }
        for (Node node : potentialStops) {
            if (CheckItIsPTStopOrNot(node) != null) {
                PTStop pts = CheckItIsPTStopOrNot(node);
                if (pts.findServingWays(pts) != null) {
                    if (pts.findServingWays(pts).equals(w)) {
                        allStops.add(pts);
                    }
                }
            }
        }
        return allStops;
    }
    public List<PTStop> getRightStops(Way w){
      List<PTStop> allStp = getAllStops(w);
      for (PTStop stop : allStp) {
          if (CrossProduct(w.firstNode(), w.lastNode(), stop)) {
              RightStops.add(stop);
          } else {
              LeftStops.add(stop);
          }
      }
      return RightStops;
    }

    public List<PTStop> getLeftStops(Way w){
      List<PTStop> allStp = getAllStops(w);
      for (PTStop stop : allStp) {
          if (CrossProduct(w.firstNode(), w.lastNode(), stop)) {
              RightStops.add(stop);
          } else {
              LeftStops.add(stop);
          }
      }
      return LeftStops;
    }

    public PTStop CheckItIsPTStopOrNot(Node stop) {
        List<OsmPrimitive> referrers = stop.getReferrers();
        for (OsmPrimitive referredPrimitive : referrers) {
            if (referredPrimitive.getType().equals(OsmPrimitiveType.RELATION)) {
                Relation referredRelation = (Relation) referredPrimitive;
                if (checkRelationContainsStop(referredRelation, stop) != null) {
                    PTStop pts = new PTStop(checkRelationContainsStop(referredRelation, stop));
                    return pts;
                }
            }
        }
        return null;
    }

    RelationMember checkRelationContainsStop(Relation rel, Node node) {
        for (RelationMember rm : rel.getMembers()) {
            if (rm.getUniqueId() == node.getUniqueId()) {
                return rm;
            }
        }
        return null;
    }

    public boolean CrossProduct(Node node1, Node node2, PTStop stop) {
        LatLon coord3;
        if (stop.getPlatform() != null) {
            coord3 = stop.getPlatform().getBBox().getCenter();
        } else {
            Node node3 = stop.getNode();
            coord3 = new LatLon(node3.lat(), node3.lon());
        }
        LatLon coord1 = new LatLon(node1.lat(), node1.lon());
        LatLon coord2 = new LatLon(node2.lat(), node2.lon());
        //       LatLon coord3 = new LatLon(node3.lat(),node3.lon());
        double x1 = coord1.getX();
        double y1 = coord1.getY();

        double x2 = coord2.getX();
        double y2 = coord2.getY();

        double x3 = coord3.getX();
        double y3 = coord3.getY();

        x1 -= x3;
        y1 -= y3;

        x2 -= x3;
        y2 -= y3;

        double crossprod = x1 * y2 - y1 * x2;

        //Right Direction
        if (crossprod <= 0) {
            return true;
        }
        //left Direction
        return false;
    }

    public List<PTStop> getRightStops() {
        return this.RightStops;
    }

    public List<PTStop> getLeftStops() {
        return this.LeftStops;
    }

    /**
     * Checks if the last Way of this PTWay is an unsplit roundabout (i.e. a way
     * that touches itself)
     *
     * @return {@code true} if the last Way of this PTWay is an unsplit roundabout
     */

    public boolean endsWithUnsplitRoundabout() {
        if (this.ways.get(this.ways.size() - 1).firstNode() == this.ways.get(this.ways.size() - 1).lastNode()) {
            return true;
        }
        return false;
    }

    public void addLeftStop(PTStop stop) {
        LeftStops.add(stop);
    }

    public void addRightStop(PTStop stop) {
        RightStops.add(stop);
    }
}
