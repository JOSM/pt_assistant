package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router.AbstractRouter;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router.FromNodeRouter;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router.FromSegmentRouter;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router.RouteSplitSuggestion;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router.RouteTarget;
import org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea.StopVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.IncompleteMembersWarningPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.tools.ImageProvider;

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
    private static final int MIN_ROUTING_DISTANCE = 1000;
    private static final int MIN_ROUTING_SEGMENTS = 5;
    private static final Color ROUTER_HIGHLIGHT_ROUTE = new Color(0xFF1A1A);
    private static final Color ROUTER_HIGHLIGHT_ROUTE_SPLIT = new Color(0xEC8181);
    private final static ImageIcon SPLIT_ICON = ImageProvider.get("splitway.svg");

    private RoutingPanelSpecialMode mode;
    private JPanel actionButtonsPanel;

    public RoutingPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoom) {
        super(new RoutingDerivedDataSet(editorAccess), editorAccess, zoom);

        // TODO: To actually determine some features, we might need more.
        if (RelationAccess.of(editorAccess)
            .getMembers()
            .stream()
            .anyMatch(it -> it.getMember().isIncomplete())) {
            add(new IncompleteMembersWarningPanel(), BorderLayout.NORTH);
        }

        setMode(defaultMode());
    }

    private RoutingPanelSpecialMode defaultMode() {
        return dataSetCopy.getRouteTraverser().getSegments().isEmpty()
            ? new SelectStartPointMode() : new NormalMode();
    }

    @Override
    protected JComponent generateActionButtons() {
        if (actionButtonsPanel == null) {
            // Cannot set in constructor => super calls this to early.
            actionButtonsPanel = new JPanel();
            actionButtonsPanel.setOpaque(false);
        }
        return actionButtonsPanel;
    }

    private void setMode(RoutingPanelSpecialMode newMode) {
        if (this.mode != null) {
            this.mode.exitMode();
            actionButtonsPanel.removeAll();
        }
        this.mode = newMode;
        newMode.createActionButtons().forEach(generateActionButtons()::add);
        newMode.enterMode();
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
        return getOsmPrimitiveAt(point, mode.primitiveFilter());
    }

    @Override
    protected void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
        mode.doAction(point, derivedPrimitive, originalPrimitive);
    }

    private static <T extends JComponent> T pad(T c) {
        c.setBorder(new EmptyBorder(5, 5, 5, 5));
        return c;
    }

    /**
     * Special modes, like traversing a route
     */
    private interface RoutingPanelSpecialMode {
        void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive);

        default void exitMode() {
            // Nop
        }

        default void enterMode() {
            // Nop
        }

        Predicate<OsmPrimitive> primitiveFilter();

        default Iterable<JComponent> createActionButtons() {
            return Collections.emptyList();
        }
    }

    private class NormalMode implements RoutingPanelSpecialMode{
        @Override
        public void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
            JPopupMenu menu = new JPopupMenu();

            // Segment starts / ends
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

                    // currently, we can only route at end of route. And we only do it if end is not ambiguous
                    List<RouteSegmentWay> previousRoute = dataSetCopy.getRouteTraverser()
                        .getSegments()
                        .stream()
                        // We should only continue the last way in a segment
                        // (user should delete part of the route first to replace it)
                        .map(seg -> seg.getWays().get(seg.getWays().size() - 1))
                        .filter(it -> it.lastNode() == originalPrimitive)
                        .collect(Collectors.toList());
                    if (previousRoute.size() == 1) {
                        menu.add(new JMenuItem(new JosmAction(tr("Add next ways (interactive)"), null, null, null, false) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                setMode(new RoutingMode(new FromSegmentRouter(previousRoute.get(0), dataSetCopy.getRouteTraverser().getType())));
                            }
                        }));
                    } else if (previousRoute.size() > 1) {
                        menu.add(new UnBoldLabel(tr("Multiple route segments end at this point")));
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

            // Remove from route
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

            menu.show(RoutingPanel.this, point.x, point.y);
        }

        @Override
        public Predicate<OsmPrimitive> primitiveFilter() {
            return primitive -> primitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_ENDS)
                || primitive.hasTag(TAG_ACTIVE_RELATION_SEGMENT_STARTS)
                || primitive.hasTag(TAG_ACTIVE_RELATION_STOP_INDEX)
                || primitive.hasTag(TAG_PART_OF_ACTIVE_ROUTE);
        }
    }

    private class SelectStartPointMode implements RoutingPanelSpecialMode {

        private final MapViewPaintable highlightHoveredNode = (g, mv, bbox) -> {
            // Normal map highlighting won't highlight the nodes, since they are not neccessarely painted.
            // We mark it with a red dot.
            g.setColor(ROUTER_HIGHLIGHT_ROUTE);
            dataSetCopy.getHighlightedPrimitives()
                .stream()
                .map(id -> editorAccess.getEditor().getLayer().getDataSet().getPrimitiveById(id))
                .filter(it -> it instanceof Node)
                .forEach(node -> g.fill(circleArountPoint(mv.getState().getPointFor((Node) node))));
        };

        @Override
        public void enterMode() {
            mapView.addTemporaryLayer(highlightHoveredNode);
        }

        @Override
        public void exitMode() {
            mapView.removeTemporaryLayer(highlightHoveredNode);
        }

        @Override
        public void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
            if (originalPrimitive instanceof Node) {
                // Index at which we should start adding our new primitives.
                // Normally, this is after stops/platforms but before the first way.
                // Since we cannot be sure that the relation is ordered propertly, we just search the first way.
                // May be -1 to start at beginning of relation
                List<RelationMember> members = RelationAccess.of(editorAccess).getMembers();
                int startAfterIndex = members
                    .stream()
                    .filter(m -> !OSMTags.STOPS_AND_PLATFORMS_ROLES.contains(m.getRole()))
                    .findFirst()
                    .map(it -> members.indexOf(it) - 1)
                    .orElse(-1);
                setMode(new RoutingMode(new FromNodeRouter((Node) originalPrimitive,
                    startAfterIndex, dataSetCopy.getRouteTraverser().getType())));
            }
        }

        @Override
        public Predicate<OsmPrimitive> primitiveFilter() {
            return p -> p instanceof Node
                // TODO: Only return a node, if those ways are acutally suited for this type of relation.
                && (p.referrers(Way.class).count() > 1
                || p.hasTag(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_POSITION_TAG_VALUE));
        }

        @Override
        public Iterable<JComponent> createActionButtons() {
            if (dataSetCopy.getRouteTraverser().getSegments().isEmpty()) {
                // No segments => we can only select the start point, do not show cancel button
                return Arrays.asList(
                    createHint(tr("Your relation does not contain any ways. Please select the start node."))
                );
            } else {
                return Arrays.asList(
                    createHint(tr("Please select the start node.")),
                    new JButton(new JosmAction(tr("Cancel"), null, null, null, false) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setMode(new NormalMode());
                        }
                    })
                );
            }
        }

        private UnBoldLabel createHint(String hint) {
            UnBoldLabel label = new UnBoldLabel(hint);
            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
            label.setBorder(new LineBorder(Color.WHITE, 3));
            return label;
        }
    }

    private class RoutingMode implements RoutingPanelSpecialMode {

        private final List<RouteTarget> targets;
        private final AbstractRouter router;
        private final Set<OsmPrimitive> targetEnds;
        private final MapViewPaintable hoverLayer;
        private final List<RouteSplitSuggestion> splitSuggestions;

        private RoutingMode(AbstractRouter router) {
            this.router = router;
            this.targets = router.findRouteTargets(MIN_ROUTING_DISTANCE, MIN_ROUTING_SEGMENTS);
            this.splitSuggestions = router.findRouteSplits();
            this.targetEnds = targets
                .stream()
                .map(RouteTarget::getEnd)
                .collect(Collectors.toSet());

            this.hoverLayer = (g, mv, bbox) -> {
                Set<Node> activeNodes = new HashSet<>();
                Set<PrimitiveId> hoveredSet = dataSetCopy.getHighlightedPrimitives();
                Optional<RouteSplitSuggestion> foundSplit = Optional.empty();
                if (hoveredSet.size() == 1) {
                    g.setStroke(new BasicStroke(3));
                    PrimitiveId hovered = hoveredSet.iterator().next();
                    foundSplit = findSplit(hovered);
                    foundSplit.ifPresent(split -> {
                        // Parts of the way that won't be used after split.
                        MapViewPath line = new MapViewPath(mapView);
                        line.append(split.getSegmentBefore(), false);
                        line.append(split.getSegmentAfter(), false);
                        g.setColor(ROUTER_HIGHLIGHT_ROUTE_SPLIT);
                        g.draw(line);

                        // This is the segment used after split
                        MapViewPath lineActive = new MapViewPath(mapView);
                        lineActive.append(split.getSegment(), false);
                        g.setColor(ROUTER_HIGHLIGHT_ROUTE);
                        g.draw(lineActive);

                        activeNodes.add(split.getEndAtNode());
                    });

                    findTrace(hovered).ifPresent(target -> {
                        // Paint a line
                        MapViewPath line = new MapViewPath(mapView);
                        target.getTrace()
                            .stream()
                            .peek(it -> activeNodes.add(it.lastNode()))
                            .forEach(toDraw -> line.append(toDraw.getWay().getNodes(), false));

                        g.setColor(ROUTER_HIGHLIGHT_ROUTE);
                        g.draw(line);
                    });
                }

                g.setStroke(new BasicStroke(2));
                // Highlight split targets
                g.setColor(ROUTER_HIGHLIGHT_ROUTE_SPLIT);
                for (RouteSplitSuggestion it : splitSuggestions) {
                    drawHighlightCircle(mv, g, activeNodes, it.getEndAtNode());
                }

                // Highlight all possible routing targets
                g.setColor(ROUTER_HIGHLIGHT_ROUTE);
                for (OsmPrimitive it : targetEnds) {
                    drawHighlightCircle(mv, g, activeNodes, it);
                }

                // Paint split icons over everything else
                foundSplit.ifPresent(split -> {
                    // Split indicators
                    Node splitStart = split.getStartAtNode();
                    if (!split.getWay().isFirstLastNode(splitStart)) {
                        drawSplitIcon(mv, g, splitStart);
                    }
                    Node splitEnd = split.getEndAtNode();
                    if (!split.getWay().isFirstLastNode(splitEnd)) {
                        drawSplitIcon(mv, g, splitEnd);
                    }
                });
            };
        }

        private void drawSplitIcon(MapView mv, Graphics2D g, Node node) {
            MapViewPoint pos = mv.getState().getPointFor(node);
            g.setColor(new Color(0x79FFFFFF, true));
            g.fillRect((int) pos.getInViewX() - 8, (int) pos.getInViewY() - 8,
                16, 16);
            g.drawImage(SPLIT_ICON.getImage(),
                (int) pos.getInViewX() - 8, (int) pos.getInViewY() - 8,
                16, 16, null);
        }

        private void drawHighlightCircle(MapView mv, Graphics2D g, Set<Node> activeNodes, OsmPrimitive it) {
            MapViewPoint pos = mv.getState().getPointFor((Node) it);
            Ellipse2D.Double circle = circleArountPoint(pos);
            if (activeNodes.contains(it)) {
                g.fill(circle);
            } else {
                g.draw(circle);
            }
        }

        private Optional<RouteTarget> findTrace(PrimitiveId target) {
            return targets
                .stream()
                .filter(it -> it.getEnd().getPrimitiveId().equals(target))
                .findFirst();
        }


        private Optional<RouteSplitSuggestion> findSplit(PrimitiveId target) {
            return splitSuggestions
                .stream()
                .filter(it -> it.getEndAtNode().getPrimitiveId().equals(target))
                .findFirst();
        }

        @Override
        public void enterMode() {
            mapView.addTemporaryLayer(hoverLayer);
        }

        @Override
        public void exitMode() {
            mapView.removeTemporaryLayer(hoverLayer);
        }

        @Override
        public Predicate<OsmPrimitive> primitiveFilter() {
            return primitive -> targetEnds.contains(primitive)
                || splitSuggestions.stream().anyMatch(split -> split.getEndAtNode().equals(primitive));
        }

        @Override
        public void doAction(Point point, OsmPrimitive derivedPrimitive, OsmPrimitive originalPrimitive) {
            Optional<RouteTarget> foundTrace = findTrace(originalPrimitive.getPrimitiveId());
            foundTrace.ifPresent(trace -> {
                editorAccess.getMemberTableModel().addMembersAfterIdx(
                    trace.getTrace()
                        .stream()
                        .map(RouteSegmentWay::getWay)
                    .collect(Collectors.toList()), router.getIndexInMembersToAddAfter());
            });
            if (!foundTrace.isPresent()) {
                Optional<RouteSplitSuggestion> foundSplit = findSplit(originalPrimitive.getPrimitiveId());
                foundSplit.ifPresent(split -> {
                    if (1 == new AskAboutSplitDialog(split).showDialog().getValue()) {
                        HashSet<Node> segment = new HashSet<>(split.getSegment());
                        DataSet ds = split.getWay().getDataSet();
                        Objects.requireNonNull(ds, "ds");
                        Collection<OsmPrimitive> oldSelection = new ArrayList<>(ds.getSelected());

                        // There is no split way method that does not depend on context.
                        // We need to set selection, the action will then use the selected ways.
                        ds.setSelected(Stream.concat(
                            Stream.of(split.getWay()),
                            split.streamSplitNodes()
                        ).collect(Collectors.toList()));
                        SplitWayAction.runOn(ds);

                        // No return value. But all the new split result ways are selected.
                        // We try to find the way that was a result of our split.
                        // Comparing start/end nodes is not enough, since we might have loops
                        List<Way> result = ds.getSelectedWays()
                            .stream()
                            // We try to find the way that was a result of our split.
                            // Comparing start/end nodes is not enough, since we might have loops
                            .filter(it -> new HashSet<>(it.getNodes()).equals(segment))
                            .collect(Collectors.toList());
                        // silently ignore if not found => user is presented with normal route selection and can retry.
                        if (result.size() == 1) {
                            editorAccess.getMemberTableModel().addMembersAfterIdx(
                                Arrays.asList(result.get(0)),
                                router.getIndexInMembersToAddAfter());
                        }

                        // Restore selection
                        ds.setSelected(oldSelection);
                    }
                });
            }
        }

        @Override
        public Iterable<JComponent> createActionButtons() {
            return Arrays.asList(new JButton(new JosmAction(tr("Done"), null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMode(defaultMode());
                }
            }));
        }
    }

    private static Ellipse2D.Double circleArountPoint(MapViewPoint pos) {
        return new Ellipse2D.Double(pos.getInViewX() - 3, pos.getInViewY() - 3, 7, 7);
    }

    private class AskAboutSplitDialog extends ExtendedDialog {

        public AskAboutSplitDialog(RouteSplitSuggestion split) {
            super(mapView, tr("Split way in main dataset?"),
                tr("Split way"), tr("Abort"));
            setContent(tr("Do you really want to split this way?\nIt will be split in the main dataset. Closing this relation editor will not undo the split."));
            setButtonIcons("splitway", "cancel");
            setCancelButton(2);
            toggleEnable("pt_split_way_ask");
        }
    }
}
