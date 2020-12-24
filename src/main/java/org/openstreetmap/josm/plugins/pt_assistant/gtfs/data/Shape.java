package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Shape {
    private final List<ShapePoint> points;

    public Shape(List<ShapePoint> points) {
        this.points = points.stream()
            .sorted(Comparator.comparing(ShapePoint::getDistTraveled))
            .collect(Collectors.toList());

        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot create shape with empty points");
        }
    }

    public List<ShapePoint> getPoints() {
        return points;
    }


    @Override
    public String toString() {
        return "Shape{" +
            "points=" + points.size() +
            '}';
    }
}
