package org.openstreetmap.josm.plugins.pt_assistant.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/* TODO: Get a good icon image */

public class ClickableRoutes extends MapMode implements MouseListener, MouseMotionListener {

    private Point mousePosition;
    private MapView mapView;

    private final transient AbstractMapViewPaintable temporaryLayer;

    /**
     * Constructs a new ClickableRoutes
     */
    public ClickableRoutes(AbstractMapViewPaintable temporaryLayer) {
        super(tr("Select Route"), "improvewayaccuracy", tr("Select the correct route"), Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        this.temporaryLayer = temporaryLayer;
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MapFrame map = MainApplication.getMap();

        mapView = map.mapView;
        mousePosition = null;

        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        map.mapView.addTemporaryLayer(temporaryLayer);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MapFrame map = MainApplication.getMap();

        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
        map.mapView.removeTemporaryLayer(temporaryLayer);

        temporaryLayer.invalidate();
    }

    @Override
    protected void updateStatusLine() {
        String newModeHelpText = "x : " + mousePosition.getX() + " y : " + mousePosition.getY();
        MapFrame map = MainApplication.getMap();
        map.statusLine.setHelpText(newModeHelpText);
        map.statusLine.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
        updateStatusLine();
        temporaryLayer.invalidate();
    }
}
