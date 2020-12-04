package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea.StopVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.IncompleteMembersWarningPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class RoutingPanel extends AbstractVicinityPanel<RoutingDerivedDataSet> {

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
        super(new RoutingDerivedDataSet(editorAccess), editorAccess, zoom);

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
                || primitive.hasTag(TAG_ACTIVE_RELATION_STOP_INDEX)
                || primitive.hasTag(TAG_PART_OF_ACTIVE_ROUTE);
        });
    }

    @Override
    protected void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
        JPopupMenu menu = new JPopupMenu();

        if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS) || derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS)) {
            // May have both: Start and end.
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS)) {
                if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS, TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN)) {
                    menu.add(pad(new UnBoldLabel(tr("The route is broken before this point"))));
                } else {
                    menu.add(pad(new UnBoldLabel(tr("The route starts at this point"))));
                }
            }
            if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS)) {
                if (derivedPrimitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS, TAG_ACTIVE_RELATION_SEGMENT_VALUE_BROKEN)) {
                    menu.add(pad(new UnBoldLabel(tr("The route is broken after this point"))));
                } else {
                    menu.add(pad(new UnBoldLabel(tr("The route ends at this point"))));
                }
            }

            menu.add(new JMenuItem(new JosmAction(tr("Download adjacent ways"), null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DownloadUtils.downloadUsingOverpass("org/openstreetmap/josm/plugins/pt_assistant/gui/routing/downloadWaysStartingAtNode.query.txt", line -> line
                        .replace("##SELECTOR##", dataSetCopy.getRouteTraverser().getType().getOverpassFilterForPossibleWays())
                        .replace("##STARTNODEID##", "" + originalPrimitive.getId()));
                }
            }));

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

            menu.add(new JMenuItem(new JosmAction(tr("Remove this stop from the route"), null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editorAccess.getMemberTableModel().removeMembersReferringTo(Arrays.asList(originalPrimitive));
                }
            }));

            menu.add(new JSeparator());
        }

        if (derivedPrimitive.hasTag(TAG_PART_OF_ACTIVE_ROUTE)) {
            if (derivedPrimitive.hasTag(TAG_PART_OF_ACTIVE_ROUTE, TAG_PART_OF_ACTIVE_ROUTE_VALUE_MULTIPLE)) {
                menu.add(pad(new UnBoldLabel(tr("This way is included in the route multiple times"))));
            }

            menu.add(new JMenuItem(new JosmAction(tr("Remove way from route"), null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editorAccess.getMemberTableModel().removeMembersReferringTo(Arrays.asList(originalPrimitive));
                }
            }));
        }


        menu.add(StopVicinityPanel.getSelectInMainWindow(Collections.singleton(originalPrimitive)));

        menu.show(this, point.x, point.y);
    }

    private <T extends JComponent> T pad(T c) {
        c.setBorder(new EmptyBorder(5, 5, 5, 5));
        return c;
    }

}
