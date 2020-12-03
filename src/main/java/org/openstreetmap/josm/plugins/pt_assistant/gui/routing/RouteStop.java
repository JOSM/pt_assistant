package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class RouteStop {
    private final int stopIndex;
    private final List<OsmPrimitive> relationMembers;

    public RouteStop(int stopIndex,
                     List<OsmPrimitive> relationMembers) {
        this.stopIndex = stopIndex;
        this.relationMembers = relationMembers;
        if (relationMembers.isEmpty()) {
            throw new IllegalArgumentException("No relation members");
        }
    }

    RouteStop(RouteStop p) {
        this(p.stopIndex, p.relationMembers);
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public List<OsmPrimitive> getRelationMembers() {
        return relationMembers;
    }

    public Tagged getStopAttributes() {
        // Currently, we only group by stop area => easy to find
        Relation area = StopUtils.findContainingStopArea(relationMembers.get(0));
        return area == null ? relationMembers.get(0) : area;
    }

    @Override
    public String toString() {
        return "RouteStop{" +
            "stopIndex=" + stopIndex +
            ", relationMembers=" + relationMembers +
            '}';
    }
}
