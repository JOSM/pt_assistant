package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import org.openstreetmap.josm.data.coor.LatLon;

public class StopTime implements TripPoint {
    private final Stop stop;
    private final double distTraveled;
    private final int sequence;

    public StopTime(Stop stop, double distTraveled, int sequence) {
        this.stop = stop;
        this.distTraveled = distTraveled;
        this.sequence = sequence;
    }

    public Stop getStop() {
        return stop;
    }

    @Override
    public LatLon getPoint() {
        return stop.getLatLon();
    }

    @Override
    public double getDistTraveled() {
        return distTraveled;
    }


    @Override
    public String toString() {
        return "StopTime{" +
            "stop=" + stop +
            ", distTraveled=" + distTraveled +
            '}';
    }

    public int getSequence() {
        return sequence;
    }
}
