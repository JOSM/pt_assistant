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

class SortingTestTest {

    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules();

    @Test
    void testSortingBeforeFile() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.DL131_BEFORE(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        List<TestError> errors = new ArrayList<>();

        for (Relation r: ds.getRelations()) {
            RouteChecker routeChecker = new RouteChecker(r, test);
            routeChecker.performSortingTest();
            errors.addAll(routeChecker.getErrors());

        }

        assertEquals(errors.size(), 1);
        assertEquals(errors.iterator().next().getCode(), PTAssistantValidatorTest.ERROR_CODE_SORTING);
        assertEquals(errors.iterator().next().getTester().getClass().getName(), PTAssistantValidatorTest.class.getName());
    }

    @Test
    void testSortingAfterFile() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.DL131_AFTER(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        List<TestError> errors = new ArrayList<>();

        for (Relation r: ds.getRelations()) {
            RouteChecker routeChecker = new RouteChecker(r, test);
            routeChecker.performSortingTest();
            errors.addAll(routeChecker.getErrors());

        }


        assertEquals(errors.size(), 0);
    }

    // TODO: this test will only pass after the functionality for recognizing
    // and closing the gap is implemented.
//    @Test
//    public void overshootTestBeforeFile() {
//        File file = new File(AbstractTest.PATH_TO_DL286_BEFORE);
//        DataSet ds = ImportUtils.importOsmFile(file, "testLayer");
//
//        GapTest gapTest = new GapTest();
//        for (Relation r : ds.getRelations()) {
//            gapTest.visit(r);
//        }
//
//        List<TestError> errors = gapTest.getErrors();
//
//        assertEquals(errors.size(), 1);
//        assertEquals(errors.get(0).getCode(), GapTest.ERROR_CODE_OVERSHOOT);
//
//    }

    @Test
    void testOvershootAfterFile() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.DL286_AFTER(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        List<TestError> errors = new ArrayList<>();

        for (Relation r: ds.getRelations()) {
            RouteChecker routeChecker = new RouteChecker(r, test);
            routeChecker.performSortingTest();
            errors.addAll(routeChecker.getErrors());
        }

        assertEquals(errors.size(), 0);
    }
}
