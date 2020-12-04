package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

public class BusRouteType implements RouteType {
    @Override
    public String getName() {
        return tr("Bus route");
    }

    @Override
    public String getTypeIdentifier() {
        return "bus";
    }

    @Override
    public List<String> getOneWayTags() {
        return Arrays.asList("oneway");
    }

    @Override
    public List<String> getAccessTags() {
        return Arrays.asList("bus", "psv", "motor_vehicle", "vehicle", "access");
    }

    @Override
    public String getOverpassFilterForPossibleWays() {
        return "[highway]";
    }
}
