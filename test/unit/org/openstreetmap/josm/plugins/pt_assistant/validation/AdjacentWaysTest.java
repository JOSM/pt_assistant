// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class AdjacentWaysTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void test1() {
        DataSet ds = TestFiles.importOsmFile(TestFiles.ONEWAY_WRONG_DIRECTION(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        long id = 24215210;
        Way way = (Way) ds.getPrimitiveById(id, OsmPrimitiveType.WAY);

        assertEquals(RouteUtils.isOnewayForPublicTransport(way), -1);

        Relation route = null;
        for (Relation r : ds.getRelations()) {
            if (r.hasKey("route")) {
                route = r;
            }
        }

        WayChecker wayChecker = new WayChecker(route, test);
        Set<Way> set = wayChecker.checkAdjacentWays(way, new HashSet<Way>());

        assertEquals(set.size(), 1);

    }

    @Test
    public void test2() {

        DataSet ds = TestFiles.importOsmFile(TestFiles.ONEWAY_WRONG_DIRECTION2(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();
        long id = 24215210;
        Way way = (Way) ds.getPrimitiveById(id, OsmPrimitiveType.WAY);

        assertEquals(RouteUtils.isOnewayForPublicTransport(way), -1);

        Relation route = null;
        for (Relation r : ds.getRelations()) {
            if (r.hasKey("route")) {
                route = r;
            }
        }

        WayChecker wayChecker = new WayChecker(route, test);
        Set<Way> set = wayChecker.checkAdjacentWays(way, new HashSet<Way>());

        assertEquals(set.size(), 2);

    }

}
