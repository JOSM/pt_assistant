package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;


import java.awt.Color;
import java.util.Objects;

/**
 * Hierarcy:
 * - Route: Top level route. Potentially the same as a route_master in OSM
 * - TripPath: A sequence of stops. Potentially the same as a route (if used often enough)
 * - Trip: The acutal trip. We don't care much about it.
 */
public class Route {

    private final String id;
    private final Agency agency;
    private final String shortName;
    private final String longName;
    private final String desc;
    private final GtfsRouteType type;
    private final Color color;
    private final Color textColor;
    private final int sortOrder;

    public Route(String id, Agency agency, String shortName, String longName,
                 String desc, GtfsRouteType type,
                 Color color, Color textColor, int sortOrder) {
        this.id = Objects.requireNonNull(id, "id");
        this.agency = Objects.requireNonNull(agency, "agency");
        this.shortName = Objects.requireNonNull(shortName, "shortName");
        this.longName = Objects.requireNonNull(longName, "longName");
        this.desc = Objects.requireNonNull(desc, "desc");
        this.type = type;
        this.color = color;
        this.textColor = textColor;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return id;
    }

    public Agency getAgency() {
        return agency;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getDesc() {
        return desc;
    }

    public GtfsRouteType getType() {
        return type;
    }

    public Color getColor() {
        return color;
    }

    public Color getColorSafe() {
        return color == null ? Color.GRAY : color;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getTextColorSafe() {
        return textColor == null ? Color.WHITE : textColor;
    }

    public int getSortOrder() {
        return sortOrder;
    }


    @Override
    public String toString() {
        return "Route{" +
            "id='" + id + '\'' +
            ", agency=" + agency +
            ", shortName='" + shortName + '\'' +
            ", longName='" + longName + '\'' +
            ", desc='" + desc + '\'' +
            ", type=" + type +
            ", color='" + color + '\'' +
            ", textColor='" + textColor + '\'' +
            ", sortOrder=" + sortOrder +
            '}';
    }
}
