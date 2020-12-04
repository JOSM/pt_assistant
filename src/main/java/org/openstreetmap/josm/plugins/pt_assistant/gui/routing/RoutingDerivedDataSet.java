package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;

class RoutingDerivedDataSet extends DerivedDataSet {
    private final IRelationEditorActionAccess editorAccess;
    private final RouteTraverser routeTraverser;

    public RoutingDerivedDataSet(IRelationEditorActionAccess editorAccess) {
        super(editorAccess.getEditor().getLayer().getDataSet());
        this.editorAccess = editorAccess;
        this.routeTraverser = new RouteTraverser(RelationAccess.of(editorAccess));
    }

    public RouteTraverser getRouteTraverser() {
        return routeTraverser;
    }

    @Override
    protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
        RelationAccess relation = RelationAccess.of(editorAccess);

        relation.getMembers()
            .forEach(member -> {
                if (member.isNode()) {
                    addOrGetDerived(withStopOrPlatformFlag(new Node(member.getNode()), member.getRole()));
                } else if (member.isWay()) {
                    addOrGetDerived(withStopOrPlatformFlag(new Way(member.getWay()), member.getRole()));
                } else if (member.isRelation()) {
                    addOrGetDerived(withStopOrPlatformFlag(new Relation(member.getRelation()), member.getRole()));
                }
            });

        // Now we mark the route geometry as actual driveable way.
        List<RouteSegment> segments = routeTraverser.getSegments();
        for (int i = 0; i < segments.size(); i++) {
            RouteSegment segment = segments.get(i);
            // Highlights all segments currently in the route
            segment.getWays().forEach(way -> {
                OsmPrimitive p = addOrGetDerived(way.getWay());
                if (p.hasTag(RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE)) {
                    p.put(RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE, RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE_VALUE_MULTIPLE);
                } else {
                    if (way.isForward()) {
                        p.put(RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE, RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE_VALUE_FORWARD);
                    } else {
                        p.put(RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE, RoutingPanel.TAG_PART_OF_ACTIVE_ROUTE_VALUE_BACKWARD);
                    }
                }
            });
            // Mark the gaps in the line / end of the line
            // normal => everything is fine
            // broken => there should not be a gap there.
            OsmPrimitive first = addOrGetDerived(segment.getWays().get(0).firstNode());
            first.put(RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_STARTS, i == 0 ? RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_VALUE_NORMAL : RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN);
            OsmPrimitive last = addOrGetDerived(segment.getWays().get(segment.getWays().size() - 1).lastNode());
            last.put(RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_ENDS, i == segments.size() - 1 ? RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_VALUE_NORMAL : RoutingPanel.TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN);
        }

        // Stops on the route
        List<RouteStop> stops = routeTraverser.findStopPositions();
        double lastOffsetOnRoute = 0;
        for (RouteStop stop : stops) {
            OsmPrimitive stopNode = addOrGetDerived(stop.getRelationMembers().get(0));
            stop.getStopAttributes().getKeys().forEach((k, v) -> stopNode.put("activeRelationStop_" + k, v));
            stopNode.put(RoutingPanel.TAG_ACTIVE_RELATION_STOP_INDEX, stop.getStopIndex() + "");
            if (stop instanceof RouteStopPositionOnWay) {
                stopNode.put(RoutingPanel.ACTIVE_RELATION_STOP_OFFSET,
                    "" + (int) Math.round(((RouteStopPositionOnWay) stop).getPosition().getOffsetInRoute()));

                PointOnRoute myOffsetOnRoute = ((RouteStopPositionOnWay) stop).getPosition();
                if (myOffsetOnRoute.getOffsetInRoute() < lastOffsetOnRoute - 10) { // <10m error
                    stopNode.put(RoutingPanel.TAG_ACTIVE_RELATION_STOP_MISORDERED, "1");
                }
                lastOffsetOnRoute = myOffsetOnRoute.getOffsetInRoute();
            } else {
                stopNode.put(RoutingPanel.TAG_ACTIVE_RELATION_STOP_TOO_FAR, "1");
                // Do not change lastOffsetOnRoute, so that next stop might get error
            }
        }
    }

    private OsmPrimitive withStopOrPlatformFlag(OsmPrimitive p, String role) {
        if (OSMTags.ROUTE_SEGMENT_PT_ROLES.contains(role)) {
            p.put(RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION, RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_ROUTE);
        } else if (OSMTags.PLATFORM_ROLES.contains(role)) {
            p.put(RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION, RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_PLATFORM);
        } else if (OSMTags.STOP_ROLE.contains(role)) {
            p.put(RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION, RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_STOP);
        } else {
            p.put(RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION, RoutingPanel.TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_UNKNOWN);
        }
        return p;
    }

    @Override
    protected boolean isIncluded(OsmPrimitive primitive) {
        if (primitive instanceof Way) {
            return primitive.hasTag("highway")
                || primitive.hasTag("railway");
        } else if (primitive instanceof Relation) {
            return primitive.hasTag("type", "route");
        } else {
            return false;
        }
    }
}
