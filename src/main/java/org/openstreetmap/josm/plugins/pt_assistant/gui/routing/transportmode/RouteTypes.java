package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;

/**
 *
 */
public class RouteTypes {
    private static final List<RouteType> TYPES = Arrays.asList(
        new BusRouteType()
    );

    private RouteTypes() {
    }

    public static RouteType getRouteType(RelationAccess relation) {
        return getRouteType(relation.get(OSMTags.KEY_RELATION_TYPE));
    }

    public static RouteType getRouteType(String type) {
        return TYPES.stream()
            .filter(it -> it.getTypeIdentifier().equals(type))
            .findFirst()
            .orElse(new UnknownRouteType());
    }
}
