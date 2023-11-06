// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;

class AdjacentWaysTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void testAdjacentOneways(int oneWayTestSize) {
        DataSet ds = TestFiles.importOsmFile(oneWayTestSize == 1
            ? TestFiles.ONEWAY_WRONG_DIRECTION()
            : TestFiles.ONEWAY_WRONG_DIRECTION2(), "testLayer");

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

        assertEquals(set.size(), oneWayTestSize);
    }
}
