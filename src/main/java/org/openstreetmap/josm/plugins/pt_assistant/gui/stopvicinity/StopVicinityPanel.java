package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.Pair;

/**
 * Allows to show the vicintiy of a stop area end edit it in a visualized view
 *
 * Supported features:
 * - add / remove platform
 * - add / remove stop_position
 */
public class StopVicinityPanel extends AbstractVicinityPanel {
    private final PrimitiveId stopRelation;
    private final IRelationEditorActionAccess editorAccess;

    public StopVicinityPanel(Relation stopRelation, IRelationEditorActionAccess editorAccess, ZoomSaver zoomSaver) {
        super(createDataSetWithNewRelation(stopRelation, editorAccess), zoomSaver);
        this.stopRelation = stopRelation;
        this.editorAccess = editorAccess;

        addActionButtons();
    }

    private static DataSetClone createDataSetWithNewRelation(Relation stopRelation,
                                                             IRelationEditorActionAccess editorAccess) {
        DataSetClone clone = new DataSetClone(stopRelation.getDataSet());

        // Now apply the relation editor changes
        // Simulate org.openstreetmap.josm.gui.dialogs.relation.actions.SavingAction.applyChanges
        // We cannot use that method, since it uses global undo/redo queue
        Relation relation = (Relation) clone.getClone().getPrimitiveById(stopRelation.getPrimitiveId());
        if (relation == null) {
            relation = new Relation();
            clone.getClone().addPrimitive(relation);
        }
        editorAccess.getTagModel().applyToPrimitive(relation);

        // Same as getMemberTableModel().applyToRelation(relation);
        MemberTableModel membersModel = editorAccess.getMemberTableModel();
        List<RelationMember> members = IntStream.range(0, membersModel.getRowCount())
            .mapToObj(membersModel::getValue)
            .filter(rm -> !rm.getMember().isDeleted() && rm.getMember().getDataSet() != null)
            .collect(Collectors.toList());

        relation.setMembers(clone.convertMembers(members));
        // This is a hack to tag our currently active relation.
        // There is no id selector in MapCSS, so we need a way to uniquely identify our relation
        relation.put("activePtRelation", "1");
        return clone;
    }

