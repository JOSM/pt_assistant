// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTRouteDataManager;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;

class SegmentCheckerTest {
    @Test
    void testBasics() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.SEGMENT_TEST(), "testLayer");
        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        Relation route = null;

        for (Relation r: ds.getRelations()) {
            if (RouteUtils.isVersionTwoPTRoute(r)) {
                route = r;
                break;
            }
        }

        SegmentChecker segmentChecker = new SegmentChecker(route, test);
        PTRouteDataManager manager = new PTRouteDataManager(route);
        segmentChecker.setManager(manager);
        segmentChecker.setAssigner(new StopToWayAssigner(manager.getPTWays()));
        segmentChecker.performStopByStopTest();
        //since 33425 storing correct segments only of continuous routes
        test.storeCorrectRouteSegments(route,
                segmentChecker.getManager(), segmentChecker.getAssigner());
        assertEquals(27, SegmentChecker.getCorrectSegmentCount());
        assertEquals(0, segmentChecker.getErrors().size());
    }
}
