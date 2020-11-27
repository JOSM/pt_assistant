package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.tools.Utils;

public abstract class AbstractVicinityPanel extends JPanel {
    // TODO: On remove, clean up dataSetCopy

    protected final DataSetClone dataSetCopy;
    protected final MapView mapView;

    public AbstractVicinityPanel(DataSetClone dataSetCopy, ZoomSaver zoom) {
        super(new BorderLayout());
        this.dataSetCopy = dataSetCopy;

        MainLayerManager layerManager = new MainLayerManager();
        layerManager.addLayer(new OsmDataLayer(dataSetCopy.getClone(), "", null) {
            @Override
            protected MapViewPaintable.LayerPainter createMapViewPainter(MapViewEvent event) {
                return new FixedStyleLayerPainter(this, readStyle());
            }
        });
        mapView = new MapView(layerManager, null) {
            boolean initial = true;
            @Override
            public boolean prepareToDraw() {
                super.prepareToDraw();
                if (initial) {
                    if (zoom.getLastZoom() != null) {
                        zoomTo(zoom.getLastZoom());
                    } else {
                        doInitialZoom();
                    }
                    initial = false;
                }
                zoom.setLastZoom(getState().getViewArea().getProjectionBounds());
                return true;
            }
        };
        mapView.setMinimumSize(new Dimension(100, 100));
        mapView.setPreferredSize(new Dimension(500, 300));

        ClickAndHoverListener listener = new ClickAndHoverListener();
        mapView.addMouseListener(listener);
        mapView.addMouseMotionListener(listener);

        add(mapView);
    }

    protected abstract void doInitialZoom();

    private MapCSSStyleSource readStyle() {
        return new MapCSSStyleSource("") {
            @Override
            public InputStream getSourceInputStream() throws IOException {
                InputStream resource = Utils.getResourceAsStream(getClass(),
                    getStylePath());
                if (resource == null) {
                    throw new IllegalStateException("Could not open bundled mapcss file");
                }
                return resource;
            }
        };
    }

    protected abstract String getStylePath();

    /**
     * Get the primitive to select for the given point
     * @param point The point
     * @return The primitive. May be null.
     */
    protected OsmPrimitive getPrimitiveAt(Point point) {
        return null;
    }

    private class ClickAndHoverListener implements MouseListener, MouseMotionListener {

        private Set<OsmPrimitive> lastHighlighted = Collections.emptySet();

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
            Set<OsmPrimitive> currentHighlight = toHighlight == null ? Collections.emptySet() : Collections.singleton(toHighlight);
            if (!lastHighlighted.equals(currentHighlight)) {
                lastHighlighted.forEach(it -> it.setHighlighted(false));
                currentHighlight.forEach(it -> it.setHighlighted(true));
                lastHighlighted = currentHighlight;
            }

        }
    }

    protected void doAction(Point point) {
        // default nop
    }

    private static class FixedStyleLayerPainter implements MapViewPaintable.LayerPainter {
        private final OsmDataLayer layer;
        private final ElemStyles styles;

        FixedStyleLayerPainter(OsmDataLayer layer, MapCSSStyleSource style) {
            this.layer = layer;
            styles = new ElemStyles();
            // Ugly hack
            addStyle(styles, style);
            style.loadStyleSource(); // < we need to do this, JOSM won't do it.
        }

        @Override
        public void paint(MapViewGraphics graphics) {
            StyledMapRenderer renderer = new StyledMapRenderer(graphics.getDefaultGraphics(),
                graphics.getMapView(),
                false);
            renderer.setStyles(styles);
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