    private void addActionButtons() {
        JButton zoomToButton = new JButton(new JosmAction(
            tr("Zoom to"),
            "dialogs/autoscale/data",
            tr("Zoom to the current station area."),
            null,
            false
        ){
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToRelation();
            }
        });
        zoomToButton.setSize(zoomToButton.getPreferredSize());
        setLocationToTopRight(mapView, zoomToButton);
        mapView.add(zoomToButton);
        mapView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setLocationToTopRight(mapView, zoomToButton);
            }
            @Override
            public void componentShown(ComponentEvent e) {
                setLocationToTopRight(mapView, zoomToButton);
            }
        });
    }

    private void setLocationToTopRight(MapView mapView, JButton zoomToButton) {
        zoomToButton.setLocation(mapView.getWidth() - zoomToButton.getWidth() - 10, 10);
    }

    private void zoomToRelation() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        v.visit(getStopRelationClone());
        mapView.zoomTo(v.getBounds());
    }

    private Relation getStopRelationClone() {
        return (Relation) dataSetCopy.getClone().getPrimitiveById(stopRelation);
    }

    @Override
    protected void doInitialZoom() {
        zoomToRelation();
    }

    @Override
    protected String getStylePath() {
        return "org/openstreetmap/josm/plugins/pt_assistant/gui/stopvicinity/vicinitystyle.mapcss";
    }

    @Override
    protected OsmPrimitive getPrimitiveAt(Point point) {
        // Cannot use the mapView methods - they use a global ref to the active layer
        MapViewState state = mapView.getState();
        MapViewPoint center = state.getForView(point.getX(), point.getY());
        BBox bbox = getBoundsAroundMouse(point, state);
        List<Node> nodes = dataSetCopy.getClone().searchNodes(bbox);
        Optional<Node> nearest = nodes.stream()
            .filter(it -> !getAvailableActions(it).isEmpty())
            .min(Comparator.comparing(node -> state.getPointFor(node).distanceToInViewSq(center)));
        if (nearest.isPresent()) {
            return nearest.get();
        } else {
            // No nearest node => search way
            List<Way> ways = dataSetCopy.getClone().searchWays(bbox)
                .stream()
                .filter(it -> !getAvailableActions(it).isEmpty())
                .collect(Collectors.toList());

            Integer snapDistance = MapView.PROP_SNAP_DISTANCE.get();
            return ways.stream()
                .filter(way -> wayAreaContains(way, point))
                .findFirst()
                .orElseGet(() -> ways.stream()
                    .map(way -> new Pair<>(way, distanceSq(way, point)))
                    // Acutally, it is snap way distance, but we don't have that as prop
                    .filter(wd -> wd.b < snapDistance * snapDistance)
                    .min(Comparator.comparing(wd -> wd.b))
                    .map(wd -> wd.a)
                    .orElse(null));
        }
    }

    private double distanceSq(Way way, Point point) {
        double minDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < way.getNodesCount() - 1; i++) {
            Point2D pA = mapView.getState().getPointFor(way.getNode(i)).getInView();
            Point2D pB = mapView.getState().getPointFor(way.getNode(i + 1)).getInView();
            double c = pA.distanceSq(pB);
            if (c < 1) {
                continue;
            }

            double a = point.distanceSq(pB);
            double b = point.distanceSq(pA);
            if (a > c || b > c) {
                continue;
            }

            double perDistSq = a - (a - b + c) * (a - b + c) / 4 / c;
            minDistance = Math.min(perDistSq, minDistance);
        }
        return minDistance;
    }

    private boolean wayAreaContains(Way way, Point point) {
        MapViewPath path = new MapViewPath(mapView.getState());
        path.appendClosed(way.getNodes(), false);
        return path.contains(point);
    }

    private BBox getBoundsAroundMouse(Point point, MapViewState state) {
        Integer snapDistance = MapView.PROP_SNAP_DISTANCE.get();
        MapViewRectangle rect = state.getForView(point.getX() - snapDistance, point.getY() - snapDistance)
            .rectTo(state.getForView(point.getX() + snapDistance, point.getY() + snapDistance));
        BBox bbox = rect.getLatLonBoundsBox().toBBox();
        return bbox;
    }

    @Override
    protected void doAction(Point point) {
        OsmPrimitive primitive = getPrimitiveAt(point);
        if (primitive != null) {
            List<EStopVicinityAction> actions = getAvailableActions(primitive);
            if (!actions.isEmpty()) {
                showActionsMenu(point, primitive, actions);
            }
        }
    }

    private void showActionsMenu(Point point, OsmPrimitive primitive, List<EStopVicinityAction> actions) {
        ActionsMenu menu = new ActionsMenu(primitive, actions);
        menu.show(mapView, point.x, point.y);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                dataSetCopy.getClone().setSelected(Collections.singleton(primitive));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                dataSetCopy.getClone().setSelected(Collections.emptySet());
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    protected List<EStopVicinityAction> getAvailableActions(OsmPrimitive primitive) {
        for (RelationMember m : getStopRelationClone().getMembers()) {
            if (m.getMember().equals(primitive)) {
                return getAvailableActionsForMember(primitive, m.getRole());
            }
        }
        return getAvailableActionsForNonmember(primitive);
    }

    private List<EStopVicinityAction> getAvailableActionsForMember(OsmPrimitive primitive, String role) {
        if (OSMTags.PLATFORM_ROLE.equals(role)) {
            return Arrays.asList(EStopVicinityAction.REMOVE_FROM_STOP_AREA);
        } else if (OSMTags.STOP_ROLE.equals(role)) {
            return Arrays.asList(EStopVicinityAction.REMOVE_FROM_STOP_AREA);
        } else {
            return Collections.emptyList();
        }
    }

    private List<EStopVicinityAction> getAvailableActionsForNonmember(OsmPrimitive primitive) {
        if (primitive.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.PLATFORM_TAG_VALUE)) {
            return Arrays.asList(EStopVicinityAction.ADD_PLATFORM_TO_STOP_AREA);
        } else if (primitive.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_POSITION_TAG_VALUE)
            || isStopMemberAnywhere(primitive)) {
            return Arrays.asList(EStopVicinityAction.ADD_STOP_TO_STOP_AREA);
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isStopMemberAnywhere(OsmPrimitive primitive) {
        return primitive
            .getReferrers()
            .stream()
            .filter(r -> r instanceof Relation)
            .map(r -> (Relation) r)
            .filter(RouteUtils::isPTRoute)
            .anyMatch(r -> r.getMembers()
                .stream()
            .anyMatch(member -> member.getMember().equals(primitive) && OSMTags.STOP_ROLES.contains(member.getRole())));
    }

    // Enum to make it fast, e.g for getAvailableActions
    enum EStopVicinityAction {
        // Add with stop role
        ADD_STOP_TO_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Add this stop to the stop area relation"), () -> {
                    addMember(editorAccess, primitive, OSMTags.STOP_ROLE);
                }));
            }
        },
        // Add with platform role
        ADD_PLATFORM_TO_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Add this platform to the stop area relation"), () -> {
                    addMember(editorAccess, primitive, OSMTags.PLATFORM_ROLE);
                }));
            }
        },
        // Remove any member
        REMOVE_FROM_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Remove this from the stop area relation"), () -> {
                    MemberTableModel model = editorAccess.getMemberTableModel();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if (model.getValue(i).getMember().equals(primitive)) {
                            model.remove(i);
                            break;
                        }
                    }

                }));
            }
        };

        private static void addMember(IRelationEditorActionAccess editorAccess, OsmPrimitive primitive, String role) {
            MemberTableModel table = editorAccess.getMemberTableModel();
            table.addMembersAtEnd(Collections.singletonList(primitive));
            table.updateRole(new int[] {table.getRowCount() - 1}, role);
        }

        abstract void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess);

        private static JMenuItem createActionButton(String text, Runnable action) {
            return new JMenuItem(new AbstractAction(text) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }
    }

    private class ActionsMenu extends JPopupMenu {
        ActionsMenu(OsmPrimitive forPrimitive, List<EStopVicinityAction> actions) {
            for (EStopVicinityAction action: actions) {
                action.addActionButtons(this, forPrimitive, editorAccess);
            }
        }
    }
}
