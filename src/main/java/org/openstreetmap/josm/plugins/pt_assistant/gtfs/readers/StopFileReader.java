package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Stop;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopBoardingArea;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopGenericNode;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopPlatform;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopStation;

public class StopFileReader extends AbstractGtfsFileReader {
    @Override
    protected String getFileName() {
        return "stops.txt";
    }


    public Map<String, Stop> getAll() {
        FieldAccessor id = getRequiredAccessor("stop_id");
        FieldAccessor name = getRequiredAccessor("stop_name");
        FieldAccessor desc = getOptionalAccessor("stop_desc", "");
        FieldAccessor lat = getRequiredAccessor("stop_lat");
        FieldAccessor lon = getRequiredAccessor("stop_lon");
        FieldAccessor type = getOptionalAccessor("location_type", "0");
        FieldAccessor parentId = getOptionalAccessor("parent_station", "");

        Function<LineAccess, Stop> base = lineAccess -> new Stop(
            lineAccess.get(id),
            lineAccess.get(name),
            lineAccess.get(desc),
            new LatLon(
                Double.parseDouble(lineAccess.get(lat)),
                Double.parseDouble(lineAccess.get(lon))
            ));

        Map<String, StopStation> stations = streamLinesAs(lineAccess -> lineAccess.get(type).equals("1"),
            lineAccess -> new StopStation(
                base.apply(lineAccess)
            )).collect(Collectors.toConcurrentMap(Stop::getId, Function.identity()));

        Map<String, StopPlatform> platforms = streamLinesAs(lineAccess -> lineAccess.get(type).equals("0"),
            lineAccess -> new StopPlatform(
                base.apply(lineAccess),
                stations.get(lineAccess.get(parentId))
            )).collect(Collectors.toConcurrentMap(Stop::getId, Function.identity()));

        Map<String, Stop> all = Collections.synchronizedMap(new HashMap<>(stations));
        all.putAll(platforms);
        // Entrance/exit or Generic Node
        streamLinesAs(lineAccess -> lineAccess.get(type).equals("2") || lineAccess.get(type).equals("3"),
            lineAccess -> new StopGenericNode(
                base.apply(lineAccess),
                stations.get(lineAccess.get(parentId))
            )).forEach(stop -> all.put(stop.getId(), stop));

        // Boarding Area
        streamLinesAs(lineAccess -> lineAccess.get(type).equals("4"),
            lineAccess -> new StopBoardingArea(
                base.apply(lineAccess),
                platforms.get(lineAccess.get(parentId))
            )).forEach(stop -> all.put(stop.getId(), stop));

        return all;
    }

    public static Map<String, Stop> read(FileSystem zip) throws IOException {
        StopFileReader stop = new StopFileReader();
        stop.readFrom(zip);
        return stop.getAll();
    }
}
