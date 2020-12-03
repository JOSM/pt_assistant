package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea.IncompleteMembersWarningPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea.StopVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class RoutingPanel extends AbstractVicinityPanel {

    public static final String TAG_PART_OF_ACTIVE_ROUTE = "partOfActiveRoute";
    public static final String TAG_PART_OF_ACTIVE_ROUTE_VALUE_FORWARD = "forward";
    public static final String TAG_PART_OF_ACTIVE_ROUTE_VALUE_BACKWARD = "backward";
    public static final String TAG_PART_OF_ACTIVE_ROUTE_VALUE_MULTIPLE = "multiple";
    public static final String TAG_ACTIVE_RELATION_SEGMENT_STARTS = "activeRelationSegmentStarts";
    public static final String TAG_ACTIVE_RELATION_SEGMENT_ENDS = "activeRelationSegmentEnds";
    public static final String TAG_ACTIVE_RELATION_SEGMENT_VALUE_NORMAL = "normal";
    public static final String TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN = "broken";
    public static final String TAG_ACTIVE_RELATION_STOP_INDEX = "activeRelationStopIndex";
    public static final String TAG_ACTIVE_RELATION_STOP_MISORDERED = "activeRelationStopMisordered";
    public static final String TAG_ACTIVE_RELATION_STOP_TOO_FAR = "activeRelationStopTooFar";
    public static final String TAG_MEMBER_OF_ACTIVE_RELATION = "memberOfActiveRelation";
    public static final String TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_ROUTE = "route";
    public static final String TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_PLATFORM = "platform";
    public static final String TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_STOP = "stop";
    public static final String TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_UNKNOWN = "unknown";
    public static final String ACTIVE_RELATION_STOP_OFFSET = "activeRelationStopOffset";

    public RoutingPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoom) {
        super(new DerivedDataSet(editorAccess.getEditor().getLayer().getDataSet()) {
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
                RouteTraverser routeTraverser = new RouteTraverser(relation);
                List<RouteSegment> segments = routeTraverser.getSegments();
                for (int i = 0; i < segments.size(); i++) {
                    RouteSegment segment = segments.get(i);
                    // Highlights all segments currently in the route
                    segment.getWays().forEach(way -> {
                        OsmPrimitive p = addOrGetDerived(way.getWay());
                        if (p.hasTag(TAG_PART_OF_ACTIVE_ROUTE)) {
                            p.put(TAG_PART_OF_ACTIVE_ROUTE, TAG_PART_OF_ACTIVE_ROUTE_VALUE_MULTIPLE);
                        } else {
                            if (way.isForward()) {
                                p.put(TAG_PART_OF_ACTIVE_ROUTE, TAG_PART_OF_ACTIVE_ROUTE_VALUE_FORWARD);
                            } else {
                                p.put(TAG_PART_OF_ACTIVE_ROUTE, TAG_PART_OF_ACTIVE_ROUTE_VALUE_BACKWARD);
                            }
                        }
                    });
                    // Mark the gaps in the line / end of the line
                    // normal => everything is fine
                    // broken => there should not be a gap there.
                    OsmPrimitive first = addOrGetDerived(segment.getWays().get(0).firstNode());
                    first.put(TAG_ACTIVE_RELATION_SEGMENT_STARTS, i == 0 ? TAG_ACTIVE_RELATION_SEGMENT_VALUE_NORMAL : TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN);
                    OsmPrimitive last = addOrGetDerived(segment.getWays().get(segment.getWays().size() - 1).lastNode());
                    last.put(TAG_ACTIVE_RELATION_SEGMENT_ENDS, i == segments.size() - 1 ? TAG_ACTIVE_RELATION_SEGMENT_VALUE_NORMAL : TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN);
                }

                // Stops on the route
                List<RouteStop> stops = routeTraverser.findStopPositions();
                double lastOffsetOnRoute = 0;
                for (RouteStop stop : stops) {
                    OsmPrimitive stopNode = addOrGetDerived(stop.getRelationMembers().get(0));
                    stop.getStopAttributes().getKeys().forEach((k, v) -> stopNode.put("activeRelationStop_" + k, v));
                    stopNode.put(TAG_ACTIVE_RELATION_STOP_INDEX, stop.getStopIndex() + "");
                    if (stop instanceof RouteStopPositionOnWay) {
                        stopNode.put(ACTIVE_RELATION_STOP_OFFSET,
                            "" + (int) Math.round(((RouteStopPositionOnWay) stop).getPosition().getOffsetInRoute()));

                        PointOnRoute myOffsetOnRoute = ((RouteStopPositionOnWay) stop).getPosition();
                        if (myOffsetOnRoute.getOffsetInRoute() < lastOffsetOnRoute - 10) { // <10m error
                            stopNode.put(TAG_ACTIVE_RELATION_STOP_MISORDERED, "1");
                        }
                        lastOffsetOnRoute = myOffsetOnRoute.getOffsetInRoute();
                    } else {
                        stopNode.put(TAG_ACTIVE_RELATION_STOP_TOO_FAR, "1");
                        // Do not change lastOffsetOnRoute, so that next stop might get error
                    }
                }
            }

            private OsmPrimitive withStopOrPlatformFlag(OsmPrimitive p, String role) {
                if (OSMTags.ROUTE_SEGMENT_PT_ROLES.contains(role)) {
                    p.put(TAG_MEMBER_OF_ACTIVE_RELATION, TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_ROUTE);
                } else if (OSMTags.PLATFORM_ROLES.contains(role)) {
                    p.put(TAG_MEMBER_OF_ACTIVE_RELATION, TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_PLATFORM);
                } else if (OSMTags.STOP_ROLE.contains(role)) {
                    p.put(TAG_MEMBER_OF_ACTIVE_RELATION, TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_STOP);
                } else {
                    p.put(TAG_MEMBER_OF_ACTIVE_RELATION, TAG_MEMBER_OF_ACTIVE_RELATION_VALUE_UNKNOWN);
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
        }, editorAccess, zoom);

        // TODO: To actually determine some features, we might need more.
        if (RelationAccess.of(editorAccess)
            .getMembers()
            .stream()
            .anyMatch(it -> it.getMember().isIncomplete())) {
            add(new IncompleteMembersWarningPanel(), BorderLayout.NORTH);
        }
    }

    @Override
    protected List<String> getStylePath() {
        return Arrays.asList(
            "org/openstreetmap/josm/plugins/pt_assistant/gui/routing/base.mapcss",
            "org/openstreetmap/josm/plugins/pt_assistant/gui/routing/bus.mapcss"
        );
    }

    @Override
    protected OsmPrimitive getPrimitiveAt(Point point) {
        return getOsmPrimitiveAt(point, primitive -> {
            return primitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS)
                || primitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS)
                || primitive.hasTag(TAG_ACTIVE_RELATION_STOP_INDEX);
        });
    }

    @Override
    protected void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
        JPopupMenu menu = new JPopupMenu();

        // May have both: Start and end.
        if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS)) {
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS, TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN)) {
                menu.add(pad(new UnBoldLabel(tr("The route is broken before this point"))));
            } else {
                menu.add(pad(new UnBoldLabel(tr("The route starts at this point"))));
            }
            menu.add(new JSeparator());
        }
        if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS)) {
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS, TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN)) {
                menu.add(pad(new UnBoldLabel(tr("The route is broken after this point"))));
            } else {
                menu.add(pad(new UnBoldLabel(tr("The route ends at this point"))));
            }
            menu.add(new JSeparator());
        }

        if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_STOP_INDEX)) {
            Relation area = StopUtils.findContainingStopArea(originalPrimitive);
            String name = area != null ? area.get("name") : originalPrimitive.get("name");
            menu.add(pad(new UnBoldLabel(tr("Stop {0}: {1}",
                derivedPrimitive.get(TAG_ACTIVE_RELATION_STOP_INDEX), name))));
            if (derivedPrimitive.hasTag(ACTIVE_RELATION_STOP_OFFSET)) {
                menu.add(pad(new UnBoldLabel(tr("Distance from route start: {0} meters",
                    derivedPrimitive.get(ACTIVE_RELATION_STOP_OFFSET)))));
            }
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_STOP_MISORDERED)) {
                menu.add(pad(new UnBoldLabel(tr("This stop is not in the correct order along the route."))));
            }
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_STOP_TOO_FAR)) {
                menu.add(pad(new UnBoldLabel(tr("This stop is too far away from the route. Consider adding a stop_position."))));
            }
        }


        menu.add(StopVicinityPanel.getSelectInMainWindow(Collections.singleton(originalPrimitive)));

        menu.show(this, point.x, point.y);
    }

    private <T extends JComponent> T pad(T c) {
        c.setBorder(new EmptyBorder(5, 5, 5, 5));
        return c;
    }
}
