package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.IndexedRelationMember;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteSegmentWay;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.WaySuitability;

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
        return getAccessTags()
            .stream()
            .map(tag -> tag.equals("access") ? "oneway" : "oneway:" + tag)
            .collect(Collectors.toList());
    }

    default List<String> getAccessTags() {
        return Collections.emptyList();
    }

    default List<String> getAccessValues() {
        return Arrays.asList("yes", "permissive", "designated", "official");
    }

    default String getOverpassFilterForPossibleWays() {
        return "";
    }

    default AccessDirection mayDriveOn(Map<String, String> tags) {
        // OSM can be complicated …
        ACCESS:
        {
            for (String accessTag : getAccessTags()) {
                String value = tags.get(accessTag);
                if (value != null) {
                    if (!getAccessValues().contains(value)) {
                        return AccessDirection.NONE;
                    } else {
                        break ACCESS;
                    }
                }
            }
            if (!mayDefaultAccess(tags)) {
                return AccessDirection.NONE;
            }
        }

        for (String oneWayTag : getOneWayTags()) {
            String value = tags.get(oneWayTag);
            if ("-1".equals(value)) {
                return AccessDirection.BACKWARD_ONLY;
            } else if (Boolean.TRUE.equals(OsmUtils.getOsmBoolean(value))) {
                return AccessDirection.FORWARD_ONLY;
            }
        }
        // TODO: "junction" = "roundabout"

        if (Arrays.asList("motorway", "motorway_link").contains(tags.get("highway"))) {
            return AccessDirection.FORWARD_ONLY;
        }

        return AccessDirection.BOTH;
    }

    default boolean mayDefaultAccess(Map<String, String> tags) {
        return true;
    }

    default String getRestrictionValue(Relation r) {
        String value = r.get("restriction:" + getTypeIdentifier());
        if (value != null) {
            return value;
        } else {
            return r.get("restriction");
        }
    }

    default RouteSegmentWay createRouteSegmentWay(IndexedRelationMember way, boolean forward, List<RouteSegmentWay> previous) {
        return createRouteSegmentWay(way.getMember().getWay(), forward, way.getIndex(), previous);
    }

    default RouteSegmentWay createRouteSegmentWay(Way way, boolean forward, int indexInMembers, List<RouteSegmentWay> previous) {
        // Check way is suited
        RouteType.AccessDirection mayDriveOn = mayDriveOn(way.getKeys());
        WaySuitability suitability;
        if ((mayDriveOn == RouteType.AccessDirection.FORWARD_ONLY && !forward)
            || mayDriveOn == RouteType.AccessDirection.BACKWARD_ONLY && forward) {
            suitability = WaySuitability.WRONG_DIRECTION;
        } else if (mayDriveOn == RouteType.AccessDirection.NONE) {
            suitability = WaySuitability.WRONG_TYPE;
        } else {
            // Negative turn restricitons (preventing us from turning into this way)
            // Single via node matches
            Optional<Relation> wrongTurnRestriction = findTurnRestrictions(way, "no_")
                .filter(r -> r.findRelationMembers("to").contains(way))
                .filter(r -> {
                    Set<Way> fromPath = findFromPath(r);
                    if (fromPath.size() < previous.size()) {
                        return false;
                    } else {
                        Set<Way> whereWeComeFrom = previous.subList(previous.size() - fromPath.size(), previous.size())
                            .stream().map(RouteSegmentWay::getWay).collect(Collectors.toSet());
                        return fromPath.equals(whereWeComeFrom)
                            && (fromPath.size() > 1
                            // Single via node matches
                            || getViaNode(r).map(via -> previous.get(previous.size() - 1).lastNode().equals(via)).orElse(true));
                    }
                })
                .findFirst();
            if (wrongTurnRestriction.isPresent()) {
                suitability = WaySuitability.CANNOT_TURN_INTO;
            } else {
                if (previous.size() > 0 && findTurnRestrictions(previous.get(previous.size() - 1).getWay(), "only_")
                    // Only relations starting at previous way
                    .filter(restriction -> restriction.getMembers()
                        .stream()
                        .filter(RelationMember::isWay)
                        .filter(member -> "from".equals(member.getRole()))
                        .map(RelationMember::getWay)
                        .anyMatch(previous.get(previous.size() - 1).getWay()::equals))
                    // Only restrictions continuing in our direction
                    .anyMatch(restriction -> getViaNode(restriction)
                        .map(viaNode -> previous.get(previous.size() - 1).lastNode().equals(viaNode))
                        .orElseGet(() -> restriction.getMembers()
                            .stream()
                            .filter(RelationMember::isWay)
                            .map(RelationMember::getWay)
                            .anyMatch(way::equals)))) {
                    suitability = WaySuitability.INVALID_TURN_FROM;
                } else {
                    suitability = WaySuitability.GOOD;
                }
            }
        }

        return new RouteSegmentWay(way, forward, indexInMembers, suitability);
    }

    /**
     * Stream all turn restricitons of that type
     * @param way The way the turn restriction should be on (no matter if it is from / to / via)
     * @param prefix The prefix the turn restriction type needs to have
     * @return A stream of those relations
     */
    default Stream<Relation> findTurnRestrictions(Way way, String prefix) {
        return way.referrers(Relation.class)
            .filter(r -> r.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.RELATION_TYPE_TURN_RESTRICTION))
            .filter(r -> {
                String restrictionValue = getRestrictionValue(r);
                return restrictionValue != null && restrictionValue.startsWith(prefix);
            });
    }

    static Optional<Node> getViaNode(Relation restriction) {
        return restriction
            .getMembers()
            .stream()
            .filter(RelationMember::isNode)
            .filter(it -> "via".equals(it.getRole()))
            .map(RelationMember::getNode)
            .findFirst();
    }

    static Set<Way> findFromPath(Relation restriction) {
        return restriction
            .getMembers()
            .stream()
            .filter(RelationMember::isWay)
            .filter(it -> "via".equals(it.getRole()) || "from".equals(it.getRole()))
            .map(RelationMember::getWay)
            .collect(Collectors.toSet());
    }

    enum AccessDirection {
        NONE,
        FORWARD_ONLY,
        BACKWARD_ONLY,
        BOTH
    }
}
