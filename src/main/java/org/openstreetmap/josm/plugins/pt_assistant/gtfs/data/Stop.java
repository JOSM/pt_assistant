package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;

public class Stop {

    private final String id;
    private final String name;
    private final String desc;
    private final LatLon latLon;

    public Stop(Stop copyFrom) {
        this(copyFrom.id, copyFrom.name, copyFrom.desc, copyFrom.latLon);
    }

    public Stop(String id, String name, String desc, LatLon latLon) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.desc = Objects.requireNonNull(desc, "desc");
        this.latLon = Objects.requireNonNull(latLon, "latLon");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public LatLon getLatLon() {
        return latLon;
    }

    public Stop getMostGenericStop() {
        return this;
    }

    @Override
    public String toString() {
        return "Stop{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", desc='" + desc + '\'' +
            ", latLon=" + latLon +
            '}';
    }
}
