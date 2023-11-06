// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;

class RoadTypeTestTest {
    @Test
    void testRoadTypeErrors() {

        DataSet ds = TestFiles.importOsmFile(TestFiles.ROAD_TYPE_ERROR(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        List<TestError> errors = new ArrayList<>();

        for (Relation r : ds.getRelations()) {
            WayChecker wayChecker = new WayChecker(r, test);
            wayChecker.performRoadTypeTest();
            errors.addAll(wayChecker.getErrors());
        }

        assertEquals(2, errors.size());

        for (TestError e : errors) {
            assertEquals(PTAssistantValidatorTest.ERROR_CODE_ROAD_TYPE, e.getCode());
            Way way = (Way) e.getHighlighted().iterator().next();
            assertTrue(way.getId() == 8169083 || way.getId() == 8034569);
        }
    }
}
