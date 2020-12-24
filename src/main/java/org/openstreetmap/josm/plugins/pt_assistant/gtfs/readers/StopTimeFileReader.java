package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Stop;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopTime;
import org.openstreetmap.josm.tools.Pair;

public class StopTimeFileReader extends AbstractGtfsFileReader {
    @Override
    protected String getFileName() {
        return "stop_times.txt";
    }

    private Map<String, List<StopTime>> getStopsByTrip(Map<String, Stop> stops) {
        FieldAccessor trip = getRequiredAccessor("trip_id");
        FieldAccessor stop = getRequiredAccessor("stop_id");
        FieldAccessor distTraveled = getOptionalAccessor("shape_dist_traveled", "0");
        FieldAccessor sequence = getRequiredAccessor("stop_sequence");

        return streamLinesAs(lineAccess -> {
            Stop myStop = stops.get(lineAccess.get(stop));
            if (myStop == null) {
                throw new IllegalArgumentException("Unknown stop: " + lineAccess.get(stop));
            }
            StopTime time = new StopTime(
                myStop,
                Double.parseDouble(lineAccess.get(distTraveled)),
                Integer.parseInt(lineAccess.get(sequence))
            );
            return new Pair<>(lineAccess.get(trip), time);
        })
            .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b,
                Collectors.collectingAndThen(Collectors.toList(),
                    // Sort after collect => much more performant.
                    list -> { list.sort(Comparator.comparing(StopTime::getSequence)); return list; } ))));
    }

    /**
     * Read the stop times
     * @return The stop times by trip_id
     * @param zip ZIP file
     * @param stops The stops that were already read
     */
    public static Map<String, List<StopTime>> read(FileSystem zip, Map<String, Stop> stops) throws IOException {
        StopTimeFileReader reader = new StopTimeFileReader();
        reader.readFrom(zip);
        return reader.getStopsByTrip(stops);
    }

}
