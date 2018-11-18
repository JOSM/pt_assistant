package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.Collection;
import java.util.Optional;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Way;

public final class BoundsUtils {
    private BoundsUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Calculates the minimal bounds in which the given ways fit.
     * @param ways the ways that should fit inside the resulting bounds
     * @param paddingFactor the bounds will be padded in each cardinal direction by {@code paddingFactor} times the
     *     width/height of the {@link Bounds} without padding
     * @return the resulting bounds with the requested padding in an Optional.
     *     If the given collection is null or empty, the Optional will be empty.
     */
    public static Optional<Bounds> createBoundsWithPadding(final Collection<Way> ways, final double paddingFactor) {
        return createBoundsWithPadding(Optional.ofNullable(ways).map(it ->
            it.stream().map(Way::getBBox).reduce(null, (a, b) -> {
                if (a == null) {
                    return b;
                } else {
                    a.add(b);
                    return a;
                }
            })
        ).orElse(null), paddingFactor);
    }

    /**
     * Converts {@link BBox} to {@link Bounds} and adds padding in each direction
     * @param bbox the bounding box to convert
     * @param paddingFactor this factor is multiplied with the width/height of the bbox and then added
     *     as padding to the edges of the {@link Bounds}.
     * @return the resulting {@link Bounds} with padding
     */
    public static Optional<Bounds> createBoundsWithPadding(final BBox bbox, final double paddingFactor) {
        return Optional.ofNullable(bbox).map(it -> {
            final double xPadding = it.width() * paddingFactor;
            final double yPadding = it.height() * paddingFactor;
            return new Bounds(
                it.getBottomRightLat() - yPadding,
                it.getTopLeftLon() - xPadding,
                it.getTopLeftLat() + yPadding,
                it.getBottomRightLon() + xPadding
            );
        });
    }
}
