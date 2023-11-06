// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;

class SolitaryStopPositionTest {
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
        assertEquals(1, checkerPlatform.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_PLATFORM_PART_OF_HIGHWAY,
                checkerPlatform.getErrors().get(0).getCode());

        NodeChecker checkerStopPosition = new NodeChecker(stopPosition, test);
        checkerStopPosition.performSolitaryStopPositionTest();
        assertEquals(1, checkerStopPosition.getErrors().size());
        assertEquals(PTAssistantValidatorTest.ERROR_CODE_SOLITARY_STOP_POSITION,
                checkerStopPosition.getErrors().get(0).getCode());

    }
}
