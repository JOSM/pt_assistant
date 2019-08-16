package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public final class WayUtils {
    private WayUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * @param w1 a way
     * @param w2 another way
     * @return The first node of the ways {@code w1} and {@code w2} that is also part of the other
     *     of those ways. If the two ways do not have a {@link Node} in common, or if at least
     *     one of the given ways is {@link null}, the returned {@link Optional} is empty.
     */
    public static Optional<Node> findFirstCommonNode(final Way w1, final Way w2) {
        if (w1 == null || w2 == null) {
            return Optional.empty();
        }
        return w2.getNodes().stream().filter(w1::containsNode).findFirst();
    }

    public static Optional<Node> findCommonFirstLastNode(final Way w1, final Way w2) {
        return findCommonFirstLastNode(w1, w2, null);
    }

    public static Optional<Node> findCommonFirstLastNode(final Way w1, final Way w2, final Node notThisOne) {
        final NodePair nodes = findCommonFirstLastNodes(w1, w2);
        if (nodes.getA() != null && !nodes.getA().equals(notThisOne)) {
            return Optional.of(nodes.getA());
        }
        if (nodes.getB() != null && !nodes.getB().equals(notThisOne)) {
            return Optional.of(nodes.getB());
        }
        return Optional.empty();
    }

    public static NodePair findCommonFirstLastNodes(final Way w1, final Way w2) {
        final Node[] nodes = {
            /* [0] */ w1.firstNode(),
            /* [1] */ w1.lastNode(),
            /* [2] */ w2.firstNode(),
            /* [3] */ w2.lastNode()
        };
        return new NodePair(
            nodes[0].equals(nodes[2]) || nodes[0].equals(nodes[3]) ? nodes[0] : null,
            nodes[1].equals(nodes[2]) || nodes[1].equals(nodes[3]) ? nodes[1] : null
        );
    }

    public static List<Relation> findPTRouteParents(final Way w) {
        return w.getReferrers().stream()
            .filter(it -> it instanceof Relation && RouteUtils.isPTRoute((Relation) it))
            .map(it -> (Relation) it)
            .collect(Collectors.toList());
    }

    public static int findNumberOfCommonFirstLastNodes(final Way w1, final Way w2) {
        final NodePair pair = findCommonFirstLastNodes(w1, w2);
        return (pair.getA() != null ? 1 : 0) + (pair.getB() != null ? 1 : 0);
    }

    public static boolean isOneWay(Way w) {
        return w.isOneway() != 0;
    }

    public static boolean isSuitableForBuses(Way way) {
        return
            way.hasTag(
                "highway",
                "motorway", "trunk", "primary", "secondary", "tertiary",
                "unclassified", "road", "residential", "service", "motorway_link", "trunk_link", "primary_link",
                "secondary_link", "tertiary_link", "living_street", "bus_guideway", "road"
            )
            || way.hasTag(
                "cycleway",
                "share_busway", "shared_lane"
            )
            || (
                way.hasTag("highway", "pedestrian")
                && (
                    way.hasTag("bus", "yes", "designated")
                    || way.hasTag("psv", "yes", "designated")
                )
            );
    }
    public static boolean isSuitableForBicycle(Way way) {
        return
            way.hasTag(
                "highway",
                "trunk", "primary", "secondary", "tertiary","track",
                "unclassified", "road", "residential", "service", "trunk_link", "primary_link",
                "secondary_link", "tertiary_link", "living_street", "bus_guideway", "road","path","footway"
            )
            || way.hasTag(
                "cycleway",
                "share_busway", "shared_lane"
            )
            ||
                way.hasTag("highway", "pedestrian")
            ||
              way.hasTag(
                  "cycleway"
              )
            ||
              way.hasTag("highway", "cycleway");
      }

}
