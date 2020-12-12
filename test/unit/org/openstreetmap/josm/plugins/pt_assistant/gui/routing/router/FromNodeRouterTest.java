package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.WaySuitability;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.transportmode.BusRouteType;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class FromNodeRouterTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void testFindRouteTargetsSimpleLine() {
        DataSet ds = TestFiles.importOsmFile(getClass().getResourceAsStream("/router/find-route-targets-simple-line.xml"), "testLayer");

        FromNodeRouter router = new FromNodeRouter(findWithTags(ds, "start"),
            0, new BusRouteType());

        // Normal targets that do not require a split
        // Targets are sorted by distance.
        List<RouteTarget> targets = router.findRouteTargets(1000, 5);

        assertEquals(2, targets.size());

        assertEquals(findWithTags(ds, "targetNode1"), targets.get(0).getEnd());
        assertEquals(1, targets.get(0).getTrace().size());
        assertEquals(findWithTags(ds, "start"), targets.get(0).getTrace().get(0).firstNode());
        assertEquals(findWithTags(ds, "trace1"), targets.get(0).getTrace().get(0).getWay());
        assertEquals(findWithTags(ds, "targetNode1"), targets.get(0).getTrace().get(0).lastNode());

        assertEquals(findWithTags(ds, "targetNode2"), targets.get(1).getEnd());
        assertEquals(2, targets.get(1).getTrace().size());
        assertEquals(findWithTags(ds, "start"), targets.get(1).getTrace().get(0).firstNode());
        assertEquals(findWithTags(ds, "trace1"), targets.get(1).getTrace().get(0).getWay());
        assertEquals(findWithTags(ds, "targetNode1"), targets.get(1).getTrace().get(0).lastNode());
        assertEquals(findWithTags(ds, "targetNode1"), targets.get(1).getTrace().get(1).firstNode());
        assertEquals(findWithTags(ds, "trace2"), targets.get(1).getTrace().get(1).getWay());
        assertEquals(findWithTags(ds, "targetNode2"), targets.get(1).getTrace().get(1).lastNode());
    }

    @Test
    public void testFindRouteTargetsAccessRestrictions() {
        DataSet ds = TestFiles.importOsmFile(getClass().getResourceAsStream("/router/find-route-targets-access-restrictions.xml"), "testLayer");

        FromNodeRouter router = new FromNodeRouter(findWithTags(ds, "start"),
            0, new BusRouteType());
        List<RouteTarget> targets = router.findRouteTargets(1000, 5);

        assertEquals(7, targets.size());
        IntStream.range(0, 7).forEach(i -> {
            RouteTarget routeTarget = targets.get(i);
            assertEquals(1, routeTarget.getTrace().size(), "size " + i);
            Way way = findWithTags(ds, "trace" + i);
            assertEquals(way, routeTarget.getTrace().get(0).getWay(), "Way " + i);
            assertEquals(WaySuitability.GOOD, routeTarget.getTrace().get(0).getSuitability(), "getSuitability " + i);
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends OsmPrimitive> T findWithTags(DataSet ds, String tagName) {
        Collection<OsmPrimitive> primitives = ds.getPrimitives(it -> it.hasTag(tagName));
        if (primitives.isEmpty()) {
            throw new IllegalArgumentException("No primitive with tag " + tagName);
        }
        if (primitives.size() > 1) {
            throw new IllegalArgumentException("Multiple primitive with tag " + tagName);
        }
        return (T) primitives.iterator().next();
    }
}
