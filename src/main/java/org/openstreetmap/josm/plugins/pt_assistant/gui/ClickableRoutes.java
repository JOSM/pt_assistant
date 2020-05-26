package org.openstreetmap.josm.plugins.pt_assistant.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;

import java.awt.*;

/* TODO: Get a good icon image */

public class ClickableRoutes extends MapMode {

    private final transient AbstractMapViewPaintable temporaryLayer = new AbstractMapViewPaintable() {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {

        }
    };

    /**
     * Constructs a new ClickableRoutes
     */
    public ClickableRoutes() {
        super(tr("Select Route"), "improvewayaccuracy", tr("Select the correct route"), Cursor.getDefaultCursor());
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MapFrame map = MainApplication.getMap();
        MapView mv = map.mapView;
        Point mousePos = null;
        String oldModeHelpText = "";

        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        map.mapView.addTemporaryLayer(temporaryLayer);
    }
}
