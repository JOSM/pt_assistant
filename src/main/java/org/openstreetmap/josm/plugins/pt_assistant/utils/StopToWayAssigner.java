// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTWay;

/**
 * Assigns stops to ways in following steps: (1) checks if the stop is in the
 * list of already assigned stops, (2) checks if the stop has a stop position,
 * (3) calculates it using proximity / growing bounding boxes
 *
 * @author darya
 *
 */
public final class StopToWayAssigner {

    /* contains assigned stops */
    private final Map<PTStop, List<Way>> stopToWay = new HashMap<>();

    /*
     * contains all PTWays of the route relation for which this assigner was created
     */
    private final Set<Way> ways = new HashSet<>();

    public StopToWayAssigner(List<PTWay> ptways) {
        for (PTWay ptway : ptways) {
            ways.addAll(ptway.getWays());
        }
    }

    public StopToWayAssigner(Collection<Way> ways) {
        this.ways.addAll(ways);
    }

    public StopToWayAssigner() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Returns the PTWay for the given PTStop
     *
     * @param stop stop
     * @return the PTWay for the given PTStop
     */

    public Way get(PTStop stop) {

        // 1) Search if this stop has already been assigned:
        if (stopToWay.containsKey(stop)) {
            List<Way> assignedWays = stopToWay.get(stop);
            for (Way assignedWay : assignedWays) {
                if (this.ways.contains(assignedWay)) {
                    return assignedWay;
                }
            }
        }

        // 2) Search if the stop has a stop position:
        Way wayOfStopPosition = findWayForNode(stop.getStopPosition());
        if (wayOfStopPosition != null) {
            addAssignedWayToMap(stop, wayOfStopPosition);
            return wayOfStopPosition;
        }

        // 3) Search if the stop has a stop_area:
        List<OsmPrimitive> stopElements = new ArrayList<>(2);
        if (stop.getStopPosition() != null) {
            stopElements.add(stop.getStopPosition());
        }
        if (stop.getPlatform() != null) {
            stopElements.add(stop.getPlatform());
        }
        Set<Relation> parents = Node.getParentRelations(stopElements);
        for (Relation parentRelation : parents) {
            if (StopUtils.isStopArea(parentRelation)) {
                for (RelationMember rm : parentRelation.getMembers()) {
                    if (StopUtils.isStopPosition(rm.getMember())) {
                        Way rmWay = this.findWayForNode(rm.getNode());
                        if (rmWay != null) {
                            addAssignedWayToMap(stop, rmWay);
                            return rmWay;
                        }
                    }
                }
            }
        }

        // 4) Search if a stop position is in the vicinity of a platform:
        if (stop.getPlatform() != null) {
            List<Node> potentialStopPositionList = stop.findPotentialStopPositions();
            final Optional<Node> closestStopPosition = potentialStopPositionList.stream()
                .min(
                    Comparator.comparingDouble(stopPosition ->
                        GeometryUtils.distanceSquared(stopPosition, stop.getPlatform().getBBox().getCenter())
                    )
                );
            if (closestStopPosition.isPresent()) {
                final Optional<Way> closestWay = this.ways.stream()
                    .filter(way -> way.containsNode(closestStopPosition.get()))
                    .collect(WayUtils.nearestToPointCollector(stop.getPlatform().getBBox().getCenter()));
                if (closestWay.isPresent()) {
                    addAssignedWayToMap(stop, closestWay.get());
                    return closestWay.get();
                }
            }
        }

        // 5) Run the growing-bounding-boxes algorithm:
        double searchRadius = 0.001;
        while (searchRadius < 0.005) {

            Way foundWay = this.findNearestWayInRadius(stop.getPlatform(), searchRadius);

            if (foundWay != null) {
                addAssignedWayToMap(stop, foundWay);
                return foundWay;
            }

            foundWay = this.findNearestWayInRadius(stop.getStopPosition(), searchRadius);

            if (foundWay != null) {
                addAssignedWayToMap(stop, foundWay);
                return foundWay;
            }

            searchRadius = searchRadius + 0.001;
        }

        return null;
    }

    /**
     * Finds the PTWay of the given stop_position by looking at its referrers
     *
     * @param stopPosition stop position
     * @return the PTWay of the given stop_position by looking at its referrers
     */

    private Way findWayForNode(Node stopPosition) {

        if (stopPosition == null) {
            return null;
        }

        // search in the referrers:
        List<OsmPrimitive> referrers = stopPosition.getReferrers();
        for (OsmPrimitive referredPrimitive : referrers) {
            if (referredPrimitive.getType().equals(OsmPrimitiveType.WAY)) {
                Way referredWay = (Way) referredPrimitive;
                if (this.ways.contains(referredWay)) {
                    return referredWay;
                }
            }
        }

        return null;
    }

    public Way findClosestWayFromRelation(Relation rel, PTStop stop, Node closestStopPosition) {
        if (rel == null || stop == null || closestStopPosition == null) {
            return null;
        }
        return rel.getMembers().stream()
            .filter(it -> it.getType() == OsmPrimitiveType.WAY)
            .map(RelationMember::getWay)
            .filter(way -> way.containsNode(closestStopPosition))
            .collect(WayUtils.nearestToPointCollector(stop))
            .orElse(null);
    }

    /**
     * Finds the PTWay in the given radius of the OsmPrimitive. The PTWay has to
     * belong to the route relation for which this StopToWayAssigner was
     * created. If multiple PTWays were found, the closest one is chosen.
     *
     * @param platform platform
     * @param searchRadius search radius
     * @return the PTWay in the given radius of the OsmPrimitive
     */
    private Way findNearestWayInRadius(OsmPrimitive platform, double searchRadius) {

        if (platform == null) {
            return null;
        }

        LatLon platformCenter = platform.getBBox().getCenter();
        double ax = platformCenter.getX() - searchRadius;
        double bx = platformCenter.getX() + searchRadius;
        double ay = platformCenter.getY() - searchRadius;
        double by = platformCenter.getY() + searchRadius;
        BBox platformBBox = new BBox(ax, ay, bx, by);

        final ILatLon platformNode;
        if (OsmPrimitiveType.NODE.equals(platform.getType())) {
            platformNode = (Node) platform;
        } else {
            platformNode = platform.getBBox().getCenter();
        }

        return platform.getDataSet().getNodes().stream()
            .filter(node -> platformBBox.bounds(node.getBBox())) // only nodes in BBox
            .flatMap(node -> node.getReferrers().stream()) // operate on the referrers of the nodes
            .map(referrer -> OsmPrimitiveType.WAY.equals(referrer.getType()) ? (Way) referrer : null) // keep only the ways
            .filter(Objects::nonNull)
            .filter(this.ways::contains)
            .collect(WayUtils.nearestToPointCollector(platformNode)) // find the way that is closest to the platformNode
            .orElse(null);
    }

    /**
     * Adds the given way to the map of assigned ways. Assumes that the given
     * way is not contained in the map.
     *
     * @param stop stop
     * @param way way
     */
    private void addAssignedWayToMap(PTStop stop, Way way) {
        if (stopToWay.containsKey(stop)) {
            List<Way> assignedWays = stopToWay.get(stop);
            assignedWays.add(way);
        } else {
            List<Way> assignedWays = new ArrayList<>();
            assignedWays.add(way);
            stopToWay.put(stop, assignedWays);
        }
    }
}
