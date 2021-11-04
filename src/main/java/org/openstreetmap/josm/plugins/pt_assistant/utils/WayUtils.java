// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public final class WayUtils {
    private WayUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Checks if the ways have start/end nodes in common
     *
     * @param w1 first way
     * @param w2 second way
     * @return {@code true} iff the ways have at least one of their {@link IWay#firstNode()}/{@link IWay#lastNode()} in common
     */
    public static boolean isTouchingOtherWay(IWay<Node> w1, IWay<Node> w2) {
        if (w1 == null || w2 == null) {
            return false;
        }
        return w1.isFirstLastNode(w2.firstNode()) || w1.isFirstLastNode(w2.lastNode());
    }

    /**
     * Checks if any way from the first collection touches any way from the
     * second collection
     *
     * @param ways1 first collection
     * @param ways2 second collection
     * @return {@code true} if at least one of the ways from {@code ways1} touch any one from {@code ways2}
     *         according to {@link #isTouchingOtherWay(IWay, IWay)}, false otherwise
     */
    public static boolean isAnyWayTouchingAnyOtherWay(Collection<? extends IWay<Node>> ways1, Collection<? extends IWay<Node>> ways2) {
        if (ways1 == null || ways2 == null) {
            return false;
        }
        return ways1.stream().anyMatch(way1 ->
            ways2.stream().anyMatch(way2 -> WayUtils.isTouchingOtherWay(way1, way2))
        );
    }

    /**
     * @param w1 a way
     * @param w2 another way
     * @return The first node of the ways {@code w1} and {@code w2} that is also part of the other
     *     of those ways. If the two ways do not have a {@link Node} in common, or if at least
     *     one of the given ways is {@code null}, the returned {@link Optional} is empty.
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
                "secondary_link", "tertiary_link", "living_street", "busway", "bus_guideway", "road"
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
                "trunk", "primary", "secondary", "tertiary", "track", "unclassified", "road",
                "residential", "service", "trunk_link", "primary_link", "secondary_link",
                "tertiary_link", "living_street", "bus_guideway", "road", "path", "footway"
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

    public static boolean isRoundabout(Way w) {
        return w.hasTag("junction", "roundabout");
    }

    public static boolean isNonSplitRoundAbout(final Way way) {
        return isRoundabout(way) && way.firstNode().equals(way.lastNode());
    }

    public static boolean isPartOfSplitRoundAbout(final Way way) {
        return isRoundabout(way) && !way.firstNode().equals(way.lastNode());
    }

    /**
     * Collects from a {@link java.util.stream.Stream} of {@link Way}s the way that is nearest to the location
     * given as argument to this method.
     * @param point the location for which you want to find the way that is nearest
     * @return a collector that will find the way that is nearest to the given {@code point} from a stream
     */
    public static Collector<Way, ?, Optional<Way>> nearestToPointCollector(final ILatLon point) {
        return Collectors.minBy(
            Comparator.comparingDouble(way ->
                GeometryUtils.findNearestSegment(way.getNodePairs(false), point)
                    .map(GeometryUtils.NearestSegment::getDistance)
                    .orElse(Double.MAX_VALUE)
            )
        );
    }

    /**
     * Calculates the {@link LatLon#distanceSq(LatLon)} from {@code origin} to the {@link Way#firstNode()}  and the
     * {@link Way#lastNode()}. Whichever value is lower is returned.
     *
     * @param origin the point from which the distance is measured
     * @param way the way to whose first or last node the distance is measured
     * @return the lower of these two values: the squared distance from the origin to the first node of the way,
     *   or the squared distance from the origin to the last node of the way
     */
    public static double calcDistanceSqToFirstOrLastWayNode(final ILatLon origin, final IWay<? extends INode> way) {
        final LatLon o = new LatLon(origin);
        return Math.min(o.distanceSq(way.firstNode().getCoor()), o.distanceSq(way.lastNode().getCoor()));
    }
}
