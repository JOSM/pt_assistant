package org.openstreetmap.josm.plugins.pt_assistant.gtfs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Route;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Trip;

/**
 * Our own cache of some trips that share the same stops and thus belong to the same line
 */
public class MergedTrip {

    private final List<Trip> trips;
    private final long servicesNextWeek;
    private final Route route;

    public MergedTrip(List<Trip> trips) {
        this.trips = trips;
        if (trips.isEmpty()) {
            throw new IllegalArgumentException("trips cannot be empty");
        }
        this.route = trips.get(0).getRoute();
        for (Trip trip : trips) {
            if (trip.getRoute() != this.route) {
                throw new IllegalArgumentException("Expected trip " + trip + " to have route " + this.route);
            }
        }

        LocalDate now = LocalDate.now();
        LocalDate oneWeek = now.plus(7, ChronoUnit.DAYS);
        servicesNextWeek = trips
            .stream()
            .mapToLong(trip -> trip.getService().countOperatingDays(now, oneWeek))
            .sum();
    }

    public long getServicesNextWeek() {
        return servicesNextWeek;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public Trip getSampleTrip() {
        return trips.get(0);
    }

    public Bounds getBounds() {
        return getSampleTrip().getBounds();
    }

    public Route getRoute() {
        return route;
    }
}
