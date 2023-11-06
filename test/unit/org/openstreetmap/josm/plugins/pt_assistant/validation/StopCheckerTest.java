// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;

class StopCheckerTest {
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
        assertEquals(1, nodeChecker.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_NOT_PART_OF_STOP_AREA,
            nodeChecker.getErrors().get(0).getCode());
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
        assertEquals(1, stopChecker.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_COMPARE_RELATIONS,
            stopChecker.getErrors().get(0).getCode());
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
        assertEquals(1, stopChecker.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_STOPS,
            stopChecker.getErrors().get(0).getCode());
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
        assertEquals(1, stopChecker.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_PLATFORM,
            stopChecker.getErrors().get(0).getCode());

    }
}
