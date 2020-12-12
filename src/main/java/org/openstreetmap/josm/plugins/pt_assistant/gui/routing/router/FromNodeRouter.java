package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import java.util.Collections;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteSegmentWay;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.RouteType;
import org.openstreetmap.josm.tools.Pair;

/**
 * Router that starts at a node.
 */
public class FromNodeRouter extends AbstractRouter {
    private final Node node;
    private final int insertionIndexInRelation;

    public FromNodeRouter(Node node, int insertionIndexInRelation, RouteType type) {
        super(type);
        this.node = node;
        this.insertionIndexInRelation = insertionIndexInRelation;
    }

    @Override
    protected Stream<Pair<RouteSegmentWay, Double>> getRouterStartSegments() {
        // All ways starting at node
        return node
            .referrers(Way.class)
            .filter(way -> way.isFirstLastNode(node))
            .map(way -> type.createRouteSegmentWay(way, way.firstNode().equals(node),
                insertionIndexInRelation, Collections.emptyList()))
            .filter(this::isWaySuitable)
            .map(segment -> new Pair<>(segment, 0.0));
    }

    @Override
    protected Node getRoutingStartNode() {
        return node;
    }

    @Override
    public int getIndexInMembersToAddAfter() {
        return insertionIndexInRelation;
    }


    @Override
    public String toString() {
        return "FromNodeRouter{" +
            "type=" + type +
            ", node=" + node +
            ", insertionIndexInRelation=" + insertionIndexInRelation +
            '}';
    }
}
