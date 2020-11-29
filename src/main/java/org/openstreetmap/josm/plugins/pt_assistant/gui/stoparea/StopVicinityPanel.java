package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationEditorAccessUtils;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.Pair;

/**
 * Allows to show the vicintiy of a stop area end edit it in a visualized view
 * <p>
 * Supported features:
 * - add / remove platform
 * - add / remove stop_position
 */
public class StopVicinityPanel extends AbstractVicinityPanel {
    private final IRelationEditorActionAccess editorAccess;

    public StopVicinityPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoomSaver) {
        super(createDataSetWithNewRelation(editorAccess.getEditor().getLayer(), editorAccess.getEditor().getRelation(), editorAccess), zoomSaver);
        this.editorAccess = editorAccess;

        if (RelationAccess.of(editorAccess)
                .getMembers()
                .stream()
                .anyMatch(it -> it.getMember().isIncomplete())) {
            UnBoldLabel warnPanel = new UnBoldLabel(MessageFormat.format(
                "<html><p>{0}</p><p>{1}</p></html>",
                tr("This relation contains incomplete (not downloaded) members!"),
                tr("Some features may not be visible on this map.")));
            warnPanel.setForeground(new Color(0xAA0000));
            warnPanel.setBorder(new CompoundBorder(
                new LineBorder(warnPanel.getForeground(), 2),
                new EmptyBorder(5, 10, 5, 10)
            ));
            warnPanel.setBackground(new Color(0xFFBABA));
            warnPanel.setOpaque(true);
            //warnPanel.setSize(warnPanel.getMinimumSize());
            //warnPanel.setLocation(10, 40);
            add(warnPanel, BorderLayout.NORTH);
        }
    }

    private static DerivedDataSet createDataSetWithNewRelation(OsmDataLayer layer, Relation stopRelation,
                                                               IRelationEditorActionAccess editorAccess) {
        long editedRelationId = stopRelation == null ? 0 : stopRelation.getId();
        BBox bBox = new BBox();
        RelationEditorAccessUtils.streamMembers(editorAccess)
            // Extra space: Something around 200.500m depending on where we are on the map.
            .forEach(p -> bBox.addPrimitive(p.getMember(), 0.005));

        return new DerivedDataSet(layer.getDataSet()) {
            @Override
            protected boolean isIncluded(OsmPrimitive primitive) {
                return primitive.getType() != OsmPrimitiveType.RELATION
                    // Normal primitives: all in bbox
                    ? bBox.intersects(primitive.getBBox())
                    // Relations: all except the one we edit
                    // todo: restrict this, e.g. only PT relations + multipolygons in bbox
                    : primitive.getId() != editedRelationId;
            }

            @Override
            protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
                // Now apply the relation editor changes
                // Simulate org.openstreetmap.josm.gui.dialogs.relation.actions.SavingAction.applyChanges
                Relation relation = new Relation();
                editorAccess.getTagModel().applyToPrimitive(relation);
                // This is a hack to tag our currently active relation.
                // There is no id selector in MapCSS, so we need a way to uniquely identify our relation
                relation.put("activePtRelation", "1");

                if (stopRelation != null) {
                    addTo.addAsCopy(stopRelation, relation);

                    // Now we search for all sibling relations.
                    // Due to https://josm.openstreetmap.de/ticket/6129#comment:24 we cannot do it in MapCSS
                    stopRelation.getReferrers()
                        .stream()
                        .filter(r -> r instanceof Relation && StopUtils.isStopAreaGroup((Relation) r))
                        .flatMap(parent -> ((Relation) parent).getMembers().stream())
                        .map(RelationMember::getMember)
                        .filter(sibling -> sibling != stopRelation)
                        .forEach(sibling -> {
                            Relation copy = new Relation((Relation) sibling);
                            copy.put("siblingOfActive", "1");
                            // This will add the copy with the fake tag.
                            addOrGetDerived(copy);
                        });

                } else {
                    addTo.add(relation);
                }

                RelationEditorAccessUtils.getRelationMembers(editorAccess)
                    .forEach(m -> relation.addMember(addOrGetDerivedMember(m)));
            }
        };
    }


    @Override
    protected JComponent generateActionButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);

        JButton downloadButton = new JButton(new JosmAction(
            tr("Download vicinity"),
            "download",
            tr("Download data around the current station area."),
            null,
            false
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadUtils.downloadUsingOverpass(
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadStopAreaVicinity.query.txt",
                    line -> line
                        .replace("##NODEIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.NODE))
                        .replace("##WAYIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.WAY))
                        .replace("##RELATIONIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.RELATION)));
            }
        });
        panel.add(downloadButton);
        JButton zoomToButton = new JButton(new JosmAction(
            tr("Zoom to"),
            "dialogs/autoscale/data",
            tr("Zoom to the current station area."),
            null,
            false
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToRelation();
            }
        });
        panel.add(zoomToButton);

        return panel;
    }


    private void zoomToRelation() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        RelationAccess.of(editorAccess).getMembers().forEach(
            m -> m.getMember().accept((OsmPrimitiveVisitor) v));
        mapView.zoomTo(v.getBounds());
        mapView.zoomOut();
    }

    @Override
    protected void doInitialZoom() {
        zoomToRelation();
    }

    @Override
    protected String getStylePath() {
        return "org/openstreetmap/josm/plugins/pt_assistant/gui/stoparea/stopareavicinity.mapcss";
    }

    @Override
    protected void paintComponent(Graphics g) {
        // TODO REMOVE
        super.paintComponent(g);
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
        return rect.getLatLonBoundsBox().toBBox();
    }

    @Override
    protected void doAction(Point point) {
        OsmPrimitive primitive = getPrimitiveAt(point);
        if (primitive != null) {
            OsmPrimitive originalPrimitive = dataSetCopy.findOriginal(primitive);
            if (originalPrimitive != null) {
                List<EStopVicinityAction> actions = getAvailableActions(originalPrimitive);
                if (!actions.isEmpty()) {
                    showActionsMenu(point, originalPrimitive, actions);
                }
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
        for (RelationMember m : RelationAccess.of(editorAccess).getMembers()) {
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
        if (StopUtils.findContainingStopArea(primitive) != null) {
            // If the item is in a different stop area, we don't allow adding it.
            return Arrays.asList(EStopVicinityAction.OPEN_AREA_RELATION);
        } else if (StopUtils.isPlatform(primitive)) {
            return Arrays.asList(EStopVicinityAction.ADD_PLATFORM_TO_STOP_AREA);
        } else if (primitive.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_POSITION_TAG_VALUE)
            || isStopMemberInAnyRoute(primitive)) {
            return Arrays.asList(EStopVicinityAction.ADD_STOP_TO_STOP_AREA);
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isStopMemberInAnyRoute(OsmPrimitive primitive) {
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
                menu.add(createActionButton(tr("Remove from the stop area relation"), () -> {
                    MemberTableModel model = editorAccess.getMemberTableModel();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if (model.getValue(i).getMember().equals(primitive)) {
                            model.remove(i);
                            break;
                        }
                    }
                }));
            }
        },

        OPEN_AREA_RELATION {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                Relation area = StopUtils.findContainingStopArea(primitive);
                if (area != null) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
                    panel.setBorder(new EmptyBorder(5, 5, 5, 5));
                    panel.add(new UnBoldLabel("<html>" + tr("This element belongs to the stop area <i>{0}</i>",
                        UnBoldLabel.safeHtml(area.getName())) + "</html>"));
                    if (StopUtils.findParentStopGroup(area).equals(StopUtils.findParentStopGroup(editorAccess.getEditor().getRelation()))) {
                        panel.add(new UnBoldLabel(tr("This element belongs to the same stop area group.")));
                    }
                    menu.add(panel);
                    menu.add(createActionButton(tr("Open area relation"), () -> {
                        DialogUtils.showRelationEditor(RelationEditor.getEditor(
                            editorAccess.getEditor().getLayer(),
                            area,
                            area.getMembers()
                                .stream()
                                .filter(it -> it.getMember().equals(primitive))
                                .collect(Collectors.toList())
                        ));
                    }));
                }
            }
        }
        ;

        private static void addMember(IRelationEditorActionAccess editorAccess, OsmPrimitive primitive, String role) {
            MemberTableModel table = editorAccess.getMemberTableModel();
            table.addMembersAtEnd(Collections.singletonList(primitive));
            table.updateRole(new int[]{table.getRowCount() - 1}, role);
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
            add(new TagsOfPrimitive(forPrimitive));
            add(new JSeparator());
            for (EStopVicinityAction action : actions) {
                action.addActionButtons(this, forPrimitive, editorAccess);
            }
        }
    }
}
