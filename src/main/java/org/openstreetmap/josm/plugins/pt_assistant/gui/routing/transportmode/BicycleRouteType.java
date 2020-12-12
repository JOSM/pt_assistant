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
    public List<String> getAccessTags() {
        return Arrays.asList("bicycle", "vehicle", "access");
    }

    @Override
    public boolean mayDefaultAccess(Map<String, String> tags) {
        String highway = tags.get("highway");
        // Those highways won't allow cycelists
        return highway != null && !Arrays.asList("motorway", "motorway_link", "trunk", "footway", "pedestrian").contains(highway);
    }

    @Override
    public String getOverpassFilterForPossibleWays() {
        return "(if: is_tag(\"highway\") || is_tag(\"bicycle\"))";
    }
}
