// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class BusTransportMode extends AbstractTransportMode {

    private static final List<String> suitableHighways = Stream.concat(
        Stream.of("unclassified", "residential", "service", "living_street", "cyclestreet"),
        Stream.of("tertiary", "secondary", "primary", "trunk", "motorway").flatMap(it -> Stream.of(it, it + "_link"))
    ).collect(Collectors.toList());

    protected BusTransportMode() {
        // should only be instantiable in `ITransportMode`
        modeOfTransport = "bus";
        additionalTypesForTurnRestriction = new String[]{"bus", "psv"};
        turnRestrictionExceptionsFor = new String[]{"bus", "psv"};
        oneWayExceptionsFor = new String[]{"bus", "psv"};
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        return ( way.hasTag("highway", suitableHighways)
              || way.hasTag("psv", "yes")
              || way.hasTag ("bus", "yes"))
            && super.canTraverseWay(way, direction);
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("route", "bus", "coach", "minibus") && super.canBeUsedForRelation(relation);
    }

    @Override
    public ImageProvider getIcon() {
        return PTIcons.BUS;
    }

    @Override
    public String getName() {
        return I18n.marktr("bus");
    }
}
