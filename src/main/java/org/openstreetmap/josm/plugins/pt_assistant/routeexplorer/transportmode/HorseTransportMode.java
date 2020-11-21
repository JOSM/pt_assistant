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

public class HorseTransportMode extends AbstractTransportMode {

    private static final List<String> suitableHighways = Stream.concat(
        // This list is ordered from most suitable to least suitable
        Stream.of(
            "bridleway", "pedestrian", "footway", "path", "track", "living_street", "residential",
            "unclassified", "cyclestreet", "service", "cycleway"
        ),
        Stream.of("tertiary", "secondary", "primary", "trunk").flatMap(it -> Stream.of(it, it + "_link"))
    ).collect(Collectors.toList());

    protected HorseTransportMode() {
        // should only be instantiable in `ITransportMode`
        modeOfTransport = "horse";
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        final String onewayValue = way.get("oneway");
        return
            !way.hasTag("horse", "no")
            && (way.hasTag("highway", suitableHighways) || way.hasTag("horse", "yes"))
            && (
                onewayValue == null
                || ("yes".equals(onewayValue) && direction == WayTraversalDirection.FORWARD)
                || ("-1".equals(onewayValue) && direction == WayTraversalDirection.BACKWARD)
            );
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("route", "horse") && super.canBeUsedForRelation(relation);
    }

    @Override
    public ImageProvider getIcon() {
        return PTIcons.HORSE;
    }

    @Override
    public String getName() {
        return I18n.marktr("equestrian");
    }
}
