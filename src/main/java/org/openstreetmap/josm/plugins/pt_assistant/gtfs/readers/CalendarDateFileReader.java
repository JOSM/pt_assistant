package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.ServiceSpecial;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.ServiceSpecialType;
import org.openstreetmap.josm.tools.Pair;

public class CalendarDateFileReader extends AbstractGtfsFileReader {

    @Override
    protected String getFileName() {
        return "calendar_dates.txt";
    }
    private Map<String, List<ServiceSpecial>> getDates() {
        FieldAccessor service = getRequiredAccessor("service_id");
        FieldAccessor date = getRequiredAccessor("date");
        FieldAccessor type = getRequiredAccessor("exception_type");

        return streamLinesAs(lineAccess -> new Pair<>(lineAccess.get(service),
            new ServiceSpecial(
                AbstractGtfsFileReader.parseDate(lineAccess.get(date)),
                ServiceSpecialType.getByIdentifier(lineAccess.get(type)))
        ))
            .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toList())));
    }

    public static Map<String, List<ServiceSpecial>> read(FileSystem zip) throws IOException {
        CalendarDateFileReader reader = new CalendarDateFileReader();
        if (reader.isFilePresent(zip)) {
            reader.readFrom(zip);
            return reader.getDates();
        } else {
            return Collections.emptyMap();
        }
    }
}
