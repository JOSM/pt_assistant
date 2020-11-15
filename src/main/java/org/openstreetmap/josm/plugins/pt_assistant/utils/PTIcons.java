package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.tools.ImageProvider;

public final class PTIcons {
    private PTIcons() {
        // Private constructor to avoid instantiation
    }

    public static final ImageProvider BUS = new ImageProvider("bus");

    public static final ImageProvider STOP_SIGN = new ImageProvider("misc", "error");
    public static final ImageProvider GREEN_CHECKMARK = new ImageProvider("misc", "green_check");
    public static final ImageProvider BUFFER_STOP = new ImageProvider("presets/transport/railway", "buffer_stop");
}
