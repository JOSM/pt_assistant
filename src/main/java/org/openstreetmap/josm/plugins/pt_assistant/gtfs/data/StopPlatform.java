package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

public class StopPlatform extends Stop {
    private final StopStation station;

    public StopPlatform(Stop base, StopStation station) {
        super(base);
        this.station = station; // nullable
    }

    @Override
    public Stop getMostGenericStop() {
        return station == null ? super.getMostGenericStop() : station.getMostGenericStop();
    }
}
