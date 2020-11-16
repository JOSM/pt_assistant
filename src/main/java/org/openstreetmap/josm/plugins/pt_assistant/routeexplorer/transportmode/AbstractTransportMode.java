package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection;

public abstract class AbstractTransportMode implements ITransportMode {
    String modeOfTransport = "";
    String additionalTypeForTurnRestriction = "";
    String oneWayExceptionFor = "";

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("type", "route");
    }

    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        final String onewayValue = way.get("oneway");
        final Boolean onewayException = (!"".equals(oneWayExceptionFor)) ? "no".equals(way.get("oneway:" + oneWayExceptionFor)) : false;
        return onewayValue == null
                || onewayException
                || ("yes".equals(onewayValue) && direction == WayTraversalDirection.FORWARD)
                || ("-1".equals(onewayValue) && direction == WayTraversalDirection.BACKWARD);
    }

    @Override
    public boolean canTurn(@NotNull final Way from, @NotNull final Node via, @NotNull final Way to) {
        List<String> types = new java.util.ArrayList<>(Collections.singletonList("restriction"));
        if (!additionalTypeForTurnRestriction.equals("")) types.add("restriction:" + additionalTypeForTurnRestriction);
        final Set<Relation> restrictionRelations = from.getReferrers().stream()
            .map(it -> it.getType() == OsmPrimitiveType.RELATION ? (Relation) it : null)
            .filter(Objects::nonNull)
            .filter(it -> it.hasTag("type", types))
            .filter(it -> it.findRelationMembers("from").contains(from))
            .filter(it -> it.findRelationMembers("via").contains(via))
            .filter(it -> it.findRelationMembers("to").contains(to))
            .collect(Collectors.toSet());
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
