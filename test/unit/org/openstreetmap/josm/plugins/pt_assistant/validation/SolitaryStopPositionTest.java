// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

class SolitaryStopPositionTest {

    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules();

    @Test
    void testSolitaryStopPosition() {

        DataSet ds = TestFiles.importOsmFile(TestFiles.SOLITARY_STOP_POSITION(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        Node platform = null;
        Node stopPosition = null;
        for (Node n : ds.getNodes()) {
            if (n.hasTag("public_transport", "stop_position")) {
                stopPosition = n;
            }
            if (n.hasTag("public_transport", "platform")) {
                platform = n;
            }
        }

        NodeChecker checkerPlatform = new NodeChecker(platform, test);
        checkerPlatform.performPlatformPartOfWayTest();
        assertEquals(checkerPlatform.getErrors().size(), 1);
        assertEquals(checkerPlatform.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_PLATFORM_PART_OF_HIGHWAY);

        NodeChecker checkerStopPosition = new NodeChecker(stopPosition, test);
        checkerStopPosition.performSolitaryStopPositionTest();
        assertEquals(checkerStopPosition.getErrors().size(), 1);
        assertEquals(checkerStopPosition.getErrors().get(0).getCode(),
                PTAssistantValidatorTest.ERROR_CODE_SOLITARY_STOP_POSITION);

    }
}
