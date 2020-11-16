// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class BicycleTransportMode extends AbstractTransportMode {

    private static final List<String> suitableHighways = Stream.concat(
        // This list is ordered from most suitable to least suitable
        Stream.of("cycleway", "cyclestreet", "path", "residential", "unclassified", "service", "track", "living_street"),
        Stream.of("tertiary", "secondary", "primary", "trunk").flatMap(it -> Stream.of(it, it + "_link"))
    ).collect(Collectors.toList());

    public BicycleTransportMode() {
        // should only be instantiable in `ITransportMode`
        modeOfTransport = "bicycle";
        additionalTypeForTurnRestriction = "bicycle";
        oneWayExceptionFor = "bicycle";
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        return !way.hasTag(modeOfTransport, "no")
                && (way.hasTag("highway", suitableHighways) || way.hasTag(modeOfTransport, "yes"))
                && super.canTraverseWay(way, direction);
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("route", modeOfTransport) && super.canBeUsedForRelation(relation);
    }

    @Override
    public ImageProvider getIcon() {
        return PTIcons.BICYCLE;
    }

    @Override
    public String getName() {
        return I18n.marktr("bicycle");
    }
}
