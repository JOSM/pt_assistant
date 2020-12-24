package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Service {
    private final String id;
    private final Set<DayOfWeek> days;
    private final LocalDate from;
    private final LocalDate to;
    private final Map<LocalDate, ServiceSpecialType> specials;

    public Service(String id, Set<DayOfWeek> days, LocalDate from, LocalDate to, List<ServiceSpecial> specials) {
        this.id = id;
        this.days = days;
        this.from = from;
        this.to = to;
        this.specials = specials.stream()
                .collect(Collectors.toMap(ServiceSpecial::getDate, ServiceSpecial::getType));
    }

    public String getId() {
        return id;
    }

    public Set<DayOfWeek> getDays() {
        return days;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "Service{" +
            "id='" + id + '\'' +
            ", days=" + days +
            ", from=" + from +
            ", to=" + to +
            ", specials=" + specials +
            '}';
    }

    public long countOperatingDays(LocalDate fromIncluding, LocalDate toIncluding) {
        // TODO: Java 11  fromIncluding.datesUntil()
        return Stream.iterate(fromIncluding, it -> it.plus(1, ChronoUnit.DAYS))
            .limit(fromIncluding.until(toIncluding, ChronoUnit.DAYS))
            .filter(this::operates)
            .count();
    }

    private boolean operates(LocalDate day) {
        ServiceSpecialType special = specials.get(day);
        if (special != null) {
            return special == ServiceSpecialType.ADDED;
        }
        return !day.isBefore(from) && !day.isAfter(to) && days.contains(day.getDayOfWeek());
    }
}
