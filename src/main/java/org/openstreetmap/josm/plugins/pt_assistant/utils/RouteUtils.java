// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode.FROM_FIRST_MEMBER;
import static org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode.TO_LAYER;
import static org.openstreetmap.josm.plugins.pt_assistant.data.PTStop.isPTPlatform;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;


/**
 * Utils class for routes
 *
 * @author darya, giacomo, polyglot
 *
 */
public final class RouteUtils {

    private static final String PT_VERSION_TAG = "public_transport:version";
    public static final String TAG_ROUTE = "route";

    private RouteUtils() {
        // private constructor for util classes
    }

    /**
     * Checks if the relation is a route of one of the following categories:
     * bus, trolleybus, share_taxi, tram, light_rail, subway, train.
     *
     * @param r
     *            Relation to be checked
     * @return true if the route belongs to the categories that can be validated
     *         with the pt_assistant plugin, false otherwise.
     */
    public static boolean isVersionTwoPTRoute(Relation r) {
        return isPTRoute(r) && r.hasTag(PT_VERSION_TAG, "2") && !r.hasTag("bus", "on_demand");
    }

    private static final String[] acceptedRouteTags = {"bus", "trolleybus", "share_taxi", "tram", "light_rail",
            "subway", "train" };

    /**
     * Adds the version of the PT route schema to the given PT route.
     *
     * If the given relation is not a PT route (as determined by {@link #isPTRoute(Relation)}),
     * then this method does NOT change the given relation!
     * @param r the relation for which the version of the PT route is set
     * @param version the version that should be set for the given relation
     */
    public static void setPTRouteVersion(final Relation r, final String version) {
        if (isPTRoute(r)) {
            r.put(PT_VERSION_TAG, version);
        }
    }

    public static void removeOnewayAndSplitRoundaboutWays(final Relation r) {
        if (isPTRoute(r)) {
            ExportRelationToGpxAction ea = new ExportRelationToGpxAction(EnumSet.of(FROM_FIRST_MEMBER, TO_LAYER));
            ea.setPrimitives(Collections.singleton(r));
            ea.actionPerformed(null);
            for (RelationMember nestedRelationMember : r.getMembers()) {
                if (nestedRelationMember.getType().equals(OsmPrimitiveType.WAY)) {
                    if (nestedRelationMember.getWay().hasTag("oneway", "yes") ||
                        (nestedRelationMember.getWay().hasTag("junction", "roundabout") &&
                            !nestedRelationMember.getWay().isClosed()
                        )
                    )
                {
                    r.removeMembersFor(nestedRelationMember.getWay());
                }
                }
            }
        }
    }

    public static void removeMembersWithRoles(final Relation r, final String... roles) {
        if (isPTRoute(r)) {
            List<RelationMember> members = r.getMembers();
            for (int i = members.size()-1; i > 0; i--) {
                RelationMember relationMember = members.get(i);
                for (String role : roles) {
                    if (relationMember.getRole().contains(role)) {
                        r.removeMember(i);
                    }
                }
            }
        }
    }

    public static void addWayMembersAdjacentToPlatforms(final Relation r) {
        // todo this adds the stops multiple times and the ways too.
        if (isPTRoute(r)) {
            List<RelationMember> members = r.getMembers();
            for (RelationMember relationMember : r.getMembers()) {
                if (isPTPlatform(relationMember)){
                    PTStop ptStop = new PTStop(relationMember);
                    Way served_way = ptStop.findServingWays(ptStop);
                    if (served_way != null) {
                        boolean found = false;
                        for (RelationMember rm : r.getMembers()) {
                            if (rm.getMember().equals(served_way)) {
                                found = true;
                                break;
                            }
                        if (!found) {
                            r.addMember(new RelationMember("", served_way));
                        }
                    }
                    }
                }
            }
        }
    }

    public static boolean isPTRoute(Relation r) {
        return r != null && r.hasTag(OSMTags.KEY_ROUTE, acceptedRouteTags);
    }

    /**
     * Checks, if the given relation member is a way that represents part of the route itself
     * (i.e. not something along the way like a stop area for public transport).
     * At the moment this check just checks the primitive type and the role of the member.
     *
     * @param member the relation member to check
     * @return {@code true} iff the given member contains a primitive of type {@link Way} and has role
     *         {@code forward}, {@code backward} or the empty role. Otherwise {@code false}.
     */
    public static boolean isRouteWayMember(final RelationMember member) {
        return member.isWay() && Arrays.asList("", "forward", "backward").contains(member.getRole());
    }

    public static boolean isRoute(Relation r) {
        return r.get(OSMTags.KEY_ROUTE) != null;
    }

