package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Type of the route relation
 */
public interface RouteType {
    String getName();

    String getTypeIdentifier();

    /**
     * The tags to search for oneway information
     * @return The tags in the order in which they should be searched.
     */
    default List<String> getOneWayTags() {
        return Collections.emptyList();
    }

    default List<String> getAccessTags() {
        return Collections.emptyList();
    }

    default List<String> getAccessValues() {
        return Arrays.asList("yes", "permissive", "designated", "official");
    }

    default String getOverpassSelectorForPossibleWays() {
        return "";
    }

    default AccessDirection mayDriveOn(Map<String, String> tags) {
        // OSM can be complicated …
        for (String accessTag : getAccessTags()) {
            String value = tags.get(accessTag);
            if (value != null) {
                if (!getAccessValues().contains(value)) {
                    return AccessDirection.NONE;
                } else {
                    break;
                }
            }
        }

        for (String oneWayTag : getOneWayTags()) {
            String value = tags.get(oneWayTag);
            if ("-1".equals(value)) {
                return AccessDirection.BACKWARD_ONLY;
            } else if (OsmUtils.getOsmBoolean(value)) {
                return AccessDirection.FORWARD_ONLY;
            }
        }

        if (Arrays.asList("motorway", "motorway_link").contains(tags.get("highway"))) {
            return AccessDirection.FORWARD_ONLY;
        }

        return AccessDirection.BOTH;
    }

    default String getRestrictionValue(Relation r) {
        String value = r.get("restriction:" + getTypeIdentifier());
        if (value != null) {
            return value;
        } else {
            return r.get("restriction");
        }
    }

    enum AccessDirection {
        NONE,
        FORWARD_ONLY,
        BACKWARD_ONLY,
        BOTH
    }
}
