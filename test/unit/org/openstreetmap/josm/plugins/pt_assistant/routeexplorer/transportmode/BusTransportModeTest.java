package org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.transportmode;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.plugins.pt_assistant.routeexplorer.WayTraversalDirection.*;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class BusTransportModeTest {
    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    private static final ITransportMode transportMode = new BusTransportMode();

    @Test
    public void testCanBeUsedForRelation() {
        Relation route = new Relation();
        route.put("type", "route");
        route.put("route", "bus");
        assertTrue(transportMode.canBeUsedForRelation(route));

        route.put("route", "trolleybus");
        assertFalse(transportMode.canBeUsedForRelation(route));

        route.put("route", "coach");
        assertTrue(transportMode.canBeUsedForRelation(route));

        route.put("route", "tram");
        assertFalse(transportMode.canBeUsedForRelation(route));

        route.put("route", "train");
        assertFalse(transportMode.canBeUsedForRelation(route));

        route.put("route", "minibus");
        assertTrue(transportMode.canBeUsedForRelation(route));

        route.remove("route");
        assertFalse(transportMode.canBeUsedForRelation(route));
    }

    @Test
    public void testCanTraverseWay() {
        Node n1 = new Node();
        Node n2 = new Node();

        Way w12 = new Way();
        w12.addNode(n1);
        w12.addNode(n2);

        Way residentialWay = new Way(w12);
        residentialWay.put("highway", "residential");

        Way unclassifiedWay = new Way(w12);
        unclassifiedWay.put("highway", "unclassified");

        Way serviceWay = new Way(w12);
        serviceWay.put("highway", "service");

        Way livingStreetWay = new Way(w12);
        livingStreetWay.put("highway", "living_street");

        Way cyclestreetWay = new Way(w12);
        cyclestreetWay.put("highway", "cyclestreet");

        Way primaryWay = new Way(w12);
        primaryWay.put("highway", "primary");

        Way secondaryWay = new Way(w12);
        secondaryWay.put("highway", "secondary");

        Way tertiaryWay = new Way(w12);
        tertiaryWay.put("highway", "tertiary");

        Way trunkWay = new Way(w12);
        trunkWay.put("highway", "trunk");

        Way motorWay = new Way(w12);
        motorWay.put("highway", "motorway");

        Way primaryLinkWay = new Way(w12);
        primaryLinkWay.put("highway", "primary_link");

        Way secondaryLinkWay = new Way(w12);
        secondaryLinkWay.put("highway", "secondary_link");

        Way tertiaryLinkWay = new Way(w12);
        tertiaryLinkWay.put("highway", "tertiary_link");

        Way trunkLinkWay = new Way(w12);
        trunkLinkWay.put("highway", "trunk_link");

        Way motorWayLinkWay = new Way(w12);
        motorWayLinkWay.put("highway", "motorway_link");

        Way trackWay = new Way(w12);
        trackWay.put("highway", "track");

        Way pathWay = new Way(w12);
        pathWay.put("highway", "path");

        Way cycleWay = new Way(w12);
        cycleWay.put("highway", "cycleway");

        Way footWay = new Way(w12);
        footWay.put("highway", "footway");

        Way pedestrianWay = new Way(w12);
        footWay.put("highway", "pedestrian");

        Way railWay = new Way(w12);
        footWay.put("railway", "rail");

        Way tramWay = new Way(w12);
        footWay.put("railway", "tram");

        Way subWay = new Way(w12);
        footWay.put("railway", "subway");

        Way light_railWay = new Way(w12);
        footWay.put("railway", "light_rail");

        Way[] suitableWaysForBuses = new Way[]{
            residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
            primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
            primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay};

        Way[] unSuitableWaysForBuses = new Way[]{cycleWay, footWay, pedestrianWay, railWay, tramWay, subWay, light_railWay};

        for (Way way : suitableWaysForBuses) {
            assertTrue(transportMode.canTraverseWay(way));
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        for (Way way : unSuitableWaysForBuses) {
            assertFalse(transportMode.canTraverseWay(way));
            assertFalse(transportMode.canTraverseWay(way, FORWARD));
            assertFalse(transportMode.canTraverseWay(way, BACKWARD));
        }

        for (Way way : unSuitableWaysForBuses) {
            way.put("bus", "yes");
            assertTrue(transportMode.canTraverseWay(way));
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        for (Way way : unSuitableWaysForBuses) {
            way.put("psv", "yes");
            assertTrue(transportMode.canTraverseWay(way));
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is a general oneway tag?
        for (Way way : suitableWaysForBuses) {
            way.put("oneway", "yes");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertFalse(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is an exception for buses?
        for (Way way : suitableWaysForBuses) {
            way.put("oneway:bus", "no");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is an additional exception for psv?
        for (Way way : suitableWaysForBuses) {
            way.put("oneway:psv", "no");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is just an exception for psv?;
        for (Way way : suitableWaysForBuses) {
            way.remove("oneway:bus");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }
    }

    @Test
    public void testCanTurn() {
    }
}
