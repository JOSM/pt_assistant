package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

class NopStop implements FoundStop {
    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public boolean matches(OsmPrimitive p) {
        return false;
    }

    @Override
    public String getNameAndInfos() {
        return "";
    }
}
