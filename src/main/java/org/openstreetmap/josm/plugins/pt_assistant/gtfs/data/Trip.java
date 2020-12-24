package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

public class Trip {
    private final Route route;
    private final Service service;
    private final String id;
    private final String headsign;
    private final String shortName;
    private final Shape shape;
    private final List<StopTime> stops;
    private final Bounds bounds;

    public Trip(Route route, Service service, String id, String headsign, String shortName, Shape shape,
                List<StopTime> stops) {
        this.route = Objects.requireNonNull(route, "route");
        this.service = Objects.requireNonNull(service, "service");
        this.id = Objects.requireNonNull(id, "id");
        this.headsign = Objects.requireNonNull(headsign, "headsign");
        this.shortName = Objects.requireNonNull(shortName, "shortName");
        this.shape = shape;
        this.stops = Objects.requireNonNull(stops, "stops");

        List<LatLon> points = getPoints();
        bounds = new Bounds(points.get(0));
        points.forEach(bounds::extend);
    }

    public Route getRoute() {
        return route;
    }

    public Service getService() {
        return service;
    }

    public String getId() {
        return id;
    }

    public String getHeadsign() {
        return headsign;
    }

    public String getShortName() {
        return shortName;
    }

    public Shape getShape() {
        return shape;
    }

    public List<StopTime> getStops() {
        return stops;
    }

    public List<LatLon> getPoints() {
        return Stream.concat(
            shape == null ? Stream.<ShapePoint>empty() : shape.getPoints().stream(),
            stops.stream()
        ).sorted(Comparator.comparing(TripPoint::getDistTraveled))
            .map(TripPoint::getPoint)
            .collect(Collectors.toList());
    }

    public List<Stop> getStopsWithoutTime() {
        return stops.stream().map(StopTime::getStop).collect(Collectors.toList());
    }

    public Bounds getBounds() {
        return bounds;
    }
}
