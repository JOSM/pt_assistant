package org.openstreetmap.josm.plugins.pt_assistant.gtfs.data;

import java.util.Objects;

public class Agency {
    private final String id;
    private final String name;
    private final String url;

    /**
     * THe Agency
     * @param id ID to reference in other tables. Required but may be empty string
     * @param name required
     * @param url required
     */
    public Agency(String id, String name, String url) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.url = Objects.requireNonNull(url, "url");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }


    @Override
    public String toString() {
        return "Agency{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}
