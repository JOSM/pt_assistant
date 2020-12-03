package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;

/**
 * A "drivable"/walkable part of the route. A list of conclusive lines
 */
public class RouteSegment {
    private final List<RouteSegmentWay> ways;

    public RouteSegment(List<RouteSegmentWay> ways) {
        this.ways = Collections.unmodifiableList(new ArrayList<>(ways));
    }

    public List<RouteSegmentWay> getWays() {
        return ways;
    }

    public double getLength() {
        return ways
            .stream()
            .mapToDouble(RouteSegmentWay::getLength)
            .sum();
    }

    @Override
    public String toString() {
        return "RouteSegment{" +
            "ways=" + ways +
            '}';
    }
}
