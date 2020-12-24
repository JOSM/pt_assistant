package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Shape;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.ShapePoint;
import org.openstreetmap.josm.tools.Pair;

public class ShapeFileReader extends AbstractGtfsFileReader {
    @Override
    protected String getFileName() {
        return "shapes.txt";
    }

    private Map<String, Shape> getShapes() {
        FieldAccessor shape = getRequiredAccessor("shape_id");
        FieldAccessor lat = getRequiredAccessor("shape_pt_lat");
        FieldAccessor lon = getRequiredAccessor("shape_pt_lon");
        FieldAccessor sequence = getRequiredAccessor("shape_pt_sequence");
        FieldAccessor distance = getOptionalAccessor("shape_dist_traveled", "0");

        return streamLinesAs(lineAccess -> {
            ShapePoint point = new ShapePoint(
                new LatLon(Double.parseDouble(lineAccess.get(lat)), Double.parseDouble(lineAccess.get(lon))),
                Integer.parseInt(lineAccess.get(sequence)),
                Double.parseDouble(lineAccess.get(distance))
            );
            return new Pair<>(lineAccess.get(shape), point);
        }).collect(Collectors.groupingBy(it -> it.a, Collectors.collectingAndThen(Collectors.mapping(it -> it.b, Collectors.toList()), Shape::new)));
    }

    public static Map<String, Shape> read(FileSystem zip) throws IOException {
        ShapeFileReader reader = new ShapeFileReader();
        if (reader.isFilePresent(zip)) {
            reader.readFrom(zip);
            return reader.getShapes();
        } else {
            return Collections.emptyMap();
        }
    }
}
