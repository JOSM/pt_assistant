package org.openstreetmap.josm.plugins.pt_assistant.gtfs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Agency;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Route;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Service;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.ServiceSpecial;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Shape;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Stop;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopTime;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Trip;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.AgencyFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.CalendarDateFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.CalendarFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.RouteFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.ShapeFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.StopFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.StopTimeFileReader;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers.TripFileReader;
import org.openstreetmap.josm.tools.Pair;

public class GtfsFile {

    private final String fileName;
    private final Map<String, Agency> agencies;
    private final Map<String, Route> routes;
    private final Map<String, Stop> stops;
    private final List<Trip> trips;
    private final Collection<MergedTrip> mergedTrips;

    /* package */ GtfsFile(String fileName, FileSystem zip) throws IOException {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.agencies = AgencyFileReader.read(zip);
        this.routes = RouteFileReader.read(zip, agencies);
        this.stops = StopFileReader.read(zip);
        Map<String, List<ServiceSpecial>> specialServices = CalendarDateFileReader.read(zip);
        Map<String, Service> calendar = CalendarFileReader.read(zip, specialServices);
        Map<String, List<StopTime>> stopsForTrips = StopTimeFileReader.read(zip, stops);
        Map<String, Shape> shapes = ShapeFileReader.read(zip);

        this.trips = TripFileReader.read(zip, routes, calendar, shapes, stopsForTrips);
        mergedTrips = trips
            .stream()
            .collect(Collectors.groupingBy(
                trip -> new Pair<>(trip.getRoute(), trip.getStopsWithoutTime()),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    MergedTrip::new
                    )
            )).values();
    }

    public Map<String, Stop> getStops() {
        return Collections.unmodifiableMap(stops);
    }

    public String getFileName() {
        return fileName;
    }

    public Collection<MergedTrip> getMergedTrips() {
        return mergedTrips;
    }

    @Override
    public String toString() {
        return "GtfsFile{" +
            "agencies=" + agencies.size() +
            ", routes=" + routes.size() +
            ", stops=" + stops.size() +
            '}';
    }
}
