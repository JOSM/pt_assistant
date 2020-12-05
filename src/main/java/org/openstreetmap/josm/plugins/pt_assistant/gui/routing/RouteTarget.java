package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.Node;

public class RouteTarget {
    private final List<RouteSegmentWay> trace;

    public RouteTarget(List<RouteSegmentWay> trace) {
        this.trace = Objects.requireNonNull(trace, "trace");
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
}
