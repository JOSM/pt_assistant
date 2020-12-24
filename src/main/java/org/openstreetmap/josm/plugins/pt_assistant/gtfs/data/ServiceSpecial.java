package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.time.LocalDate;

public class ServiceSpecial {
    private final LocalDate date;
    private final ServiceSpecialType type;

    public ServiceSpecial(LocalDate date, ServiceSpecialType type) {
        this.date = date;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public ServiceSpecialType getType() {
        return type;
    }


    @Override
    public String toString() {
        return "ServiceSpecial{" +
            "date=" + date +
            ", type=" + type +
            '}';
    }
}
