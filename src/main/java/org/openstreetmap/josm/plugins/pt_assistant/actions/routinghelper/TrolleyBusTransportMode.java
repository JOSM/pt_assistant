package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

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
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class TrolleyBusTransportMode extends BusTransportMode {
    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        return canTraverseWay(way, direction) && way.hasTag("trolley_wire", "yes");
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return canBeUsedForRelation(relation) && relation.hasTag("route", "trolleybus");
    }

    @Override
    public ImageProvider getIcon() {
        return PTIcons.TROLLEY_BUS;
    }

    @Override
    public String getName() {
        return I18n.marktr("trolley bus");
    }
}
