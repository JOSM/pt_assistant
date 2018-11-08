package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Way;

import java.util.Optional;

public final class WayUtils {
    private WayUtils() {
        // Private constructor to avoid instantiation
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
            ) ||
                way.hasTag(
                    "cycleway",
                    "share_busway", "shared_lane"
                ) ||
                (way.hasTag("highway", "pedestrian") && (way.hasTag("bus", "yes", "designated") || way.hasTag("psv", "yes", "designated")));
    }
}
