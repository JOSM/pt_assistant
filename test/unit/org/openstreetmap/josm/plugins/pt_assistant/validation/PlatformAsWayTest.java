// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class PlatformAsWayTest {

    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules();

    @Test
    void sortingTest() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.PLATFORM_AS_WAY(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        List<TestError> errors = new ArrayList<>();

        for (Relation r: ds.getRelations()) {
            WayChecker wayChecker = new WayChecker(r, test);
            wayChecker.performDirectionTest();
            wayChecker.performRoadTypeTest();
            errors.addAll(wayChecker.getErrors());
            RouteChecker routeChecker = new RouteChecker(r, test);
            routeChecker.performSortingTest();
            errors.addAll(routeChecker.getErrors());
        }

        assertEquals(errors.size(), 0);
    }
}
