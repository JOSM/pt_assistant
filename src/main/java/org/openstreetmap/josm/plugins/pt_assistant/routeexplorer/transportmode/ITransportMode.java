// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public interface ITransportMode {
    Set<ITransportMode> TRANSPORT_MODES = Stream.of(
        new BicycleTransportMode(),
        new BusTransportMode(),
        new HorseTransportMode(),
        new PedestrianTransportMode(),
        new TrolleyBusTransportMode()
    ).collect(Collectors.toSet());

    /**
     * Just a convenience method for {@link #canTraverseWay(IWay, WayTraversalDirection)} that assumes {@link WayTraversalDirection#FORWARD}
     * @param way the way for which we check, if it can be traversed by the transport mode
     * @return {@code true} if the transport mode can travel along the way in the forward direction. Otherwise {@code false}.
     */
    default boolean canTraverseWay(final Way way) {
        return canTraverseWay(way, WayTraversalDirection.FORWARD);
    }

    /**
     * @param way the way that is checked, if the transport mode can traverse
     * @param direction the travel direction for which we check
     * @return {@code true} iff the transport mode can travel along the given way in the given direction. Otherwise {@code false}.
     */
    boolean canTraverseWay(@NotNull IWay<?> way, @NotNull WayTraversalDirection direction);

    /**
     * Checks if this transport mode should be used for the given relation
     * @param relation the relation that is checked, if it is suitable for the transport mode
     * @return {@code true} if the transport mode is suitable for the relation. Otherwise {@code false}.
     */
    boolean canBeUsedForRelation(@NotNull final IRelation<?> relation);

    /**
     * @param from the way from which the vehicle is coming
     * @param via the node that the vehicle travels through, must be part of {@code from} and {@code to} ways,
     *            or this method will return false
     * @param to the way onto which the vehicle makes the turn
     * @return {@code true} if the transport mode can make a turn from the given {@code from} way,
     *         via the given {@code via} node to the given {@code to} way. Otherwise {@code false}.
     *         This method assumes that both ways can be traversed by the transport mode, it does not check that.
     */
    boolean canTurn(@NotNull final Way from, @NotNull final Node via, @NotNull final Way to);

    /**
     * @param from the way from which the vehicle is coming
     * @param via the way that the vehicle travels through
     * @param to the way onto which the vehicle makes the turn
     * @return {@code true} if the transport mode can make a turn from the given {@code from} way,
     *         through the given {@code via} way to the given {@code to} way. Otherwise {@code false}.
     *         This method assumes that all three ways can be traversed by the transport mode, it does not check that.
     */
    boolean canTurn(@NotNull final Way from, @NotNull final Way via, @NotNull final Way to);

    /**
     * @return an icon representing the transport mode
     */
    ImageProvider getIcon();

    /**
     * @return a unique name for the transport mode. This string should be translatable, so please use {@link I18n#marktr} on the string that's returned!
     */
    String getName();
}
