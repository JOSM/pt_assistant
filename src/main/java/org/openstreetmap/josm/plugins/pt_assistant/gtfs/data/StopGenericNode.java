package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Objects;

public class StopGenericNode extends Stop {
    private final StopStation station;

    public StopGenericNode(Stop base, StopStation station) {
        super(base);
        this.station = Objects.requireNonNull(station, "station");
    }

    @Override
    public Stop getMostGenericStop() {
        return station.getMostGenericStop();
    }

    public StopStation getStation() {
        return station;
    }
}
