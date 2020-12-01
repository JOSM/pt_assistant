package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class LineRefKeyPlatforms implements LineRefKey {
    private final List<OsmPrimitive> platforms;

    public LineRefKeyPlatforms(List<OsmPrimitive> platforms) {
        this.platforms = platforms;
    }

    @Override
    public String getRef() {
        return tr("- multiple - ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineRefKeyPlatforms that = (LineRefKeyPlatforms) o;
        return Objects.equals(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }


    @Override
    public String toString() {
        return "LineRefKeyPlatforms{" +
            "platforms=" + platforms +
            '}';
    }
}
