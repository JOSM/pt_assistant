package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteType;

/**
 * Allows to find possible routes around a given starting point.
 *
 * Does a forward search on the route.
 */
public class Router {
    private final RouteSegmentWay startAfter;
    private final RouteType type;

    public Router(RouteSegmentWay startAfter, RouteType type) {
        this.startAfter = startAfter;
        this.type = type;
        if (startAfter.lastNode().getDataSet() == null) {
            throw new IllegalArgumentException("The route segment is not in the dataset");
        }
    }

    public RouteSegmentWay getStartAfter() {
        return startAfter;
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

        distances.put(startAfter.firstNode(), -startAfter.getLength());
        // Put the first one in the list => prevents directly going back that way
        visited.add(startAfter.firstNode());
        toSearchAfterNext.add(startAfter);

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
            targets.add(new RouteTarget(trace));

            next.lastNode()
                .referrers(Way.class)
                // We only want ways starting / ending at next.lastNode()
                .filter(it -> it.isFirstLastNode(next.lastNode()))
                // We only want ways that we have not seen.
                // next.lastNode is one end and in the visited set => the other one may not be visited.
                .filter(it -> !visited.contains(it.firstNode()) || !visited.contains(it.lastNode()))
                // Now convert to a way segment
                .map(it -> {
                    RouteSegmentWay way = type.createRouteSegmentWay(it, it.firstNode().equals(next.lastNode()), startAfter.getIndexInMembers() + trace.size(), trace);
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
                .filter(it -> it.getSuitability() == WaySuitability.GOOD)
                // Handle the way segments => schedule them and mark their parent
                .forEach(it -> {
                    parents.put(it, next);
                    toSearchAfterNext.add(it);
                });
        }
        return targets;
    }

    private LinkedList<RouteSegmentWay> backtrace(Map<RouteSegmentWay, RouteSegmentWay> parents, RouteSegmentWay next) {
        LinkedList<RouteSegmentWay> myParents = new LinkedList<>();
        myParents.addFirst(next);
        while (parents.containsKey(myParents.getFirst())) {
            myParents.addFirst(parents.get(myParents.getFirst()));
        }
        return myParents;
    }
}
