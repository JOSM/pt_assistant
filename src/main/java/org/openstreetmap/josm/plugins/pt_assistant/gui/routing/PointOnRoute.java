package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

public class PointOnRoute {
    private final RouteSegmentWay way;
    private final double offsetFromSegmentStart;
    private final double offsetInRoute;
    private final double sidewardsDistanceFromRoute;

    public PointOnRoute(RouteSegmentWay way, double offsetFromSegmentStart, double offsetInRoute, double sidewardsDistanceFromRoute) {
        this.way = way;
        this.offsetFromSegmentStart = offsetFromSegmentStart;
        this.offsetInRoute = offsetInRoute;
        this.sidewardsDistanceFromRoute = sidewardsDistanceFromRoute;
    }

    public RouteSegmentWay getWay() {
        return way;
    }

    public double getOffsetFromSegmentStart() {
        return offsetFromSegmentStart;
    }

    public double getOffsetInRoute() {
        return offsetInRoute;
    }

    public double getSidewardsDistanceFromRoute() {
        return sidewardsDistanceFromRoute;
    }


    @Override
    public String toString() {
        return "PointOnRoute{" +
            "way=" + way +
            ", offsetFromSegmentStart=" + offsetFromSegmentStart +
            ", offsetInRoute=" + offsetInRoute +
            ", sidewardsDistanceFromRoute=" + sidewardsDistanceFromRoute +
            '}';
    }
}
