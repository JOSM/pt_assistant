package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BicycleRouteType implements RouteType {

    @Override
    public String getName() {
        return tr("Bicycle route");
    }

    @Override
    public String getTypeIdentifier() {
        return "bicycle";
    }

    @Override
    public List<String> getOneWayTags() {
        return Arrays.asList("oneway:bicycle", "oneway");
    }

    @Override
    public List<String> getAccessTags() {
        return Arrays.asList("bicycle", "vehicle", "access");
    }

    @Override
    public AccessDirection mayDriveOn(Map<String, String> tags) {
        if (!tags.containsKey("highway")) {
            return AccessDirection.NONE;
        } else {
            return RouteType.super.mayDriveOn(tags);
        }
    }
}
