package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;

public class ShapePoint implements TripPoint {
    private final LatLon point;
    private final int sequence;
    private final double distTraveled;

    public ShapePoint(LatLon point, int sequence, double distTraveled) {
        this.point = Objects.requireNonNull(point, "point");
        this.sequence = sequence;
        this.distTraveled = distTraveled;
    }

    @Override
    public LatLon getPoint() {
        return point;
    }

    @Override
    public double getDistTraveled() {
        return distTraveled;
    }

    @Override
    public String toString() {
        return "ShapePoint{" +
            "point=" + point +
            ", distTraveled=" + distTraveled +
            '}';
    }
}
