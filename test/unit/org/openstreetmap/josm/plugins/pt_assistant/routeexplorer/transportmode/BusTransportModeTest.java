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

//        // Create stops:
//        Node n1 = new Node();
//        n1.put("name", "Stop1");
//        n1.put("public_transport", "stop_position");
//        RelationMember rm1 = new RelationMember("stop", n1);
//        members.add(rm1);
//        Way w1 = new Way();
//        w1.put("name", "Stop2");
//        w1.put("highway", "platform");
//        RelationMember rm2 = new RelationMember("platform", w1);
//        members.add(rm2);
//        Node n2 = new Node();
//        n2.put("name", "Stop3");
//        n2.put("public_transport", "platform");
//        RelationMember rm3 = new RelationMember("platform", n2);
//        members.add(rm3);
//        Node n3 = new Node();
//        n3.put("name", "Stop4");
//        n3.put("public_transport", "stop_position");
//        RelationMember rm4 = new RelationMember("stop", n3);
//        members.add(rm4);
//        Node n4 = new Node();
//        n4.put("name", "Stop4");
//        n4.put("public_transport", "platform");
//        RelationMember rm5 = new RelationMember("platform", n4);
//        members.add(rm5);
//        Node n5 = new Node();
//        n5.put("name", "Stop5");
//        n5.put("highway", "platform");
//        RelationMember rm6 = new RelationMember("platform_exit_only", n5);
//        members.add(rm6);
//
//        // Create ways:
//        Way w2 = new Way();
//        RelationMember rm7 = new RelationMember("", w2);
//        members.add(rm7);
//        Way w3 = new Way();
//        RelationMember rm8 = new RelationMember("", w3);
//        members.add(rm8);
//        Relation r3 = new Relation(); // nested relation
//        Way w4 = new Way();
//        Way w5 = new Way();
//        Way w6 = new Way();
//        r3.addMember(new RelationMember("", w4));
//        r3.addMember(new RelationMember("", w5));
//        r3.addMember(new RelationMember("", w6));
//        RelationMember rm9 = new RelationMember("", r3);
//        members.add(rm9);
//        Way w7 = new Way();
//        RelationMember rm10 = new RelationMember("", w7);
//        members.add(rm10);

//    public static final Relation route = new Relation();
//
//    @BeforeClass
//    public static void setUp() {
//
//        ArrayList<RelationMember> members = new ArrayList<>();
//
////        route.setMembers(members);
//        route.put("type", "route");
//        route.put("route", "bus");
//
//        transportMode = new BusTransportMode();
//    }

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

        for (Way way : new Way[]{residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
                primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
                primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay}) {
            assertTrue(transportMode.canTraverseWay(way));
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        for (Way way : new Way[]{cycleWay, footWay}) {
            assertFalse(transportMode.canTraverseWay(way));
            assertFalse(transportMode.canTraverseWay(way, FORWARD));
            assertFalse(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is a general oneway tag?
        for (Way way : new Way[]{residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
            primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
            primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay}) {
            way.put("oneway", "yes");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertFalse(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is an exception for buses?
        for (Way way : new Way[]{residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
            primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
            primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay}) {
            way.put("oneway:bus", "no");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is an additional exception for psv?
        for (Way way : new Way[]{residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
            primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
            primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay}) {
            way.put("oneway:psv", "no");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }

        // what if there is just an exception for psv?
        for (Way way : new Way[]{residentialWay, unclassifiedWay, serviceWay, livingStreetWay, cyclestreetWay,
            primaryWay, secondaryWay, tertiaryWay, trunkWay, motorWay,
            primaryLinkWay, secondaryLinkWay, tertiaryLinkWay, trunkLinkWay, motorWayLinkWay}) {
            way.remove("oneway:bus");
            assertTrue(transportMode.canTraverseWay(way, FORWARD));
            assertTrue(transportMode.canTraverseWay(way, BACKWARD));
        }
    }

    @Test
    public void testCanTurn() {
    }
}
