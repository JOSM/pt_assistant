package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Pair;


/**
 * Several utilities that help with calculating distances between points or line segments,
 * or have to do with relative orientation (e.g. to the right or left).
 *
 * At the moment the euclidean distance is used, but most of the time the great circle distance is probably best.
 */
public final class GeometryUtils {
    private GeometryUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * @param line a pair of two points A and B, representing a line from A to B
     * @param point the point for which the direction relative to {@code line} is returned
     * @return a positive value when {@code point} is to the right of the line from A to B (looking in that direction).
     * A negative value when the point is to the left.
     * Otherwise 0 (also in cases where start and end of the line are identical and thus the direction can't be determined).
     */
    public static double direction(final Pair<ILatLon, ILatLon> line, final ILatLon point) {
        return (line.b.lon() - line.a.lon()) * (point.lat() - line.a.lat())
            - (line.b.lat() - line.a.lat()) * (point.lon() - line.a.lon());
    }

    public static double distance(final ILatLon point1, final ILatLon point2) {
        return Math.sqrt(distanceSquared(point1, point2));
    }

    public static double distanceSquared(final ILatLon point1, final ILatLon point2) {
        return Math.pow(point1.lon() - point2.lon(), 2) + Math.pow(point1.lat() - point2.lat(), 2);
    }

    public static <T extends ILatLon> double distPointToSegment(final Pair<T, T> lineSegment, final ILatLon point) {
        final double segmentLengthSquared = distanceSquared(lineSegment.a, lineSegment.b);
        if (segmentLengthSquared == 0) {
            return distance(point, lineSegment.a);
        }

        /*
         * A number between 0 and 1 corresponding to the point on the segment between the end points.
         * 0 means {@code lineSegment.a} is the point on the segment closest to {@code point},
         * 1 means {@code lineSegment.b} is the closest point. A value in between tells you where
         * on the segment the closest point is located.
         */
        final double nearestPositionOnSegment = Math.max(
            0,
            Math.min(
                1,
                (
                    (point.lon() - lineSegment.a.lon()) * (lineSegment.b.lon() - lineSegment.a.lon()) +
                    (point.lat() - lineSegment.a.lat()) * (lineSegment.b.lat() - lineSegment.a.lat())
                ) / segmentLengthSquared
            )
        );
        return distance(point, new LatLon(
            lineSegment.a.lat() + nearestPositionOnSegment * (lineSegment.b.lat() - lineSegment.a.lat()),
            lineSegment.a.lon() + nearestPositionOnSegment * (lineSegment.b.lon() - lineSegment.a.lon())
        ));
    }

    public static <T extends ILatLon> Optional<NearestSegment<T>> findNearestSegment(final Collection<Pair<T, T>> segments, final ILatLon point) {
        return segments.parallelStream().map(segment -> new NearestSegment<>(distPointToSegment(segment, point), segment)).min(Comparator.comparingDouble(segment -> segment.distance));
    }

    public static class NearestSegment<T extends ILatLon> {
        private final double distance;
        private final Pair<T, T> segment;

        public NearestSegment(final double distance, final Pair<T, T> segment) {
            this.distance = distance;
            this.segment = segment;
        }

        public double getDistance() {
            return distance;
        }

        public Pair<T, T> getSegment() {
            return segment;
        }
    }
}
