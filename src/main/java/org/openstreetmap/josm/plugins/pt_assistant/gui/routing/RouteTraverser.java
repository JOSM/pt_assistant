package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    public RouteType getType() {
        return type;
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
        List<IndexedRelationMember> ways = getSegmentMembers()
            .filter(rm -> OSMTags.ROUTE_SEGMENT_PT_ROLES.contains(rm.getMember().getRole()))
            .filter(it -> it.getMember().isWay())
            .filter(it -> !it.getMember().getWay().isIncomplete())
            .collect(Collectors.toList());

        List<RouteSegment> segments = new ArrayList<>();
        List<IndexedRelationMember> currentSegment = new ArrayList<>();
        Collection<Node> lastNodes = Collections.emptySet();
        for (IndexedRelationMember wayWithIndex : ways) {
            Way way = wayWithIndex.getMember().getWay();
            List<Node> wayEnds = getWayEnds(way);
            if (lastNodes.isEmpty()) {
                currentSegment.add(wayWithIndex);
            } else {
                if (lastNodes.stream().noneMatch(wayEnds::contains)) {
                    // Not connected => commit current segment
                    segments.add(createRouteSegment(currentSegment));
                    currentSegment.clear();
                }
                currentSegment.add(wayWithIndex);
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

    private RouteSegment createRouteSegment(List<IndexedRelationMember> rawWays) {
        List<RouteSegmentWay> ways = new ArrayList<>();
        if (rawWays.isEmpty()) {
            throw new IllegalArgumentException("RawWays may not be empty.");
        } else if (rawWays.size() == 1) {
            // We don't know the direction. Let's just assume forward.
            ways.add(type.createRouteSegmentWay(rawWays.get(0), true, ways));
        } else {
            // Determine direction by next segment
            ways.add(type.createRouteSegmentWay(rawWays.get(0),
                rawWays.get(1).getMember().getWay().isFirstLastNode(rawWays.get(0).getMember().getWay().lastNode()), ways));
            for (int i = 1; i < rawWays.size(); i++) {
                // Determine direction by previous segment
                ways.add(type.createRouteSegmentWay(rawWays.get(i),
                    rawWays.get(i - 1).getMember().getWay().isFirstLastNode(rawWays.get(i).getMember().getWay().firstNode()), ways));
            }
        }
        return new RouteSegment(ways);
    }


    public List<RouteSegment> getSegments() {
        return segments;
    }

    private static List<Node> getWayEnds(Way way) {
        return Arrays.asList(
            way.firstNode(),
            way.lastNode());
    }

    private Stream<IndexedRelationMember> getSegmentMembers() {
        if (relation.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.KEY_ROUTE)) {
            return streamMembersIndexed(relation);
        } else if (relation.hasTag(OSMTags.KEY_RELATION_TYPE, "superroute")) {
            return relation
                .getMembers()
                .stream()
                .filter(RelationMember::isRelation)
                .map(RelationMember::getRelation)
                .map(RelationAccess::of)
                .flatMap(this::streamMembersIndexed);
        } else {
            return Stream.of(); // < None
        }
    }

    private Stream<IndexedRelationMember> streamMembersIndexed(RelationAccess relation) {
        return IntStream.range(0, relation.getMembers().size())
            .mapToObj(index -> new IndexedRelationMember(relation.getMembers().get(index), index, relation));
    }

}
