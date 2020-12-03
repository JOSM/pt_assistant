package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

public class RouteStopPositionOnWay extends RouteStop {
    private final PointOnRoute position;

    public RouteStopPositionOnWay(RouteStop p,
                                  PointOnRoute position) {
        super(p);
        this.position = position;
    }

    public PointOnRoute getPosition() {
        return position;
    }


    @Override
    public String toString() {
        return "RouteStopPositionOnWay{" +
            "stopIndex=" + getStopIndex() +
            ", relationMembers=" + getRelationMembers() +
            ", position=" + position +
            '}';
    }
}
