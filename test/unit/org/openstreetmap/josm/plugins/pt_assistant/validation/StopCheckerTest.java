// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class StopCheckerTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void nodePartOfStopAreaTest() {

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
        Assert.assertEquals(nodeChecker.getErrors().size(), 1);
        Assert.assertEquals(nodeChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_NOT_PART_OF_STOP_AREA);
    }

    @Test
    public void stopAreaRelationsTest() {

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
        Assert.assertEquals(stopChecker.getErrors().size(), 1);
        Assert.assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_COMPARE_RELATIONS);
    }

    @Test
    public void stopAreaStopPositionTest() {

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
        Assert.assertEquals(stopChecker.getErrors().size(), 1);
        Assert.assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_STOPS);
    }

    @Test
    public void stopAreaPlatformTest() {

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
        Assert.assertEquals(stopChecker.getErrors().size(), 1);
        Assert.assertEquals(stopChecker.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_STOP_AREA_NO_PLATFORM);

    }
}
