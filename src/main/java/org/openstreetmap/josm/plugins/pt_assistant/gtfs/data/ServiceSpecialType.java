package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

public enum ServiceSpecialType {
    ADDED("1"),
    REMOVED("2");

    private final String identifier;

    ServiceSpecialType(String identifier) {
        this.identifier = identifier;
    }

    public static ServiceSpecialType getByIdentifier(String identifier) {
        for(ServiceSpecialType value: values()) {
            if (value.identifier.equals(identifier)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Not a valid service type: " + identifier);
    }
}
