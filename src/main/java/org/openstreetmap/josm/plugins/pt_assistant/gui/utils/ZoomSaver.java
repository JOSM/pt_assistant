package org.openstreetmap.josm.plugins.pt_assistant.gui.utils;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MapViewState;

public class ZoomSaver {
    private MapViewState lastZoom;

    public ZoomSaver() {
    }

    public void setLastZoom(MapViewState lastZoom) {
        this.lastZoom = lastZoom;
    }

    public MapViewState getLastZoom() {
        return lastZoom;
    }
}
