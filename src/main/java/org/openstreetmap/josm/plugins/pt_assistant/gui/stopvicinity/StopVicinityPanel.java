package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.Utils;

public class StopVicinityPanel extends JPanel {
    private final DataSetClone dataSetCopy;
    private final Relation stopRelation;

    public StopVicinityPanel(Relation stopRelation) {
        super(new BorderLayout());
        this.stopRelation = stopRelation;
        MainLayerManager layerManager = new MainLayerManager();
        dataSetCopy = new DataSetClone(stopRelation.getDataSet(), Arrays.asList(stopRelation));
        layerManager.addLayer(new OsmDataLayer(dataSetCopy.getClone(), "", null) {
            @Override
            protected LayerPainter createMapViewPainter(MapViewEvent event) {
                return new FixedStyleLayerPainter(this, readStyle());
            }
        });
        MapView mapView = new MapView(layerManager, null) {
            boolean initial = true;
            @Override
            public boolean prepareToDraw() {
                super.prepareToDraw();
                if (initial) {
                    zoomToRelation(this);
                    initial = false;
                }
                return true;
            }
        };
        mapView.setMinimumSize(new Dimension(100, 100));
        mapView.setPreferredSize(new Dimension(500, 300));
        mapView.setBorder(new LineBorder(Color.RED));
        add(mapView);


        addActionButtons(mapView);
    }

    private void addActionButtons(MapView mapView) {
        JButton zoomToButton = new JButton(new JosmAction(
            tr("Zoom to"),
            "dialogs/autoscale/data",
            tr("Zoom to the current station area."),
            null,
            false
        ){
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomToRelation(mapView);
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

    private void zoomToRelation(MapView mapView) {
        BoundingXYVisitor v = new BoundingXYVisitor();
        v.visit(stopRelation);
        mapView.zoomTo(v);
    }

    // TODO: On remove, clean up dataSetCopy

    private MapCSSStyleSource readStyle() {
        return new MapCSSStyleSource("") {
            @Override
            public InputStream getSourceInputStream() throws IOException {
                InputStream resource = Utils.getResourceAsStream(getClass(),
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/stopvicinity/vicinitystyle.mapcss");
                if (resource == null) {
                    throw new IllegalStateException("Could not open bundled mapcss file");
                }
                return resource;
            }
        };
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
}
