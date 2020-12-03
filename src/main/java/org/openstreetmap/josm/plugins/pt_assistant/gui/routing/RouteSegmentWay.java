package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

public class RouteSegmentWay {
    private final Way way;
    private final boolean forward;
    // We need it so often …
    private final double length;
    private final WaySuitability suitability;

    public RouteSegmentWay(Way way, boolean forward, WaySuitability suitability) {
        this.way = Objects.requireNonNull(way, "way");
        this.forward = forward;
        this.length = way.getLength();
        this.suitability = Objects.requireNonNull(suitability, "suitability");
    }

    public Way getWay() {
        return way;
    }

    public boolean isForward() {
        return forward;
    }

    public Node firstNode() {
        return forward ? way.firstNode() : way.lastNode();
    }

    public Node lastNode() {
        return forward ? way.lastNode() : way.firstNode();
    }

    public double getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "RouteSegmentWay{" +
            "way=" + way +
            ", forward=" + forward +
            ", length=" + length +
            ", suitability=" + suitability +
            '}';
    }
}
