package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines;


import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.PublicTransportLinePanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops.FoundStop;

import java.awt.Component;
import java.util.List;

/**
 * Provides the relations for createing a {@link PublicTransportLinePanel}
 */
public interface LineRelationsProvider {
    List<LineRelation> getRelations();

    Component createHeadlinePanel();

    default boolean shouldHighlightStop(FoundStop foundStop) {
        return false;
    }

    OsmDataLayer getLayer();
}
