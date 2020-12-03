package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteType;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteTypes;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.Pair;

public class RouteTraverser {
    /**
     * How far may stops be away from the route?
     */
    private static final double MAX_SIDEWARD_DISTANCE = 30;
    private final RelationAccess relation;
    private final List<RouteSegment> segments;
    private final RouteType type;

    public RouteTraverser(RelationAccess relation) {
        this.relation = relation;
        this.type = RouteTypes.getRouteType(relation);
        this.segments = findSegments();
    }

    public List<RouteStop> findStopPositions() {
        List<List<RelationMember>> groupedByStop = groupByStop();

        ArrayList<RouteStop> result = new ArrayList<>();
        for (int i = 0; i < groupedByStop.size(); i++) {
            RouteStop p = new RouteStop(i + 1, groupedByStop.get(i).stream()
                .map(RelationMember::getMember).collect(Collectors.toList()));
            PointOnRoute position = p.getRelationMembers()
                .stream()
                .map(it -> it.getBBox().getCenter())
                .filter(Objects::nonNull)
                .findFirst()
                .map(this::findCloseToRoute)
                .orElse(null);
            result.add(position != null && position.getSidewardsDistanceFromRoute() < MAX_SIDEWARD_DISTANCE ? new RouteStopPositionOnWay(p, position) : p);
        }
        return result;
    }

    private List<List<RelationMember>> groupByStop() {
        List<List<RelationMember>> groupedByStop = new ArrayList<>();
        Relation lastArea = null;
        for (RelationMember m : this.relation.getMembers()) {
            if (OSMTags.STOPS_AND_PLATFORMS_ROLES.contains(m.getRole())) {
                Relation area = StopUtils.findContainingStopArea(m.getMember());
                if (area != null && area == lastArea) {
                    // Found one more item for the last area.
                    List<RelationMember> areaMembers = groupedByStop.get(groupedByStop.size() - 1);
                    // Always put stops first
                    areaMembers.add(OSMTags.STOP_ROLES.contains(m.getRole()) ? 0 : areaMembers.size(), m);
                } else {
                    groupedByStop.add(new ArrayList<>(Collections.singletonList(m)));
                    lastArea = area;
                }
            }
        }
        return groupedByStop;
    }

    private PointOnRoute findCloseToRoute(LatLon point) {
        BBox bounds = new BBox();
        bounds.add(point);
        bounds = BoundsUtils.increaseSize(bounds, MAX_SIDEWARD_DISTANCE);
        ArrayList<PointOnRoute> distances = new ArrayList<>();
        double offset = 0;
        for (RouteSegment segment : segments) {
            for (RouteSegmentWay way : segment.getWays()) {
                if (bounds.intersects(way.getWay().getBBox())) {
                    PointOnRoute found = findCloseToRoute(point, way, offset);
                    if (found != null) {
                        distances.add(found);
                    }
                }
                offset += way.getLength();
            }
        }
        return distances
            .stream()
            .min(Comparator.comparing(PointOnRoute::getSidewardsDistanceFromRoute))
            .orElse(null);
    }

    private PointOnRoute findCloseToRoute(LatLon point, RouteSegmentWay way, double startOffset) {
        ArrayList<Node> nodes = new ArrayList<>(way.getWay().getNodes());
        if (!way.isForward()) {
            Collections.reverse(nodes);
        }
        if (nodes.size() == 0) {
            throw new IllegalArgumentException("Way without nodes: " + way);
        }
        Pair<LatLon, Double> bestClosest = null;
        double bestOffset = 0;
        double offset = 0;
        LatLon lastNode = nodes.get(0).getCoor();
        for (int i = 1; i < nodes.size(); i++) {
            LatLon currentNode = nodes.get(i).getCoor();
            if (currentNode.isValid() && lastNode.isValid()) {
                Pair<LatLon, Double> closest = findClosest(lastNode, currentNode, point);
                if (bestClosest == null || closest.b < bestClosest.b) {
                    bestClosest = closest;
                    bestOffset = offset + currentNode.greatCircleDistance(closest.a);
                }
                offset += lastNode.greatCircleDistance(currentNode);
            }
            lastNode = currentNode;
        }
        if (bestClosest == null) {
            return null;
        } else {
            return new PointOnRoute(
                way,
                bestOffset,
                startOffset + bestOffset,
                bestClosest.b
            );
        }
    }

    private Pair<LatLon, Double> findClosest(LatLon start, LatLon end, LatLon toPoint) {
        double toStart = start.greatCircleDistance(toPoint);
        double toEnd = end.greatCircleDistance(toPoint);

        if (start.greatCircleDistance(end) < 5) {
            return toStart < toEnd
                ? new Pair<>(start, toStart)
                : new Pair<>(end, toEnd);
        } else {
            LatLon center = start.interpolate(end, 0.5);
            return toStart < toEnd
                ? findClosest(start, center, toPoint)
                : findClosest(center, end, toPoint);
        }
    }

    private List<RouteSegment> findSegments() {
        List<Way> ways = getSegmentMembers()
            .filter(rm -> OSMTags.ROUTE_SEGMENT_PT_ROLES.contains(rm.getRole()))
            .filter(RelationMember::isWay)
            .map(RelationMember::getWay)
            .filter(way -> !way.isIncomplete())
            .collect(Collectors.toList());

        List<RouteSegment> segments = new ArrayList<>();
        List<Way> currentSegment = new ArrayList<>();
        Collection<Node> lastNodes = Collections.emptySet();
        for (Way way : ways) {
            List<Node> wayEnds = getWayEnds(way);
            if (lastNodes.isEmpty()) {
                currentSegment.add(way);
            } else {
                if (lastNodes.stream().noneMatch(wayEnds::contains)) {
                    // Not connected => commit current segment
                    segments.add(createRouteSegment(currentSegment));
                    currentSegment.clear();
                }
                currentSegment.add(way);
            }

            Collection<Node> lastNodes1 = lastNodes;
            lastNodes = wayEnds
                .stream()
                .filter(it -> !lastNodes1.contains(it))
                .collect(Collectors.toList());

        }
        segments.add(createRouteSegment(currentSegment));
        return segments;
    }

