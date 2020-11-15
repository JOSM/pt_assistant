// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import java.io.InputStream;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

public final class TestFiles {

    public static InputStream DL131_BEFORE() { return TestFiles.class.getResourceAsStream("/DL131_before.osm"); }
    public static InputStream DL131_AFTER() { return TestFiles.class.getResourceAsStream("/DL131_after.osm"); }

    public static InputStream DL4_BEFORE() { return TestFiles.class.getResourceAsStream("/DL4_before.osm"); }
    public static InputStream DL4_AFTER() { return TestFiles.class.getResourceAsStream("/DL4_after.osm"); }

    public static InputStream DL49_BEFORE() { return TestFiles.class.getResourceAsStream("/DL49_before.osm"); } // has wrong way sorting
    public static InputStream DL49_AFTER() { return TestFiles.class.getResourceAsStream("/DL49_after.osm"); }

    public static InputStream DL60_BEFORE() { return TestFiles.class.getResourceAsStream("/DL60_before.osm"); }
    public static InputStream DL60_AFTER() { return TestFiles.class.getResourceAsStream("/DL60_after.osm"); }

    public static InputStream DL94_BEFORE() { return TestFiles.class.getResourceAsStream("/DL94_before.osm"); }
    public static InputStream DL94_AFTER() { return TestFiles.class.getResourceAsStream("/DL94_after.osm"); }

    public static InputStream DL286_BEFORE() { return TestFiles.class.getResourceAsStream("/DL286_before.osm"); }
    public static InputStream DL286_AFTER() { return TestFiles.class.getResourceAsStream("/DL286_after.osm"); }

    public static InputStream TEC366_BEFORE() { return TestFiles.class.getResourceAsStream("/TL366_before.osm"); }
    public static InputStream TEC366_AFTER() { return TestFiles.class.getResourceAsStream("/TL366_after.osm"); }

    public static InputStream PLATFORM_AS_WAY() { return TestFiles.class.getResourceAsStream("/route-with-platform-as-way.osm"); }

    public static InputStream ROUNDABOUT() { return TestFiles.class.getResourceAsStream("/roundabout.osm"); }
    public static InputStream ROUNDABOUT_ONEWAY() { return TestFiles.class.getResourceAsStream("/duesseldorf_roundabout.osm"); }

    public static InputStream ROUNDABOUT1_BEFORE() { return TestFiles.class.getResourceAsStream("/rdb1_before.osm"); }
    public static InputStream ROUNDABOUT1_AFTER() { return TestFiles.class.getResourceAsStream("/rdb1_fixed.osm"); }

    public static InputStream SORT_PT_STOPS() { return TestFiles.class.getResourceAsStream("/sort_test.osm"); }
    public static InputStream SORT_PT_STOPS_WITH_REPEATED_STOPS() { return TestFiles.class.getResourceAsStream("/sort_test2.osm"); }

    public static InputStream ROAD_TYPE_ERROR() { return TestFiles.class.getResourceAsStream("/road-type.osm"); }

    public static InputStream ONEWAY_BAD_MEMBER_SORTING() { return TestFiles.class.getResourceAsStream("/oneway-bad-member-sorting.osm"); }

    public static InputStream ONEWAY_WRONG_DIRECTION() { return TestFiles.class.getResourceAsStream("/oneway-wrong-direction.osm"); }
    public static InputStream ONEWAY_WRONG_DIRECTION2() { return TestFiles.class.getResourceAsStream("/oneway-wrong-direction2.osm"); }

    public static InputStream SOLITARY_STOP_POSITION() { return TestFiles.class.getResourceAsStream("/solitary-stop-position.osm"); }

    public static InputStream STOP_AREA_MEMBERS() { return TestFiles.class.getResourceAsStream("/stop-area-members.osm"); }
    public static InputStream STOP_AREA_RELATIONS() { return TestFiles.class.getResourceAsStream("/stop-area-relations.osm"); }
    public static InputStream STOP_AREA_NO_STOPS() { return TestFiles.class.getResourceAsStream("/stop-area-no-stops.osm"); }
    public static InputStream STOP_AREA_MANY_STOPS() { return TestFiles.class.getResourceAsStream("/stop-area-many-stops.osm"); }
    public static InputStream STOP_AREA_NO_PLATFORMS() { return TestFiles.class.getResourceAsStream("/stop-area-no-platform.osm"); }
    public static InputStream STOP_AREA_MANY_PLATFORMS() { return TestFiles.class.getResourceAsStream("/stop-area-many-platforms.osm"); }

    public static InputStream SEGMENT_TEST() { return TestFiles.class.getResourceAsStream("/segment-test.osm"); }

    public static InputStream BICYCLE_LOOP_BACK_TEST() { return TestFiles.class.getResourceAsStream("/map4.osm"); }

    public static DataSet importOsmFile(final InputStream fileContent, String layerName) {
        try {
            return new OsmImporter().loadLayer(fileContent, null, layerName, NullProgressMonitor.INSTANCE).getLayer().getDataSet();
        } catch (IllegalDataException e) {
            throw new RuntimeException(e);
        }
    }
}