    /**
     * Checks if the relation member refers to a way in a public transport
     * route. Some OsmPrimitiveType.WAY have to be excluded because platforms
     * can be modeled with ways.
     *
     * @param rm
     *            relation member to be checked
     * @return true if the relation member refers to a way in a public transport
     *         route, false otherwise.
     */
    public static boolean isPTWay(RelationMember rm) {

        if (rm.getType().equals(OsmPrimitiveType.NODE)) {
            return false;
        }

        if (rm.getType().equals(OsmPrimitiveType.WAY)) {
            return !(rm.getWay().hasTag("public_transport", "platform") || rm.getWay().hasTag("highway", "platform")
                    || rm.getWay().hasTag("railway", "platform"));
        }

        Relation nestedRelation = rm.getRelation();

        for (RelationMember nestedRelationMember : nestedRelation.getMembers()) {
            if (!nestedRelationMember.getType().equals(OsmPrimitiveType.WAY)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given way has tags that make it oneway for public
     * transport. The test does not check whether the way violates those
     * restrictions.
     * @param way way to check
     *
     * @return 0 if the way is not oneway for public transport, 1 if the way is
     *         oneway for public transport, -1 if the way is reversely oneway
     *         for public transport
     */
    public static int isOnewayForPublicTransport(Way way) {

        if (OsmUtils.isTrue(way.get("oneway")) || OsmUtils.isReversed(way.get("oneway"))
                || way.hasTag("junction", "roundabout") || way.hasTag("highway", "motorway")) {

            if (!way.hasTag("busway", "lane") && !way.hasTag("busway", "opposite_lane")
                    && !way.hasTag("busway:left", "lane") && !way.hasTag("busway:right", "lane")
                    && !way.hasTag("oneway:bus", "no") && !way.hasTag("oneway:psv", "no")
                    && !way.hasTag("trolley_wire", "backward")) {

                if (OsmUtils.isReversed(way.get("oneway"))) {
                    return -1;
                }
                return 1;
            }
        }
        return 0;
    }

    /**
     * Checks if the given way has tags that make it oneway for bicycles
     * The test does not check whether the way violates those
     * restrictions.
     * @param way way to check
     *
     * @return 0 if the way is not oneway for bicycles, 1 if the way is
     *         oneway for bicycles, -1 if the way is reversely oneway
     *         for bicycles
     */
    public static int isOnewayForBicycles(Way way) {

        if (OsmUtils.isTrue(way.get("oneway")) || OsmUtils.isReversed(way.get("oneway"))
                || way.hasTag("junction", "roundabout")) {

            if (!way.hasTag("busway", "lane") && !way.hasTag("cycleway", "opposite_lane")
                    && !way.hasTag("cycleway:left", "lane") && !way.hasTag("cycleway:right", "lane")
                    && !way.hasTag("oneway:bicycle", "no")) {

                if (OsmUtils.isReversed(way.get("oneway"))) {
                    return -1;
                }
                return 1;
            }
        }
        return 0;
    }

    /**
     * Checks if the type of the way is suitable for buses to go on it. The
     * direction of the way (i.e. one-way roads) is irrelevant for this test.
     *
     * @param way
     *            to be checked
     * @return true if the way is suitable for buses, false otherwise.
     */
    public static boolean isWaySuitableForBuses(Way way) {

        String[] acceptedHighwayTags = new String[] {"motorway", "trunk", "primary", "secondary", "tertiary",
                "unclassified", "road", "residential", "service", "motorway_link", "trunk_link", "primary_link",
                "secondary_link", "tertiary_link", "living_street", "bus_guideway", "road" };

        if (way.hasTag("highway", acceptedHighwayTags) || way.hasTag("cycleway", "share_busway")
                || way.hasTag("cycleway", "shared_lane")) {
            return true;
        }

        return (way.hasTag("highway", "pedestrian")
                && (way.hasTag("bus", "yes", "designated") || way.hasTag("psv", "yes", "designated")));
    }

    /**
     * Checks if this way is suitable for public transport (not only for buses)
     * @param way way
     * @return {@code true} if this way is suitable for public transport
     */
    public static boolean isWaySuitableForPublicTransport(Way way) {
        String[] acceptedRailwayTags = new String[] {"tram", "subway", "light_rail", "rail" };

        return isWaySuitableForBuses(way) || way.hasTag("railway", acceptedRailwayTags);
    }

    public static boolean isBicycleRoute(Relation r) {
        if (r == null) {
            return false;
        }

        return r.hasTag(OSMTags.KEY_ROUTE, "bicycle", "mtb");
    }

    public static boolean isFootRoute(Relation r) {
        if (r == null) {
            return false;
        }
        return r.hasTag(OSMTags.KEY_ROUTE, "foot", "walking", "hiking");
    }

    public static boolean isHorseRoute(Relation r) {
        if (r == null) {
            return false;
        }

        return r.hasTag(OSMTags.KEY_ROUTE, "horse");
    }
}
