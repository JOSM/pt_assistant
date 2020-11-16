// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class BicycleTransportMode implements ITransportMode {

    private static final List<String> suitableHighwaysForBicycle = Stream.concat(
        // This list is ordered from most suitable to least suitable
        Stream.of("cycleway", "cyclestreet", "path", "residential", "unclassified", "service", "track", "living_street"),
        Stream.of("tertiary", "secondary", "primary", "trunk").flatMap(it -> Stream.of(it, it + "_link"))
    ).collect(Collectors.toList());

    protected BicycleTransportMode() {
        // should only be instantiable in `ITransportMode`
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        final String onewayValue = way.get("oneway");
        return
            !way.hasTag("bicycle", "no")
            && (way.hasTag("highway", suitableHighwaysForBicycle) || way.hasTag("bicycle", "yes"))
            && (
                onewayValue == null
                || "no".equals(way.get("oneway:bicycle"))
                || ("yes".equals(onewayValue) && direction == WayTraversalDirection.FORWARD)
                || ("-1".equals(onewayValue) && direction == WayTraversalDirection.BACKWARD)
            );
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("type", "route") && relation.hasTag("route", "bicycle");
    }

    @Override
    public boolean canTurn(@NotNull final Way from, @NotNull final Node via, @NotNull final Way to) {
        final Set<Relation> restrictionRelations = from.getReferrers().stream()
            .map(it -> it.getType() == OsmPrimitiveType.RELATION ? (Relation) it : null)
            .filter(Objects::nonNull)
            .filter(it -> "restriction".equals(it.get("type")) || "restriction:bicycle".equals(it.get("type")))
            .filter(it -> it.findRelationMembers("from").contains(from))
            .filter(it -> it.findRelationMembers("via").contains(via))
            .filter(it -> it.findRelationMembers("to").contains(to))
            .collect(Collectors.toSet());
        for (Relation restrictionRelation : restrictionRelations) {
            final String restriction = restrictionRelation.get("restriction");
            final String except = restrictionRelation.get("except");
            if (restriction.startsWith("no_") && !except.contains("bicycle")) {
                return false;
            }
        }

        return from.containsNode(via) && to.containsNode(via);
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
