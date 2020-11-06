package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

public final class WayTriplet<previousWay extends OsmPrimitive, currentWay extends OsmPrimitive, nextWay extends OsmPrimitive> {
    public Way previousWay;
    public Way currentWay;
    public Way nextWay;

    public WayTriplet(Way previousWay, Way currentWay, Way nextWay) {
        this.previousWay = previousWay;
        this.currentWay = currentWay;
        this.nextWay = nextWay;
    }
}
