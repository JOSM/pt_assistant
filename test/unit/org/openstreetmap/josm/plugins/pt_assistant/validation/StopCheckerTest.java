// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

class StopCheckerTest {

    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules();

    @Test
    void testNodePartOfStopArea() {

        // check if stop positions or platforms are in any stop_area relation:

        DataSet ds = TestFiles.importOsmFile(TestFiles.STOP_AREA_MEMBERS(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        Node node = null;

        for (Node n : ds.getNodes()) {
            if (n.hasTag("public_transport", "stop_position") | n.hasTag("public_transport", "platform")) {
                node = n;
            }
        }

        NodeChecker nodeChecker = new NodeChecker(node, test);
        nodeChecker.performNodePartOfStopAreaTest();
        assertEquals(nodeChecker.getErrors().size(), 1);
        assertEquals(nodeChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_NOT_PART_OF_STOP_AREA);
    }

    @Test
    void testStopAreaRelations() {

        // Check if stop positions belong the same routes as related platform(s)

        DataSet ds = TestFiles.importOsmFile(TestFiles.STOP_AREA_RELATIONS(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        Relation stopArea = null;

        for (Relation r : ds.getRelations()) {
            if (r.hasTag("public_transport", "stop_area")) {
                stopArea = r;
            }
        }

        StopChecker stopChecker = new StopChecker(stopArea, test);
        stopChecker.performStopAreaRelationsTest();
        assertEquals(stopChecker.getErrors().size(), 1);
        assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_COMPARE_RELATIONS);
    }

    @Test
    void testStopAreaStopPosition() {

        // Check if stop area relation has at least one stop position.
        DataSet ds = TestFiles.importOsmFile(TestFiles.STOP_AREA_NO_STOPS(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        Relation stopArea = null;

        for (Relation r : ds.getRelations()) {
            if (r.hasTag("public_transport", "stop_area")) {
                stopArea = r;
            }
        }

        StopChecker stopChecker = new StopChecker(stopArea, test);
        stopChecker.performStopAreaStopPositionTest();
        assertEquals(stopChecker.getErrors().size(), 1);
        assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_STOPS);
    }

    @Test
    void testStopAreaPlatform() {

        // Check if stop area relation has at least one platform.
        DataSet ds = TestFiles.importOsmFile(TestFiles.STOP_AREA_NO_PLATFORMS(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        Relation stopArea = null;

        for (Relation r : ds.getRelations()) {
            if (r.hasTag("public_transport", "stop_area")) {
                stopArea = r;
            }
        }

        StopChecker stopChecker = new StopChecker(stopArea, test);
        stopChecker.performStopAreaPlatformTest();
        assertEquals(stopChecker.getErrors().size(), 1);
        assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_PLATFORM);

    }
}
