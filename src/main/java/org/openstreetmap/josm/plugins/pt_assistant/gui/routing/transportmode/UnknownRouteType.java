package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

public class UnknownRouteType implements RouteType {
    @Override
    public String getName() {
        return tr("Unknown route");
    }

    @Override
    public String getTypeIdentifier() {
        return "";
    }

    @Override
    public String getOverpassSelectorForPossibleWays() {
        return "";
    }
}
