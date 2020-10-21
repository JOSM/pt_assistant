package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

import static org.junit.Assert.*;
import static org.openstreetmap.josm.io.OsmReader.parseDataSet;

/**
 * An ExpectedValue for each index
 */
@SuppressWarnings("unused")
class Val {
    int index;
    int iDOfNextWay;
    String nameOfNextWay;
    int iDOfFirstWay;
    int iDOfLastWay;
    String note = null;
    String expectedRouteRef = null;

    public Val(int index, int iDOfFirstWay, int iDOfLastWay, int iDOfNextWay,
               String nameOfNextWay, String note, String expectedRouteRef) {
        this.index = index;
        this.nameOfNextWay = nameOfNextWay;
        this.iDOfFirstWay = iDOfFirstWay;
        this.iDOfLastWay = iDOfLastWay;
        this.iDOfNextWay = iDOfNextWay;
        this.note = note;
        this.expectedRouteRef = expectedRouteRef;
    }

    public Val(int index, int iDOfFirstWay, int iDOfLastWay, int iDOfNextWay, String nameOfNextWay) {
        this.index = index;
        this.nameOfNextWay = nameOfNextWay;
        this.iDOfFirstWay = iDOfFirstWay;
        this.iDOfLastWay = iDOfLastWay;
        this.iDOfNextWay = iDOfNextWay;
    }
}

@SuppressWarnings("NonAsciiCharacters")
public class RouteSegmentToExtractTest extends AbstractTest{

    private static DataSet ds;
    private static Collection<Relation> allRelations;
    private static Collection<Way> allWays;
    private static DataSet ds2;
    private static Collection<Relation> allRelations2;
    private final static String rc = " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way";

    @BeforeClass
    public static void init() throws FileNotFoundException, IllegalDataException {
        ds = parseDataSet(new FileInputStream(PATH_TO_PT_BEFORE_SPLITTING_TEST), null);
        allRelations = ds.getRelations();
        allWays = ds.getWays();
        ds2 = parseDataSet(new FileInputStream(PATH_TO_F74_F75_TEST), null);
        allRelations2 = ds2.getRelations();
    }

    @Test
    public void isItineraryInSameDirectionTest() {
        Relation bus601RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3612781)
            .findFirst().orElse(null);

