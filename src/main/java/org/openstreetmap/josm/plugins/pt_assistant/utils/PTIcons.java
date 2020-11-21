package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.tools.ImageProvider;

public final class PTIcons {
    private PTIcons() {
        // Private constructor to avoid instantiation
    }

    public static final ImageProvider BICYCLE_DESIGNATED = new ImageProvider("presets/vehicle/restriction", "bicycle-designated");
    public static final ImageProvider BICYCLE = new ImageProvider("presets/sport", "cycling");
    public static final ImageProvider BUS = new ImageProvider("bus");
    public static final ImageProvider TROLLEY_BUS = new ImageProvider("presets/transport", "trolleybus");
    public static final ImageProvider PEDESTRIAN = new ImageProvider("presets/vehicle/restriction","foot-designated");
    public static final ImageProvider HORSE = new ImageProvider("presets/leisure","horse_riding");

    public static final ImageProvider STOP_SIGN = new ImageProvider("misc", "error");
    public static final ImageProvider GREEN_CHECKMARK = new ImageProvider("misc", "green_check");
    public static final ImageProvider BUFFER_STOP = new ImageProvider("presets/transport/railway", "buffer_stop");
}
