package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Route;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Service;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Shape;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopTime;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Trip;

public class TripFileReader extends AbstractGtfsFileReader {
    @Override
    protected String getFileName() {
        return "trips.txt";
    }

    private List<Trip> getTrips(Map<String, Route> routes, Map<String, Service> services,
                                Map<String, Shape> shapes, Map<String, List<StopTime>> stopTimesForTrip) {
        FieldAccessor route = getRequiredAccessor("route_id");
        FieldAccessor service = getRequiredAccessor("service_id");
        FieldAccessor trip = getRequiredAccessor("trip_id");
        FieldAccessor headsign = getOptionalAccessor("trip_headsign", "");
        FieldAccessor shortName = getOptionalAccessor("trip_short_name", "");
        FieldAccessor shape = getOptionalAccessor("trip_short_name", "");

        return streamLinesAs(lineAccess -> {
            String myTrip = lineAccess.get(trip);
            Service myService = services.get(lineAccess.get(service));
            Route myRoute = routes.get(lineAccess.get(route));
            Shape myShape = shapes.get(lineAccess.get(shape));
            List<StopTime> stopTimes = stopTimesForTrip.get(myTrip);

            if (stopTimes == null) {
                throw new IllegalArgumentException("No stop times provided for " + myTrip);
            }
            if (myService == null) {
                throw new IllegalArgumentException("Service not found for " + myTrip
                    + " (service id: " + lineAccess.get(service) + ")");
            }
            if (myRoute == null) {
                throw new IllegalArgumentException("Route not found for " + myTrip
                    + " (route id: " + lineAccess.get(route) + ")");
            }

            return new Trip(
                myRoute,
                myService,
                myTrip,
                lineAccess.get(headsign),
                lineAccess.get(shortName),
                myShape,
                stopTimes
            );
        }).collect(Collectors.toList());
    }

    public static List<Trip> read(FileSystem zip,
                                  Map<String, Route> routes, Map<String, Service> services,
                                  Map<String, Shape> shapes, Map<String, List<StopTime>> stopTimesForTrip) throws IOException {
        TripFileReader tripFileReader = new TripFileReader();
        tripFileReader.readFrom(zip);
        return tripFileReader.getTrips(routes, services, shapes, stopTimesForTrip);
    }
}