    private RouteSegment createRouteSegment(List<Way> rawWays) {
        List<RouteSegmentWay> ways = new ArrayList<>();
        if (rawWays.isEmpty()) {
            throw new IllegalArgumentException("RawWays may not be empty.");
        } else if (rawWays.size() == 1) {
            // We don't know the direction. Let's just assume forward.
            ways.add(createRouteSegmentWay(rawWays.get(0), true, ways));
        } else {
            ways.add(createRouteSegmentWay(rawWays.get(0),
                rawWays.get(1).isFirstLastNode(rawWays.get(0).lastNode()), ways));
            for (int i = 1; i < rawWays.size(); i++) {
                ways.add(createRouteSegmentWay(rawWays.get(i),
                    rawWays.get(i - 1).isFirstLastNode(rawWays.get(i).firstNode()), ways));
            }
        }
        return new RouteSegment(ways);
    }

    private RouteSegmentWay createRouteSegmentWay(Way way, boolean forward, List<RouteSegmentWay> previous) {
        // Check way is suited
        RouteType.AccessDirection mayDriveOn = type.mayDriveOn(way.getKeys());
        WaySuitability suitability;
        if ((mayDriveOn == RouteType.AccessDirection.FORWARD_ONLY && !forward)
         || mayDriveOn == RouteType.AccessDirection.BACKWARD_ONLY && forward) {
            suitability = WaySuitability.WRONG_DIRECTION;
        } else if (mayDriveOn == RouteType.AccessDirection.NONE) {
            suitability = WaySuitability.WRONG_TYPE;
        } else {
            // Negative turn restricitons (preventing us from turning into this way)
            // Single via node matches
            Optional<Relation> wrongTurnRestriction = findTurnRestrictions(way, "no_")
                .filter(r -> r.findRelationMembers("to").contains(way))
                .filter(r -> {
                    Set<Way> fromPath = findFromPath(r);
                    if (fromPath.size() < previous.size()) {
                        return false;
                    } else {
                        Set<Way> whereWeComeFrom = previous.subList(previous.size() - fromPath.size(), previous.size())
                            .stream().map(RouteSegmentWay::getWay).collect(Collectors.toSet());
                        return fromPath.equals(whereWeComeFrom)
                            && (fromPath.size() > 1
                            // Single via node matches
                            || getViaNode(r).map(via -> previous.get(previous.size() - 1).lastNode().equals(via)).orElse(true));
                    }
                })
                .findFirst();
            if (wrongTurnRestriction.isPresent()) {
                suitability = WaySuitability.CANNOT_TURN_INTO;
            } else {
                if (previous.size() > 0 && findTurnRestrictions(previous.get(previous.size() - 1).getWay(), "only_")
                    // Only relations starting at previous way
                    .filter(restriction -> restriction.getMembers()
                            .stream()
                            .filter(RelationMember::isWay)
                            .filter(member -> "from".equals(member.getRole()))
                            .map(RelationMember::getWay)
                            .anyMatch(previous.get(previous.size() - 1).getWay()::equals))
                    // Only restrictions continuing in our direction
                    .anyMatch(restriction -> getViaNode(restriction)
                        .map(viaNode -> previous.get(previous.size() - 1).lastNode().equals(viaNode))
                        .orElseGet(() -> restriction.getMembers()
                        .stream()
                        .filter(RelationMember::isWay)
                        .map(RelationMember::getWay)
                        .anyMatch(way::equals)))) {
                    suitability = WaySuitability.INVALID_TURN_FROM;
                } else {
                    suitability = WaySuitability.GOOD;
                }
            }
        }

        return new RouteSegmentWay(way, forward, suitability);
    }

    private Stream<Relation> findTurnRestrictions(Way way, String prefix) {
        return way.referrers(Relation.class)
            .filter(r -> r.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.RELATION_TYPE_TURN_RESTRICTION))
            .filter(r -> {
                String restrictionValue = type.getRestrictionValue(r);
                return restrictionValue != null && restrictionValue.startsWith(prefix);
            });
    }

    private Optional<Node> getViaNode(Relation restriction) {
        return relation
            .getMembers()
            .stream()
            .filter(RelationMember::isNode)
            .filter(it -> "via".equals(it.getRole()))
            .map(RelationMember::getNode)
            .findFirst();
    }

    private Set<Way> findFromPath(Relation restriction) {
        return restriction
            .getMembers()
            .stream()
            .filter(RelationMember::isWay)
            .filter(it -> "via".equals(it.getRole()) || "from".equals(it.getRole()))
            .map(RelationMember::getWay)
            .collect(Collectors.toSet());
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    private static List<Node> getWayEnds(Way way) {
        return Arrays.asList(
            way.firstNode(),
            way.lastNode());
    }

    private Stream<RelationMember> getSegmentMembers() {
        if (relation.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.KEY_ROUTE)) {
            return relation.getMembers()
                .stream();
        } else if (relation.hasTag(OSMTags.KEY_RELATION_TYPE, "superroute")) {
            return relation
                .getMembers()
                .stream()
                .filter(RelationMember::isRelation)
                .map(RelationMember::getRelation)
                .flatMap(r -> r.getMembers().stream());
        } else {
            return Stream.of(); // < None
        }
    }

}