        Relation bus358RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 6695469)
            .findFirst().orElse(null);

        Relation bus371RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 1606056)
            .findFirst().orElse(null);

        assertNotNull(bus601RouteRelation);
        WaySequence waysInParentRouteOf601 = new WaySequence(
            bus601RouteRelation.getMembers().get(156).getWay(),
            bus601RouteRelation.getMembers().get(157).getWay(),
            bus601RouteRelation.getMembers().get(158).getWay());

        assertNotNull(bus358RouteRelation);
        WaySequence waysInParentRouteOf358 = new WaySequence(
            bus358RouteRelation.getMembers().get(114).getWay(),
            bus358RouteRelation.getMembers().get(115).getWay(),
            bus358RouteRelation.getMembers().get(116).getWay());

        assertNotNull(bus371RouteRelation);
        WaySequence waysInParentRouteOf371 = new WaySequence(
            bus371RouteRelation.getMembers().get(132).getWay(),
            bus371RouteRelation.getMembers().get(133).getWay(),
            bus371RouteRelation.getMembers().get(134).getWay());
        RouteSegmentToExtract segment601_1 = new RouteSegmentToExtract(bus601RouteRelation);
        segment601_1.setActiveDataSet(ds);
        assertTrue(segment601_1.isItineraryInSameDirection(waysInParentRouteOf601, waysInParentRouteOf358));
        assertFalse(segment601_1.isItineraryInSameDirection(waysInParentRouteOf601, waysInParentRouteOf371));

        Way commonWay = allWays.stream()
            .filter(way -> way.getId() == 75113358)
            .findFirst().orElse(null);

        Way toHaasrode = allWays.stream()
            .filter(way -> way.getId() == 4866090)
            .findFirst().orElse(null);

        Way toBlanden = allWays.stream()
            .filter(way -> way.getId() == 809484771)
            .findFirst().orElse(null);

        Way toNeervelp = allWays.stream()
            .filter(way -> way.getId() == 243961945)
            .findFirst().orElse(null);

        Way toLeuven = allWays.stream()
            .filter(way -> way.getId() == 809485597)
            .findFirst().orElse(null);

        WaySequence leuvenToHaasrode = new WaySequence(toLeuven,commonWay,toHaasrode);
        WaySequence leuvenToBlanden = new WaySequence(toLeuven,commonWay,toBlanden);
        WaySequence neervelpToHaasrode = new WaySequence(toNeervelp,commonWay,toHaasrode);
        WaySequence neervelpToBlanden = new WaySequence(toNeervelp,commonWay,toBlanden);
        WaySequence haasrodeToLeuven = new WaySequence(toHaasrode,commonWay,toLeuven);
        WaySequence blandenToLeuven = new WaySequence(toBlanden,commonWay,toLeuven);
        WaySequence haasrodeToNeervelp = new WaySequence(toHaasrode,commonWay,toNeervelp);
        WaySequence blandenToNeervelp = new WaySequence(toBlanden,commonWay,toNeervelp);

        assertTrue(segment601_1.isItineraryInSameDirection(leuvenToHaasrode, leuvenToBlanden));
        assertTrue(segment601_1.isItineraryInSameDirection(leuvenToBlanden, leuvenToHaasrode));
        assertTrue(segment601_1.isItineraryInSameDirection(haasrodeToLeuven, blandenToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(blandenToLeuven, haasrodeToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(neervelpToBlanden, neervelpToHaasrode));
        assertTrue(segment601_1.isItineraryInSameDirection(neervelpToHaasrode, neervelpToBlanden));
        assertTrue(segment601_1.isItineraryInSameDirection(blandenToNeervelp, blandenToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(blandenToLeuven, blandenToNeervelp));

        assertFalse(segment601_1.isItineraryInSameDirection(leuvenToBlanden, blandenToLeuven));
        assertFalse(segment601_1.isItineraryInSameDirection(blandenToLeuven, leuvenToBlanden));
        assertFalse(segment601_1.isItineraryInSameDirection(leuvenToHaasrode, haasrodeToLeuven));
        assertFalse(segment601_1.isItineraryInSameDirection(haasrodeToLeuven, leuvenToHaasrode));
        assertFalse(segment601_1.isItineraryInSameDirection(neervelpToBlanden, blandenToNeervelp));
        assertFalse(segment601_1.isItineraryInSameDirection(blandenToNeervelp, neervelpToBlanden));
        assertFalse(segment601_1.isItineraryInSameDirection(neervelpToHaasrode, haasrodeToNeervelp));
        assertFalse(segment601_1.isItineraryInSameDirection(haasrodeToNeervelp, neervelpToHaasrode));

        WaySequence missingToBlanden = new WaySequence(null,commonWay,toBlanden);
        WaySequence missingToHaasrode = new WaySequence(null,commonWay,toHaasrode);
        WaySequence missingToLeuven = new WaySequence(null,commonWay,toLeuven);
        WaySequence missingToNeervelp = new WaySequence(null,commonWay,toNeervelp);
        WaySequence blandenToMissing = new WaySequence(toBlanden,commonWay,null);
        WaySequence haasrodeToMissing = new WaySequence(toHaasrode,commonWay,null);
        WaySequence leuvenToMissing = new WaySequence(toNeervelp,commonWay,null);
        WaySequence neervelpToMissing = new WaySequence(toNeervelp,commonWay,null);

        assertTrue(segment601_1.isItineraryInSameDirection(missingToBlanden, leuvenToBlanden));
        assertTrue(segment601_1.isItineraryInSameDirection(leuvenToBlanden, missingToBlanden));
        assertTrue(segment601_1.isItineraryInSameDirection(missingToLeuven, blandenToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(blandenToLeuven, missingToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(missingToBlanden, neervelpToHaasrode));
        assertTrue(segment601_1.isItineraryInSameDirection(neervelpToHaasrode, missingToBlanden));
        assertTrue(segment601_1.isItineraryInSameDirection(missingToNeervelp, blandenToLeuven));
        assertTrue(segment601_1.isItineraryInSameDirection(blandenToLeuven, missingToNeervelp));

        assertFalse(segment601_1.isItineraryInSameDirection(leuvenToBlanden, blandenToLeuven));
        assertFalse(segment601_1.isItineraryInSameDirection(blandenToLeuven, leuvenToBlanden));
        assertFalse(segment601_1.isItineraryInSameDirection(leuvenToHaasrode, haasrodeToLeuven));
        assertFalse(segment601_1.isItineraryInSameDirection(haasrodeToLeuven, leuvenToHaasrode));
        assertFalse(segment601_1.isItineraryInSameDirection(neervelpToBlanden, blandenToNeervelp));
        assertFalse(segment601_1.isItineraryInSameDirection(blandenToNeervelp, neervelpToBlanden));
        assertFalse(segment601_1.isItineraryInSameDirection(neervelpToHaasrode, haasrodeToNeervelp));
        assertFalse(segment601_1.isItineraryInSameDirection(haasrodeToNeervelp, neervelpToHaasrode));
    }


    @Test
    public void f74_F75_Test() {
        Relation f75BicycleRouteRelation = allRelations2.stream()
            .filter(relation -> relation.getId() == 11021011)
            .findFirst().orElse(null);
        assertNotNull(f75BicycleRouteRelation);
        assertEquals(30, f75BicycleRouteRelation.getMembersCount());

        RouteSegmentToExtract s1 = new RouteSegmentToExtract(
            f75BicycleRouteRelation,
            Arrays.asList(23, 24, 25));
        s1.setActiveDataSet(ds2);
        s1.put("state", "proposed");

        assertEquals(3, s1.getWayMembers().size());
        Relation rel1 = s1.extractToRelation(Arrays.asList("type", "route"), true);
        assertEquals(28, f75BicycleRouteRelation.getMembersCount());
        assertEquals("proposed", rel1.get("state"));
    }

    @Test
    public void bus601_600_3_Test() {
        Relation bus601RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3612781)
            .findFirst().orElse(null);

        assertNotNull(bus601RouteRelation);
        Relation cloneOfBus601RouteRelation = new Relation(bus601RouteRelation);
        assertNotNull(cloneOfBus601RouteRelation);
        RouteSegmentToExtract segment1 = new RouteSegmentToExtract(cloneOfBus601RouteRelation);
        segment1.setActiveDataSet(ds);

        assertEquals(cloneOfBus601RouteRelation.get("ref"), segment1.getLineIdentifiersSignature());
        assertEquals(cloneOfBus601RouteRelation.get("colour"), segment1.getColoursSignature());

        assertNull(segment1.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment1.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment1.getWayMembers());


        RouteSegmentToExtract returnValueNull;

        List<Val> expectedValues = Arrays.asList(
            new Val( 158,  78579065,  78579065,  78579065, null),
            new Val( 157, 377814547,  78579065, 377814547, null),

            new Val( 156, 377814547,  78579065,  79596986, null,
                "perron 1 & 2 (1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658)",
                "1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658"),


            new Val( 155,  79596986,  79596986,  79211473, "Tiensevest",
                "(305;306;310;318;358;410;433;475;485;601;658)",
                "305;306;310;318;358;410;433;475;485;601;658"),


            new Val( 154,  79211473,  79211473,  79211472, "Tiensevest",
                "Tiensevest (1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658)",
                "1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658"),


            new Val( 153,  79211472,  79211472,  79175435, "Tiensevest",
                "Tiensevest (2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658)",
                "2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658"),


            new Val( 152,  79175435,  79175435,  80458208, "Tiensevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658)",
                "284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658"),


            new Val( 151,  80458208,  80458208,  19793164, "Tiensevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658)",
                "284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658"),


            new Val( 150,  19793164,  19793164,   6184898, "Diestsevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658)",
                "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658"),

            new Val( 149,  81457878,   6184898,  81457878, "Diestsevest"),
            new Val( 148,   4003924,   6184898,   4003924, "Diestsevest"),
            new Val( 147,   8133608,   6184898,   8133608, "Diestsevest"),

            new Val( 146,   8133608,   6184898, 254800931, "Joanna-Maria Artoisplein",
                "Diestsevest (305;318;334;335;358;410;513;601;630;651;652;658)",
                "305;318;334;335;358;410;513;601;630;651;652;658"),


            new Val( 145, 254800931, 254800931,  23691158, "Joanna-Maria Artoisplein",
                "Joanna-Maria Artoisplein (178;305;318;334;335;358;410;513;601;630;651;652;658)",
                "178;305;318;334;335;358;410;513;601;630;651;652;658"),

            new Val( 144, 254800932,  23691158, 254800932, "Lüdenscheidsingel"),
            new Val( 143,  44932921,  23691158,  44932921, "Lüdenscheidsingel"),
            new Val( 142,   3993388,  23691158,   3993388, "Lüdenscheidsingel"),
            new Val( 141, 109267417,  23691158, 109267417, "Lüdenscheidsingel"),

            new Val( 140, 109267417,  23691158,   3993387, "Lüdenscheidsingel",
                "Lüdenscheidsingel - Joanna-Maria Artoisplein (178;305;318;358;410;601;651;652;658)",
                "178;305;318;358;410;601;651;652;658"),

            new Val( 139,   8131125,   3993387,   8131125, null),
            new Val( 138,   3877105,   3993387,   3877105, null),
            new Val( 137, 146171871,   3993387, 146171871, "Den Boschsingel"),
            new Val( 136,  23837544,   3993387,  23837544, "Den Boschsingel"),

            new Val( 135,  23837544,   3993387, 225605630, "Den Boschsingel",
                "Den Boschsingel - Lüdenscheidsingel (318;358;410;601;651;658)",
                "318;358;410;601;651;658"),

            new Val( 134,   8131121, 225605630,   8131121, "Den Boschsingel"),
            new Val( 133,   3680456, 225605630,   3680456, "Rennes-Singel"),
            new Val( 132,   3994257, 225605630,   3994257, "Rennes-Singel"),
            new Val( 131, 249333185, 225605630, 249333185, "Rennes-Singel"),

            new Val( 130, 249333185, 225605630, 249333184, "Herestraat",
                "Rennes-Singel - Den Boschsingel (318;410;601)",
                "318;410;601"),

            new Val( 129, 813970231, 249333184, 813970231, "Herestraat"),
            new Val( 128, 681081951, 249333184, 681081951, "Herestraat"),

            new Val( 127, 681081951, 249333184, 813970227, "Herestraat",
                "Herestraat (410;601)",
                "410;601"),

            new Val( 126,  41403544, 813970227,  41403544, "Herestraat"),
            new Val( 125,   8079995, 813970227,   8079995, "Herestraat"),

            new Val( 124,   8079995, 813970227,  41403538, "Rotonde Het Teken",
                "Herestraat (410;600;601)",
                "410;600;601"),

            new Val( 123,  79340950,  41403538,  79340950, "Ring Zuid"),

            new Val( 122,  79340950,  41403538,  11369123, "Ring Zuid",
                "Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 121,  11369123,  11369123, 159949154, null,
                "Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 120, 159949154, 159949154, 332258104, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 119,  78852604, 332258104,  78852604, null),

            new Val( 118,  78852604, 332258104, 377918641, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 117,  14508736, 377918641,  14508736, null),

            new Val( 116,  14508736, 377918641, 109267436, "Ring Zuid",
                "(3;317;395;410;601)",
                "3;317;395;410;601"),


            new Val( 115, 109267436, 109267436,  14508739, "Ring Zuid",
                "Ring Zuid (3;317;395;410;600;601)",
                "3;317;395;410;600;601"),


            new Val( 114,  14508739,  14508739,  14508740, "Ring Zuid",
                "Ring Zuid (3;317;334;335;395;410;600;601)",
                "3;317;334;335;395;410;600;601"),


            new Val( 113,  14508740,  14508740, 502328838, "Ring Zuid",
                "Ring Zuid (3;317;334;335;380;395;410;600;601)",
                "3;317;334;335;380;395;410;600;601"),

            new Val( 112, 502328837, 502328838, 502328837, "Ring Zuid"),
            new Val( 111,   8080023, 502328838,   8080023, "Ring Zuid"),
            new Val( 110,  78568660, 502328838,  78568660, "Rotonde Het Teken"),

            new Val( 109,  78568660, 502328838,  78873921, "Rotonde Het Teken",
                "Rotonde Het Teken - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 108,  78873921,  78873921,   3752557, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 107,   3752557,   3752557, 249333188, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 106, 249333188, 249333188, 249333187, "Rotonde Het Teken",
                "Rotonde Het Teken (3;410;600;601)",
                "3;410;600;601"),


            new Val( 105, 249333187, 249333187,  13067134, "Herestraat",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 104, 813970232,  13067134, 813970232, "Herestraat"),
            new Val( 103, 813970226,  13067134, 813970226, "Herestraat"),
            new Val( 102, 813970228,  13067134, 813970228, "Herestraat"),

            new Val( 101, 813970228,  13067134, 813970229, "Herestraat",
                "Herestraat (410;600;601)",
                "410;600;601"),

            new Val( 100, 813970231, 813970229, 813970231, "Herestraat"),
            new Val( 99, 249333184, 813970229, 249333184, "Herestraat"),

            new Val( 98, 249333184, 813970229, 249333186, "Rennes-Singel",
                "Herestraat (601)",
                "601"),

            new Val( 97, 192559628, 249333186, 192559628, "Rennes-Singel"),
            new Val( 96, 161166589, 249333186, 161166589, "Rennes-Singel"),
            new Val( 95, 813979470, 249333186, 813979470, "Rennes-Singel"),
            new Val( 94,  79289746, 249333186,  79289746, "Rennes-Singel"),
            new Val( 93, 813979472, 249333186, 813979472, "Rennes-Singel"),
            new Val( 92,   8131120, 249333186,   8131120, "Rennes-Singel"),
            new Val( 91, 429706864, 249333186, 429706864, "Rennes-Singel"),

            new Val( 90, 429706864, 249333186,  99583853, "Tervuursevest",
                "Rennes-Singel (318;601)",
                "318;601"),

            new Val( 89, 521193379,  99583853, 521193379, "Tervuursevest"),
            new Val( 88, 521193380,  99583853, 521193380, "Tervuursevest"),
            new Val( 87, 813979465,  99583853, 813979465, "Tervuursevest"),
            new Val( 86, 174338458,  99583853, 174338458, "Tervuursevest"),
            new Val( 85, 608715520,  99583853, 608715520, "Tervuursevest"),
            new Val( 84, 608715521,  99583853, 608715521, "Tervuursevest"),
            new Val( 83,   3677944,  99583853,   3677944, "Tervuursevest"),
            new Val( 82, 174338459,  99583853, 174338459, "Tervuursevest"),
            new Val( 81, 429706866,  99583853, 429706866, "Tervuursevest"),
            new Val( 80,  88361317,  99583853,  88361317, "Tervuursevest"),

            new Val( 79,  88361317,  99583853, 461159345, "Tervuursevest",
                "Tervuursevest (601)",
                "601"),


            new Val( 78, 461159345, 461159345, 461159362, "Tervuursevest",
                "Tervuursevest (178;179;306 (student);601)",
                "178;179;306 (student);601"),

            new Val( 77, 344507822, 461159362, 344507822, "Tervuursevest"),
            new Val( 76, 461159367, 461159362, 461159367, "Tervuursevest"),
            new Val( 75,   3677335, 461159362,   3677335, "Tervuursevest"),
            new Val( 74,  31474001, 461159362,  31474001, "Tervuursevest"),
            new Val( 73,  23237288, 461159362,  23237288, "Tervuursevest"),
            new Val( 72,  90168773, 461159362,  90168773, "Tervuursevest"),
            new Val( 71,  23237287, 461159362,  23237287, "Tervuursevest"),
            new Val( 70, 608715561, 461159362, 608715561, "Tervuursevest"),
            new Val( 69, 608715562, 461159362, 608715562, "Tervuursevest"),
            new Val( 68,   3677330, 461159362,   3677330, "Tervuursevest"),

            new Val( 67,   3677330, 461159362,  86164005, "Naamsevest",
                "Tervuursevest (178;179;306 (student);520;524;525;537;601)",
                "178;179;306 (student);520;524;525;537;601"),

            new Val( 66, 655251293,  86164005, 655251293, "Naamsevest"),
            new Val( 65, 131571763,  86164005, 131571763, "Naamsevest"),
            new Val( 64, 661733369,  86164005, 661733369, "Naamsevest"),
            new Val( 63, 655251292,  86164005, 655251292, "Naamsevest"),
            new Val( 62,   3677823,  86164005,   3677823, "Naamsevest"),
            new Val( 61,  24905257,  86164005,  24905257, "Geldenaaksevest"),

            new Val( 60,  24905257,  86164005, 608715605, "Geldenaaksevest",
                "Geldenaaksevest - Naamsevest (18;178;179;306 (student);337;601;616)",
                "18;178;179;306 (student);337;601;616"),

            new Val( 59, 608715606, 608715605, 608715606, "Geldenaaksevest"),
            new Val( 58,  79299303, 608715605,  79299303, "Geldenaaksevest"),
            new Val( 57,  10296368, 608715605,  10296368, "Geldenaaksevest"),
            new Val( 56, 521193607, 608715605, 521193607, "Geldenaaksevest"),
            new Val( 55,  94585453, 608715605,  94585453, "Geldenaaksevest"),
            new Val( 54, 586268893, 608715605, 586268893, "Geldenaaksevest"),
            new Val( 53,   8130906, 608715605,   8130906, "Geldenaaksevest"),

            new Val( 52,   8130906, 608715605,  16775171, "Tiensepoort",
                "Geldenaaksevest (18;178;179;306 (student);337;601;616;630)",
                "18;178;179;306 (student);337;601;616;630"),

            new Val( 51,   8131717,  16775171,   8131717, "Tiensevest"),
            new Val( 50,  12712557,  16775171,  12712557, "Tiensevest"),
            new Val( 49,   8590231,  16775171,   8590231, "Tiensevest"),

            new Val( 48,   8590231,  16775171, 185988814, "Tiensevest",
                "Tiensevest - Tiensepoort (7;8;9;18;178;179;306 (student);337;380;527;601;616;630)",
                "7;8;9;18;178;179;306 (student);337;380;527;601;616;630"),


            new Val( 47, 185988814, 185988814,  76856823, "Martelarenplein",
                "Tiensevest (1;4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;616;630)",
                "1;4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;616;630"),

            new Val( 46, 459446598,  76856823, 459446598, "Martelarenplein"),
            new Val( 45, 459446600,  76856823, 459446600, "Martelarenplein"),
            new Val( 44,  78815533,  76856823,  78815533, null),

            new Val( 43,  78815533,  76856823,  79264899, null,
                "Martelarenplein (4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;630)",
                "4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;630"),


            new Val( 42,  79264899,  79264899, 377918635, null,
                "(4;5;6;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601;630)",
                "4;5;6;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601;630"),


            new Val( 41, 377918635, 377918635,  79264888, null,
                "(3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601)",
                "3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601"),


            new Val( 40,  79264888,  79264888,  79264897, null,
                "(2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601)",
                "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601"),

            new Val( 39,  71754927,  79264897,  71754927, null),

            new Val( 38,  71754927,  79264897, 377918638, null,
                "(2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601)",
                "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601"),


            new Val( 37, 377918638, 377918638,  79264891, null,
                "(18;601)",
                "18;601"),


            new Val( 36,  79264891,  79264891,  78568409, "Tiensevest",
                "(18;601)",
                "18;601"),


            new Val( 35,  78568409,  78568409,  79193579, "Tiensevest",
                "Tiensevest (4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;334;335;337;380;601;616;658)",
                "4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;334;335;337;380;601;616;658"),


            new Val( 34,  79193579,  79193579,  19793394, null,
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658"),


            new Val( 33,  19793394,  19793394,  19793164, "Tiensevest",
                "(334;335;513;601)",
                "334;335;513;601"),


            new Val( 32,  19793164,  19793164,   6184898, "Diestsevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658)",
                "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658"),

            new Val( 31,  81457878,   6184898,  81457878, "Diestsevest"),
            new Val( 30,   4003924,   6184898,   4003924, "Diestsevest"),
            new Val( 29,   8133608,   6184898,   8133608, "Diestsevest"),

            new Val( 28,   8133608,   6184898, 254800931, "Joanna-Maria Artoisplein",
                "Diestsevest (305;318;334;335;358;410;513;601;630;651;652;658)",
                "305;318;334;335;358;410;513;601;630;651;652;658"),


            new Val( 27, 254800931, 254800931,   3992548, "Zoutstraat",
                "Joanna-Maria Artoisplein (178;305;318;334;335;358;410;513;601;630;651;652;658)",
                "178;305;318;334;335;358;410;513;601;630;651;652;658"),


            new Val( 26,   3992548,   3992548, 510790349, "Havenkant",
                "Zoutstraat (334;335;513;601;630)",
                "334;335;513;601;630"),

            new Val( 25, 510790348, 510790349, 510790348, "Havenkant"),
            new Val( 24, 314635787, 510790349, 314635787, "Havenkant"),
            new Val( 23, 843534478, 510790349, 843534478, "Havenkant"),
            new Val( 22, 406205781, 510790349, 406205781, "Havenkant"),
            new Val( 21, 270181176, 510790349, 270181176, "Havenkant"),
            new Val( 20, 330300725, 510790349, 330300725, "Havenkant"),
            new Val( 19,   3869822, 510790349,   3869822, "Burchtstraat"),
            new Val( 18, 330300723, 510790349, 330300723, "Achter de latten"),
            new Val( 17, 659297690, 510790349, 659297690, "Wolvengang"),
            new Val( 16,  25928482, 510790349,  25928482, "Wolvengang"),
            new Val( 15,   3869812, 510790349,   3869812, "Engels Plein"),
            new Val( 14, 338057820, 510790349, 338057820, "Engels Plein"),

            new Val( 13, 338057820, 510790349, 338057820, "Engels Plein",
                "Engels Plein - Havenkant (601)",
                "601")
        );        RouteSegmentToExtract previousSegment = new RouteSegmentToExtract(cloneOfBus601RouteRelation);
        previousSegment.setActiveDataSet(ds);
        RouteSegmentToExtract segment;
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, cloneOfBus601RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }
        System.out.print("\n");

        // ***********************************************************
        // ***********************************************************
        // ***********************************************************


        Relation bus600RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 955908)
            .findFirst().orElse(null);

        assertNotNull(bus600RouteRelation);
        Relation cloneOfBus600RouteRelation = new Relation(bus600RouteRelation);
        assertNotNull(cloneOfBus600RouteRelation);
        RouteSegmentToExtract segment101 = new RouteSegmentToExtract(cloneOfBus600RouteRelation);
        segment101.setActiveDataSet(ds);

        assertEquals(cloneOfBus600RouteRelation.get("ref"), segment101.getLineIdentifiersSignature());
        assertEquals(cloneOfBus600RouteRelation.get("colour"), segment101.getColoursSignature());

        assertNull(segment101.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment101.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment101.getWayMembers());

        expectedValues = Arrays.asList(
            new Val( 169, 608715622, 608715622, 608715622, "Engels Plein"),
            new Val( 168, 338057819, 608715622, 338057819, "Engels Plein"),
            new Val( 167, 305316104, 608715622, 305316104, "Engels Plein"),
            new Val( 166,   3869812, 608715622,   3869812, "Engels Plein"),
            new Val( 165,  25928482, 608715622,  25928482, "Wolvengang"),
            new Val( 164, 659297690, 608715622, 659297690, "Wolvengang"),
            new Val( 163, 330300723, 608715622, 330300723, "Achter de latten"),
            new Val( 162,   3869822, 608715622,   3869822, "Burchtstraat"),
            new Val( 161, 330300725, 608715622, 330300725, "Havenkant"),
            new Val( 160, 270181176, 608715622, 270181176, "Havenkant"),
            new Val( 159, 406205781, 608715622, 406205781, "Havenkant"),
            new Val( 158, 843534478, 608715622, 843534478, "Havenkant"),
            new Val( 157, 314635787, 608715622, 314635787, "Havenkant"),
            new Val( 156, 510790348, 608715622, 510790348, "Havenkant"),
            new Val( 155,  29283599, 608715622,  29283599, "Havenkant"),

            new Val( 154,  29283599, 608715622, 304241968, "Havenkant",
                "Havenkant - Engels Plein (600)",
                "600"),

            new Val( 153, 304241967, 304241968, 304241967, "Aarschotsesteenweg"),

            new Val( 152, 304241967, 304241968, 340265961, "Redersstraat",
                "Aarschotsesteenweg - Havenkant (334;335;513;600;630)",
                "334;335;513;600;630"),

            new Val( 151, 318825613, 340265961, 318825613, "Redersstraat"),

            new Val( 150, 318825613, 340265961, 340265962, "Redersstraat",
                "Redersstraat (600)",
                "600"),


            new Val( 149, 340265962, 340265962, 254801390, "Joanna-Maria Artoisplein",
                "Redersstraat (333;334;335;513;600;630)",
                "333;334;335;513;600;630"),

            new Val( 148,  61540068, 254801390,  61540068, "Joanna-Maria Artoisplein"),

            new Val( 147,  61540068, 254801390,  23691160, "Vuurkruisenlaan",
                "Joanna-Maria Artoisplein (178;305;318;333;334;335;410;513;600;630;651;652;658)",
                "178;305;318;333;334;335;410;513;600;630;651;652;658"),

            new Val( 146,   4061640,  23691160,   4061640, "Vuurkruisenlaan"),
            new Val( 145,   8109264,  23691160,   8109264, "Diestsepoort"),

            new Val( 144,   8109264,  23691160,  61556877, "Diestsepoort",
                "Diestsepoort - Vuurkruisenlaan (305;333;334;335;513;600;630;651;652;658)",
                "305;333;334;335;513;600;630;651;652;658"),

            new Val( 143, 663770966,  61556877, 663770966, "Diestsepoort"),
            new Val( 142, 584356749,  61556877, 584356749, "Diestsepoort"),
            new Val( 141, 584356745,  61556877, 584356745, "Diestsepoort"),

            new Val( 140, 584356745,  61556877, 198559166, "Diestsepoort",
                "Diestsepoort (2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",
                "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658"),

            new Val( 139, 584356751, 198559166, 584356751, "Diestsepoort"),
            new Val( 138, 451873773, 198559166, 451873773, "Diestsepoort"),
            new Val( 137, 584356742, 198559166, 584356742, "Diestsepoort"),
            new Val( 136, 451873774, 198559166, 451873774, "Diestsepoort"),

            new Val( 135, 451873774, 198559166,  76867049, null,
                "Diestsepoort (2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",
                "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658"),

            new Val( 134,  79264890,  76867049,  79264890, null),
            new Val( 133,  79596965,  76867049,  79596965, null),

            new Val( 132,  79596965,  76867049,  79596974, null,
                "(2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",
                "2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630"),

            new Val( 131,  79596982,  79596974,  79596982, null),

            new Val( 130,  79596982,  79596974,  79596987, null,
                "perron 11 & 12 (3;333;334;335;433;600;630)",
                "3;333;334;335;433;600;630"),

            new Val( 129, 377918658,  79596987, 377918658, null),
            new Val( 128,  79596980,  79596987,  79596980, null),

            new Val( 127,  79596980,  79596987,  79193579, "Tiensevest",
                "perron 11 & 12 (3;333;334;335;513;600;630)",
                "3;333;334;335;513;600;630"),


            new Val( 126,  79193579,  79193579, 258936980, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658"),


            new Val( 125, 258936980, 258936980,  79193580, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658"),


            new Val( 124,  79193580,  79193580,  79193581, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),


            new Val( 123,  79193581,  79193581,  78815505, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),


            new Val( 122,  78815505,  78815505,  84696751, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),

            new Val( 121,  79265237,  84696751,  79265237, "Tiensevest"),

            new Val( 120,  79265237,  84696751,  89574079, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630"),

            new Val( 119,  81522744,  89574079,  81522744, "Tiensevest"),

            new Val( 118,  81522744,  89574079,  19793223, "Tiensevest",
                "Tiensevest (1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630)",
                "1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630"),

            new Val( 117, 185988814,  19793223, 185988814, "Tiensevest"),

            new Val( 116, 185988814,  19793223, 185988816, "Tiensevest",
                "Tiensevest (1;4;5;6;7;8;9;18;179;306 (student);337;380;600;616;630)",
                "1;4;5;6;7;8;9;18;179;306 (student);337;380;600;616;630"),

            new Val( 115,  15083398, 185988816,  15083398, "Tiensevest"),

            new Val( 114,  15083398, 185988816,   8154434, "Tiensevest",
                "Tiensevest (7;8;9;18;179;306 (student);337;380;600;616;630)",
                "7;8;9;18;179;306 (student);337;380;600;616;630"),

            new Val( 113,   4003927,   8154434,   4003927, "Tiensevest"),
            new Val( 112, 521193609,   8154434, 521193609, "Tiensevest"),
            new Val( 111, 586268892,   8154434, 586268892, "Tiensevest"),
            new Val( 110, 521193611,   8154434, 521193611, "Tiensevest"),

            new Val( 109, 521193611,   8154434,   4003928, "Tiensepoort",
                "Tiensevest (7;8;9;18;178;179;306 (student);337;380;600;616;630)",
                "7;8;9;18;178;179;306 (student);337;380;600;616;630"),

            new Val( 108,   8130905,   4003928,   8130905, "Geldenaaksevest"),
            new Val( 107,  24905256,   4003928,  24905256, "Geldenaaksevest"),
            new Val( 106,  24905255,   4003928,  24905255, "Geldenaaksevest"),
            new Val( 105, 491728135,   4003928, 491728135, "Geldenaaksevest"),
            new Val( 104,  24905254,   4003928,  24905254, "Geldenaaksevest"),
            new Val( 103, 199381120,   4003928, 199381120, "Geldenaaksevest"),

            new Val( 102, 199381120,   4003928,   3991775, "Geldenaaksevest",
                "Geldenaaksevest - Tiensepoort (18;178;179;306 (student);337;600;616;630)",
                "18;178;179;306 (student);337;600;616;630"),

            new Val( 101, 120086001,   3991775, 120086001, "Erasme Ruelensvest"),
            new Val( 100, 608715575,   3991775, 608715575, "Erasme Ruelensvest"),
            new Val( 99, 608715579,   3991775, 608715579, "Erasme Ruelensvest"),

            new Val( 98, 608715579,   3991775,   3677822, "Erasme Ruelensvest",
                "Erasme Ruelensvest - Geldenaaksevest (18;178;179;306 (student);337;600;616)",
                "18;178;179;306 (student);337;600;616"),

            new Val( 97, 120086003,   3677822, 120086003, "Tervuursevest"),

            new Val( 96, 120086003,   3677822,  90168774, "Tervuursevest",
                "Tervuursevest - Erasme Ruelensvest (178;179;306 (student);600)",
                "178;179;306 (student);600"),

            new Val( 95,   3677329,  90168774,   3677329, "Tervuursevest"),
            new Val( 94, 608715614,  90168774, 608715614, "Tervuursevest"),
            new Val( 93, 608715615,  90168774, 608715615, "Tervuursevest"),
            new Val( 92,  85044922,  90168774,  85044922, "Tervuursevest"),

            new Val( 91,  85044922,  90168774,  16771611, "Tervuursevest",
                "Tervuursevest (178;179;306 (student);520;524;525;537;586;600)",
                "178;179;306 (student);520;524;525;537;586;600"),


            new Val( 90,  16771611,  16771611,  13246158, "Tervuursevest",
                "Tervuursevest (178;520;524;525;537;586;600)",
                "178;520;524;525;537;586;600"),


            new Val( 89,  13246158,  13246158, 260405216, "Tervuursevest",
                "Tervuursevest (7;8;9;178;520;524;525;527;537;586;600)",
                "7;8;9;178;520;524;525;527;537;586;600"),

            new Val( 88,  16771609, 260405216,  16771609, "Tervuursevest"),

            new Val( 87,  16771609, 260405216, 608715518, "Tervuursevest",
                "Tervuursevest (7;8;9;178;527;600)",
                "7;8;9;178;527;600"),

            new Val( 86, 608715519, 608715518, 608715519, "Tervuursevest"),
            new Val( 85,   3677336, 608715518,   3677336, "Tervuursevest"),
            new Val( 84,  15051052, 608715518,  15051052, "Tervuursevest"),
            new Val( 83, 814322832, 608715518, 814322832, "Tervuursevest"),
            new Val( 82,  80194318, 608715518,  80194318, "Tervuursevest"),
            new Val( 81, 813979466, 608715518, 813979466, "Tervuursevest"),
            new Val( 80, 521193382, 608715518, 521193382, "Tervuursevest"),
            new Val( 79, 521193383, 608715518, 521193383, "Tervuursevest"),
            new Val( 78,   3677945, 608715518,   3677945, "Tervuursevest"),

            new Val( 77,   3677945, 608715518, 192559627, "Rennes-Singel",
                "Tervuursevest (178;600)",
                "178;600"),

            new Val( 76, 813979469, 192559627, 813979469, "Rennes-Singel"),
            new Val( 75, 192559626, 192559627, 192559626, "Rennes-Singel"),
            new Val( 74, 813979473, 192559627, 813979473, "Rennes-Singel"),
            new Val( 73,  28982660, 192559627,  28982660, "Rennes-Singel"),

            new Val( 72,  28982660, 192559627,  78568455, "Herestraat",
                "Rennes-Singel (178;318;600)",
                "178;318;600"),


            new Val( 71,  78568455,  78568455, 813970227, "Herestraat",
                "Herestraat (600)",
                "600"),

            new Val( 70,  41403544, 813970227,  41403544, "Herestraat"),
            new Val( 69,   8079995, 813970227,   8079995, "Herestraat"),

            new Val( 68,   8079995, 813970227,  41403538, "Rotonde Het Teken",
                "Herestraat (410;600;601)",
                "410;600;601"),

            new Val( 67,  79340950,  41403538,  79340950, "Ring Zuid"),

            new Val( 66,  79340950,  41403538,  11369123, "Ring Zuid",
                "Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 65,  11369123,  11369123, 159949154, null,
                "Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 64, 159949154, 159949154, 332258104, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 63,  78852604, 332258104,  78852604, null),

            new Val( 62,  78852604, 332258104,  14508735, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 61,  14508735,  14508735, 318878531, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600"),

            new Val( 60,  14506241, 318878531,  14506241, null),

            new Val( 59,  14506241, 318878531, 109267436, "Ring Zuid",
                "(3;317;395;410;600)",
                "3;317;395;410;600"),


            new Val( 58, 109267436, 109267436,  14508739, "Ring Zuid",
                "Ring Zuid (3;317;395;410;600;601)",
                "3;317;395;410;600;601"),


            new Val( 57,  14508739,  14508739,  14508740, "Ring Zuid",
                "Ring Zuid (3;317;334;335;395;410;600;601)",
                "3;317;334;335;395;410;600;601"),


            new Val( 56,  14508740,  14508740, 502328838, "Ring Zuid",
                "Ring Zuid (3;317;334;335;380;395;410;600;601)",
                "3;317;334;335;380;395;410;600;601"),

            new Val( 55, 502328837, 502328838, 502328837, "Ring Zuid"),
            new Val( 54,   8080023, 502328838,   8080023, "Ring Zuid"),
            new Val( 53,  78568660, 502328838,  78568660, "Rotonde Het Teken"),

            new Val( 52,  78568660, 502328838,  78873921, "Rotonde Het Teken",
                "Rotonde Het Teken - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 51,  78873921,  78873921,   3752557, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 50,   3752557,   3752557, 249333188, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 49, 249333188, 249333188, 249333187, "Rotonde Het Teken",
                "Rotonde Het Teken (3;410;600;601)",
                "3;410;600;601"),


            new Val( 48, 249333187, 249333187,  13067134, "Herestraat",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 47, 813970232,  13067134, 813970232, "Herestraat"),
            new Val( 46, 813970226,  13067134, 813970226, "Herestraat"),
            new Val( 45, 813970228,  13067134, 813970228, "Herestraat"),

            new Val( 44, 813970228,  13067134, 270181177, "Herestraat",
                "Herestraat (410;600;601)",
                "410;600;601"),


            new Val( 43, 270181177, 270181177,  78568454, "Rennes-Singel",
                "Herestraat (410;600)",
                "410;600"),

            new Val( 42, 225605633,  78568454, 225605633, "Rennes-Singel"),

            new Val( 41, 225605633,  78568454,   8131123, "Rennes-Singel",
                "Rennes-Singel (178;318;410;600)",
                "178;318;410;600"),

            new Val( 40,   3680457,   8131123,   3680457, "Rennes-Singel"),
            new Val( 39, 192559625,   8131123, 192559625, "Den Boschsingel"),
            new Val( 38,  23837543,   8131123,  23837543, "Den Boschsingel"),

            new Val( 37,  23837543,   8131123, 146171867, "Den Boschsingel",
                "Den Boschsingel - Rennes-Singel (318;410;600)",
                "318;410;600"),

            new Val( 36, 125835586, 146171867, 125835586, "Den Boschsingel"),
            new Val( 35,   3877104, 146171867,   3877104, null),
            new Val( 34,  85048201, 146171867,  85048201, "Lüdenscheidsingel"),

            new Val( 33,  85048201, 146171867,  12891213, "Lüdenscheidsingel",
                "Lüdenscheidsingel - Den Boschsingel (318;410;600;651;658)",
                "318;410;600;651;658"),

            new Val( 32,  44932919,  12891213,  44932919, "Lüdenscheidsingel"),
            new Val( 31,   8131040,  12891213,   8131040, "Lüdenscheidsingel"),
            new Val( 30,  61540098,  12891213,  61540098, "Joanna-Maria Artoisplein"),

            new Val( 29,  61540098,  12891213, 254801390, "Joanna-Maria Artoisplein",
                "Joanna-Maria Artoisplein - Lüdenscheidsingel (178;305;318;410;600;651;652;658)",
                "178;305;318;410;600;651;652;658"),

            new Val( 28,  61540068, 254801390,  61540068, "Joanna-Maria Artoisplein"),

            new Val( 27,  61540068, 254801390,  23691160, "Vuurkruisenlaan",
                "Joanna-Maria Artoisplein (178;305;318;333;334;335;410;513;600;630;651;652;658)",
                "178;305;318;333;334;335;410;513;600;630;651;652;658"),

            new Val( 26,   4061640,  23691160,   4061640, "Vuurkruisenlaan"),
            new Val( 25,   8109264,  23691160,   8109264, "Diestsepoort"),

            new Val( 24,   8109264,  23691160,  61556877, "Diestsepoort",
                "Diestsepoort - Vuurkruisenlaan (305;333;334;335;513;600;630;651;652;658)",
                "305;333;334;335;513;600;630;651;652;658"),

            new Val( 23, 663770966,  61556877, 663770966, "Diestsepoort"),
            new Val( 22, 584356749,  61556877, 584356749, "Diestsepoort"),
            new Val( 21, 584356745,  61556877, 584356745, "Diestsepoort"),

            new Val( 20, 584356745,  61556877, 198559166, "Diestsepoort",
                "Diestsepoort (2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",
                "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658"),

            new Val( 19, 584356751, 198559166, 584356751, "Diestsepoort"),
            new Val( 18, 451873773, 198559166, 451873773, "Diestsepoort"),
            new Val( 17, 584356742, 198559166, 584356742, "Diestsepoort"),
            new Val( 16, 451873774, 198559166, 451873774, "Diestsepoort"),

            new Val( 15, 451873774, 198559166, 116797180, "Diestsepoort",
                "Diestsepoort (2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",
                "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658"),

            new Val( 14,  23691157, 116797180,  23691157, "Diestsepoort"),

            new Val( 13,  23691157, 116797180,  78815527, null,
                "Diestsepoort (4;5;6;7;8;9;179;306 (student);334;335;337;380;600;616;651;652;658)",
                "4;5;6;7;8;9;179;306 (student);334;335;337;380;600;616;651;652;658"),

            new Val( 12, 377918658,  78815527, 377918658, null),

            new Val( 11, 377918658,  78815527, 377918658, null,
                "perron 11 & 12 (395;600;651;652)",
                "395;600;651;652")
        );
        previousSegment = new RouteSegmentToExtract(cloneOfBus600RouteRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, cloneOfBus600RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }

        System.out.print("\n");

        // ***********************************************************
        // Line 3 has the particularity that there are 2 variants
        // The longer version has a spoon to serve another hospital
        // The following test block is for the last stretch of the shorter version
        // ***********************************************************

        Relation bus3_GHB_Lubbeek_RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3297543)
            .findFirst().orElse(null);

        assertNotNull(bus3_GHB_Lubbeek_RouteRelation);
        RouteSegmentToExtract segment201 = new RouteSegmentToExtract(bus3_GHB_Lubbeek_RouteRelation);
        segment201.setActiveDataSet(ds);

        assertEquals(bus3_GHB_Lubbeek_RouteRelation.get("ref"), segment201.getLineIdentifiersSignature());
        assertEquals(bus3_GHB_Lubbeek_RouteRelation.get("colour"), segment201.getColoursSignature());

        assertNull(segment201.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment201.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment201.getWayMembers());

        expectedValues = Arrays.asList(
            new Val( 194,  27684829,  27684829,  27684829, "Dorpskring"),
            new Val( 193, 349396917,  27684829, 349396917, "Dorpskring"),
            new Val( 192, 349396921,  27684829, 349396921, "Dorpskring"),
            new Val( 191, 112917099,  27684829, 112917099, "Dorpskring"),

            new Val( 190, 112917099,  27684829, 125835538, "Dorpskring",
                "Dorpskring (3)",
                "3"),


            new Val( 189, 125835538, 125835538,  81197019, "Bollenberg",
                "Dorpskring (3;373;485)",
                "3;373;485"),

            new Val( 188, 112917100,  81197019, 112917100, "Bollenberg"),
            new Val( 187, 694551612,  81197019, 694551612, "Bollenberg"),
            new Val( 186, 645048305,  81197019, 645048305, "Bollenberg"),
            new Val( 185,  27688813,  81197019,  27688813, "Bollenberg"),
            new Val( 184, 232474772,  81197019, 232474772, "Lubbeekstraat"),
            new Val( 183,  10658839,  81197019,  10658839, "Lubbeekstraat"),
            new Val( 182, 319269331,  81197019, 319269331, "Kapelstraat"),
            new Val( 181, 636950670,  81197019, 636950670, "Kapelstraat"),
            new Val( 180,  16377722,  81197019,  16377722, "Kapelstraat"),
            new Val( 179,  16377612,  81197019,  16377612, "Kapelstraat"),

            new Val( 178,  16377612,  81197019,  40189518, "Lostraat",
                "Kapelstraat - Bollenberg (3)",
                "3"),

            new Val( 177, 351196789,  40189518, 351196789, "Lostraat"),
            new Val( 176, 350992067,  40189518, 350992067, "Lostraat"),
            new Val( 175, 636491595,  40189518, 636491595, "Lostraat"),
            new Val( 174, 636491594,  40189518, 636491594, "Lostraat"),
            new Val( 173, 124913579,  40189518, 124913579, "Lostraat"),
            new Val( 172,  10672044,  40189518,  10672044, "Lostraat"),
            new Val( 171, 633919766,  40189518, 633919766, "Lostraat"),
            new Val( 170,  10672080,  40189518,  10672080, "Heidebergstraat"),
            new Val( 169,  10672083,  40189518,  10672083, "Heidebergstraat"),
            new Val( 168,  79801761,  40189518,  79801761, "Heidebergstraat"),
            new Val( 167, 112917249,  40189518, 112917249, "Heidebergstraat"),
            new Val( 166, 112917247,  40189518, 112917247, "Heidebergstraat"),
            new Val( 165, 192706264,  40189518, 192706264, "Heidebergstraat"),
            new Val( 164, 318216944,  40189518, 318216944, "Heidebergstraat"),
            new Val( 163, 192706265,  40189518, 192706265, "Heidebergstraat"),
            new Val( 162, 586861164,  40189518, 586861164, "Heidebergstraat"),
            new Val( 161,  10672031,  40189518,  10672031, "Heidebergstraat"),
            new Val( 160,  10672135,  40189518,  10672135, "Koetsweg"),
            new Val( 159, 608754691,  40189518, 608754691, "Koetsweg"),
            new Val( 158, 608754692,  40189518, 608754692, "Koetsweg"),
            new Val( 157,  10672174,  40189518,  10672174, "Gaston Eyskenslaan"),
            new Val( 156,  23436182,  40189518,  23436182, "Platte-Lostraat"),
            new Val( 155, 112917234,  40189518, 112917234, "Duivenstraat"),
            new Val( 154,  23707216,  40189518,  23707216, "Duivenstraat"),
            new Val( 153, 608754700,  40189518, 608754700, "Lijsterlaan"),
            new Val( 152,  10672177,  40189518,  10672177, "Lijsterlaan"),
            new Val( 151, 112917228,  40189518, 112917228, "Prins Regentplein"),
            new Val( 150, 112917229,  40189518, 112917229, "Prins Regentplein"),
            new Val( 149,  10672178,  40189518,  10672178, "Prins Regentplein"),
            new Val( 148,  10657957,  40189518,  10657957, "Prins-Regentlaan"),
            new Val( 147,  25155107,  40189518,  25155107, "Willem Coosemansstraat"),
            new Val( 146,  79784138,  40189518,  79784138, "Koning Albertlaan"),
            new Val( 145,  23707241,  40189518,  23707241, "Koning Albertlaan"),
            new Val( 144, 112917227,  40189518, 112917227, "Koning Albertlaan"),
            new Val( 143, 101619135,  40189518, 101619135, "Koning Albertlaan"),
            new Val( 142, 622063635,  40189518, 622063635, "Koning Albertlaan"),
            new Val( 141, 622063636,  40189518, 622063636, "Koning Albertlaan"),
            new Val( 140, 101619136,  40189518, 101619136, "Koning Albertlaan"),
            new Val( 139,  16377030,  40189518,  16377030, "Martelarenlaan"),
            new Val( 138, 485802425,  40189518, 485802425, "Spoordijk"),
            new Val( 137,  23707242,  40189518,  23707242, "Martelarenlaan"),
            new Val( 136, 608715582,  40189518, 608715582, "Martelarenlaan"),
            new Val( 135, 608715584,  40189518, 608715584, "Martelarenlaan"),
            new Val( 134, 284832275,  40189518, 284832275, "Martelarenlaan"),
            new Val( 133, 608715586,  40189518, 608715586, "Martelarenlaan"),
            new Val( 132, 608715590,  40189518, 608715590, "Martelarenlaan"),
            new Val( 131,   3992546,  40189518,   3992546, "Martelarenlaan"),
            new Val( 130,  10230617,  40189518,  10230617, "Oude Diestsesteenweg"),

            new Val( 129,  10230617,  40189518,  23707243, "Diestsesteenweg",
                "Oude Diestsesteenweg - Lostraat (3)",
                "3"),


            new Val( 128,  23707243,  23707243,  23707244, "Diestsesteenweg",
                "Diestsesteenweg (3;370;371;373;374;475;485;524;525)",
                "3;370;371;373;374;475;485;524;525"),

            new Val( 127,  12715116,  23707244,  12715116, "Diestsesteenweg"),

            new Val( 126,  12715116,  23707244,  61556877, "Diestsepoort",
                "Diestsesteenweg (2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525)",
                "2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525"),

            new Val( 125, 663770966,  61556877, 663770966, "Diestsepoort"),
            new Val( 124, 584356749,  61556877, 584356749, "Diestsepoort"),
            new Val( 123, 584356745,  61556877, 584356745, "Diestsepoort"),

            new Val( 122, 584356745,  61556877, 198559166, "Diestsepoort",
                "Diestsepoort (2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",
                "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658"),

            new Val( 121, 584356751, 198559166, 584356751, "Diestsepoort"),
            new Val( 120, 451873773, 198559166, 451873773, "Diestsepoort"),
            new Val( 119, 584356742, 198559166, 584356742, "Diestsepoort"),
            new Val( 118, 451873774, 198559166, 451873774, "Diestsepoort"),

            new Val( 117, 451873774, 198559166,  76867049, null,
                "Diestsepoort (2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",
                "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658"),

            new Val( 116,  79264890,  76867049,  79264890, null),
            new Val( 115,  79596965,  76867049,  79596965, null),

            new Val( 114,  79596965,  76867049,  71754927, null,
                "(2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",
                "2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630"),


            new Val( 113,  71754927,  71754927,  79264897, null,
                "(2;3;310;370;371;373;374;475;485;513;520;524;525)",
                "2;3;310;370;371;373;374;475;485;513;520;524;525"),

            new Val( 112,  79596983,  79264897,  79596983, null),
            new Val( 111, 377918665,  79264897, 377918665, null),

            new Val( 110, 377918665,  79264897,  78815513, null,
                "perron 7 & 8 (2;3;310)",
                "2;3;310"),

            new Val( 109, 377918666,  78815513, 377918666, null),
            new Val( 108,  79596978,  78815513,  79596978, null),

            new Val( 107,  79596978,  78815513,  79193581, "Tiensevest",
                "perron 7 & 8 (2;3)",
                "2;3"),


            new Val( 106,  79193581,  79193581,  78815505, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),


            new Val( 105,  78815505,  78815505,  84696751, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),

            new Val( 104,  79265237,  84696751,  79265237, "Tiensevest"),

            new Val( 103,  79265237,  84696751,  89574079, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630"),

            new Val( 102,  81522744,  89574079,  81522744, "Tiensevest"),

            new Val( 101,  81522744,  89574079, 305434579, "Bondgenotenlaan",
                "Tiensevest (1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630)",
                "1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630"),

            new Val( 100, 578662093, 305434579, 578662093, "Bondgenotenlaan"),
            new Val( 99, 578662092, 305434579, 578662092, "Bondgenotenlaan"),
            new Val( 98, 578662095, 305434579, 578662095, "Bondgenotenlaan"),
            new Val( 97, 578662094, 305434579, 578662094, "Bondgenotenlaan"),
            new Val( 96,   3991636, 305434579,   3991636, "Bondgenotenlaan"),
            new Val( 95, 174985125, 305434579, 174985125, "Rector De Somerplein"),
            new Val( 94, 521211977, 305434579, 521211977, "Rector De Somerplein"),

            new Val( 93, 521211977, 305434579, 521211976, "Rector De Somerplein",
                "Rector De Somerplein - Bondgenotenlaan (2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539)",
                "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539"),

            new Val( 92,  16771741, 521211976,  16771741, "Rector De Somerplein"),

            new Val( 91,  16771741, 521211976,   3991635, "Margarethaplein",
                "Rector De Somerplein (2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539)",
                "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539"),

            new Val( 90, 521211973,   3991635, 521211973, "Margarethaplein"),
            new Val( 89, 608715546,   3991635, 608715546, "Margarethaplein"),
            new Val( 88, 453538107,   3991635, 453538107, "Margarethaplein"),
            new Val( 87,  80194313,   3991635,  80194313, "Mathieu de Layensplein"),
            new Val( 86, 293288706,   3991635, 293288706, "Mathieu de Layensplein"),
            new Val( 85,  10269271,   3991635,  10269271, "Dirk Boutslaan"),
            new Val( 84, 578662072,   3991635, 578662072, "Dirk Boutslaan"),
            new Val( 83, 608715541,   3991635, 608715541, "Dirk Boutslaan"),
            new Val( 82, 293149632,   3991635, 293149632, "Dirk Boutslaan"),
            new Val( 81,   3992578,   3991635,   3992578, "Dirk Boutslaan"),
            new Val( 80, 438252643,   3991635, 438252643, "Dirk Boutslaan"),
            new Val( 79, 284664268,   3991635, 284664268, "Brouwersstraat"),

            new Val( 78, 284664268,   3991635, 608715545, "Brouwersstraat",
                "Brouwersstraat - Margarethaplein (3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 77, 608715543, 608715545, 608715543, "Brouwersstraat"),
            new Val( 76, 608715542, 608715545, 608715542, "Brouwersstraat"),
            new Val( 75, 608715544, 608715545, 608715544, "Brouwersstraat"),
            new Val( 74, 284664272, 608715545, 284664272, "Brouwersstraat"),

            new Val( 73, 284664272, 608715545, 147856945, "Tessenstraat - Fonteinstraat",
                "Brouwersstraat (3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 72, 123929547, 147856945, 123929547, "Kapucijnenvoer"),

            new Val( 71, 123929547, 147856945,   3358673, "Biezenstraat",
                "Kapucijnenvoer - Tessenstraat - Fonteinstraat (3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 70, 578662071,   3358673, 578662071, "Sint-Jacobsplein"),
            new Val( 69, 521211966,   3358673, 521211966, "Sint-Jacobsplein"),
            new Val( 68,   3358671,   3358673,   3358671, "Sint-Jacobsplein"),
            new Val( 67, 123929615,   3358673, 123929615, "Sint-Hubertusstraat"),

            new Val( 66, 123929615,   3358673, 189453003, "Monseigneur Van Waeyenberghlaan",
                "Sint-Hubertusstraat - Biezenstraat (3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513)",
                "3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513"),

            new Val( 65,  79289753, 189453003,  79289753, "Monseigneur Van Waeyenberghlaan"),
            new Val( 64, 189453004, 189453003, 189453004, "Monseigneur Van Waeyenberghlaan"),
            new Val( 63, 189453002, 189453003, 189453002, "Monseigneur Van Waeyenberghlaan"),
            new Val( 62, 189453001, 189453003, 189453001, "Monseigneur Van Waeyenberghlaan"),
            new Val( 61, 810592121, 189453003, 810592121, "Monseigneur Van Waeyenberghlaan"),
            new Val( 60, 249333181, 189453003, 249333181, "Monseigneur Van Waeyenberghlaan"),

            new Val( 59, 249333181, 189453003, 249333187, "Rotonde Het Teken",
                "Monseigneur Van Waeyenberghlaan (3;317;333;334;335;370;371;373;374;380;395;513)",
                "3;317;333;334;335;370;371;373;374;380;395;513"),


            new Val( 58, 249333187, 249333187, 813970230, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 57,  41403540, 813970230,  41403540, "Rotonde Het Teken"),

            new Val( 56,  41403540, 813970230,  41403538, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;513)",
                "3;317;333;334;335;370;371;373;374;380;395;513"),

            new Val( 55,  79340950,  41403538,  79340950, "Ring Zuid"),

            new Val( 54,  79340950,  41403538,  11369123, "Ring Zuid",
                "Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 53,  11369123,  11369123, 159949154, null,
                "Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 52, 159949154, 159949154, 332258104, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 51,  78852604, 332258104,  78852604, null),

            new Val( 50,  78852604, 332258104,  14508735, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 49,  14508735,  14508735, 318878531, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600"),

            new Val( 48,  14506241, 318878531,  14506241, null),

            new Val( 47,  14506241, 318878531, 109267436, "Ring Zuid",
                "(3;317;395;410;600)",
                "3;317;395;410;600"),


            new Val( 46, 109267436, 109267436,  14508739, "Ring Zuid",
                "Ring Zuid (3;317;395;410;600;601)",
                "3;317;395;410;600;601"),


            new Val( 45,  14508739,  14508739,  14508740, "Ring Zuid",
                "Ring Zuid (3;317;334;335;395;410;600;601)",
                "3;317;334;335;395;410;600;601"),


            new Val( 44,  14508740,  14508740, 502328838, "Ring Zuid",
                "Ring Zuid (3;317;334;335;380;395;410;600;601)",
                "3;317;334;335;380;395;410;600;601"),

            new Val( 43, 502328837, 502328838, 502328837, "Ring Zuid"),
            new Val( 42,   8080023, 502328838,   8080023, "Ring Zuid"),
            new Val( 41,  78568660, 502328838,  78568660, "Rotonde Het Teken"),

            new Val( 40,  78568660, 502328838,  15945426, "Ring Noord",
                "Rotonde Het Teken - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),

            new Val( 39, 502317795,  15945426, 502317795, "Ring Noord"),
            new Val( 38, 810580948,  15945426, 810580948, "Ring Noord"),
            new Val( 37, 502317796,  15945426, 502317796, "Ring Noord"),
            new Val( 36, 112917238,  15945426, 112917238, "Ring Noord"),
            new Val( 35, 608754690,  15945426, 608754690, "Ring Noord"),
            new Val( 34, 377918625,  15945426, 377918625, "Ring Noord"),

            new Val( 33, 377918625,  15945426, 377918625, "Ring Noord",
                "Ring Noord (3)",
                "3")
        );        previousSegment = new RouteSegmentToExtract(bus3_GHB_Lubbeek_RouteRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, bus3_GHB_Lubbeek_RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }

        System.out.print("\n");

        // ***********************************************************
        // Line 3 has the particularity that there are 2 variants
        // The longer version has a spoon to serve another hospital
        // This is testing the longer version completely
        // ***********************************************************

        Relation bus3_GHB_Pellenberg_Lubbeek_RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 2815)
            .findFirst().orElse(null);

        assertNotNull(bus3_GHB_Pellenberg_Lubbeek_RouteRelation);
        RouteSegmentToExtract segment301 = new RouteSegmentToExtract(bus3_GHB_Pellenberg_Lubbeek_RouteRelation);
        segment301.setActiveDataSet(ds);

        assertEquals(bus3_GHB_Pellenberg_Lubbeek_RouteRelation.get("ref"), segment301.getLineIdentifiersSignature());
        assertEquals(bus3_GHB_Pellenberg_Lubbeek_RouteRelation.get("colour"), segment301.getColoursSignature());

        assertNull(segment301.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment301.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment301.getWayMembers());

        expectedValues = Arrays.asList(
            new Val( 217,  27684829,  27684829,  27684829, "Dorpskring"),
            new Val( 216, 349396917,  27684829, 349396917, "Dorpskring"),
            new Val( 215, 349396921,  27684829, 349396921, "Dorpskring"),
            new Val( 214, 112917099,  27684829, 112917099, "Dorpskring"),

            new Val( 213, 112917099,  27684829, 125835538, "Dorpskring",
                "Dorpskring (3)",
                "3"),


            new Val( 212, 125835538, 125835538,  81197019, "Bollenberg",
                "Dorpskring (3;373;485)",
                "3;373;485"),

            new Val( 211, 112917100,  81197019, 112917100, "Bollenberg"),
            new Val( 210, 694551612,  81197019, 694551612, "Bollenberg"),
            new Val( 209, 645048305,  81197019, 645048305, "Bollenberg"),
            new Val( 208,  27688813,  81197019,  27688813, "Bollenberg"),
            new Val( 207, 232474772,  81197019, 232474772, "Lubbeekstraat"),
            new Val( 206,  10658839,  81197019,  10658839, "Lubbeekstraat"),
            new Val( 205, 319269331,  81197019, 319269331, "Kapelstraat"),
            new Val( 204, 636950670,  81197019, 636950670, "Kapelstraat"),
            new Val( 203,  16377722,  81197019,  16377722, "Kapelstraat"),
            new Val( 202,  16377612,  81197019,  16377612, "Kapelstraat"),

            new Val( 201,  16377612,  81197019, 112917224, "Ganzendries",
                "Kapelstraat - Bollenberg (3)",
                "3"),

            new Val( 200, 319264895, 112917224, 319264895, "Ganzendries"),
            new Val( 199,  27682732, 112917224,  27682732, "Sint Barbaradreef"),
            new Val( 198, 527248228, 112917224, 527248228, null),
            new Val( 197,  27686451, 112917224,  27686451, null),
            new Val( 196, 847554912, 112917224, 847554912, null),

            new Val( 195, 847554912, 112917224,  27686453, null,
                "Sint Barbaradreef - Ganzendries (3)",
                "3"),

            new Val( 194, 110643012,  27686453, 110643012, null),
            new Val( 193, 112917220,  27686453, 112917220, null),
            new Val( 192,  70869366,  27686453,  70869366, null),
            new Val( 191,  27682735,  27686453,  27682735, null),
            new Val( 190, 319269351,  27686453, 319269351, "Sint Barbaradreef"),
            new Val( 189, 112917225,  27686453, 112917225, "Sint Barbaradreef"),

            new Val( 188, 112917225,  27686453, 847554912, null,
                "Sint Barbaradreef (3)",
                "3"),

            new Val( 187,  27686451, 847554912,  27686451, null),
            new Val( 186, 527248228, 847554912, 527248228, null),
            new Val( 185,  27682732, 847554912,  27682732, "Sint Barbaradreef"),
            new Val( 184, 319264895, 847554912, 319264895, "Ganzendries"),
            new Val( 183, 112917224, 847554912, 112917224, "Ganzendries"),

            new Val( 182, 112917224, 847554912,  40189518, "Lostraat",
                "Ganzendries - Sint Barbaradreef (3)",
                "3"),

            new Val( 181, 351196789,  40189518, 351196789, "Lostraat"),
            new Val( 180, 350992067,  40189518, 350992067, "Lostraat"),
            new Val( 179, 636491595,  40189518, 636491595, "Lostraat"),
            new Val( 178, 636491594,  40189518, 636491594, "Lostraat"),
            new Val( 177, 124913579,  40189518, 124913579, "Lostraat"),
            new Val( 176,  10672044,  40189518,  10672044, "Lostraat"),
            new Val( 175, 633919766,  40189518, 633919766, "Lostraat"),
            new Val( 174,  10672080,  40189518,  10672080, "Heidebergstraat"),
            new Val( 173,  10672083,  40189518,  10672083, "Heidebergstraat"),
            new Val( 172,  79801761,  40189518,  79801761, "Heidebergstraat"),
            new Val( 171, 112917249,  40189518, 112917249, "Heidebergstraat"),
            new Val( 170, 112917247,  40189518, 112917247, "Heidebergstraat"),
            new Val( 169, 192706264,  40189518, 192706264, "Heidebergstraat"),
            new Val( 168, 318216944,  40189518, 318216944, "Heidebergstraat"),
            new Val( 167, 192706265,  40189518, 192706265, "Heidebergstraat"),
            new Val( 166, 586861164,  40189518, 586861164, "Heidebergstraat"),
            new Val( 165,  10672031,  40189518,  10672031, "Heidebergstraat"),
            new Val( 164,  10672135,  40189518,  10672135, "Koetsweg"),
            new Val( 163, 608754691,  40189518, 608754691, "Koetsweg"),
            new Val( 162, 608754692,  40189518, 608754692, "Koetsweg"),
            new Val( 161,  10672174,  40189518,  10672174, "Gaston Eyskenslaan"),
            new Val( 160,  23436182,  40189518,  23436182, "Platte-Lostraat"),
            new Val( 159, 112917234,  40189518, 112917234, "Duivenstraat"),
            new Val( 158,  23707216,  40189518,  23707216, "Duivenstraat"),
            new Val( 157, 608754700,  40189518, 608754700, "Lijsterlaan"),
            new Val( 156,  10672177,  40189518,  10672177, "Lijsterlaan"),
            new Val( 155, 112917228,  40189518, 112917228, "Prins Regentplein"),
            new Val( 154, 112917229,  40189518, 112917229, "Prins Regentplein"),
            new Val( 153,  10672178,  40189518,  10672178, "Prins Regentplein"),
            new Val( 152,  10657957,  40189518,  10657957, "Prins-Regentlaan"),
            new Val( 151,  25155107,  40189518,  25155107, "Willem Coosemansstraat"),
            new Val( 150,  79784138,  40189518,  79784138, "Koning Albertlaan"),
            new Val( 149,  23707241,  40189518,  23707241, "Koning Albertlaan"),
            new Val( 148, 112917227,  40189518, 112917227, "Koning Albertlaan"),
            new Val( 147, 101619135,  40189518, 101619135, "Koning Albertlaan"),
            new Val( 146, 622063635,  40189518, 622063635, "Koning Albertlaan"),
            new Val( 145, 622063636,  40189518, 622063636, "Koning Albertlaan"),
            new Val( 144, 101619136,  40189518, 101619136, "Koning Albertlaan"),
            new Val( 143,  16377030,  40189518,  16377030, "Martelarenlaan"),
            new Val( 142, 485802425,  40189518, 485802425, "Spoordijk"),
            new Val( 141,  23707242,  40189518,  23707242, "Martelarenlaan"),
            new Val( 140, 608715582,  40189518, 608715582, "Martelarenlaan"),
            new Val( 139, 608715584,  40189518, 608715584, "Martelarenlaan"),
            new Val( 138, 284832275,  40189518, 284832275, "Martelarenlaan"),
            new Val( 137, 608715586,  40189518, 608715586, "Martelarenlaan"),
            new Val( 136, 608715590,  40189518, 608715590, "Martelarenlaan"),
            new Val( 135,   3992546,  40189518,   3992546, "Martelarenlaan"),
            new Val( 134,  10230617,  40189518,  10230617, "Oude Diestsesteenweg"),

            new Val( 133,  10230617,  40189518,  23707243, "Diestsesteenweg",
                "Oude Diestsesteenweg - Lostraat (3)",
                "3"),


            new Val( 132,  23707243,  23707243,  23707244, "Diestsesteenweg",
                "Diestsesteenweg (3;370;371;373;374;475;485;524;525)",
                "3;370;371;373;374;475;485;524;525"),

            new Val( 131,  12715116,  23707244,  12715116, "Diestsesteenweg"),

            new Val( 130,  12715116,  23707244,  61556877, "Diestsepoort",
                "Diestsesteenweg (2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525)",
                "2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525"),

            new Val( 129, 663770966,  61556877, 663770966, "Diestsepoort"),
            new Val( 128, 584356749,  61556877, 584356749, "Diestsepoort"),
            new Val( 127, 584356745,  61556877, 584356745, "Diestsepoort"),

            new Val( 126, 584356745,  61556877, 198559166, "Diestsepoort",
                "Diestsepoort (2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",
                "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658"),

            new Val( 125, 584356751, 198559166, 584356751, "Diestsepoort"),
            new Val( 124, 451873773, 198559166, 451873773, "Diestsepoort"),
            new Val( 123, 584356742, 198559166, 584356742, "Diestsepoort"),
            new Val( 122, 451873774, 198559166, 451873774, "Diestsepoort"),

            new Val( 121, 451873774, 198559166,  76867049, null,
                "Diestsepoort (2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",
                "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658"),

            new Val( 120,  79264890,  76867049,  79264890, null),
            new Val( 119,  79596965,  76867049,  79596965, null),

            new Val( 118,  79596965,  76867049,  79596974, null,
                "(2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",
                "2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630"),

            new Val( 117,  79596982,  79596974,  79596982, null),

            new Val( 116,  79596982,  79596974,  79596987, null,
                "perron 11 & 12 (3;333;334;335;433;600;630)",
                "3;333;334;335;433;600;630"),

            new Val( 115, 377918658,  79596987, 377918658, null),
            new Val( 114,  79596980,  79596987,  79596980, null),

            new Val( 113,  79596980,  79596987,  79193579, "Tiensevest",
                "perron 11 & 12 (3;333;334;335;513;600;630)",
                "3;333;334;335;513;600;630"),


            new Val( 112,  79193579,  79193579, 258936980, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658"),


            new Val( 111, 258936980, 258936980,  79193580, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658"),


            new Val( 110,  79193580,  79193580,  79193581, "Tiensevest",
                "Tiensevest (3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),


            new Val( 109,  79193581,  79193581,  78815505, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),


            new Val( 108,  78815505,  78815505,  84696751, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630"),

            new Val( 107,  79265237,  84696751,  79265237, "Tiensevest"),

            new Val( 106,  79265237,  84696751,  89574079, "Tiensevest",
                "Tiensevest (2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630)",
                "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630"),

            new Val( 105,  81522744,  89574079,  81522744, "Tiensevest"),

            new Val( 104,  81522744,  89574079, 305434579, "Bondgenotenlaan",
                "Tiensevest (1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630)",
                "1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630"),

            new Val( 103, 578662093, 305434579, 578662093, "Bondgenotenlaan"),
            new Val( 102, 578662092, 305434579, 578662092, "Bondgenotenlaan"),
            new Val( 101, 578662095, 305434579, 578662095, "Bondgenotenlaan"),
            new Val( 100, 578662094, 305434579, 578662094, "Bondgenotenlaan"),
            new Val( 99,   3991636, 305434579,   3991636, "Bondgenotenlaan"),
            new Val( 98, 174985125, 305434579, 174985125, "Rector De Somerplein"),
            new Val( 97, 521211977, 305434579, 521211977, "Rector De Somerplein"),

            new Val( 96, 521211977, 305434579, 521211976, "Rector De Somerplein",
                "Rector De Somerplein - Bondgenotenlaan (2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539)",
                "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539"),

            new Val( 95,  16771741, 521211976,  16771741, "Rector De Somerplein"),

            new Val( 94,  16771741, 521211976,   3991635, "Margarethaplein",
                "Rector De Somerplein (2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539)",
                "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539"),

            new Val( 93, 521211973,   3991635, 521211973, "Margarethaplein"),
            new Val( 92, 608715546,   3991635, 608715546, "Margarethaplein"),
            new Val( 91, 453538107,   3991635, 453538107, "Margarethaplein"),
            new Val( 90,  80194313,   3991635,  80194313, "Mathieu de Layensplein"),
            new Val( 89, 293288706,   3991635, 293288706, "Mathieu de Layensplein"),
            new Val( 88,  10269271,   3991635,  10269271, "Dirk Boutslaan"),
            new Val( 87, 578662072,   3991635, 578662072, "Dirk Boutslaan"),
            new Val( 86, 608715541,   3991635, 608715541, "Dirk Boutslaan"),
            new Val( 85, 293149632,   3991635, 293149632, "Dirk Boutslaan"),
            new Val( 84,   3992578,   3991635,   3992578, "Dirk Boutslaan"),
            new Val( 83, 438252643,   3991635, 438252643, "Dirk Boutslaan"),
            new Val( 82, 284664268,   3991635, 284664268, "Brouwersstraat"),

            new Val( 81, 284664268,   3991635, 608715545, "Brouwersstraat",
                "Brouwersstraat - Margarethaplein (3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 80, 608715543, 608715545, 608715543, "Brouwersstraat"),
            new Val( 79, 608715542, 608715545, 608715542, "Brouwersstraat"),
            new Val( 78, 608715544, 608715545, 608715544, "Brouwersstraat"),
            new Val( 77, 284664272, 608715545, 284664272, "Brouwersstraat"),

            new Val( 76, 284664272, 608715545, 147856945, "Tessenstraat - Fonteinstraat",
                "Brouwersstraat (3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 75, 123929547, 147856945, 123929547, "Kapucijnenvoer"),

            new Val( 74, 123929547, 147856945,   3358673, "Biezenstraat",
                "Kapucijnenvoer - Tessenstraat - Fonteinstraat (3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537)",
                "3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537"),

            new Val( 73, 578662071,   3358673, 578662071, "Sint-Jacobsplein"),
            new Val( 72, 521211966,   3358673, 521211966, "Sint-Jacobsplein"),
            new Val( 71,   3358671,   3358673,   3358671, "Sint-Jacobsplein"),
            new Val( 70, 123929615,   3358673, 123929615, "Sint-Hubertusstraat"),

            new Val( 69, 123929615,   3358673, 189453003, "Monseigneur Van Waeyenberghlaan",
                "Sint-Hubertusstraat - Biezenstraat (3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513)",
                "3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513"),

            new Val( 68,  79289753, 189453003,  79289753, "Monseigneur Van Waeyenberghlaan"),
            new Val( 67, 189453004, 189453003, 189453004, "Monseigneur Van Waeyenberghlaan"),
            new Val( 66, 189453002, 189453003, 189453002, "Monseigneur Van Waeyenberghlaan"),
            new Val( 65, 189453001, 189453003, 189453001, "Monseigneur Van Waeyenberghlaan"),
            new Val( 64, 810592121, 189453003, 810592121, "Monseigneur Van Waeyenberghlaan"),
            new Val( 63, 249333181, 189453003, 249333181, "Monseigneur Van Waeyenberghlaan"),

            new Val( 62, 249333181, 189453003, 249333187, "Rotonde Het Teken",
                "Monseigneur Van Waeyenberghlaan (3;317;333;334;335;370;371;373;374;380;395;513)",
                "3;317;333;334;335;370;371;373;374;380;395;513"),


            new Val( 61, 249333187, 249333187, 813970230, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 60,  41403540, 813970230,  41403540, "Rotonde Het Teken"),

            new Val( 59,  41403540, 813970230,  41403538, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;513)",
                "3;317;333;334;335;370;371;373;374;380;395;513"),

            new Val( 58,  79340950,  41403538,  79340950, "Ring Zuid"),

            new Val( 57,  79340950,  41403538,  11369123, "Ring Zuid",
                "Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 56,  11369123,  11369123, 159949154, null,
                "Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 55, 159949154, 159949154, 332258104, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 54,  78852604, 332258104,  78852604, null),

            new Val( 53,  78852604, 332258104,  14508735, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 52,  14508735,  14508735, 109267438, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600"),


            new Val( 51, 109267438, 109267438, 318878532, null,
                "(3;333;334;335;370;371;373;374;380;513)",
                "3;333;334;335;370;371;373;374;380;513"),


            new Val( 50, 318878532, 318878532, 100687528, null,
                "(3;333;334;335;513)",
                "3;333;334;335;513"),


            new Val( 49, 100687528, 100687528,  14508739, "Ring Zuid",
                "(3;334;335)",
                "3;334;335"),


            new Val( 48,  14508739,  14508739,  14508740, "Ring Zuid",
                "Ring Zuid (3;317;334;335;395;410;600;601)",
                "3;317;334;335;395;410;600;601"),


            new Val( 47,  14508740,  14508740, 502328838, "Ring Zuid",
                "Ring Zuid (3;317;334;335;380;395;410;600;601)",
                "3;317;334;335;380;395;410;600;601"),

            new Val( 46, 502328837, 502328838, 502328837, "Ring Zuid"),
            new Val( 45,   8080023, 502328838,   8080023, "Ring Zuid"),
            new Val( 44,  78568660, 502328838,  78568660, "Rotonde Het Teken"),

            new Val( 43,  78568660, 502328838,  15945426, "Ring Noord",
                "Rotonde Het Teken - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),

            new Val( 42, 502317795,  15945426, 502317795, "Ring Noord"),
            new Val( 41, 810580948,  15945426, 810580948, "Ring Noord"),
            new Val( 40, 502317796,  15945426, 502317796, "Ring Noord"),
            new Val( 39, 112917238,  15945426, 112917238, "Ring Noord"),
            new Val( 38, 608754690,  15945426, 608754690, "Ring Noord"),
            new Val( 37, 377918625,  15945426, 377918625, "Ring Noord"),

            new Val( 36, 377918625,  15945426, 377918625, "Ring Noord",
                "Ring Noord (3)",
                "3")
        );        previousSegment = new RouteSegmentToExtract(bus3_GHB_Pellenberg_Lubbeek_RouteRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, bus3_GHB_Pellenberg_Lubbeek_RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }

        System.out.print("\n");

        // ***********************************************************
        // Line 3 has the particularity that there are 2 variants
        // The longer version has a spoon to serve another hospital
        // This is testing the longer version completely
        // From Lubbeek, over Pellenberg to Leuven Gasthuisberg
        // ***********************************************************

        Relation bus3_Lubbeek_Pellenberg_GHB_RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 1583303)
            .findFirst().orElse(null);

        assertNotNull(bus3_Lubbeek_Pellenberg_GHB_RouteRelation);
        RouteSegmentToExtract segment401 = new RouteSegmentToExtract(bus3_Lubbeek_Pellenberg_GHB_RouteRelation);
        segment401.setActiveDataSet(ds);

        assertEquals(bus3_Lubbeek_Pellenberg_GHB_RouteRelation.get("ref"), segment401.getLineIdentifiersSignature());
        assertEquals(bus3_Lubbeek_Pellenberg_GHB_RouteRelation.get("colour"), segment401.getColoursSignature());

        assertNull(segment401.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment401.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment401.getWayMembers());

        expectedValues = Arrays.asList(
            new Val( 221, 377918626, 377918626, 377918626, "Ring Noord"),
            new Val( 220, 243915691, 377918626, 243915691, "Ring Noord"),
            new Val( 219, 126304975, 377918626, 126304975, null),
            new Val( 218, 326736776, 377918626, 326736776, null),
            new Val( 217, 333580518, 377918626, 333580518, null),
            new Val( 216, 608754690, 377918626, 608754690, "Ring Noord"),
            new Val( 215, 112917238, 377918626, 112917238, "Ring Noord"),
            new Val( 214, 502317797, 377918626, 502317797, "Ring Noord"),
            new Val( 213, 112917239, 377918626, 112917239, "Ring Noord"),

            new Val( 212, 112917239, 377918626,   3752557, "Rotonde Het Teken",
                "Ring Noord (3)",
                "3"),


            new Val( 211,   3752557,   3752557, 249333188, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 210, 249333188, 249333188, 249333187, "Rotonde Het Teken",
                "Rotonde Het Teken (3;410;600;601)",
                "3;410;600;601"),


            new Val( 209, 249333187, 249333187, 813970230, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 208,  41403540, 813970230,  41403540, "Rotonde Het Teken"),

            new Val( 207,  41403540, 813970230,  41403538, "Rotonde Het Teken",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;513)",
                "3;317;333;334;335;370;371;373;374;380;395;513"),

            new Val( 206,  79340950,  41403538,  79340950, "Ring Zuid"),

            new Val( 205,  79340950,  41403538,  11369123, "Ring Zuid",
                "Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 204,  11369123,  11369123, 159949154, null,
                "Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),


            new Val( 203, 159949154, 159949154, 332258104, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 202,  78852604, 332258104,  78852604, null),

            new Val( 201,  78852604, 332258104, 377918641, null,
                "(3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"),

            new Val( 200,  14508736, 377918641,  14508736, null),

            new Val( 199,  14508736, 377918641, 109267436, "Ring Zuid",
                "(3;317;395;410;601)",
                "3;317;395;410;601"),


            new Val( 198, 109267436, 109267436,  14508739, "Ring Zuid",
                "Ring Zuid (3;317;395;410;600;601)",
                "3;317;395;410;600;601"),


            new Val( 197,  14508739,  14508739,  14508740, "Ring Zuid",
                "Ring Zuid (3;317;334;335;395;410;600;601)",
                "3;317;334;335;395;410;600;601"),


            new Val( 196,  14508740,  14508740, 502328838, "Ring Zuid",
                "Ring Zuid (3;317;334;335;380;395;410;600;601)",
                "3;317;334;335;380;395;410;600;601"),

            new Val( 195, 502328837, 502328838, 502328837, "Ring Zuid"),
            new Val( 194,   8080023, 502328838,   8080023, "Ring Zuid"),
            new Val( 193,  78568660, 502328838,  78568660, "Rotonde Het Teken"),
            new Val( 192,  78873921, 502328838,  78873921, "Rotonde Het Teken"),

            new Val( 191,  78873921, 502328838,   3752557, "Rotonde Het Teken",
                "Rotonde Het Teken - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),


            new Val( 190,   3752557,   3752557, 249333183, "Monseigneur Van Waeyenberghlaan",
                "Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",
                "3;317;333;334;335;370;371;373;374;380;395;410;600;601"),

            new Val( 189, 810592121, 249333183, 810592121, "Monseigneur Van Waeyenberghlaan"),
            new Val( 188, 189453001, 249333183, 189453001, "Monseigneur Van Waeyenberghlaan"),
            new Val( 187, 189453002, 249333183, 189453002, "Monseigneur Van Waeyenberghlaan"),
            new Val( 186, 189453004, 249333183, 189453004, "Monseigneur Van Waeyenberghlaan"),
            new Val( 185,  79289753, 249333183,  79289753, "Monseigneur Van Waeyenberghlaan"),
            new Val( 184, 189453003, 249333183, 189453003, "Monseigneur Van Waeyenberghlaan"),

            new Val( 183, 189453003, 249333183, 123929615, "Sint-Hubertusstraat",
                "Monseigneur Van Waeyenberghlaan (3;317;333;334;335;370;371;373;374;380;395)",
                "3;317;333;334;335;370;371;373;374;380;395"),

            new Val( 182,   3358671, 123929615,   3358671, "Sint-Jacobsplein"),
            new Val( 181, 521211966, 123929615, 521211966, "Sint-Jacobsplein"),
            new Val( 180, 578662071, 123929615, 578662071, "Sint-Jacobsplein"),
            new Val( 179,   3358673, 123929615,   3358673, "Biezenstraat"),

            new Val( 178,   3358673, 123929615, 123929547, "Kapucijnenvoer",
                "Biezenstraat - Sint-Hubertusstraat (3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395)",
                "3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395"),

            new Val( 177, 147856945, 123929547, 147856945, "Tessenstraat - Fonteinstraat"),

            new Val( 176, 147856945, 123929547, 284664272, "Brouwersstraat",
                "Tessenstraat - Fonteinstraat - Kapucijnenvoer (3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586)",
                "3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586"),

            new Val( 175, 608715544, 284664272, 608715544, "Brouwersstraat"),
            new Val( 174, 608715542, 284664272, 608715542, "Brouwersstraat"),
            new Val( 173, 608715543, 284664272, 608715543, "Brouwersstraat"),
            new Val( 172, 608715545, 284664272, 608715545, "Brouwersstraat"),

            new Val( 171, 608715545, 284664272, 284664268, "Brouwersstraat",
                "Brouwersstraat (3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586)",
                "3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586"),

            new Val( 170, 438252643, 284664268, 438252643, "Dirk Boutslaan"),
            new Val( 169,   3992578, 284664268,   3992578, "Dirk Boutslaan"),
            new Val( 168, 293149632, 284664268, 293149632, "Dirk Boutslaan"),
            new Val( 167, 608715541, 284664268, 608715541, "Dirk Boutslaan"),
            new Val( 166, 578662072, 284664268, 578662072, "Dirk Boutslaan"),
            new Val( 165,  10269271, 284664268,  10269271, "Dirk Boutslaan"),
            new Val( 164, 293288706, 284664268, 293288706, "Mathieu de Layensplein"),
            new Val( 163,  80194313, 284664268,  80194313, "Mathieu de Layensplein"),
            new Val( 162, 453538107, 284664268, 453538107, "Margarethaplein"),
            new Val( 161, 608715546, 284664268, 608715546, "Margarethaplein"),
            new Val( 160, 521211973, 284664268, 521211973, "Margarethaplein"),
            new Val( 159,   3991635, 284664268,   3991635, "Margarethaplein"),

            new Val( 158,   3991635, 284664268,  16771741, "Rector De Somerplein",
                "Margarethaplein - Brouwersstraat (3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586)",
                "3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586"),

            new Val( 157, 521211976,  16771741, 521211976, "Rector De Somerplein"),
            new Val( 156, 521211977,  16771741, 521211977, "Rector De Somerplein"),
            new Val( 155, 174985125,  16771741, 174985125, "Rector De Somerplein"),
            new Val( 154,   3991636,  16771741,   3991636, "Bondgenotenlaan"),
            new Val( 153, 578662094,  16771741, 578662094, "Bondgenotenlaan"),
            new Val( 152, 578662095,  16771741, 578662095, "Bondgenotenlaan"),
            new Val( 151, 578662092,  16771741, 578662092, "Bondgenotenlaan"),
            new Val( 150, 578662093,  16771741, 578662093, "Bondgenotenlaan"),
            new Val( 149, 305434579,  16771741, 305434579, "Bondgenotenlaan"),

            new Val( 148, 305434579,  16771741,  81522744, "Tiensevest",
                "Bondgenotenlaan - Rector De Somerplein (2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586)",
                "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586"),

            new Val( 147,  89574079,  81522744,  89574079, "Tiensevest"),

            new Val( 146,  89574079,  81522744,  79265237, "Tiensevest",
                "Tiensevest (1;2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586;616)",
                "1;2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586;616"),


            new Val( 145,  79265237,  79265237,  79211473, "Tiensevest",
                "Tiensevest (1;2;3;4;5;6;7;8;9;284;285;315;316;317;351;352;358;395;520;524;525;537;539;586;616)",
                "1;2;3;4;5;6;7;8;9;284;285;315;316;317;351;352;358;395;520;524;525;537;539;586;616"),


            new Val( 144,  79211473,  79211473,  79605527, null,
                "Tiensevest (1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658)",
                "1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658"),

            new Val( 143,  79605524,  79605527,  79605524, null),
            new Val( 142, 377918661,  79605527, 377918661, null),
            new Val( 141,  79596984,  79605527,  79596984, null),
            new Val( 140, 377918662,  79605527, 377918662, null),
            new Val( 139,  79596971,  79605527,  79596971, null),

            new Val( 138,  79596971,  79605527, 377918635, null,
                "perron 3 & 4 (3;4;5;6;7;8;9)",
                "3;4;5;6;7;8;9"),


            new Val( 137, 377918635, 377918635,  79264888, null,
                "(3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601)",
                "3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601"),


            new Val( 136,  79264888,  79264888,  79264897, null,
                "(2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601)",
                "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601"),

            new Val( 135,  71754927,  79264897,  71754927, null),

            new Val( 134,  71754927,  79264897,  79596965, null,
                "(2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601)",
                "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601"),

            new Val( 133,  79264890,  79596965,  79264890, null),
            new Val( 132,  76867049,  79596965,  76867049, null),

            new Val( 131,  76867049,  79596965, 451873774, "Diestsepoort",
                "(2;3;4;5;6;7;8;9;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525)",
                "2;3;4;5;6;7;8;9;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525"),

            new Val( 130, 584356742, 451873774, 584356742, "Diestsepoort"),

            new Val( 129, 584356742, 451873774, 451873773, "Diestsepoort",
                "Diestsepoort (2;3;178;179;306;306 (student);310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525)",
                "2;3;178;179;306;306 (student);310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525"),

            new Val( 128, 584356751, 451873773, 584356751, "Diestsepoort"),
            new Val( 127, 198559166, 451873773, 198559166, "Diestsepoort"),
            new Val( 126, 584356745, 451873773, 584356745, "Diestsepoort"),
            new Val( 125, 584356749, 451873773, 584356749, "Diestsepoort"),
            new Val( 124, 663770966, 451873773, 663770966, "Diestsepoort"),
            new Val( 123,  61556877, 451873773,  61556877, "Diestsepoort"),

            new Val( 122,  61556877, 451873773,   8109264, "Diestsepoort",
                "Diestsepoort (2;3;178;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525)",
                "2;3;178;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525"),

            new Val( 121, 125835524,   8109264, 125835524, "Diestsesteenweg"),
            new Val( 120,  23707244,   8109264,  23707244, "Diestsesteenweg"),

            new Val( 119,  23707244,   8109264,  23707243, "Diestsesteenweg",
                "Diestsesteenweg - Diestsepoort (2;3;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525)",
                "2;3;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525"),


            new Val( 118,  23707243,  23707243,  10230617, "Oude Diestsesteenweg",
                "Diestsesteenweg (3;370;371;373;374;475;485;524;525)",
                "3;370;371;373;374;475;485;524;525"),

            new Val( 117,   3992546,  10230617,   3992546, "Martelarenlaan"),
            new Val( 116, 608715590,  10230617, 608715590, "Martelarenlaan"),
            new Val( 115, 608715586,  10230617, 608715586, "Martelarenlaan"),
            new Val( 114, 284832275,  10230617, 284832275, "Martelarenlaan"),
            new Val( 113, 608715584,  10230617, 608715584, "Martelarenlaan"),
            new Val( 112, 608715582,  10230617, 608715582, "Martelarenlaan"),
            new Val( 111,  23707242,  10230617,  23707242, "Martelarenlaan"),
            new Val( 110, 485802425,  10230617, 485802425, "Spoordijk"),
            new Val( 109,  16377030,  10230617,  16377030, "Martelarenlaan"),
            new Val( 108, 101619136,  10230617, 101619136, "Koning Albertlaan"),
            new Val( 107, 622063636,  10230617, 622063636, "Koning Albertlaan"),
            new Val( 106, 622063635,  10230617, 622063635, "Koning Albertlaan"),
            new Val( 105, 101619135,  10230617, 101619135, "Koning Albertlaan"),
            new Val( 104,  12195285,  10230617,  12195285, "Koning Albertlaan"),
            new Val( 103,  25155107,  10230617,  25155107, "Willem Coosemansstraat"),
            new Val( 102,  10657957,  10230617,  10657957, "Prins-Regentlaan"),
            new Val( 101,  10672178,  10230617,  10672178, "Prins Regentplein"),
            new Val( 100, 112917229,  10230617, 112917229, "Prins Regentplein"),
            new Val( 99, 112917228,  10230617, 112917228, "Prins Regentplein"),
            new Val( 98,  10672177,  10230617,  10672177, "Lijsterlaan"),
            new Val( 97, 608754700,  10230617, 608754700, "Lijsterlaan"),
            new Val( 96, 112917233,  10230617, 112917233, "Lijsterlaan"),
            new Val( 95, 608754699,  10230617, 608754699, "Lijsterlaan"),
            new Val( 94,  10672176,  10230617,  10672176, "Duivenstraat"),
            new Val( 93, 112917234,  10230617, 112917234, "Duivenstraat"),
            new Val( 92,  23436182,  10230617,  23436182, "Platte-Lostraat"),
            new Val( 91,  10672174,  10230617,  10672174, "Gaston Eyskenslaan"),
            new Val( 90, 608754692,  10230617, 608754692, "Koetsweg"),
            new Val( 89, 608754691,  10230617, 608754691, "Koetsweg"),
            new Val( 88,  10672135,  10230617,  10672135, "Koetsweg"),
            new Val( 87,  10672031,  10230617,  10672031, "Heidebergstraat"),
            new Val( 86, 586861164,  10230617, 586861164, "Heidebergstraat"),
            new Val( 85, 192706265,  10230617, 192706265, "Heidebergstraat"),
            new Val( 84, 318216944,  10230617, 318216944, "Heidebergstraat"),
            new Val( 83, 192706264,  10230617, 192706264, "Heidebergstraat"),
            new Val( 82, 112917248,  10230617, 112917248, "Heidebergstraat"),
            new Val( 81,  79801760,  10230617,  79801760, "Heidebergstraat"),
            new Val( 80,  61596758,  10230617,  61596758, "Heidebergstraat"),
            new Val( 79,  10672083,  10230617,  10672083, "Heidebergstraat"),
            new Val( 78,  10672080,  10230617,  10672080, "Heidebergstraat"),
            new Val( 77, 633919766,  10230617, 633919766, "Lostraat"),
            new Val( 76,  10672044,  10230617,  10672044, "Lostraat"),
            new Val( 75, 124913579,  10230617, 124913579, "Lostraat"),
            new Val( 74, 636491594,  10230617, 636491594, "Lostraat"),
            new Val( 73, 636491595,  10230617, 636491595, "Lostraat"),
            new Val( 72, 350992067,  10230617, 350992067, "Lostraat"),
            new Val( 71, 351196789,  10230617, 351196789, "Lostraat"),
            new Val( 70,  40189518,  10230617,  40189518, "Lostraat"),

            new Val( 69,  40189518,  10230617, 112917224, "Ganzendries",
                "Lostraat - Oude Diestsesteenweg (3)",
                "3"),

            new Val( 68, 319264895, 112917224, 319264895, "Ganzendries"),
            new Val( 67,  27682732, 112917224,  27682732, "Sint Barbaradreef"),
            new Val( 66, 527248228, 112917224, 527248228, null),
            new Val( 65,  27686451, 112917224,  27686451, null),
            new Val( 64, 847554912, 112917224, 847554912, null),

            new Val( 63, 847554912, 112917224,  27686453, null,
                "Sint Barbaradreef - Ganzendries (3)",
                "3"),

            new Val( 62, 110643012,  27686453, 110643012, null),
            new Val( 61, 112917220,  27686453, 112917220, null),
            new Val( 60,  70869366,  27686453,  70869366, null),
            new Val( 59,  27682735,  27686453,  27682735, null),
            new Val( 58, 319269351,  27686453, 319269351, "Sint Barbaradreef"),
            new Val( 57, 112917225,  27686453, 112917225, "Sint Barbaradreef"),

            new Val( 56, 112917225,  27686453, 847554912, null,
                "Sint Barbaradreef (3)",
                "3"),

            new Val( 55,  27686451, 847554912,  27686451, null),
            new Val( 54, 527248228, 847554912, 527248228, null),
            new Val( 53,  27682732, 847554912,  27682732, "Sint Barbaradreef"),
            new Val( 52, 319264895, 847554912, 319264895, "Ganzendries"),
            new Val( 51, 112917224, 847554912, 112917224, "Ganzendries"),

            new Val( 50, 112917224, 847554912,  16377612, "Kapelstraat",
                "Ganzendries - Sint Barbaradreef (3)",
                "3"),

            new Val( 49,  16377722,  16377612,  16377722, "Kapelstraat"),
            new Val( 48, 636950670,  16377612, 636950670, "Kapelstraat"),
            new Val( 47, 319269331,  16377612, 319269331, "Kapelstraat"),
            new Val( 46,  10658839,  16377612,  10658839, "Lubbeekstraat"),
            new Val( 45, 232474772,  16377612, 232474772, "Lubbeekstraat"),
            new Val( 44,  27688813,  16377612,  27688813, "Bollenberg"),
            new Val( 43, 645048305,  16377612, 645048305, "Bollenberg"),
            new Val( 42, 694551612,  16377612, 694551612, "Bollenberg"),
            new Val( 41, 112917100,  16377612, 112917100, "Bollenberg"),
            new Val( 40,  81197019,  16377612,  81197019, "Bollenberg"),

            new Val( 39,  81197019,  16377612, 125835541, "Dorpskring",
                "Bollenberg - Kapelstraat (3)",
                "3"),


            new Val( 38, 125835541, 125835541,  81083332, "Dorpskring",
                "Dorpskring (3;373;485)",
                "3;373;485"),

            new Val( 37, 125835543,  81083332, 125835543, "Dorpskring"),

            new Val( 36, 125835543,  81083332, 608754701, "Dorpskring",
                "Dorpskring (3;373;485)",
                "3;373;485"),


            new Val( 35, 608754701,  608754701, 608754701, "Dorpskring",
                "Dorpskring (3;373;485)",
                "3;373;485")
        );
        previousSegment = new RouteSegmentToExtract(bus3_Lubbeek_Pellenberg_GHB_RouteRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, bus3_Lubbeek_Pellenberg_GHB_RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }

        System.out.print("\n");

        // ***********************************************************
        // Line 310 has 3 variants
        // The shorter version goes from Leuven station to
        // Aarschot station, making a spoon loop in Holsbeek
        // ***********************************************************

        Relation bus310_Leuven_Aarschot_Station_RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3297278)
            .findFirst().orElse(null);

        assertNotNull(bus310_Leuven_Aarschot_Station_RouteRelation);
        RouteSegmentToExtract segment501 = new RouteSegmentToExtract(bus310_Leuven_Aarschot_Station_RouteRelation);
        segment501.setActiveDataSet(ds);

        assertEquals(bus310_Leuven_Aarschot_Station_RouteRelation.get("ref"), segment501.getLineIdentifiersSignature());
        assertEquals(bus310_Leuven_Aarschot_Station_RouteRelation.get("colour"), segment501.getColoursSignature());

        assertNull(segment501.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment501.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment501.getWayMembers());

        expectedValues = Arrays.asList(
            new Val( 213,  13856192,  13856192,  13856192, null),
            new Val( 212, 100289896,  13856192, 100289896, "Statieplein"),

            new Val( 211, 100289896,  13856192, 100289859, "Statieplein",
                "Statieplein (310;334;335)",
                "310;334;335"),

            new Val( 210, 100289892, 100289859, 100289892, "Statieplein"),
            new Val( 209, 100289852, 100289859, 100289852, "Statieplein"),
            new Val( 208, 125101590, 100289859, 125101590, "Statieplein"),
            new Val( 207, 101376793, 100289859, 101376793, "Statiestraat"),
            new Val( 206,  41405911, 100289859,  41405911, "Statiestraat"),

            new Val( 205,  41405911, 100289859, 125101554, "Albertlaan",
                "Statiestraat - Statieplein (305;306;306 (student);310;334;335)",
                "305;306;306 (student);310;334;335"),


            new Val( 204, 125101554, 125101554,  13892080, "Albertlaan",
                "Albertlaan (305;306;306 (student);310;334;335;513)",
                "305;306;306 (student);310;334;335;513"),

            new Val( 203, 412810122,  13892080, 412810122, "Leuvensesteenweg"),
            new Val( 202, 125101584,  13892080, 125101584, "Leuvensesteenweg"),

            new Val( 201, 125101584,  13892080, 188863432, "Leuvensesteenweg",
                "Leuvensesteenweg - Albertlaan (310;334;335;513)",
                "310;334;335;513"),

            new Val( 200, 125101583, 188863432, 125101583, "Leuvensesteenweg"),

            new Val( 199, 125101583, 188863432, 345152127, "Steenweg op Sint-Joris-Winge",
                "Leuvensesteenweg (305;306;306 (student);310;334;335;513)",
                "305;306;306 (student);310;334;335;513"),

            new Val( 198, 412810127, 345152127, 412810127, "Steenweg op Sint-Joris-Winge"),
            new Val( 197, 345152132, 345152127, 345152132, "Steenweg op Sint-Joris-Winge"),
            new Val( 196, 602060323, 345152127, 602060323, "Steenweg op Sint-Joris-Winge"),
            new Val( 195, 181252214, 345152127, 181252214, "Steenweg op Sint-Joris-Winge"),
            new Val( 194, 102853195, 345152127, 102853195, "Steenweg op Sint-Joris-Winge"),
            new Val( 193, 345152123, 345152127, 345152123, "Steenweg op Sint-Joris-Winge"),
            new Val( 192, 345088986, 345152127, 345088986, "Steenweg op Sint-Joris-Winge"),
            new Val( 191, 112565995, 345152127, 112565995, "Steenweg op Sint-Joris-Winge"),
            new Val( 190, 102853233, 345152127, 102853233, null),
            new Val( 189, 345088979, 345152127, 345088979, "Steenweg op Sint-Joris-Winge"),
            new Val( 188, 345088980, 345152127, 345088980, "Steenweg op Sint-Joris-Winge"),
            new Val( 187,  13226856, 345152127,  13226856, "Steenweg op Sint-Joris-Winge"),
            new Val( 186, 137111763, 345152127, 137111763, "Steenweg op Sint-Joris-Winge"),
            new Val( 185, 521257383, 345152127, 521257383, "Steenweg op Sint-Joris-Winge"),
            new Val( 184, 521257382, 345152127, 521257382, "Steenweg op Sint-Joris-Winge"),
            new Val( 183,  26431904, 345152127,  26431904, "Nieuwrodesesteenweg"),
            new Val( 182, 345078866, 345152127, 345078866, "Nieuwrodesesteenweg"),
            new Val( 181, 345078870, 345152127, 345078870, "Steenweg op Sint-Joris-Winge"),
            new Val( 180, 345078865, 345152127, 345078865, "Steenweg op Sint-Joris-Winge"),
            new Val( 179, 136845549, 345152127, 136845549, "Nieuwrodesesteenweg"),
            new Val( 178,  22870199, 345152127,  22870199, "Rijksweg"),
            new Val( 177, 345078875, 345152127, 345078875, "Rijksweg"),
            new Val( 176,  13856508, 345152127,  13856508, "Rijksweg Aarschot-Winge"),

            new Val( 175,  13856508, 345152127, 345078877, "Rijksweg",
                "Rijksweg Aarschot-Winge - Steenweg op Sint-Joris-Winge (305;306;306 (student);310)",
                "305;306;306 (student);310"),

            new Val( 174, 345078878, 345078877, 345078878, "Rijksweg"),
            new Val( 173,  38208830, 345078877,  38208830, "Rijksweg"),
            new Val( 172,  38208831, 345078877,  38208831, "Rijksweg"),
            new Val( 171, 609510456, 345078877, 609510456, "Rijksweg"),
            new Val( 170, 609510455, 345078877, 609510455, "Rijksweg"),
            new Val( 169, 151083447, 345078877, 151083447, "Sint-Lambertusstraat"),
            new Val( 168, 137613438, 345078877, 137613438, "Sint-Lambertusstraat"),
            new Val( 167, 187995152, 345078877, 187995152, "Dorp"),
            new Val( 166,  10242649, 345078877,  10242649, "Dorp"),
            new Val( 165, 137613417, 345078877, 137613417, "Appelweg"),
            new Val( 164, 112565994, 345078877, 112565994, "Appelweg"),
            new Val( 163,  10242646, 345078877,  10242646, "Rodestraat"),
            new Val( 162, 615260297, 345078877, 615260297, "Rodestraat"),
            new Val( 161, 137613436, 345078877, 137613436, "Rodestraat"),
            new Val( 160, 318267938, 345078877, 318267938, "Rodestraat"),
            new Val( 159, 318267937, 345078877, 318267937, "Rodestraat"),
            new Val( 158, 117329495, 345078877, 117329495, "Rodestraat"),
            new Val( 157, 182275361, 345078877, 182275361, "Rodestraat"),
            new Val( 156, 182275362, 345078877, 182275362, "Rodestraat"),
            new Val( 155, 118479385, 345078877, 118479385, "Rodestraat"),
            new Val( 154,  13858504, 345078877,  13858504, "Rodestraat"),
            new Val( 153, 137613435, 345078877, 137613435, "Rodestraat"),
            new Val( 152,  23334102, 345078877,  23334102, "Gravenstraat"),
            new Val( 151,  13858501, 345078877,  13858501, "Gravenstraat"),
            new Val( 150,  38501970, 345078877,  38501970, "Gravenstraat"),
            new Val( 149,  13858502, 345078877,  13858502, "Gravenstraat"),
            new Val( 148,  13858500, 345078877,  13858500, "Gravenstraat"),
            new Val( 147, 182275357, 345078877, 182275357, "Gravenstraat"),
            new Val( 146, 540115503, 345078877, 540115503, "Gravenstraat"),
            new Val( 145,  13858519, 345078877,  13858519, "Dutselstraat"),
            new Val( 144, 185929630, 345078877, 185929630, "Dutselstraat"),
            new Val( 143, 137613419, 345078877, 137613419, "Dutselstraat"),
            new Val( 142, 185929632, 345078877, 185929632, "Dutselstraat"),
            new Val( 141, 185929631, 345078877, 185929631, "Dutselstraat"),
            new Val( 140, 310306260, 345078877, 310306260, "Dutselstraat"),
            new Val( 139,  22724214, 345078877,  22724214, "Dutselstraat"),
            new Val( 138, 310306259, 345078877, 310306259, "Dutselstraat"),
            new Val( 137, 310306431, 345078877, 310306431, "Kortrijksebaan"),
            new Val( 136,  38502235, 345078877,  38502235, "Kortrijksebaan"),
            new Val( 135, 659289513, 345078877, 659289513, "Kortrijksebaan"),
            new Val( 134, 441280731, 345078877, 441280731, "Kortrijksebaan"),
            new Val( 133, 659289514, 345078877, 659289514, "Kortrijksebaan"),
            new Val( 132,  19112638, 345078877,  19112638, "Kortrijksebaan"),
            new Val( 131, 310435230, 345078877, 310435230, "Kortrijksebaan"),
            new Val( 130,  23957648, 345078877,  23957648, "Kortrijksebaan"),
            new Val( 129, 173566709, 345078877, 173566709, "Kortrijksebaan"),
            new Val( 128,  13858520, 345078877,  13858520, "Kortrijksebaan"),
            new Val( 127, 181347876, 345078877, 181347876, "Kortrijksebaan"),
            new Val( 126,  19166177, 345078877,  19166177, "Kortrijksebaan"),
            new Val( 125, 310435229, 345078877, 310435229, "Kortrijksebaan"),
            new Val( 124, 450119553, 345078877, 450119553, "Kortrijksebaan"),
            new Val( 123, 181347873, 345078877, 181347873, "Nobelberg"),
            new Val( 122,  16302925, 345078877,  16302925, "Nobelberg"),

            new Val( 121,  16302925, 345078877,  64487963, "Rotselaarsebaan",
                "Nobelberg - Rijksweg (310)",
                "310"),

            new Val( 120, 310435233,  64487963, 310435233, "Rotselaarsebaan"),

            new Val( 119, 310435233,  64487963, 112566004, "Sint-Maurusstraat",
                "Rotselaarsebaan (310)",
                "310"),

            new Val( 118,  16302928, 112566004,  16302928, null),
            new Val( 117,  25667798, 112566004,  25667798, "Rotselaarsebaan"),

            new Val( 116,  25667798, 112566004, 310435233, "Rotselaarsebaan",
                "Rotselaarsebaan - Sint-Maurusstraat (310)",
                "310"),

            new Val( 115,  64487963, 310435233,  64487963, "Rotselaarsebaan"),

            new Val( 114,  64487963, 310435233, 310435232, "Nobelberg",
                "Rotselaarsebaan (310)",
                "310"),

            new Val( 113,  19166179, 310435232,  19166179, "Nobelberg"),
            new Val( 112, 492765776, 310435232, 492765776, "Nobelberg"),
            new Val( 111, 310461931, 310435232, 310461931, "Nobelberg"),
            new Val( 110,  37370702, 310435232,  37370702, "Nobelberg"),
            new Val( 109,  16302927, 310435232,  16302927, "Leuvensebaan"),
            new Val( 108,  16302926, 310435232,  16302926, "Leuvensebaan"),
            new Val( 107, 440732697, 310435232, 440732697, "Leuvensebaan"),
            new Val( 106, 311007037, 310435232, 311007037, "Leuvensebaan"),
            new Val( 105, 311007042, 310435232, 311007042, "Leuvensebaan"),
            new Val( 104, 311007035, 310435232, 311007035, "Leuvensebaan"),
            new Val( 103, 311007040, 310435232, 311007040, "Leuvensebaan"),
            new Val( 102, 192559618, 310435232, 192559618, "Leuvensebaan"),
            new Val( 101, 311007036, 310435232, 311007036, "Leuvensebaan"),
            new Val( 100, 311007033, 310435232, 311007033, "Leuvensebaan"),
            new Val( 99, 311007041, 310435232, 311007041, "Leuvensebaan"),
            new Val( 98, 311007034, 310435232, 311007034, "Leuvensebaan"),
            new Val( 97, 311007039, 310435232, 311007039, "Leuvensebaan"),
            new Val( 96, 311007038, 310435232, 311007038, "Leuvensebaan"),
            new Val( 95,  10699037, 310435232,  10699037, "Leuvensebaan"),
            new Val( 94,  14393833, 310435232,  14393833, "Leuvensebaan"),
            new Val( 93,  18943297, 310435232,  18943297, "Wilselsesteenweg"),
            new Val( 92, 192559637, 310435232, 192559637, "Wilselsesteenweg"),
            new Val( 91, 114935420, 310435232, 114935420, "Wilselsesteenweg"),
            new Val( 90, 243884410, 310435232, 243884410, "Wilselsesteenweg"),

            new Val( 89, 243884410, 310435232,  98645243, "Wilselsesteenweg",
                "Wilselsesteenweg - Nobelberg (310)",
                "310"),

            new Val( 88,  98645223,  98645243,  98645223, "Wilselsesteenweg"),
            new Val( 87,  38209648,  98645243,  38209648, "Wilselsesteenweg"),
            new Val( 86, 125835556,  98645243, 125835556, "Kesseldallaan"),
            new Val( 85, 112566013,  98645243, 112566013, "Kesseldallaan"),

            new Val( 84, 112566013,  98645243, 112566017, "Kesseldallaan",
                "Kesseldallaan - Wilselsesteenweg (2;310)",
                "2;310"),

            new Val( 83, 112566021, 112566017, 112566021, "Kesseldallaan"),
            new Val( 82, 440732696, 112566017, 440732696, "Kesseldallaan"),
            new Val( 81, 112566010, 112566017, 112566010, "Kesseldallaan"),
            new Val( 80, 440732694, 112566017, 440732694, "Kesseldallaan"),
            new Val( 79, 440732695, 112566017, 440732695, "Kesseldallaan"),
            new Val( 78, 112566011, 112566017, 112566011, "Kesseldallaan"),
            new Val( 77, 112565986, 112566017, 112565986, "Kesseldallaan"),
            new Val( 76, 242622340, 112566017, 242622340, "Kesseldallaan"),

            new Val( 75, 242622340, 112566017,  33233154, "Kesseldallaan",
                "Kesseldallaan (2;179;306;306 (student);310;433)",
                "2;179;306;306 (student);310;433"),


            new Val( 74,  33233154,  33233154, 112566015, "Eénmeilaan",
                "Kesseldallaan (2;179;306;306 (student);310;433)",
                "2;179;306;306 (student);310;433"),

            new Val( 73,  80284458, 112566015,  80284458, "Eénmeilaan"),
            new Val( 72,  38245753, 112566015,  38245753, "Eénmeilaan"),
            new Val( 71,  38245752, 112566015,  38245752, "Eénmeilaan"),
            new Val( 70, 138017385, 112566015, 138017385, "Eénmeilaan"),
            new Val( 69, 125835546, 112566015, 125835546, "Eénmeilaan"),
            new Val( 68, 180167956, 112566015, 180167956, "Eénmeilaan"),
            new Val( 67, 608715591, 112566015, 608715591, "Eénmeilaan"),
            new Val( 66, 608715592, 112566015, 608715592, "Eénmeilaan"),
            new Val( 65,  22476094, 112566015,  22476094, "Eénmeilaan"),
            new Val( 64, 138017384, 112566015, 138017384, "Eénmeilaan"),
            new Val( 63, 138017382, 112566015, 138017382, "Eénmeilaan"),
            new Val( 62,  22476095, 112566015,  22476095, "Eénmeilaan"),
            new Val( 61, 138017381, 112566015, 138017381, "Eénmeilaan"),
            new Val( 60, 125835547, 112566015, 125835547, "Eénmeilaan"),

            new Val( 59, 125835547, 112566015,  79366165, "Baron August de Becker Remyplein",
                "Eénmeilaan (179;306;306 (student);310;433)",
                "179;306;306 (student);310;433"),


            new Val( 58,  79366165,  79366165,  79366167, "Baron August de Becker Remyplein",
                "Baron August de Becker Remyplein (2;179;306;306 (student);310;433;520)",
                "2;179;306;306 (student);310;433;520"),

            new Val( 57,  79366164,  79366167,  79366164, "Baron August de Becker Remyplein"),
            new Val( 56,  35633068,  79366167,  35633068, "Baron August de Becker Remyplein"),
            new Val( 55, 608715602,  79366167, 608715602, "Baron August de Becker Remyplein"),
            new Val( 54, 608715601,  79366167, 608715601, "Baron August de Becker Remyplein"),
            new Val( 53, 608715604,  79366167, 608715604, "Baron August de Becker Remyplein"),
            new Val( 52, 608715603,  79366167, 608715603, "Baron August de Becker Remyplein"),
            new Val( 51, 429571921,  79366167, 429571921, "Baron August de Becker Remyplein"),
            new Val( 50, 125835568,  79366167, 125835568, "Leuvensestraat"),

            new Val( 49, 125835568,  79366167,  23707244, "Diestsesteenweg",
                "Leuvensestraat - Baron August de Becker Remyplein (2;179;306;306 (student);310;433;520)",
                "2;179;306;306 (student);310;433;520"),

            new Val( 48,  12715116,  23707244,  12715116, "Diestsesteenweg"),

            new Val( 47,  12715116,  23707244,  61556877, "Diestsepoort",
                "Diestsesteenweg (2;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525)",
                "2;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525"),

            new Val( 46, 663770966,  61556877, 663770966, "Diestsepoort"),
            new Val( 45, 584356749,  61556877, 584356749, "Diestsepoort"),
            new Val( 44, 584356745,  61556877, 584356745, "Diestsepoort"),

            new Val( 43, 584356745,  61556877, 198559166, "Diestsepoort",
                "Diestsepoort (2;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",
                "2;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658"),

            new Val( 42, 584356751, 198559166, 584356751, "Diestsepoort"),
            new Val( 41, 451873773, 198559166, 451873773, "Diestsepoort"),
            new Val( 40, 584356742, 198559166, 584356742, "Diestsepoort"),
            new Val( 39, 451873774, 198559166, 451873774, "Diestsepoort"),

            new Val( 38, 451873774, 198559166,  76867049, null,
                "Diestsepoort (2;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",
                "2;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658"),

            new Val( 37,  79264890,  76867049,  79264890, null),
            new Val( 36,  79596965,  76867049,  79596965, null),

            new Val( 35,  79596965,  76867049,  71754927, null,
                "(2;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",
                "2;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630"),


            new Val( 34,  71754927,  71754927,  79264897, null,
                "(2;310;370;371;373;374;475;485;513;520;524;525)",
                "2;310;370;371;373;374;475;485;513;520;524;525"),

            new Val( 33,  79596983,  79264897,  79596983, null),

            new Val( 32,  79596983,  79264897, 377918665, null,
                "(2;310)",
                "2;310"),


            new Val( 31,  377918665,  377918665, 377918665, null,
                "perron 7 & 8 (2;310)",
                "2;310")
        );

        previousSegment = new RouteSegmentToExtract(bus310_Leuven_Aarschot_Station_RouteRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, bus310_Leuven_Aarschot_Station_RouteRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
        }

        System.out.print("\n");

        // ***********************************************************
        // Line 433 is an express bus
        // During interactive testing it causes an Index out of bounds exception
        // for the variant that goes from Tremelo to Leuven
        // Line 333 has the same issue, but this one is a bit shorter
        // ***********************************************************

        Relation bus433_Tremelo_Leuven_RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 5451452)
            .findFirst().orElse(null);

        assertNotNull(bus433_Tremelo_Leuven_RouteRelation);
        RouteSegmentToExtract segment601 = new RouteSegmentToExtract(bus433_Tremelo_Leuven_RouteRelation);
        segment601.setActiveDataSet(ds);

        assertEquals(bus433_Tremelo_Leuven_RouteRelation.get("ref"), segment601.getLineIdentifiersSignature());
        assertEquals(bus433_Tremelo_Leuven_RouteRelation.get("colour"), segment601.getColoursSignature());

        assertNull(segment601.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment601.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment601.getWayMembers());

        Relation clonedRelation = bus433_Tremelo_Leuven_RouteRelation;

        expectedValues = Arrays.asList(
            new Val( 167,  78579065,  78579065,  78579065, null),
            new Val( 166, 377814547,  78579065, 377814547, null),

            new Val( 165, 377814547,  78579065,  79596986, null,
                "perron 1 & 2 (1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658)",
                "1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658"),


            new Val( 164,  79596986,  79596986,  79211473, "Tiensevest",
                "(305;306;310;318;358;410;433;475;485;601;658)",
                "305;306;310;318;358;410;433;475;485;601;658"),


            new Val( 163,  79211473,  79211473,  79211472, "Tiensevest",
                "Tiensevest (1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658)",
                "1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658"),


            new Val( 162,  79211472,  79211472,  79175435, "Tiensevest",
                "Tiensevest (2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658)",
                "2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658"),


            new Val( 161,  79175435,  79175435,  80458208, "Tiensevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658)",
                "284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658"),


            new Val( 160,  80458208,  80458208,  19793164, "Tiensevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658)",
                "284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658"),


            new Val( 159,  19793164,  19793164,   4884707, "Tiensevest",
                "Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658)",
                "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658"),


            new Val( 158,   4884707,   4884707, 116797179, "Diestsepoort",
                "Tiensevest (284;285;306;310;315;316;317;395;433;475;485)",
                "284;285;306;310;315;316;317;395;433;475;485"),


            new Val( 157, 116797179, 116797179, 116797180, "Diestsepoort",
                "Diestsepoort (306;310;318;410;433)",
                "306;310;318;410;433"),


            new Val( 156, 116797180, 116797180, 451873774, "Diestsepoort",
                "Diestsepoort (306;310;318;410;433;475;485)",
                "306;310;318;410;433;475;485"),

            new Val( 155, 584356742, 451873774, 584356742, "Diestsepoort"),

            new Val( 154, 584356742, 451873774, 451873773, "Diestsepoort",
                "Diestsepoort (2;3;178;179;306;306 (student);310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525)",
                "2;3;178;179;306;306 (student);310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525"),

            new Val( 153, 584356751, 451873773, 584356751, "Diestsepoort"),
            new Val( 152, 198559166, 451873773, 198559166, "Diestsepoort"),
            new Val( 151, 584356745, 451873773, 584356745, "Diestsepoort"),
            new Val( 150, 584356749, 451873773, 584356749, "Diestsepoort"),
            new Val( 149, 663770966, 451873773, 663770966, "Diestsepoort"),
            new Val( 148,  61556877, 451873773,  61556877, "Diestsepoort"),

            new Val( 147,  61556877, 451873773,   8109264, "Diestsepoort",
                "Diestsepoort (2;3;178;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525)",
                "2;3;178;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525"),

            new Val( 146, 125835524,   8109264, 125835524, "Diestsesteenweg"),
            new Val( 145,  23707244,   8109264,  23707244, "Diestsesteenweg"),

            new Val( 144,  23707244,   8109264, 125835568, "Leuvensestraat",
                "Diestsesteenweg - Diestsepoort (2;3;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525)",
                "2;3;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525"),

            new Val( 143, 429571921, 125835568, 429571921, "Baron August de Becker Remyplein"),
            new Val( 142, 608715603, 125835568, 608715603, "Baron August de Becker Remyplein"),
            new Val( 141, 608715604, 125835568, 608715604, "Baron August de Becker Remyplein"),
            new Val( 140, 608715601, 125835568, 608715601, "Baron August de Becker Remyplein"),
            new Val( 139, 608715602, 125835568, 608715602, "Baron August de Becker Remyplein"),
            new Val( 138,  35633068, 125835568,  35633068, "Baron August de Becker Remyplein"),
            new Val( 137, 125835500, 125835568, 125835500, "Baron August de Becker Remyplein"),

            new Val( 136, 125835500, 125835568, 125835547, "Eénmeilaan",
                "Baron August de Becker Remyplein - Leuvensestraat (2;179;306;306 (student);310;333;433;512;520)",
                "2;179;306;306 (student);310;333;433;512;520"),

            new Val( 135, 138017386, 125835547, 138017386, "Eénmeilaan"),
            new Val( 134, 138017384, 125835547, 138017384, "Eénmeilaan"),
            new Val( 133,  22476094, 125835547,  22476094, "Eénmeilaan"),
            new Val( 132, 608715592, 125835547, 608715592, "Eénmeilaan"),
            new Val( 131, 608715591, 125835547, 608715591, "Eénmeilaan"),
            new Val( 130, 138017383, 125835547, 138017383, "Eénmeilaan"),
            new Val( 129, 138017385, 125835547, 138017385, "Eénmeilaan"),
            new Val( 128,  38245752, 125835547,  38245752, "Eénmeilaan"),
            new Val( 127,  38245753, 125835547,  38245753, "Eénmeilaan"),
            new Val( 126,  80284458, 125835547,  80284458, "Eénmeilaan"),
            new Val( 125, 112566018, 125835547, 112566018, "Eénmeilaan"),

            new Val( 124, 112566018, 125835547, 242622344, "Kesseldallaan",
                "Eénmeilaan (179;306;306 (student);310;333;433;512)",
                "179;306;306 (student);310;333;433;512"),

            new Val( 123, 125835557, 242622344, 125835557, "Kesseldallaan"),
            new Val( 122, 112566011, 242622344, 112566011, "Kesseldallaan"),
            new Val( 121, 440732695, 242622344, 440732695, "Kesseldallaan"),
            new Val( 120, 440732694, 242622344, 440732694, "Kesseldallaan"),
            new Val( 119, 112566010, 242622344, 112566010, "Kesseldallaan"),
            new Val( 118, 440732696, 242622344, 440732696, "Kesseldallaan"),
            new Val( 117,  13823565, 242622344,  13823565, "Kesseldallaan"),
            new Val( 116, 112565989, 242622344, 112565989, "Kesseldallaan"),

            new Val( 115, 112565989, 242622344, 192559635, "Duchesnelaan",
                "Kesseldallaan (2;179;306;306 (student);310;333;433;512)",
                "2;179;306;306 (student);310;333;433;512"),

            new Val( 114, 149117671, 192559635, 149117671, "Duchesnelaan"),
            new Val( 113,  17540907, 192559635,  17540907, "Vuntcomplex"),
            new Val( 112,  17540932, 192559635,  17540932, "Vuntcomplex"),
            new Val( 111, 315666514, 192559635, 315666514, "Vuntcomplex"),

            new Val( 110, 315666514, 192559635, 315666515, "Vuntcomplex",
                "Vuntcomplex - Duchesnelaan (179;306;306 (student);333;433;512)",
                "179;306;306 (student);333;433;512"),


            new Val( 109, 315666515, 315666515, 345381449, "Vuntcomplex",
                "Vuntcomplex (179;306;306 (student);333;433;512)",
                "179;306;306 (student);333;433;512"),

            new Val( 108, 225497053, 345381449, 225497053, "Vuntcomplex"),

            new Val( 107, 225497053, 345381449, 345381452, "Vuntcomplex",
                "Vuntcomplex (179;306;306 (student);333;433;512)",
                "179;306;306 (student);333;433;512"),

            new Val( 106, 345381453, 345381452, 345381453, "Vuntcomplex"),
            new Val( 105,   4004855, 345381452,   4004855, "Vuntcomplex"),

            new Val( 104,   4004855, 345381452,  13882237, "Aarschotsesteenweg",
                "Vuntcomplex (333;433;512)",
                "333;433;512"),

            new Val( 103, 351000218,  13882237, 351000218, "Aarschotsesteenweg"),
            new Val( 102, 440732691,  13882237, 440732691, "Aarschotsesteenweg"),
            new Val( 101, 345381437,  13882237, 345381437, "Aarschotsesteenweg"),
            new Val( 100, 377918607,  13882237, 377918607, "Aarschotsesteenweg"),
            new Val( 99, 377918608,  13882237, 377918608, "Aarschotsesteenweg"),
            new Val( 98,  37713149,  13882237,  37713149, "Aarschotsesteenweg"),
            new Val( 97, 631979428,  13882237, 631979428, "Aarschotsesteenweg"),
            new Val( 96, 345381435,  13882237, 345381435, "Aarschotsesteenweg"),
            new Val( 95,  79299306,  13882237,  79299306, "Aarschotsesteenweg"),

            new Val( 94,  79299306,  13882237, 345381434, "Aarschotsesteenweg",
                "Aarschotsesteenweg (333;334;335;433;512;513;630)",
                "333;334;335;433;512;513;630"),

            new Val( 93,  38159440, 345381434,  38159440, "Aarschotsesteenweg"),
            new Val( 92, 780600572, 345381434, 780600572, "Aarschotsesteenweg"),
            new Val( 91, 780600573, 345381434, 780600573, "Aarschotsesteenweg"),
            new Val( 90,  38159437, 345381434,  38159437, "Aarschotsesteenweg"),
            new Val( 89,  16201617, 345381434,  16201617, "Aarschotsesteenweg"),
            new Val( 88, 663909831, 345381434, 663909831, "Aarschotsesteenweg"),
            new Val( 87, 440732693, 345381434, 440732693, "Aarschotsesteenweg"),
            new Val( 86, 439397679, 345381434, 439397679, "Aarschotsesteenweg"),
            new Val( 85, 441677446, 345381434, 441677446, "Aarschotsesteenweg"),
            new Val( 84,  14099896, 345381434,  14099896, "Aarschotsesteenweg"),
            new Val( 83,  14099966, 345381434,  14099966, "Aarschotsesteenweg"),
            new Val( 82,  16201055, 345381434,  16201055, "Aarschotsesteenweg"),

            new Val( 81,  16201055, 345381434,  23781189, "Stationsstraat",
                "Aarschotsesteenweg (333;334;335;433;512;513)",
                "333;334;335;433;512;513"),

            new Val( 80, 663909838,  23781189, 663909838, "Stationsstraat"),
            new Val( 79,  98825487,  23781189,  98825487, "Stationsstraat"),
            new Val( 78,  23024091,  23781189,  23024091, "Stationsstraat"),
            new Val( 77,  23024090,  23781189,  23024090, "Stationsstraat"),
            new Val( 76,  15220439,  23781189,  15220439, "Stationsstraat"),

            new Val( 75,  15220439,  23781189,  13899067, "Provinciebaan",
                "Stationsstraat (333;335;433;513)",
                "333;335;433;513"),

            new Val( 74, 116535756,  13899067, 116535756, "Provinciebaan"),
            new Val( 73, 116535766,  13899067, 116535766, "Provinciebaan"),
            new Val( 72, 521082436,  13899067, 521082436, "Provinciebaan"),
            new Val( 71, 100083559,  13899067, 100083559, "Provinciebaan"),
            new Val( 70,  25706275,  13899067,  25706275, "Provinciebaan"),
            new Val( 69,  25706276,  13899067,  25706276, "Provinciebaan"),
            new Val( 68,  37712785,  13899067,  37712785, "Provinciebaan"),
            new Val( 67, 116535777,  13899067, 116535777, "Provinciebaan"),
            new Val( 66, 100086271,  13899067, 100086271, "Provinciebaan"),
            new Val( 65,  27107407,  13899067,  27107407, "Provinciebaan"),

            new Val( 64,  27107407,  13899067,  24041208, "Provinciebaan",
                "Provinciebaan (333;433;512;513)",
                "333;433;512;513"),

            new Val( 63, 292670112,  24041208, 292670112, "Nieuwebaan"),
            new Val( 62, 292670110,  24041208, 292670110, "Nieuwebaan"),
            new Val( 61, 292670113,  24041208, 292670113, "Nieuwebaan"),
            new Val( 60, 292670115,  24041208, 292670115, "Nieuwebaan"),
            new Val( 59, 292670109,  24041208, 292670109, "Nieuwebaan"),
            new Val( 58,  98918943,  24041208,  98918943, "Nieuwebaan"),
            new Val( 57, 292820652,  24041208, 292820652, "Nieuwebaan"),
            new Val( 56,  13858894,  24041208,  13858894, "Hoekje"),
            new Val( 55,  24037032,  24041208,  24037032, "Hoekje"),
            new Val( 54,  26591703,  24041208,  26591703, "Werchterplein"),
            new Val( 53,  21355673,  24041208,  21355673, "Sint-Jansstraat"),
            new Val( 52, 116911351,  24041208, 116911351, "Sint-Jansstraat"),
            new Val( 51, 205303321,  24041208, 205303321, "Sint-Jansstraat"),
            new Val( 50, 205303322,  24041208, 205303322, "Sint-Jansstraat"),
            new Val( 49, 205303320,  24041208, 205303320, "Sint-Jansstraat"),
            new Val( 48,  49577807,  24041208,  49577807, "Sint-Jansstraat"),
            new Val( 47, 116704290,  24041208, 116704290, "Tremelobaan"),
            new Val( 46,  32823731,  24041208,  32823731, "Tremelobaan"),
            new Val( 45, 349648271,  24041208, 349648271, "Tremelobaan"),
            new Val( 44,  25706363,  24041208,  25706363, "Tremelobaan"),
            new Val( 43, 349648272,  24041208, 349648272, "Tremelobaan"),
            new Val( 42, 349648273,  24041208, 349648273, "Tremelobaan"),
            new Val( 41, 349648270,  24041208, 349648270, "Tremelobaan"),
            new Val( 40,  25706405,  24041208,  25706405, "Tremelobaan"),
            new Val( 39, 126264637,  24041208, 126264637, "Tremelobaan"),
            new Val( 38,  22080651,  24041208,  22080651, "Tremelobaan"),
            new Val( 37,  22080665,  24041208,  22080665, "Werchtersebaan"),
            new Val( 36, 116911317,  24041208, 116911317, "Werchtersebaan"),
            new Val( 35, 116911322,  24041208, 116911322, "Werchtersebaan"),
            new Val( 34, 116911310,  24041208, 116911310, "Werchtersebaan"),
            new Val( 33, 116911314,  24041208, 116911314, "Werchtersebaan"),
            new Val( 32, 106526409,  24041208, 106526409, "Werchtersebaan"),
            new Val( 31, 106526391,  24041208, 106526391, "Werchtersebaan"),
            new Val( 30, 106526331,  24041208, 106526331, null),
            new Val( 29,  79057173,  24041208,  79057173, null),
            new Val( 28, 106526457,  24041208, 106526457, null),
            new Val( 27,  25706520,  24041208,  25706520, "Schrieksebaan"),
            new Val( 26, 244077928,  24041208, 244077928, "Schrieksebaan"),
            new Val( 25,  79057162,  24041208,  79057162, "Schrieksebaan"),
            new Val( 24, 348402887,  24041208, 348402887, "Schrieksebaan"),
            new Val( 23, 116911330,  24041208, 116911330, "Schrieksebaan"),
            new Val( 22,  20402695,  24041208,  20402695, "Astridstraat"),
            new Val( 21,  26614159,  24041208,  26614159, null),
            new Val( 20, 255018560,  24041208, 255018560, null),
            new Val( 19, 845727172,  24041208, 845727172, null),
            new Val( 18, 255018553,  24041208, 255018553, null),
            new Val( 17, 520502050,  24041208, 520502050, null),

            new Val( 16, 520502050,  24041208, 520502050, null,
                "Astridstraat - Provinciebaan (333;433)",
                "333;433")
        );

        previousSegment = new RouteSegmentToExtract(clonedRelation);
        previousSegment.setActiveDataSet(ds);
        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                extractAndAssertValues(v.index, previousSegment, segment, clonedRelation, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,
                    null, v.expectedRouteRef);
                previousSegment = segment;
            }
            UndoRedoHandler.getInstance().add(new ChangeCommand(bus433_Tremelo_Leuven_RouteRelation, clonedRelation));
            UndoRedoHandler.getInstance().undo();
        }
        UndoRedoHandler.getInstance().add(new ChangeCommand(bus433_Tremelo_Leuven_RouteRelation, clonedRelation));

        System.out.print("\n");

//        printListOfExpectedValues(bus433_Tremelo_Leuven_RouteRelation, "bus433_Tremelo_Leuven_RouteRelation", false);

    }

    public Relation extractAndAssertValues(int index, RouteSegmentToExtract createdSegment, RouteSegmentToExtract newSegment,
                                       Relation superRouteRelation, int firstWayId, int lastWayId, int firstWayIdForNewSegment,
                                       String nameOfNewWay,
                                       String expectedColours, String expectedRouteRef) {
        Relation extractedRelation = createdSegment.extractToRelation(Arrays.asList("type", "route"), true);
        System.out.println(index + " " + extractedRelation.get("note"));
        assertEquals(String.format("%d first way not correct %s%s\n%s%s\n", index, rc, firstWayId, rc, extractedRelation.firstMember().getWay().getId()), firstWayId, extractedRelation.firstMember().getWay().getId());
        assertEquals(String.format("%d last way not correct %s%s\n%s%s\n", index, rc, lastWayId, rc, extractedRelation.lastMember().getWay().getId()), lastWayId, extractedRelation.lastMember().getWay().getId());
        if (expectedColours != null) assertEquals(expectedColours, createdSegment.getColoursSignature());
        final String actualRouteRef = createdSegment.getLineIdentifiersSignature();
        assertEquals(expectedRouteRef, actualRouteRef);
        assertEquals(expectedRouteRef, extractedRelation.get("route_ref"));

        assertEquals(String.format("%d relation id not correct\n", index), extractedRelation.getId(), superRouteRelation.getMember(index+1).getMember().getId());
        // newSegment should have the last way that was added to this segment
        if (firstWayIdForNewSegment != 0) {
            assertNotNull(String.format("No new segment was created for way\n%s%s at position %d", rc, firstWayIdForNewSegment, index), newSegment);
            final long wayId = newSegment.getWayMembers().get(0).getWay().getId();
            assertEquals(String.format("%d name of last added way not correct %s%s\n ", index, rc, wayId), nameOfNewWay, newSegment.getWayMembers().get(0).getWay().get("name"));
            assertEquals(String.format("%d id of first way not correct  %s%s\n", index, rc, wayId), firstWayIdForNewSegment, wayId);
        }
        return extractedRelation;
    }

    /**
     * This prints a list of known values to be used in unit tests
     *
     * @param relation the route relation to print a list of known values for
     * @param nameOfVariable the variable name repeated as a string
     * @param firstOne true if this is the first in a series of consecutive tests
     *                 and the variables still need to be declared
     */
    @SuppressWarnings("unused")
    public void printListOfExpectedValues(Relation relation, String nameOfVariable, boolean firstOne) {
        List<RelationMember> members = relation.getMembers();
        RouteSegmentToExtract segment = new RouteSegmentToExtract(relation);
        List<RelationMember> wayMembers = segment.getWayMembers();
        String listVal = "";
        String className = "";
        if (firstOne) {
            listVal = "List<Val> ";
            className = "RouteSegmentToExtract ";
        }
        Way way = null;
        int lastIndex = 0;
        System.out.printf("    %sexpectedValues = Arrays.asList(\n", listVal);
        for (int i = members.size() - 1; i >= 0; i--) {
            RelationMember member = members.get(i);
            if (member.isWay() && RouteUtils.isPTWay(member)) {
                way = member.getWay();
                RouteSegmentToExtract ns = segment.addPTWayMember(i);
                wayMembers = segment.getWayMembers();
                if (ns != null) {
                    System.out.printf("\n        new Val(% 3d,% 10d,% 10d,% 10d, %s,\n            \"%s\",\n            \"%s\"),\n\n",
                        i,
                        wayMembers.get(0).getMember().getId(),
                        wayMembers.get(wayMembers.size() - 1).getMember().getId(),
                        (way != null && way.getId() != 0) ? way.getId() : 0,
                        (way != null && way.get("name") != null) ? "\"" + way.get("name") + "\"": null,
                        segment.getNote(),
                        segment.getLineIdentifiersSignature()
                    );
                    segment = ns;
                } else {
                    System.out.printf("        new Val(% 3d,% 10d,% 10d,% 10d, %s),\n",
                        i,
                        wayMembers.get(0).getMember().getId(),
                        wayMembers.get(wayMembers.size() - 1).getMember().getId(),
                        (way != null && way.getId() != 0) ? way.getId() : 0,
                        (way != null && way.get("name") != null) ? "\"" + way.get("name") + "\"": null
                    );
                }
            } else {
                lastIndex = i;
                break;
            }
        }
        System.out.printf("\n        new Val(% 3d,% 10d,% 10d,% 10d, %s,\n            \"%s\",\n            \"%s\")\n    );\n\n",
            lastIndex,
            (wayMembers != null && wayMembers.size() > 0) ? wayMembers.get(0).getMember().getId() : 0,
            (wayMembers != null && wayMembers.size() > 1) ? wayMembers.get(wayMembers.size() - 1).getMember().getId() : 0,
            (way != null && way.getId() != 0) ? way.getId() : 0,
            (way != null && way.get("name") != null) ? "\"" + way.get("name") + "\"": null,
            segment.getNote(),
            segment.getLineIdentifiersSignature()
        );
        System.out.printf("        %spreviousSegment = new RouteSegmentToExtract(%s);\n", className, nameOfVariable);
        System.out.print ("        previousSegment.setActiveDataSet(ds);\n");
        if (firstOne) {
            System.out.printf("        %ssegment;\n", className);
        }
        System.out.print ("        for (Val v: expectedValues) {\n" +
                          "            segment = previousSegment.addPTWayMember(v.index);\n" +
                          "            if (segment != null) {\n");
        System.out.printf("                extractAndAssertValues(v.index, previousSegment, segment, %s, v.iDOfFirstWay, v.iDOfLastWay, v.iDOfNextWay, v.nameOfNextWay,\n", nameOfVariable);
        System.out.print( "                    null, v.expectedRouteRef);\n" +
                          "                previousSegment = segment;\n" +
                          "            }\n" +
                          "        }\n\n" +
                          "        System.out.print(\"\\n\");\n\n");
    }
}
