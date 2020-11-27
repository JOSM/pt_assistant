package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.Relation;

public class ZoomSaver {
    private ProjectionBounds lastZoom;

    public ZoomSaver() {
    }

    public void setLastZoom(ProjectionBounds lastZoom) {
        this.lastZoom = lastZoom;
    }

    public ProjectionBounds getLastZoom() {
        return lastZoom;
    }
}
