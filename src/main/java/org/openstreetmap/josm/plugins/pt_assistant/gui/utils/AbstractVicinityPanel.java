package org.openstreetmap.josm.plugins.pt_assistant.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public abstract class AbstractVicinityPanel extends JPanel {
    // TODO: On remove, clean up dataSetCopy

    protected final DerivedDataSet dataSetCopy;
    protected final IRelationEditorActionAccess editorAccess;
    protected final MapView mapView;
    private final List<MapCSSStyleSource> style = Collections.unmodifiableList(readStyles());

    public AbstractVicinityPanel(DerivedDataSet dataSetCopy,
                                 IRelationEditorActionAccess editorAccess,
                                 ZoomSaver zoom) {
        super(new BorderLayout());
        this.dataSetCopy = Objects.requireNonNull(dataSetCopy, "dataSetCopy");
        this.editorAccess = Objects.requireNonNull(editorAccess, "editorAccess");

        MainLayerManager layerManager = new MainLayerManager();
        layerManager.addLayer(new OsmDataLayer(dataSetCopy.getClone(), "", null) {
            @Override
            protected MapViewPaintable.LayerPainter createMapViewPainter(MapViewEvent event) {
                return new FixedStyleLayerPainter(this, style);
            }

            @Override
            public void processDatasetEvent(AbstractDatasetChangedEvent event) {
                // Parent checks for save requirements => we don't need that, it just might deadlock.
                invalidate();
            }
        });
        mapView = new MapView(layerManager, null) {
            boolean initial = true;
            @Override
            public boolean prepareToDraw() {
                dataSetCopy.refreshIfRequired();
                super.prepareToDraw();
                if (initial) {
                    if (zoom.getLastZoom() != null) {
                        zoomTo(zoom.getLastZoom().getCenter().getEastNorth(),
                            zoom.getLastZoom().getScale());
                    } else {
                        doInitialZoom();
                    }
                    initial = false;
                }
                zoom.setLastZoom(getState());
                return true;
            }
        };
        mapView.setMinimumSize(new Dimension(100, 100));
        mapView.setPreferredSize(new Dimension(500, 300));

        ClickAndHoverListener listener = new ClickAndHoverListener();
        mapView.addMouseListener(listener);
        mapView.addMouseMotionListener(listener);

        add(mapView);

        addActionButtons();
    }

    private void addActionButtons() {
        JComponent actionButtons = generateActionButtons();
        if (actionButtons == null) {
            return;
        }
        actionButtons.setSize(actionButtons.getPreferredSize());
        setLocationToTopRight(mapView, actionButtons);
        mapView.add(actionButtons);
        mapView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setLocationToTopRight(mapView, actionButtons);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                setLocationToTopRight(mapView, actionButtons);
            }
        });
    }

    private void setLocationToTopRight(MapView mapView, JComponent actionButtons) {
        actionButtons.setLocation(mapView.getWidth() - actionButtons.getWidth() - 10, 10);
    }

    protected JComponent generateActionButtons() {
        return null;
    }

    protected JButton generateZoomToButton(final String name, final String tooltip) {
        return new JButton(new JosmAction(
            name,
            "dialogs/autoscale/data",
            tooltip,
            null,
            false
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                doInitialZoom();
            }
        });
    }

    protected abstract void doInitialZoom();

    protected List<MapCSSStyleSource> readStyles() {
        return getStylePath()
            .stream()
            .map(stylePath -> new MapCSSStyleSource(stylePath) {
                @Override
                public InputStream getSourceInputStream() {
                    InputStream resource = Utils.getResourceAsStream(getClass(), stylePath);
                    if (resource == null) {
                        throw new IllegalStateException("Could not open bundled mapcss file");
                    }
                    return resource;
                }
            })
            .collect(Collectors.toList());
    }

    protected void zoomToEditorRelation() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        RelationAccess.of(editorAccess).getMembers()
            .forEach(
            m -> m.getMember().accept((OsmPrimitiveVisitor) v));
        mapView.zoomTo(v.getBounds());
        mapView.zoomOut();
    }

    protected abstract List<String> getStylePath();

    protected List<MapCSSStyleSource> getStyles() {
        return style;
    }

    /**
     * Get the primitive to select for the given point
     * @param point The point
     * @return The primitive. May be null.
     */
    protected OsmPrimitive getPrimitiveAt(Point point) {
        return null;
    }

    protected Cascade getCascade(OsmPrimitive primitive) {
        MultiCascade mc = new MultiCascade();
        getStyles().forEach(style -> style.apply(mc, primitive, 1, false));
        return mc.getOrCreateCascade("default");
    }

    protected void doAction(Point point) {
        OsmPrimitive primitive = getPrimitiveAt(point);
        if (primitive != null) {
            OsmPrimitive originalPrimitive = dataSetCopy.findOriginal(primitive);
            if (originalPrimitive != null) {
                doAction(point, originalPrimitive);
            }
        }
    }

    protected void doAction(Point point, OsmPrimitive originalPrimitive) {
        // nop
    }

    private class ClickAndHoverListener implements MouseListener, MouseMotionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            updateMousePosition(e.getPoint());
            doAction(e.getPoint());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            updateMousePosition(e.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateMousePosition(e.getPoint());
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            updateMousePosition(e.getPoint());
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updateMousePosition(null);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateMousePosition(e.getPoint());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateMousePosition(e.getPoint());
        }

        /**
         * Called whenever the mouse position changes
         * @param point The point, may be null.
         */
        private void updateMousePosition(Point point) {
            OsmPrimitive toHighlight = point == null ? null : getPrimitiveAt(point);
            if (toHighlight == null) {
                dataSetCopy.highlight(Collections.emptySet());
            } else {
                dataSetCopy.highlight(Collections.singleton(toHighlight));
            }
        }
    }

    protected OsmPrimitive getOsmPrimitiveAt(Point point, Predicate<OsmPrimitive> matches) {
        // Cannot use the mapView methods - they use a global ref to the active layer
        MapViewState state = mapView.getState();
        MapViewState.MapViewPoint center = state.getForView(point.getX(), point.getY());
        BBox bbox = getBoundsAroundMouse(point, state);
        List<Node> nodes = dataSetCopy.getClone().searchNodes(bbox);
        Optional<Node> nearest = nodes.stream()
            .filter(matches)
            .min(Comparator.comparing(node -> state.getPointFor(node).distanceToInViewSq(center)));
        if (nearest.isPresent()) {
            return nearest.get();
        } else {
            // No nearest node => search way
            List<Way> ways = dataSetCopy.getClone().searchWays(bbox)
                .stream()
                .filter(matches)
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
        MapViewState.MapViewRectangle rect = state.getForView(point.getX() - snapDistance, point.getY() - snapDistance)
            .rectTo(state.getForView(point.getX() + snapDistance, point.getY() + snapDistance));
        return rect.getLatLonBoundsBox().toBBox();
    }

    public void dispose() {
        dataSetCopy.dispose();
    }

    private static class FixedStyleLayerPainter implements MapViewPaintable.LayerPainter {
        private final OsmDataLayer layer;
        private final ElemStyles elemStyles;

        FixedStyleLayerPainter(OsmDataLayer layer, List<MapCSSStyleSource> styles) {
            this.layer = layer;
            elemStyles = new ElemStyles();
            // Ugly hack
            styles.forEach(style -> {
                addStyle(elemStyles, style);
                style.loadStyleSource(); // < we need to do this, JOSM won't do it.
            });
        }

        @Override
        public void paint(MapViewGraphics graphics) {
            StyledMapRenderer renderer = new StyledMapRenderer(graphics.getDefaultGraphics(),
                graphics.getMapView(),
                false);
            renderer.setStyles(elemStyles);
            renderer.render(layer.getDataSet(), false, graphics.getClipBounds().getLatLonBoundsBox());
        }

        @Override
        public void detachFromMapView(MapViewPaintable.MapViewEvent event) {
        }
    }

    private static void addStyle(ElemStyles styles, StyleSource styleToAdd) {
        try {
            Method add = ElemStyles.class.getDeclaredMethod("add", StyleSource.class);
            add.setAccessible(true);
            add.invoke(styles, styleToAdd);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot invoke add", e);
        }
    }
}
