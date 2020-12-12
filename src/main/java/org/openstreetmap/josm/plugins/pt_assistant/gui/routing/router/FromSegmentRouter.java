package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import java.util.LinkedList;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteSegmentWay;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteType;
import org.openstreetmap.josm.tools.Pair;

/**
 * Allows to find possible routes after a given way segment.
 */
public class FromSegmentRouter extends AbstractRouter {
    private final RouteSegmentWay startAfter;

    public FromSegmentRouter(RouteSegmentWay startAfter, RouteType type) {
        super(type);
        this.startAfter = startAfter;
        if (startAfter.lastNode().getDataSet() == null) {
            throw new IllegalArgumentException("The route segment is not in the dataset");
        }
    }

    public RouteSegmentWay getStartAfter() {
        return startAfter;
    }

    @Override
    protected RouteTarget createRouteTarget(LinkedList<RouteSegmentWay> trace) {
        if (trace.size() == 1) {
            // Only start segment
            return null;
        } else {
            // Trace always contains the start segment, since we started at a segment
            return new RouteTarget(trace.subList(1, trace.size()));
        }
    }

    /**
     * Find the ways at which this router should start searching for more ways.
     * @return The set of ways. Includes the initial direciton and the distance of the start node of the way (may be negative)
     */
    @Override
    protected Stream<Pair<RouteSegmentWay, Double>> getRouterStartSegments() {
        return Stream.of(new Pair<>(startAfter, -startAfter.getLength()));
    }

    /**
     * Get the start node at which the router should start searching.
     * @return The start node
     */
    @Override
    protected Node getRoutingStartNode() {
        return startAfter.lastNode();
    }

    @Override
    public int getIndexInMembersToAddAfter() {
        return startAfter.getIndexInMembers();
    }

    @Override
    protected Stream<Way> findWaysThatCanBeSplit() {
        return super.findWaysThatCanBeSplit()
            // Don't go back to previous way
            .filter(it -> it != startAfter.getWay());
    }

    @Override
    public String toString() {
        return "FromSegmentRouter{" +
            "type=" + type +
            ", startAfter=" + startAfter +
            '}';
    }
}
