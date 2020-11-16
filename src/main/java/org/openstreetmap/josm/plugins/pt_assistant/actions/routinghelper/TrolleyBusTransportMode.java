package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTIcons;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class TrolleyBusTransportMode extends BusTransportMode {
    @Override
    public boolean canTraverseWay(@NotNull final IWay<?> way, @NotNull final WayTraversalDirection direction) {
        return super.canTraverseWay(way, direction) && way.hasTag("trolley_wire", "yes");
    }

    @Override
    public boolean canBeUsedForRelation(@NotNull final IRelation<?> relation) {
        return relation.hasTag("type", "route") && relation.hasTag("route", "trolleybus");
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
