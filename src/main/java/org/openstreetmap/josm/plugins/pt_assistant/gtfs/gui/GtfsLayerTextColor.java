package org.openstreetmap.josm.plugins.pt_assistant.gtfs.gui;

import java.awt.Color;

public enum GtfsLayerTextColor {
    NONE(null),
    BLACK(Color.BLACK),
    WHITE(Color.WHITE),
    RED(Color.RED),
    BLUE(Color.BLUE),
    GREEN(Color.GREEN);

    private final Color color;

    GtfsLayerTextColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
