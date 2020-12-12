package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteSegmentWay;

/**
 * A route that was found from a given start node.
 */
public class RouteTarget {
    private final List<RouteSegmentWay> trace;

    /**
     * Create a new route target
     * @param trace The ways that can be added after the end of the current route / route segment.
     */
    public RouteTarget(List<RouteSegmentWay> trace) {
        this.trace = new ArrayList<>(Objects.requireNonNull(trace, "trace"));
        if (trace.size() < 1) {
            throw new IllegalArgumentException("Trace may not be empty.");
        }
    }

    public List<RouteSegmentWay> getTrace() {
        return trace;
    }

    public Node getEnd() {
        return trace.get(trace.size() - 1).lastNode();
    }


    @Override
    public String toString() {
        return "RouteTarget{" +
            "trace=" + trace +
            '}';
    }
}
