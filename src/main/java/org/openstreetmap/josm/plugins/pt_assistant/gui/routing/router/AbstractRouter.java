package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteSegmentWay;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.WaySuitability;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteType;
import org.openstreetmap.josm.tools.Pair;

/**
 * Helps traversing possible next ways of a given route.
 *
 * Starts at a given start position. Then searches for the next possibilites that could be added to the way.
 */
public abstract class AbstractRouter {
    protected final RouteType type;

    protected AbstractRouter(RouteType type) {
        this.type = type;
    }

    public RouteType getType() {
        return type;
    }

    /**
     * Does a Dijkstra search around the start node to find the next possible ways the route may take.
     * <br/>
     * It will only return valid way endpoints as possible points the route could go to.
     * <br/>
     * Although we limit by the number of way segments,
     *
     * @param distance The minimum distance to search - in meters. Good if a road is split into many small parts.
     * @param waySegmentCount The mininum way segment count. Good for urban areas where there are not many way splits.
     * @return The possible route targets, ordered by distance.
     */
    public List<RouteTarget> findRouteTargets(int distance, int waySegmentCount) {
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance needs to be at least 1");
        }
        if (waySegmentCount <= 0) {
            throw new IllegalArgumentException("waySegmentCount needs to be at least 1");
        }

        Map<RouteSegmentWay, RouteSegmentWay> parents = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        Map<Node, Double> distances = new HashMap<>();
        HashSet<RouteSegmentWay> toSearchAfterNext = new HashSet<>();
        List<RouteTarget> targets = new ArrayList<>();

        getRouterStartSegments().forEach(startPair -> {
            RouteSegmentWay segment = startPair.a;
            distances.put(segment.firstNode(), startPair.b);
            // Put the first one in the list => prevents directly going back that way
            visited.add(segment.firstNode());
            toSearchAfterNext.add(segment);
        });

        while (!toSearchAfterNext.isEmpty()) {
            RouteSegmentWay next = toSearchAfterNext
                .stream()
                .min(Comparator.comparing(it -> distances.get(it.firstNode()) + it.getLength()))
                .orElseThrow(() -> new IllegalArgumentException("Expected one element in toSearchAfterNext"));

            Double distanceAtStart = distances.get(next.firstNode());
            if (distanceAtStart == null || Double.isNaN(distanceAtStart)) {
                throw new IllegalArgumentException("Not a vialid distance: " + distanceAtStart);
            }
            double distanceAtEndOfNext = distanceAtStart + next.getLength();
            distances.put(next.lastNode(), distanceAtEndOfNext);
            visited.add(next.lastNode());
            toSearchAfterNext.removeIf(it -> it.lastNode() == next.lastNode());
            LinkedList<RouteSegmentWay> trace = backtrace(parents, next);
            Set<Node> nodesInTrace = trace
                .stream()
                .flatMap(it -> it.getWay().getNodes().stream())
                .collect(Collectors.toSet());
            nodesInTrace.remove(next.lastNode()); // < that one is allowed
            RouteTarget target = createRouteTarget(trace);
            if (target != null) {
                targets.add(target);
            }

            next.lastNode()
                .referrers(Way.class)
                // We only want ways starting / ending at next.lastNode()
                .filter(it -> it.isFirstLastNode(next.lastNode()))
                // We only want ways that we have not seen.
                // next.lastNode is one end and in the visited set => the other one may not be visited.
                .filter(it -> !visited.contains(it.firstNode()) || !visited.contains(it.lastNode()))
                // No loops => this is very confusing to user
                // Loops can happen if the node was a middle node in the trace.
                // A split would be required at that node then to not go the loop.
                .filter(it -> it.getNodes().stream().noneMatch(nodesInTrace::contains))
                // Now convert to a way segment
                .map(it -> {
                    RouteSegmentWay way = type.createRouteSegmentWay(it, it.firstNode().equals(next.lastNode()),
                        next.getIndexInMembers() + 1, trace);
                    // Skip the ones that are too far away
                    if (distanceAtEndOfNext + way.getLength() > distance
                        && parents.size() + 1 > waySegmentCount) {
                        return null;
                    } else {
                        return way;
                    }
                })
                .filter(Objects::nonNull)
                // Exclude the ones that we cannot go on.
                .filter(this::isWaySuitable)
                // Handle the way segments => schedule them and mark their parent
                .forEach(it -> {
                    parents.put(it, next);
                    toSearchAfterNext.add(it);
                });
        }
        return targets;
    }

    protected boolean isWaySuitable(RouteSegmentWay it) {
        return it.getSuitability() == WaySuitability.GOOD;
    }

    protected RouteTarget createRouteTarget(LinkedList<RouteSegmentWay> trace) {
        return new RouteTarget(trace);
    }

    protected abstract Stream<Pair<RouteSegmentWay, Double>> getRouterStartSegments();

    private LinkedList<RouteSegmentWay> backtrace(Map<RouteSegmentWay, RouteSegmentWay> parents, RouteSegmentWay next) {
        LinkedList<RouteSegmentWay> myParents = new LinkedList<>();
        myParents.addFirst(next);
        while (parents.containsKey(myParents.getFirst())) {
            myParents.addFirst(parents.get(myParents.getFirst()));
        }
        return myParents;
    }

    /**
     * Find all possible split actions. That is all ways we can use after they were split at the right place.
     * @return A list of all suggested route splits
     */
    public List<RouteSplitSuggestion> findRouteSplits() {
        Set<Node> excludedNodes = Stream.concat(
            // Never attempt to go to our current node
            Stream.of(getRoutingStartNode()),
            // We also exclude every node that can be reached directly
            // So if there is a direct way to the target that does not require splitting, we suggest that one.
            findWaysThatCanBeSplit()
                .filter(way -> way.isFirstLastNode(getRoutingStartNode()))
                .flatMap(way -> Stream.of(way.firstNode(), way.lastNode()))
        ).collect(Collectors.toSet());
        return findWaysThatCanBeSplit()
            // Find the split points
            .flatMap(way -> findInterestingNodes(way)
                .filter(nodeIndex -> !excludedNodes.contains(way.getNode(nodeIndex)))
                .mapToObj(nodeIndex -> new Pair<>(way, nodeIndex)))
            // Currently, we do not support multiple choices for circular ways that visit lastNode() twice.
            .map(pair -> new RouteSplitSuggestion(pair.a, pair.a.getNodes().indexOf(getRoutingStartNode()),
                pair.b))
            .collect(Collectors.toList());
    }

    protected Stream<Way> findWaysThatCanBeSplit() {
        return getRoutingStartNode()
            // All ways intersecting at current point
            .referrers(Way.class);
    }

    protected abstract Node getRoutingStartNode();

    /**
     * Find all interesting points along a way that it could be split for (including end point)
     * @param way The way to search for
     * @return A stream of node indexes in the Way
     */
    private IntStream findInterestingNodes(Way way) {
        return IntStream.range(0, way.getNodesCount())
            .filter(nodeIndex -> {
                Node node = way.getNode(nodeIndex);
                return way.isFirstLastNode(node) || node.getReferrers().size() > 1
                    || node.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_POSITION_TAG_VALUE);
            });
    }

    public abstract int getIndexInMembersToAddAfter();
}
