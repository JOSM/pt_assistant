package org.openstreetmap.josm.plugins.pt_assistant.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.tools.Utils;

public abstract class AbstractVicinityPanel extends JPanel {
    // TODO: On remove, clean up dataSetCopy

    protected final DerivedDataSet dataSetCopy;
    protected final MapView mapView;
    private final MapCSSStyleSource style = readStyle();

    public AbstractVicinityPanel(DerivedDataSet dataSetCopy, ZoomSaver zoom) {
        super(new BorderLayout());
        this.dataSetCopy = dataSetCopy;

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

    protected abstract void doInitialZoom();

    protected MapCSSStyleSource readStyle() {
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

    protected MapCSSStyleSource getStyle() {
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

    protected void doAction(Point point) {
        // default nop
    }

    public void dispose() {
        dataSetCopy.dispose();
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
