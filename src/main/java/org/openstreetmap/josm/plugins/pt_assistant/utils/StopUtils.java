// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.tools.Utils;

/**
 * Utils class for stop areas
 */
public final class StopUtils {
    private StopUtils() {
        // private constructor for util classes
    }

    /**
     * Checks if a given relation is a stop_area.
     *
     * @param r Relation to be checked
     * @return true if the relation is a stop_area, false otherwise.
     */
    public static boolean isStopArea(final Relation r) {
        return r != null
            && r.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.PUBLIC_TRANSPORT_TAG)
            && r.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_AREA_TAG_VALUE);
    }

    public static boolean isStopAreaGroup(final Relation r) {
        return r != null
            && r.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.PUBLIC_TRANSPORT_TAG)
            && r.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_AREA_GROUP_TAG_VALUE);
    }

    /**
     * Checks if a given object is a stop_position.
     *
     * @param p primitive to be checked
     * @return true if the object is a stop_position, false otherwise.
     */
    public static boolean isStopPosition(final OsmPrimitive p) {
        return p != null && p.hasTag("public_transport", "stop_position");
    }

    /**
     * @param n the node that is checked
     * @return true iff one of the tags `highway=bus_stop`, `railway=tram_stop`
     *     or the combination of `public_transport=stop_position` and either `highway=*` or `railway=*` is present
     */
    public static boolean isHighwayOrRailwayStopPosition(final Node n) {
        return n != null && (
            n.hasTag("highway", "bus_stop") ||
            n.hasTag("railway", "tram_stop") ||
            (isStopPosition(n) && n.hasKey("highway", "railway"))
        );
    }

    /**
     * Checks if a given object is a platform.
     *
     * @param p primitive to be checked
     * @return true if the object is a platform, false otherwise.
     */
    public static boolean verifyStopAreaPlatform(final OsmPrimitive p) {
        return p != null && (
            p.hasTag("public_transport", "platform") ||
            p.hasTag("highway", "bus_stop") ||
            p.hasTag("highway", "platform") ||
            p.hasTag("railway", "platform")
        );
    }

    /**
     * Checks if a given object is part of an stop area relation
     *
     * @param member primitive to be checked
     * @return true if the object part of stop area relation, false otherwise.
     */
    public static boolean verifyIfMemberOfStopArea(final OsmPrimitive member) {
        return Utils.filteredCollection(member.getReferrers(), Relation.class).stream()
            .anyMatch(StopUtils::isStopArea);
    }

    /**
     * @param primitive The primitive to test
     * @return the area if that primitive is memberin any stop area relation, null if not.
     */
    public static Relation findContainingStopArea(OsmPrimitive primitive) {
        return (Relation) primitive.getReferrers().stream()
            .filter(it -> it instanceof Relation && isStopArea((Relation) it))
            .findFirst()
            .orElse(null);
    }

    public static boolean isPlatform(OsmPrimitive primitive) {
        return primitive.hasTag(OSMTags.RAILWAY_TAG, OSMTags.PLATFORM_TAG_VALUE)
            || primitive.hasTag(OSMTags.HIGHWAY_TAG, OSMTags.PLATFORM_TAG_VALUE)
            || primitive.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.PLATFORM_TAG_VALUE);
    }

    public static Relation findParentStopGroup(Relation stopAreaRelation) {
        return stopAreaRelation == null
        ? null
        : (Relation) stopAreaRelation
            .getReferrers()
            .stream()
            .filter(r -> r instanceof Relation && StopUtils.isStopAreaGroup((Relation) r))
            .findFirst()
            .orElse(null);
    }
}
