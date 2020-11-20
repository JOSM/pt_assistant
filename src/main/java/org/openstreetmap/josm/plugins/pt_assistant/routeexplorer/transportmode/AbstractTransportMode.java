package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;

public abstract class AbstractTransportMode implements ITransportMode {
    String modeOfTransport = "";
    String[] additionalTypesForTurnRestriction;
    String[] oneWayExceptionsFor;

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("type", "route");
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        final String oneway = way.get("oneway");
        return oneway == null
                || Arrays.stream(oneWayExceptionsFor)
                    .map(mode -> way.get("oneway:" + mode))
                    .anyMatch("no"::equals)
                || ("yes".equals(oneway) && direction == WayTraversalDirection.FORWARD)
                || ("-1".equals(oneway) && direction == WayTraversalDirection.BACKWARD);
    }

    @Override
    public boolean canTurn(@NotNull final Way from, @NotNull final Node via, @NotNull final Way to) {
        List<String> types = new java.util.ArrayList<>(Collections.singletonList("restriction"));
        Arrays.stream(additionalTypesForTurnRestriction).map(at -> "restriction:" + at).forEach(types::add);
        final Set<Relation> restrictionRelations = from.getReferrers().stream()
            .map(it -> it.getType() == OsmPrimitiveType.RELATION ? (Relation) it : null)
            .filter(relation ->
                relation != null && types.contains(relation.get("type"))
                && relation.findRelationMembers("from").contains(from)
                && relation.findRelationMembers("via").contains(via)
                && relation.findRelationMembers("to").contains(to)
            ).collect(Collectors.toSet());
        for (Relation restrictionRelation : restrictionRelations) {
            final String restriction = restrictionRelation.get("restriction");
            final String except = ("".equals(modeOfTransport)) ? restrictionRelation.get("except") : "";
            if (restriction.startsWith("no_") && !except.contains(modeOfTransport)) {
                return false;
            }
        }
        return from.containsNode(via) && to.containsNode(via);
    }
}
