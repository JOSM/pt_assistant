package org.openstreetmap.josm.plugins.pt_assistant.gtfs.readers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.Service;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.ServiceSpecial;

public class CalendarFileReader extends AbstractGtfsFileReader {

    @Override
    protected String getFileName() {
        return "calendar.txt";
    }

    private Map<String, Service> getServices(Map<String, List<ServiceSpecial>> specialServices) {
        FieldAccessor service = getRequiredAccessor("service_id");
        FieldAccessor start = getRequiredAccessor("start_date");
        FieldAccessor end = getRequiredAccessor("end_date");
        Map<DayOfWeek, FieldAccessor> days = new HashMap<>();
        days.put(DayOfWeek.MONDAY, getRequiredAccessor("monday"));
        days.put(DayOfWeek.TUESDAY, getRequiredAccessor("tuesday"));
        days.put(DayOfWeek.WEDNESDAY, getRequiredAccessor("wednesday"));
        days.put(DayOfWeek.THURSDAY, getRequiredAccessor("thursday"));
        days.put(DayOfWeek.FRIDAY, getRequiredAccessor("friday"));
        days.put(DayOfWeek.SATURDAY, getRequiredAccessor("saturday"));
        days.put(DayOfWeek.SUNDAY, getRequiredAccessor("sunday"));

        Map<String, Service> inCalendar = streamLinesAs(lineAccess -> {
            String id = lineAccess.get(service);
            return new Service(
                id,
                Stream.of(DayOfWeek.values())
                    .filter(day -> lineAccess.get(days.get(day)).equals("1"))
                    .collect(Collectors.toSet()),
                AbstractGtfsFileReader.parseDate(lineAccess.get(start)),
                AbstractGtfsFileReader.parseDate(lineAccess.get(end)),
                specialServices.getOrDefault(id, Collections.emptyList())
            );
        }).collect(Collectors.toMap(Service::getId, Function.identity()));
        addMissingSpecials(inCalendar, specialServices);
        return inCalendar;
    }

    public static Map<String, Service> read(FileSystem zip, Map<String, List<ServiceSpecial>> specialServices) throws IOException {
        CalendarFileReader reader = new CalendarFileReader();
        if (reader.isFilePresent(zip)) {
            reader.readFrom(zip);
            return reader.getServices(specialServices);
        } else {
            HashMap<String, Service> map = new HashMap<>();
            addMissingSpecials(map, specialServices);
            return map;
        }
    }

    private static void addMissingSpecials(Map<String, Service> services, Map<String, List<ServiceSpecial>> specialServices) {
        specialServices.forEach((id, specials) -> {
            if (!services.containsKey(id)) {
                services.put(id, new Service(id, EnumSet.noneOf(DayOfWeek.class),
                    LocalDate.of(2000, 1, 2), LocalDate.of(2000, 1, 1),
                    specials));
            }
        });
    }
}
