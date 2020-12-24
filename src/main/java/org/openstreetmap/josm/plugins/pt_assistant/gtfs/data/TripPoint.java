package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import org.openstreetmap.josm.data.coor.LatLon;

public interface TripPoint {
    LatLon getPoint();

    double getDistTraveled();
}
