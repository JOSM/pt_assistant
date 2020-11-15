package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.awt.Color;

public final class ColorPalette {
    private ColorPalette() {
        // Private constructor to avoid instantiation
    }

    public static final Color GREEN = new Color(0, 255, 0, 180);
    public static final Color RED = new Color(255, 0, 0, 180);
    public static final Color BLUE = new Color(0, 0, 255, 180);
    public static final Color YELLOW = new Color(255, 255, 0, 180);
    public static final Color CYAN = new Color(0, 255, 255, 180);

    public static final Color ORANGE = new Color(255, 200, 0, 180);
    public static final Color PINK = new Color(255, 175, 175, 180);
    public static final Color WHITE = new Color(255, 255, 255, 180);

    public static Color[] FIVE_COLORS = { GREEN, RED, BLUE, YELLOW, CYAN };
}
