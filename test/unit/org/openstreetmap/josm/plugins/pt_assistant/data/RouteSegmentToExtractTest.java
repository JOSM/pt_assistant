package org.openstreetmap.josm.plugins.pt_assistant.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.io.OsmReader.parseDataSet;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;

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

    public Val(String note, int index, int iDOfFirstWay, int iDOfLastWay, int iDOfNextWay,
               String nameOfNextWay) {
        this.index = index;
        this.nameOfNextWay = nameOfNextWay;
        this.iDOfFirstWay = iDOfFirstWay;
        this.iDOfLastWay = iDOfLastWay;
        this.iDOfNextWay = iDOfNextWay;
        this.note = note;
    }

    public Val(int index, int iDOfFirstWay, int iDOfLastWay, int iDOfNextWay, String nameOfNextWay) {
        this.index = index;
        this.nameOfNextWay = nameOfNextWay;
        this.iDOfFirstWay = iDOfFirstWay;
        this.iDOfLastWay = iDOfLastWay;
        this.iDOfNextWay = iDOfNextWay;
    }
}

public class RouteSegmentToExtractTest extends AbstractTest{

    private static final DataSet DATASET_BUSES_BEFORE_SPLITTING = loadDataSet(PATH_TO_PT_BEFORE_SPLITTING_TEST);
    private static final DataSet DATASET_F74_F75 = loadDataSet(PATH_TO_F74_F75_TEST);

    private static DataSet loadDataSet(final String path) {
        try (final FileInputStream stream = new FileInputStream(path)) {
            return parseDataSet(stream, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static final Relation PT_ROUTE_BUS_358 = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(6695469, OsmPrimitiveType.RELATION);
    private static final Relation PT_ROUTE_BUS_371 = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(1606056, OsmPrimitiveType.RELATION);
    private static final Relation PT_ROUTE_BUS_600_TO_LEUVEN_VAARTKOM = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(955908, OsmPrimitiveType.RELATION);
    private static final Relation PT_ROUTE_BUS_601_TO_LEUVEN_STATION = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(3612781, OsmPrimitiveType.RELATION);

    private static final Relation PT_ROUTE_BUS_310_TO_AARSCHOT_STATION = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(3297278, OsmPrimitiveType.RELATION);
    private static final Relation PT_ROUTE_BUS_433_TO_LEUVEN_STATION = (Relation) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(5451452, OsmPrimitiveType.RELATION);

    private static final Relation BICYCLE_ROUTE_F75 = (Relation) DATASET_F74_F75.getPrimitiveById(11021011, OsmPrimitiveType.RELATION);

    private static final String rc = " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way";

    @Test
    public void isItineraryInSameDirectionTest() {
        WaySequence waysInParentRouteOf601 = new WaySequence(
            PT_ROUTE_BUS_601_TO_LEUVEN_STATION.getMembers().get(156).getWay(),
            PT_ROUTE_BUS_601_TO_LEUVEN_STATION.getMembers().get(157).getWay(),
            PT_ROUTE_BUS_601_TO_LEUVEN_STATION.getMembers().get(158).getWay());

        WaySequence waysInParentRouteOf358 = new WaySequence(
            PT_ROUTE_BUS_358.getMembers().get(114).getWay(),
            PT_ROUTE_BUS_358.getMembers().get(115).getWay(),
            PT_ROUTE_BUS_358.getMembers().get(116).getWay());

        WaySequence waysInParentRouteOf371 = new WaySequence(
            PT_ROUTE_BUS_371.getMembers().get(132).getWay(),
            PT_ROUTE_BUS_371.getMembers().get(133).getWay(),
            PT_ROUTE_BUS_371.getMembers().get(134).getWay());
        RouteSegmentToExtract segment601_1 = new RouteSegmentToExtract(PT_ROUTE_BUS_601_TO_LEUVEN_STATION);
        segment601_1.setActiveDataSet(DATASET_BUSES_BEFORE_SPLITTING);
        assertTrue(waysInParentRouteOf601.compareTraversal(waysInParentRouteOf358));
        assertFalse(waysInParentRouteOf601.compareTraversal(waysInParentRouteOf371));

        final Way commonWay = (Way) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(75113358, OsmPrimitiveType.WAY);
        final Way toHaasrode = (Way) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(4866090, OsmPrimitiveType.WAY);
        final Way toBlanden = (Way) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(809484771, OsmPrimitiveType.WAY);
        final Way toNeervelp = (Way) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(243961945, OsmPrimitiveType.WAY);
        final Way toLeuven = (Way) DATASET_BUSES_BEFORE_SPLITTING.getPrimitiveById(809485597, OsmPrimitiveType.WAY);

        WaySequence leuvenToHaasrode = new WaySequence(toLeuven,commonWay,toHaasrode);
        WaySequence leuvenToBlanden = new WaySequence(toLeuven,commonWay,toBlanden);
        WaySequence neervelpToHaasrode = new WaySequence(toNeervelp,commonWay,toHaasrode);
        WaySequence neervelpToBlanden = new WaySequence(toNeervelp,commonWay,toBlanden);
        WaySequence haasrodeToLeuven = new WaySequence(toHaasrode,commonWay,toLeuven);
        WaySequence blandenToLeuven = new WaySequence(toBlanden,commonWay,toLeuven);
        WaySequence haasrodeToNeervelp = new WaySequence(toHaasrode,commonWay,toNeervelp);
        WaySequence blandenToNeervelp = new WaySequence(toBlanden,commonWay,toNeervelp);

        assertTrue(leuvenToHaasrode.compareTraversal(leuvenToBlanden));
        assertTrue(leuvenToBlanden.compareTraversal(leuvenToHaasrode));
        assertTrue(haasrodeToLeuven.compareTraversal(blandenToLeuven));
        assertTrue(blandenToLeuven.compareTraversal(haasrodeToLeuven));
        assertTrue(neervelpToBlanden.compareTraversal(neervelpToHaasrode));
        assertTrue(neervelpToHaasrode.compareTraversal(neervelpToBlanden));
        assertTrue(blandenToNeervelp.compareTraversal(blandenToLeuven));
        assertTrue(blandenToLeuven.compareTraversal(blandenToNeervelp));

        assertFalse(leuvenToBlanden.compareTraversal(blandenToLeuven));
        assertFalse(blandenToLeuven.compareTraversal(leuvenToBlanden));
        assertFalse(leuvenToHaasrode.compareTraversal(haasrodeToLeuven));
        assertFalse(haasrodeToLeuven.compareTraversal(leuvenToHaasrode));
        assertFalse(neervelpToBlanden.compareTraversal(blandenToNeervelp));
        assertFalse(blandenToNeervelp.compareTraversal(neervelpToBlanden));
        assertFalse(neervelpToHaasrode.compareTraversal(haasrodeToNeervelp));
        assertFalse(haasrodeToNeervelp.compareTraversal(neervelpToHaasrode));

        WaySequence missingToBlanden = new WaySequence(null,commonWay,toBlanden);
        WaySequence missingToHaasrode = new WaySequence(null,commonWay,toHaasrode);
        WaySequence missingToLeuven = new WaySequence(null,commonWay,toLeuven);
        WaySequence missingToNeervelp = new WaySequence(null,commonWay,toNeervelp);
        WaySequence blandenToMissing = new WaySequence(toBlanden,commonWay,null);
        WaySequence haasrodeToMissing = new WaySequence(toHaasrode,commonWay,null);
        WaySequence leuvenToMissing = new WaySequence(toNeervelp,commonWay,null);
        WaySequence neervelpToMissing = new WaySequence(toNeervelp,commonWay,null);

        assertTrue(missingToBlanden.compareTraversal(leuvenToBlanden));
        assertTrue(missingToHaasrode.compareTraversal(leuvenToHaasrode));
        assertTrue(leuvenToBlanden.compareTraversal(missingToBlanden));
        assertTrue(missingToLeuven.compareTraversal(blandenToLeuven));
        assertTrue(blandenToLeuven.compareTraversal(missingToLeuven));
        assertTrue(missingToBlanden.compareTraversal(neervelpToHaasrode));
        assertTrue(neervelpToHaasrode.compareTraversal(missingToBlanden));
        assertTrue(missingToNeervelp.compareTraversal(blandenToLeuven));
        assertTrue(blandenToLeuven.compareTraversal(missingToNeervelp));
        assertTrue(blandenToMissing.compareTraversal(blandenToLeuven));
        assertTrue(haasrodeToMissing.compareTraversal(haasrodeToLeuven));
        assertTrue(leuvenToMissing.compareTraversal(leuvenToBlanden));
        assertTrue(neervelpToMissing.compareTraversal(neervelpToHaasrode));

        assertFalse(leuvenToBlanden.compareTraversal(blandenToLeuven));
        assertFalse(blandenToLeuven.compareTraversal(leuvenToBlanden));
        assertFalse(leuvenToHaasrode.compareTraversal(haasrodeToLeuven));
        assertFalse(haasrodeToLeuven.compareTraversal(leuvenToHaasrode));
        assertFalse(neervelpToBlanden.compareTraversal(blandenToNeervelp));
        assertFalse(blandenToNeervelp.compareTraversal(neervelpToBlanden));
        assertFalse(neervelpToHaasrode.compareTraversal(haasrodeToNeervelp));
        assertFalse(haasrodeToNeervelp.compareTraversal(neervelpToHaasrode));
    }


    @Test
    public void f74_F75_Test() {
        assertEquals(30, BICYCLE_ROUTE_F75.getMembersCount());

        RouteSegmentToExtract s1 = new RouteSegmentToExtract(
            BICYCLE_ROUTE_F75,
            Arrays.asList(23, 24, 25));
        s1.setActiveDataSet(DATASET_F74_F75);
        s1.put("state", "proposed");

        assertEquals(3, s1.getWayMembers().size());
        Relation rel1 = s1.extractToRelation(Arrays.asList("type", "route"), true, true);
        assertEquals(28, BICYCLE_ROUTE_F75.getMembersCount());
        assertEquals("proposed", rel1.get("state"));
    }

    @Test
    public void bus601_600_3_Test() {
    List<Val> expectedValues;

        expectedValues = Arrays.asList(
        new Val(  14, 338057820,         0, 338057820, "Engels Plein"),
        new Val(  15, 338057820,   3869812,   3869812, "Engels Plein"),
        new Val(  16, 338057820,  25928482,  25928482, "Wolvengang"),
        new Val(  17, 338057820, 659297690, 659297690, "Wolvengang"),
        new Val(  18, 338057820, 330300723, 330300723, "Achter de latten"),
        new Val(  19, 338057820,   3869822,   3869822, "Burchtstraat"),
        new Val(  20, 338057820, 330300725, 330300725, "Havenkant"),
        new Val(  21, 338057820, 270181176, 270181176, "Havenkant"),
        new Val(  22, 338057820, 406205781, 406205781, "Havenkant"),
        new Val(  23, 338057820, 843534478, 843534478, "Havenkant"),
        new Val(  24, 338057820, 314635787, 314635787, "Havenkant"),
        new Val(  25, 338057820, 510790348, 510790348, "Havenkant"),
        new Val(  26, 338057820, 510790349, 510790349, "Havenkant"),
        new Val("Engels Plein - Havenkant (601)",

                  27, 338057820, 510790349,   3992548, "Zoutstraat"),
        new Val("Zoutstraat (334;335;513;601;630)",

                  28,   3992548,   3992548, 254800931, "Joanna-Maria Artoisplein"),
        new Val("Joanna-Maria Artoisplein (178;305;318;334;335;358;410;513;601;630;651;652;658)",

                  29, 254800931, 254800931,   8133608, "Diestsevest"),
        new Val(  30,   8133608,   4003924,   4003924, "Diestsevest"),
        new Val(  31,   8133608,  81457878,  81457878, "Diestsevest"),
        new Val(  32,   8133608,   6184898,   6184898, "Diestsevest"),
        new Val("Diestsevest (305;318;334;335;358;410;513;601;630;651;652;658)",

                  33,   8133608,   6184898,  19793164, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;651;652;658)",

                  34,  19793164,  19793164,  19793394, null),
        new Val("(334;335;513;601)",

                  35,  19793394,  19793394,  79193579, "Tiensevest"),
        new Val("Tiensevest (3;4;5;6;7;8;9;18;179;333;334;335;337;380;513;600;601;616;630;658)",

                  36,  79193579,  79193579,  78568409, "Tiensevest"),
        new Val("Tiensevest (4;5;6;7;8;9;18;179;284;285;315;316;317;334;335;337;380;601;616;658)",

                  37,  78568409,  78568409,  79264891, null),
        new Val("perron 13 (18;601)",

                  38,  79264891,  79264891, 377918638, null),
        new Val("perron 13 (18;601)",

                  39, 377918638, 377918638,  71754927, null),
        new Val(  40,  71754927,  79264897,  79264897, null),
        new Val("(2;3;4;5;6;7;8;9;18;178;179;306;333;334;335;337;370;371;373;374;380;512;520;524;525;601)",

                  41,  71754927,  79264897,  79264888, null),
        new Val("(2;3;4;5;6;7;8;9;18;178;179;306;333;334;335;337;370;371;373;374;380;512;520;525;601)",

                  42,  79264888,  79264888, 377918635, null),
        new Val("(3;4;5;6;7;8;9;18;178;179;306;333;334;335;337;370;371;373;374;380;512;601)",

                  43, 377918635, 377918635,  79264899, null),
        new Val("(4;5;6;18;178;179;306;333;334;335;337;370;371;373;374;380;512;601;630)",

                  44,  79264899,  79264899,  78815533, null),
        new Val(  45,  78815533, 459446600, 459446600, "Martelarenplein"),
        new Val(  46,  78815533, 459446598, 459446598, "Martelarenplein"),
        new Val(  47,  78815533,  76856823,  76856823, "Martelarenplein"),
        new Val("Martelarenplein (4;5;6;7;8;9;18;178;179;306;337;380;527;601;630)",

                  48,  78815533,  76856823, 185988814, "Tiensevest"),
        new Val("Tiensevest (1;4;5;6;7;8;9;18;178;179;306;337;380;527;601;630)",

                  49, 185988814, 185988814,   8590231, "Tiensevest"),
        new Val("Tiensevest (7;8;9;18;178;179;306;337;380;527;601;630)",

                  50,   8590231,   8590231,  12712557, "Tiensevest"),
        new Val(  51,  12712557,   8131717,   8131717, "Tiensevest"),
        new Val(  52,  12712557, 863272994, 863272994, "Tiensevest"),
        new Val(  53,  12712557, 863272993, 863272993, "Tiensevest"),
        new Val(  54,  12712557, 863272992, 863272992, "Tiensevest"),
        new Val(  55,  12712557, 863272995, 863272995, "Tiensevest"),
        new Val(  56,  12712557, 863272991, 863272991, "Tiensevest"),
        new Val(  57,  12712557,  16775171,  16775171, "Tiensepoort"),
        new Val("Tiensevest - Tiensepoort (7;8;9;18;178;179;306;337;380;527;601;616;630)",

                  58,  12712557,  16775171,   8130906, "Geldenaaksevest"),
        new Val(  59,   8130906, 586268893, 586268893, "Geldenaaksevest"),
        new Val(  60,   8130906,  94585453,  94585453, "Geldenaaksevest"),
        new Val(  61,   8130906, 521193607, 521193607, "Geldenaaksevest"),
        new Val(  62,   8130906,  10296368,  10296368, "Geldenaaksevest"),
        new Val(  63,   8130906,  79299303,  79299303, "Geldenaaksevest"),
        new Val(  64,   8130906, 608715606, 608715606, "Geldenaaksevest"),
        new Val(  65,   8130906, 608715605, 608715605, "Geldenaaksevest"),
        new Val("Geldenaaksevest (18;178;179;306;337;601;616;630)",

                  66,   8130906, 608715605,  24905257, "Geldenaaksevest"),
        new Val(  67,  24905257,   3677823,   3677823, "Naamsevest"),
        new Val(  68,  24905257, 655251292, 655251292, "Naamsevest"),
        new Val(  69,  24905257, 661733369, 661733369, "Naamsevest"),
        new Val(  70,  24905257, 131571763, 131571763, "Naamsevest"),
        new Val(  71,  24905257, 655251293, 655251293, "Naamsevest"),
        new Val(  72,  24905257,  86164005,  86164005, "Naamsevest"),
        new Val("Geldenaaksevest - Naamsevest (18;178;179;306;337;601;616)",

                  73,  24905257,  86164005,   3677330, "Tervuursevest"),
        new Val(  74,   3677330, 608715562, 608715562, "Tervuursevest"),
        new Val(  75,   3677330, 608715561, 608715561, "Tervuursevest"),
        new Val(  76,   3677330,  23237287,  23237287, "Tervuursevest"),
        new Val(  77,   3677330,  90168773,  90168773, "Tervuursevest"),
        new Val(  78,   3677330,  23237288,  23237288, "Tervuursevest"),
        new Val(  79,   3677330,  31474001,  31474001, "Tervuursevest"),
        new Val(  80,   3677330,   3677335,   3677335, "Tervuursevest"),
        new Val(  81,   3677330, 461159367, 461159367, "Tervuursevest"),
        new Val(  82,   3677330, 344507822, 344507822, "Tervuursevest"),
        new Val(  83,   3677330, 461159362, 461159362, "Tervuursevest"),
        new Val("Tervuursevest (178;179;306;520;524;525;537;601)",

                  84,   3677330, 461159362, 461159345, "Tervuursevest"),
        new Val("Tervuursevest (178;179;306;601)",

                  85, 461159345, 461159345,  88361317, "Tervuursevest"),
        new Val(  86,  88361317, 429706866, 429706866, "Tervuursevest"),
        new Val(  87,  88361317, 174338459, 174338459, "Tervuursevest"),
        new Val(  88,  88361317,   3677944,   3677944, "Tervuursevest"),
        new Val(  89,  88361317, 608715521, 608715521, "Tervuursevest"),
        new Val(  90,  88361317, 608715520, 608715520, "Tervuursevest"),
        new Val(  91,  88361317, 174338458, 174338458, "Tervuursevest"),
        new Val(  92,  88361317, 813979465, 813979465, "Tervuursevest"),
        new Val(  93,  88361317, 521193380, 521193380, "Tervuursevest"),
        new Val(  94,  88361317, 521193379, 521193379, "Tervuursevest"),
        new Val(  95,  88361317,  99583853,  99583853, "Tervuursevest"),
        new Val("Tervuursevest (601)",

                  96,  88361317,  99583853, 429706864, "Rennes-Singel"),
        new Val(  97, 429706864,   8131120,   8131120, "Rennes-Singel"),
        new Val(  98, 429706864, 813979472, 813979472, "Rennes-Singel"),
        new Val(  99, 429706864,  79289746,  79289746, "Rennes-Singel"),
        new Val( 100, 429706864, 813979470, 813979470, "Rennes-Singel"),
        new Val( 101, 429706864, 161166589, 161166589, "Rennes-Singel"),
        new Val( 102, 429706864, 192559628, 192559628, "Rennes-Singel"),
        new Val( 103, 429706864, 249333186, 249333186, "Rennes-Singel"),
        new Val("Rennes-Singel (318;601)",

                 104, 429706864, 249333186, 249333184, "Herestraat"),
        new Val( 105, 249333184, 813970231, 813970231, "Herestraat"),
        new Val( 106, 249333184, 813970229, 813970229, "Herestraat"),
        new Val("Herestraat (601)",

                 107, 249333184, 813970229, 813970228, "Herestraat"),
        new Val( 108, 813970228, 813970226, 813970226, "Herestraat"),
        new Val( 109, 813970228, 813970232, 813970232, "Herestraat"),
        new Val( 110, 813970228,  13067134,  13067134, "Herestraat"),
        new Val("Herestraat (410;600;601)",

                 111, 813970228,  13067134, 249333187, "Rotonde Het Teken"),
        new Val("Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                 112, 249333187, 249333187, 249333188, "Rotonde Het Teken (MgrVWB)"),
        new Val("Rotonde Het Teken (MgrVWB) (3;410;600;601)",

                 113, 249333188, 249333188,   3752557, "Rotonde Het Teken (to Ring Noord)"),
        new Val("Rotonde Het Teken (to Ring Noord) (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                 114,   3752557,   3752557,  78873921, "Rotonde Het Teken (Ring Noord)"),
        new Val("Rotonde Het Teken (Ring Noord) (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                 115,  78873921,  78873921,  78568660, "Rotonde Het Teken (from Ring Noord)"),
        new Val( 116,  78568660,   8080023,   8080023, "Ring Zuid"),
        new Val( 117,  78568660, 502328837, 502328837, "Ring Zuid"),
        new Val( 118,  78568660, 502328838, 502328838, "Ring Zuid"),
        new Val("Rotonde Het Teken (from Ring Noord) - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                 119,  78568660, 502328838,  14508740, "Ring Zuid (1 - 2)"),
        new Val("Ring Zuid (1 - 2) (3;317;334;335;380;395;410;600;601)",

                 120,  14508740,  14508740,  14508739, "Ring Zuid (2 - 3)"),
        new Val("Ring Zuid (2 - 3) (3;317;334;335;395;410;600;601)",

                 121,  14508739,  14508739, 109267436, "Ring Zuid (3 - 4&5)"),
        new Val("Ring Zuid (3 - 4&5) (3;317;395;410;600;601)",

                 122, 109267436, 109267436,  14508736, null),
        new Val( 123,  14508736, 377918641, 377918641, null),
        new Val("perron 5 (3;317;395;410;601)",

                 124,  14508736, 377918641,  78852604, null),
        new Val( 125,  78852604, 332258104, 332258104, null),
        new Val("GHB (ingang) - GHB (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                 126,  78852604, 332258104, 159949154, null),
        new Val("(from GHB to Ring Zuid) (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                 127, 159949154, 159949154,  11369123, "Ring Zuid (GHB)"),
        new Val("Ring Zuid (GHB) (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                 128,  11369123,  11369123,  79340950, "Ring Zuid"),
        new Val( 129,  79340950,  41403538,  41403538, "Rotonde Het Teken"),
        new Val("Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                 130,  79340950,  41403538,   8079995, "Herestraat"),
        new Val( 131,   8079995,  41403544,  41403544, "Herestraat"),
        new Val( 132,   8079995, 813970227, 813970227, "Herestraat"),
        new Val("Herestraat (410;600;601)",

                 133,   8079995, 813970227, 681081951, "Herestraat"),
        new Val( 134, 681081951, 813970231, 813970231, "Herestraat"),
        new Val( 135, 681081951, 249333184, 249333184, "Herestraat"),
        new Val("Herestraat (410;601)",

                 136, 681081951, 249333184, 249333185, "Rennes-Singel"),
        new Val( 137, 249333185,   3994257,   3994257, "Rennes-Singel"),
        new Val( 138, 249333185,   3680456,   3680456, "Rennes-Singel"),
        new Val( 139, 249333185,   8131121,   8131121, "Den Boschsingel"),
        new Val( 140, 249333185, 225605630, 225605630, "Den Boschsingel"),
        new Val("Rennes-Singel - Den Boschsingel (318;410;601)",

                 141, 249333185, 225605630,  23837544, "Den Boschsingel"),
        new Val( 142,  23837544, 146171871, 146171871, "Den Boschsingel"),
        new Val( 143,  23837544,   3877105,   3877105, null),
        new Val( 144,  23837544,   8131125,   8131125, null),
        new Val( 145,  23837544,   3993387,   3993387, "Lüdenscheidsingel"),
        new Val("Den Boschsingel - Lüdenscheidsingel (318;358;410;601;651;658)",

                 146,  23837544,   3993387, 109267417, "Lüdenscheidsingel"),
        new Val( 147, 109267417,   3993388,   3993388, "Lüdenscheidsingel"),
        new Val( 148, 109267417,  44932921,  44932921, "Lüdenscheidsingel"),
        new Val( 149, 109267417, 254800932, 254800932, "Lüdenscheidsingel"),
        new Val( 150, 109267417,  23691158,  23691158, "Joanna-Maria Artoisplein"),
        new Val("Lüdenscheidsingel - Joanna-Maria Artoisplein (178;305;318;358;410;601;651;652;658)",

                 151, 109267417,  23691158, 254800931, "Joanna-Maria Artoisplein"),
        new Val("Joanna-Maria Artoisplein (178;305;318;334;335;358;410;513;601;630;651;652;658)",

                 152, 254800931, 254800931,   8133608, "Diestsevest"),
        new Val( 153,   8133608,   4003924,   4003924, "Diestsevest"),
        new Val( 154,   8133608,  81457878,  81457878, "Diestsevest"),
        new Val( 155,   8133608,   6184898,   6184898, "Diestsevest"),
        new Val("Diestsevest (305;318;334;335;358;410;513;601;630;651;652;658)",

                 156,   8133608,   6184898,  19793164, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;651;652;658)",

                 157,  19793164,  19793164,  80458208, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;651;652;658)",

                 158,  80458208,  80458208,  79175435, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658)",

                 159,  79175435,  79175435,  79211472, "Tiensevest"),
        new Val("Tiensevest (1;2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658)",

                 160,  79211472,  79211472,  79211473, "Tiensevest"),
        new Val("Tiensevest (1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658)",

                 161,  79211473,  79211473,  79596986, null),
        new Val("(305;306;310;318;358;410;433;475;485;601;658)",

                 162,  79596986,  79596986, 377814547, null),
        new Val("perron 1 & 2 (1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658)",

                 163, 377814547,  78579065,         0, null)
    );

    testPtLine(expectedValues, PT_ROUTE_BUS_601_TO_LEUVEN_STATION);

        // ***********************************************************
        // ***********************************************************
        // ***********************************************************

    expectedValues = Arrays.asList(
        new Val(  12, 377918658,         0, 377918658, null),
        new Val(  13, 377918658,  78815527,  78815527, null),
        new Val("perron 11 & 12 (395;600;651;652)",

                  14, 377918658,  78815527,  23691157, "Diestsepoort"),
        new Val(  15,  23691157, 116797180, 116797180, "Diestsepoort"),
        new Val("Diestsepoort (4;5;6;7;8;9;179;334;335;337;380;600;616;651;652;658)",

                  16,  23691157, 116797180, 451873774, "Diestsepoort"),
        new Val(  17, 451873774, 584356742, 584356742, "Diestsepoort"),
        new Val(  18, 451873774, 451873773, 451873773, "Diestsepoort"),
        new Val(  19, 451873774, 584356751, 584356751, "Diestsepoort"),
        new Val(  20, 451873774, 584356751, 198559166, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",

                  21, 451873774, 198559166, 584356745, "Diestsepoort"),
        new Val(  22, 584356745, 584356749, 584356749, "Diestsepoort"),
        new Val(  23, 584356745, 663770966, 663770966, "Diestsepoort"),
        new Val(  24, 584356745,  61556877,  61556877, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;305;306;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",

                  25, 584356745,  61556877,   8109264, "Diestsepoort"),
        new Val(  26,   8109264,   4061640,   4061640, "Vuurkruisenlaan"),
        new Val(  27,   8109264,  23691160,  23691160, "Vuurkruisenlaan"),
        new Val("Diestsepoort - Vuurkruisenlaan (305;333;334;335;513;600;630;651;652;658)",

                  28,   8109264,  23691160,  61540068, "Joanna-Maria Artoisplein"),
        new Val(  29,  61540068, 254801390, 254801390, "Joanna-Maria Artoisplein"),
        new Val("Joanna-Maria Artoisplein (178;305;318;333;334;335;410;513;600;630;651;652;658)",

                  30,  61540068, 254801390,  61540098, "Joanna-Maria Artoisplein"),
        new Val(  31,  61540098,   8131040,   8131040, "Lüdenscheidsingel"),
        new Val(  32,  61540098,  44932919,  44932919, "Lüdenscheidsingel"),
        new Val(  33,  61540098,  12891213,  12891213, "Lüdenscheidsingel"),
        new Val("Joanna-Maria Artoisplein - Lüdenscheidsingel (178;305;318;410;600;651;652;658)",

                  34,  61540098,  12891213,  85048201, "Lüdenscheidsingel"),
        new Val(  35,  85048201,   3877104,   3877104, null),
        new Val(  36,  85048201, 125835586, 125835586, "Den Boschsingel"),
        new Val(  37,  85048201, 146171867, 146171867, "Den Boschsingel"),
        new Val("Lüdenscheidsingel - Den Boschsingel (318;410;600;651;658)",

                  38,  85048201, 146171867,  23837543, "Den Boschsingel"),
        new Val(  39,  23837543, 192559625, 192559625, "Den Boschsingel"),
        new Val(  40,  23837543,   3680457,   3680457, "Rennes-Singel"),
        new Val(  41,  23837543,   8131123,   8131123, "Rennes-Singel"),
        new Val("Den Boschsingel - Rennes-Singel (318;410;600)",

                  42,  23837543,   8131123, 225605633, "Rennes-Singel"),
        new Val(  43, 225605633,  78568454,  78568454, "Rennes-Singel"),
        new Val("Rennes-Singel (178;318;410;600)",

                  44, 225605633,  78568454, 270181177, "Herestraat"),
        new Val("Herestraat (410;600)",

                  45, 270181177, 270181177, 813970228, "Herestraat"),
        new Val(  46, 813970228, 813970226, 813970226, "Herestraat"),
        new Val(  47, 813970228, 813970232, 813970232, "Herestraat"),
        new Val(  48, 813970228,  13067134,  13067134, "Herestraat"),
        new Val("Herestraat (410;600;601)",

                  49, 813970228,  13067134, 249333187, "Rotonde Het Teken"),
        new Val("Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                  50, 249333187, 249333187, 249333188, "Rotonde Het Teken (MgrVWB)"),
        new Val("Rotonde Het Teken (MgrVWB) (3;410;600;601)",

                  51, 249333188, 249333188,   3752557, "Rotonde Het Teken (to Ring Noord)"),
        new Val("Rotonde Het Teken (to Ring Noord) (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                  52,   3752557,   3752557,  78873921, "Rotonde Het Teken (Ring Noord)"),
        new Val("Rotonde Het Teken (Ring Noord) (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                  53,  78873921,  78873921,  78568660, "Rotonde Het Teken (from Ring Noord)"),
        new Val(  54,  78568660,   8080023,   8080023, "Ring Zuid"),
        new Val(  55,  78568660, 502328837, 502328837, "Ring Zuid"),
        new Val(  56,  78568660, 502328838, 502328838, "Ring Zuid"),
        new Val("Rotonde Het Teken (from Ring Noord) - Ring Zuid (3;317;333;334;335;370;371;373;374;380;395;410;600;601)",

                  57,  78568660, 502328838,  14508740, "Ring Zuid (1 - 2)"),
        new Val("Ring Zuid (1 - 2) (3;317;334;335;380;395;410;600;601)",

                  58,  14508740,  14508740,  14508739, "Ring Zuid (2 - 3)"),
        new Val("Ring Zuid (2 - 3) (3;317;334;335;395;410;600;601)",

                  59,  14508739,  14508739, 109267436, "Ring Zuid (3 - 4&5)"),
        new Val("Ring Zuid (3 - 4&5) (3;317;395;410;600;601)",

                  60, 109267436, 109267436,  14506241, null),
        new Val(  61,  14506241, 318878531, 318878531, null),
        new Val("perron 4 (3;317;395;410;600)",

                  62,  14506241, 318878531,  14508735, null),
        new Val("(3;317;333;334;335;370;371;373;374;380;395;410;513;600)",

                  63,  14508735,  14508735,  78852604, null),
        new Val(  64,  78852604, 332258104, 332258104, null),
        new Val("GHB (ingang) - GHB (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                  65,  78852604, 332258104, 159949154, null),
        new Val("(from GHB to Ring Zuid) (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                  66, 159949154, 159949154,  11369123, "Ring Zuid (GHB)"),
        new Val("Ring Zuid (GHB) (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                  67,  11369123,  11369123,  79340950, "Ring Zuid"),
        new Val(  68,  79340950,  41403538,  41403538, "Rotonde Het Teken"),
        new Val("Ring Zuid - Rotonde Het Teken (3;317;333;334;335;370;371;373;374;380;395;410;513;600;601)",

                  69,  79340950,  41403538,   8079995, "Herestraat"),
        new Val(  70,   8079995,  41403544,  41403544, "Herestraat"),
        new Val(  71,   8079995, 813970227, 813970227, "Herestraat"),
        new Val("Herestraat (410;600;601)",

                  72,   8079995, 813970227,  78568455, "Herestraat"),
        new Val("Herestraat (600)",

                  73,  78568455,  78568455,  28982660, "Rennes-Singel"),
        new Val(  74,  28982660, 813979473, 813979473, "Rennes-Singel"),
        new Val(  75,  28982660, 192559626, 192559626, "Rennes-Singel"),
        new Val(  76,  28982660, 813979469, 813979469, "Rennes-Singel"),
        new Val(  77,  28982660, 192559627, 192559627, "Rennes-Singel"),
        new Val("Rennes-Singel (178;318;600)",

                  78,  28982660, 192559627,   3677945, "Tervuursevest"),
        new Val(  79,   3677945, 521193383, 521193383, "Tervuursevest"),
        new Val(  80,   3677945, 521193382, 521193382, "Tervuursevest"),
        new Val(  81,   3677945, 813979466, 813979466, "Tervuursevest"),
        new Val(  82,   3677945,  80194318,  80194318, "Tervuursevest"),
        new Val(  83,   3677945, 814322832, 814322832, "Tervuursevest"),
        new Val(  84,   3677945,  15051052,  15051052, "Tervuursevest"),
        new Val(  85,   3677945,   3677336,   3677336, "Tervuursevest"),
        new Val(  86,   3677945, 608715519, 608715519, "Tervuursevest"),
        new Val(  87,   3677945, 608715518, 608715518, "Tervuursevest"),
        new Val("Tervuursevest (178;600)",

                  88,   3677945, 608715518,  16771609, "Tervuursevest"),
        new Val(  89,  16771609, 260405216, 260405216, "Tervuursevest"),
        new Val("Tervuursevest (7;8;9;178;527;600)",

                  90,  16771609, 260405216,  13246158, "Tervuursevest"),
        new Val("Tervuursevest (7;8;9;178;520;524;525;527;537;586;600)",

                  91,  13246158,  13246158,  16771611, "Tervuursevest"),
        new Val("Tervuursevest (178;520;524;525;537;586;600)",

                  92,  16771611,  16771611,  85044922, "Tervuursevest"),
        new Val(  93,  85044922, 608715615, 608715615, "Tervuursevest"),
        new Val(  94,  85044922, 608715614, 608715614, "Tervuursevest"),
        new Val(  95,  85044922,   3677329,   3677329, "Tervuursevest"),
        new Val(  96,  85044922,  90168774,  90168774, "Tervuursevest"),
        new Val("Tervuursevest (178;179;520;524;525;537;586;600)",

                  97,  85044922,  90168774, 120086003, "Tervuursevest"),
        new Val(  98, 120086003,   3677822,   3677822, "Erasme Ruelensvest"),
        new Val("Tervuursevest - Erasme Ruelensvest (178;179;600)",

                  99, 120086003,   3677822, 608715579, "Erasme Ruelensvest"),
        new Val( 100, 608715579, 608715575, 608715575, "Erasme Ruelensvest"),
        new Val( 101, 608715579, 120086001, 120086001, "Erasme Ruelensvest"),
        new Val( 102, 608715579,   3991775,   3991775, "Geldenaaksevest"),
        new Val("Erasme Ruelensvest - Geldenaaksevest (18;178;179;337;600;616)",

                 103, 608715579,   3991775, 199381120, "Geldenaaksevest"),
        new Val( 104, 199381120,  24905254,  24905254, "Geldenaaksevest"),
        new Val( 105, 199381120, 491728135, 491728135, "Geldenaaksevest"),
        new Val( 106, 199381120,  24905255,  24905255, "Geldenaaksevest"),
        new Val( 107, 199381120,  24905256,  24905256, "Geldenaaksevest"),
        new Val( 108, 199381120, 863272996, 863272996, "Geldenaaksevest"),
        new Val( 109, 199381120, 857806575, 857806575, "Geldenaaksevest"),
        new Val( 110, 199381120, 857806576, 857806576, "Geldenaaksevest"),
        new Val( 111, 199381120, 857806574, 857806574, "Geldenaaksevest"),
        new Val( 112, 199381120,   8130905,   8130905, "Geldenaaksevest"),
        new Val( 113, 199381120,   4003928,   4003928, "Tiensepoort"),
        new Val("Geldenaaksevest - Tiensepoort (18;178;179;337;600;616;630)",

                 114, 199381120,   4003928, 521193611, "Tiensevest"),
        new Val( 115, 521193611, 586268892, 586268892, "Tiensevest"),
        new Val( 116, 521193611, 863272997, 863272997, "Tiensevest"),
        new Val( 117, 521193611, 521193609, 521193609, "Tiensevest"),
        new Val( 118, 521193611,   4003927,   4003927, "Tiensevest"),
        new Val( 119, 521193611,   8154434,   8154434, "Tiensevest"),
        new Val("Tiensevest (7;8;9;18;178;179;337;380;600;616;630)",

                 120, 521193611,   8154434,  15083398, "Tiensevest"),
        new Val( 121,  15083398, 185988816, 185988816, "Tiensevest"),
        new Val("Tiensevest (7;8;9;18;179;337;380;600;616;630)",

                 122,  15083398, 185988816, 185988814, "Tiensevest"),
        new Val( 123, 185988814,  19793223,  19793223, "Tiensevest"),
        new Val("Tiensevest (1;4;5;6;7;8;9;18;179;337;380;600;616;630)",

                 124, 185988814,  19793223,  81522744, "Tiensevest"),
        new Val( 125,  81522744,  89574079,  89574079, "Tiensevest"),
        new Val("Tiensevest (1;2;3;4;5;6;7;8;9;18;179;284;285;315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630)",

                 126,  81522744,  89574079,  79265237, "Tiensevest"),
        new Val( 127,  79265237,  84696751,  84696751, "Tiensevest"),
        new Val("Tiensevest (2;3;4;5;6;7;8;9;18;179;333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630)",

                 128,  79265237,  84696751,  78815505, "Tiensevest"),
        new Val("Tiensevest (2;3;4;5;6;7;8;9;18;179;333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",

                 129,  78815505,  78815505,  79193581, "Tiensevest"),
        new Val("Tiensevest (2;3;4;5;6;7;8;9;18;179;333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",

                 130,  79193581,  79193581,  79193580, "Tiensevest"),
        new Val("Tiensevest (3;4;5;6;7;8;9;18;179;333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630)",

                 131,  79193580,  79193580, 258936980, "Tiensevest"),
        new Val("Tiensevest (3;4;5;6;7;8;9;18;179;333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658)",

                 132, 258936980, 258936980,  79193579, "Tiensevest"),
        new Val("Tiensevest (3;4;5;6;7;8;9;18;179;333;334;335;337;380;513;600;601;616;630;658)",

                 133,  79193579,  79193579,  79596980, null),
        new Val( 134,  79596980, 377918658, 377918658, null),
        new Val("perron 11 & 12 (3;333;334;335;513;600;630)",

                 135,  79596980, 377918658,  79596987, null),
        new Val("perron 11 & 12 (3;333;334;335;513;600;630)",

                 136,  79596980,  79596987,  79596982, null),
        new Val( 137,  79596980,  79596974,  79596974, null),
        new Val("perron 11 & 12 (3;333;334;335;433;600;630)",

                 138,  79596982,  79596974,  79596965, null),
        new Val( 139,  79596965,  79264890,  79264890, null),
        new Val( 140,  79596965,  76867049,  76867049, null),
        new Val("(2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",

                 141,  79596965,  76867049, 451873774, "Diestsepoort"),
        new Val( 142, 451873774, 584356742, 584356742, "Diestsepoort"),
        new Val( 143, 451873774, 451873773, 451873773, "Diestsepoort"),
        new Val( 144, 451873774, 584356751, 584356751, "Diestsepoort"),
        new Val( 145, 451873774, 584356751, 198559166, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",

                 146, 451873774, 198559166, 584356745, "Diestsepoort"),
        new Val( 147, 584356745, 584356749, 584356749, "Diestsepoort"),
        new Val( 148, 584356745, 663770966, 663770966, "Diestsepoort"),
        new Val( 149, 584356745,  61556877,  61556877, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;305;306;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",

                 150, 584356745,  61556877,   8109264, "Diestsepoort"),
        new Val( 151,   8109264,   4061640,   4061640, "Vuurkruisenlaan"),
        new Val( 152,   8109264,  23691160,  23691160, "Vuurkruisenlaan"),
        new Val("Diestsepoort - Vuurkruisenlaan (305;333;334;335;513;600;630;651;652;658)",

                 153,   8109264,  23691160,  61540068, "Joanna-Maria Artoisplein"),
        new Val( 154,  61540068, 254801390, 254801390, "Joanna-Maria Artoisplein"),
        new Val("Joanna-Maria Artoisplein (178;305;318;333;334;335;410;513;600;630;651;652;658)",

                 155,  61540068, 254801390, 340265962, "Redersstraat"),
        new Val("Redersstraat (333;334;335;513;600;630)",

                 156, 340265962, 340265962, 318825613, "Redersstraat"),
        new Val( 157, 318825613, 340265961, 340265961, "Redersstraat"),
        new Val("Redersstraat (600)",

                 158, 318825613, 340265961, 304241967, "Aarschotsesteenweg"),
        new Val( 159, 304241967, 304241968, 304241968, "Havenkant"),
        new Val("Aarschotsesteenweg - Havenkant (334;335;513;600;630)",

                 160, 304241967, 304241968,  29283599, "Havenkant"),
        new Val( 161,  29283599, 510790348, 510790348, "Havenkant"),
        new Val( 162,  29283599, 314635787, 314635787, "Havenkant"),
        new Val( 163,  29283599, 843534478, 843534478, "Havenkant"),
        new Val( 164,  29283599, 406205781, 406205781, "Havenkant"),
        new Val( 165,  29283599, 270181176, 270181176, "Havenkant"),
        new Val( 166,  29283599, 330300725, 330300725, "Havenkant"),
        new Val( 167,  29283599,   3869822,   3869822, "Burchtstraat"),
        new Val( 168,  29283599, 330300723, 330300723, "Achter de latten"),
        new Val( 169,  29283599, 659297690, 659297690, "Wolvengang"),
        new Val( 170,  29283599,  25928482,  25928482, "Wolvengang"),
        new Val( 171,  29283599,   3869812,   3869812, "Engels Plein"),
        new Val( 172,  29283599, 305316104, 305316104, "Engels Plein"),
        new Val( 173,  29283599, 338057819, 338057819, "Engels Plein"),
        new Val("Havenkant - Engels Plein (600)",

                 174,  29283599, 608715622,         0, "Engels Plein")
    );

    testPtLine(expectedValues, PT_ROUTE_BUS_600_TO_LEUVEN_VAARTKOM);

        // ***********************************************************
        // Line 310 has 3 variants
        // The shorter version goes from Leuven station to
        // Aarschot station, making a spoon loop in Holsbeek
        // ***********************************************************

    expectedValues = Arrays.asList(
        new Val(  32, 377918665,         0, 377918665, null),
        new Val(  33, 377918665,  79596983,  79596983, null),
        new Val(  34, 377918665,  79264897,  79264897, null),
        new Val("perron 7 & 8 (2;3;310)",

                  35, 377918665,  79264897,  71754927, null),
        new Val("(2;3;310;370;371;373;374;475;485;513;520;524;525)",

                  36,  71754927,  71754927,  79596965, null),
        new Val(  37,  79596965,  79264890,  79264890, null),
        new Val(  38,  79596965,  76867049,  76867049, null),
        new Val("(2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630)",

                  39,  79596965,  76867049, 451873774, "Diestsepoort"),
        new Val(  40, 451873774, 584356742, 584356742, "Diestsepoort"),
        new Val(  41, 451873774, 451873773, 451873773, "Diestsepoort"),
        new Val(  42, 451873774, 584356751, 584356751, "Diestsepoort"),
        new Val(  43, 451873774, 198559166, 198559166, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658)",

                  44, 451873774, 198559166, 584356745, "Diestsepoort"),
        new Val(  45, 584356745, 584356749, 584356749, "Diestsepoort"),
        new Val(  46, 584356745, 663770966, 663770966, "Diestsepoort"),
        new Val(  47, 584356745,  61556877,  61556877, "Diestsepoort"),
        new Val("Diestsepoort (2;3;179;305;306;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658)",

                  48, 584356745,  61556877,  12715116, "Diestsesteenweg"),
        new Val(  49,  12715116,  23707244,  23707244, "Diestsesteenweg"),
        new Val("Diestsesteenweg (2;3;179;306;310;370;371;373;374;433;475;485;520;524;525)",

                  50,  12715116,  23707244, 125835568, "Leuvensestraat"),
        new Val(  51, 125835568, 429571921, 429571921, "Baron August de Becker Remyplein"),
        new Val(  52, 125835568, 608715603, 608715603, "Baron August de Becker Remyplein"),
        new Val(  53, 125835568, 608715604, 608715604, "Baron August de Becker Remyplein"),
        new Val(  54, 125835568, 608715601, 608715601, "Baron August de Becker Remyplein"),
        new Val(  55, 125835568, 608715602, 608715602, "Baron August de Becker Remyplein"),
        new Val(  56, 125835568,  35633068,  35633068, "Baron August de Becker Remyplein"),
        new Val(  57, 125835568,  79366164,  79366164, "Baron August de Becker Remyplein"),
        new Val(  58, 125835568,  79366167,  79366167, "Baron August de Becker Remyplein"),
        new Val("Leuvensestraat - Baron August de Becker Remyplein (2;179;306;310;433;520)",

                  59, 125835568,  79366167,  79366165, "Baron August de Becker Remyplein"),
        new Val("Baron August de Becker Remyplein (2;179;306;310;433;520)",

                  60,  79366165,  79366165, 125835547, "Eénmeilaan"),
        new Val(  61, 125835547, 138017381, 138017381, "Eénmeilaan"),
        new Val(  62, 125835547,  22476095,  22476095, "Eénmeilaan"),
        new Val(  63, 125835547, 138017382, 138017382, "Eénmeilaan"),
        new Val(  64, 125835547, 138017384, 138017384, "Eénmeilaan"),
        new Val(  65, 125835547,  22476094,  22476094, "Eénmeilaan"),
        new Val(  66, 125835547, 608715592, 608715592, "Eénmeilaan"),
        new Val(  67, 125835547, 608715591, 608715591, "Eénmeilaan"),
        new Val(  68, 125835547, 180167956, 180167956, "Eénmeilaan"),
        new Val(  69, 125835547, 125835546, 125835546, "Eénmeilaan"),
        new Val(  70, 125835547, 138017385, 138017385, "Eénmeilaan"),
        new Val(  71, 125835547,  38245752,  38245752, "Eénmeilaan"),
        new Val(  72, 125835547,  38245753,  38245753, "Eénmeilaan"),
        new Val(  73, 125835547,  80284458,  80284458, "Eénmeilaan"),
        new Val(  74, 125835547, 112566015, 112566015, "Eénmeilaan"),
        new Val("Eénmeilaan (179;306;310;433)",

                  75, 125835547, 112566015,  33233154, "Kesseldallaan"),
        new Val("Kesseldallaan (2;179;306;310;433)",

                  76,  33233154,  33233154, 242622340, "Kesseldallaan"),
        new Val(  77, 242622340, 112565986, 112565986, "Kesseldallaan"),
        new Val(  78, 242622340, 112566011, 112566011, "Kesseldallaan"),
        new Val(  79, 242622340, 440732695, 440732695, "Kesseldallaan"),
        new Val(  80, 242622340, 440732694, 440732694, "Kesseldallaan"),
        new Val(  81, 242622340, 112566010, 112566010, "Kesseldallaan"),
        new Val(  82, 242622340, 440732696, 440732696, "Kesseldallaan"),
        new Val(  83, 242622340, 112566021, 112566021, "Kesseldallaan"),
        new Val(  84, 242622340, 112566017, 112566017, "Kesseldallaan"),
        new Val("Kesseldallaan (2;179;306;310;433)",

                  85, 242622340, 112566017, 112566013, "Kesseldallaan"),
        new Val(  86, 112566013, 125835556, 125835556, "Kesseldallaan"),
        new Val(  87, 112566013,  38209648,  38209648, "Wilselsesteenweg"),
        new Val(  88, 112566013,  98645223,  98645223, "Wilselsesteenweg"),
        new Val(  89, 112566013,  98645243,  98645243, "Wilselsesteenweg"),
        new Val("Kesseldallaan - Wilselsesteenweg (2;310)",

                  90, 112566013,  98645243, 243884410, "Wilselsesteenweg"),
        new Val(  91, 243884410, 114935420, 114935420, "Wilselsesteenweg"),
        new Val(  92, 243884410, 192559637, 192559637, "Wilselsesteenweg"),
        new Val(  93, 243884410,  18943297,  18943297, "Wilselsesteenweg"),
        new Val(  94, 243884410,  14393833,  14393833, "Leuvensebaan"),
        new Val(  95, 243884410,  10699037,  10699037, "Leuvensebaan"),
        new Val(  96, 243884410, 311007038, 311007038, "Leuvensebaan"),
        new Val(  97, 243884410, 311007039, 311007039, "Leuvensebaan"),
        new Val(  98, 243884410, 311007034, 311007034, "Leuvensebaan"),
        new Val(  99, 243884410, 311007041, 311007041, "Leuvensebaan"),
        new Val( 100, 243884410, 311007033, 311007033, "Leuvensebaan"),
        new Val( 101, 243884410, 311007036, 311007036, "Leuvensebaan"),
        new Val( 102, 243884410, 192559618, 192559618, "Leuvensebaan"),
        new Val( 103, 243884410, 311007040, 311007040, "Leuvensebaan"),
        new Val( 104, 243884410, 311007035, 311007035, "Leuvensebaan"),
        new Val( 105, 243884410, 311007042, 311007042, "Leuvensebaan"),
        new Val( 106, 243884410, 311007037, 311007037, "Leuvensebaan"),
        new Val( 107, 243884410, 440732697, 440732697, "Leuvensebaan"),
        new Val( 108, 243884410,  16302926,  16302926, "Leuvensebaan"),
        new Val( 109, 243884410,  16302927,  16302927, "Leuvensebaan"),
        new Val( 110, 243884410,  37370702,  37370702, "Nobelberg"),
        new Val( 111, 243884410, 310461931, 310461931, "Nobelberg"),
        new Val( 112, 243884410, 492765776, 492765776, "Nobelberg"),
        new Val( 113, 243884410,  19166179,  19166179, "Nobelberg"),
        new Val( 114, 243884410, 310435232, 310435232, "Nobelberg"),
        new Val("Wilselsesteenweg - Nobelberg (310)",

                 115, 243884410, 310435232,  64487963, "Rotselaarsebaan"),
        new Val( 116,  64487963, 310435233, 310435233, "Rotselaarsebaan"),
        new Val("Rotselaarsebaan (310)",

                 117,  64487963, 310435233,  25667798, "Rotselaarsebaan"),
        new Val( 118,  25667798,  16302928,  16302928, null),
        new Val( 119,  25667798, 112566004, 112566004, "Sint-Maurusstraat"),
        new Val("Rotselaarsebaan - Sint-Maurusstraat (310)",

                 120,  25667798, 112566004, 310435233, "Rotselaarsebaan"),
        new Val( 121, 310435233,  64487963,  64487963, "Rotselaarsebaan"),
        new Val("Rotselaarsebaan (310)",

                 122, 310435233,  64487963,  16302925, "Nobelberg"),
        new Val( 123,  16302925, 181347873, 181347873, "Nobelberg"),
        new Val( 124,  16302925, 450119553, 450119553, "Kortrijksebaan"),
        new Val( 125,  16302925, 310435229, 310435229, "Kortrijksebaan"),
        new Val( 126,  16302925,  19166177,  19166177, "Kortrijksebaan"),
        new Val( 127,  16302925, 181347876, 181347876, "Kortrijksebaan"),
        new Val( 128,  16302925,  13858520,  13858520, "Kortrijksebaan"),
        new Val( 129,  16302925, 173566709, 173566709, "Kortrijksebaan"),
        new Val( 130,  16302925,  23957648,  23957648, "Kortrijksebaan"),
        new Val( 131,  16302925, 310435230, 310435230, "Kortrijksebaan"),
        new Val( 132,  16302925,  19112638,  19112638, "Kortrijksebaan"),
        new Val( 133,  16302925, 659289514, 659289514, "Kortrijksebaan"),
        new Val( 134,  16302925, 441280731, 441280731, "Kortrijksebaan"),
        new Val( 135,  16302925, 659289513, 659289513, "Kortrijksebaan"),
        new Val( 136,  16302925,  38502235,  38502235, "Kortrijksebaan"),
        new Val( 137,  16302925, 310306431, 310306431, "Kortrijksebaan"),
        new Val( 138,  16302925, 310306259, 310306259, "Dutselstraat"),
        new Val( 139,  16302925,  22724214,  22724214, "Dutselstraat"),
        new Val( 140,  16302925, 310306260, 310306260, "Dutselstraat"),
        new Val( 141,  16302925, 185929631, 185929631, "Dutselstraat"),
        new Val( 142,  16302925, 185929632, 185929632, "Dutselstraat"),
        new Val( 143,  16302925, 137613419, 137613419, "Dutselstraat"),
        new Val( 144,  16302925, 185929630, 185929630, "Dutselstraat"),
        new Val( 145,  16302925,  13858519,  13858519, "Dutselstraat"),
        new Val( 146,  16302925, 540115503, 540115503, "Gravenstraat"),
        new Val( 147,  16302925, 182275357, 182275357, "Gravenstraat"),
        new Val( 148,  16302925,  13858500,  13858500, "Gravenstraat"),
        new Val( 149,  16302925,  13858502,  13858502, "Gravenstraat"),
        new Val( 150,  16302925,  38501970,  38501970, "Gravenstraat"),
        new Val( 151,  16302925,  13858501,  13858501, "Gravenstraat"),
        new Val( 152,  16302925,  23334102,  23334102, "Gravenstraat"),
        new Val( 153,  16302925, 137613435, 137613435, "Rodestraat"),
        new Val( 154,  16302925,  13858504,  13858504, "Rodestraat"),
        new Val( 155,  16302925, 118479385, 118479385, "Rodestraat"),
        new Val( 156,  16302925, 182275362, 182275362, "Rodestraat"),
        new Val( 157,  16302925, 182275361, 182275361, "Rodestraat"),
        new Val( 158,  16302925, 117329495, 117329495, "Rodestraat"),
        new Val( 159,  16302925, 318267937, 318267937, "Rodestraat"),
        new Val( 160,  16302925, 318267938, 318267938, "Rodestraat"),
        new Val( 161,  16302925, 137613436, 137613436, "Rodestraat"),
        new Val( 162,  16302925, 615260297, 615260297, "Rodestraat"),
        new Val( 163,  16302925,  10242646,  10242646, "Rodestraat"),
        new Val( 164,  16302925, 112565994, 112565994, "Appelweg"),
        new Val( 165,  16302925, 137613417, 137613417, "Appelweg"),
        new Val( 166,  16302925,  10242649,  10242649, "Dorp"),
        new Val( 167,  16302925, 187995152, 187995152, "Dorp"),
        new Val( 168,  16302925, 137613438, 137613438, "Sint-Lambertusstraat"),
        new Val( 169,  16302925, 151083447, 151083447, "Sint-Lambertusstraat"),
        new Val( 170,  16302925, 609510455, 609510455, "Rijksweg"),
        new Val( 171,  16302925, 609510456, 609510456, "Rijksweg"),
        new Val( 172,  16302925,  38208831,  38208831, "Rijksweg"),
        new Val( 173,  16302925,  38208830,  38208830, "Rijksweg"),
        new Val( 174,  16302925, 345078878, 345078878, "Rijksweg"),
        new Val( 175,  16302925, 345078877, 345078877, "Rijksweg"),
        new Val("Nobelberg - Rijksweg (310)",

                 176,  16302925, 345078877,  13856508, "Rijksweg Aarschot-Winge"),
        new Val( 177,  13856508, 345078875, 345078875, "Rijksweg"),
        new Val( 178,  13856508,  22870199,  22870199, "Rijksweg"),
        new Val( 179,  13856508, 136845549, 136845549, "Nieuwrodesesteenweg"),
        new Val( 180,  13856508, 345078865, 345078865, "Steenweg op Sint-Joris-Winge"),
        new Val( 181,  13856508, 345078870, 345078870, "Steenweg op Sint-Joris-Winge"),
        new Val( 182,  13856508, 345078866, 345078866, "Nieuwrodesesteenweg"),
        new Val( 183,  13856508,  26431904,  26431904, "Nieuwrodesesteenweg"),
        new Val( 184,  13856508, 521257382, 521257382, "Steenweg op Sint-Joris-Winge"),
        new Val( 185,  13856508, 521257383, 521257383, "Steenweg op Sint-Joris-Winge"),
        new Val( 186,  13856508, 137111763, 137111763, "Steenweg op Sint-Joris-Winge"),
        new Val( 187,  13856508,  13226856,  13226856, "Steenweg op Sint-Joris-Winge"),
        new Val( 188,  13856508, 345088980, 345088980, "Steenweg op Sint-Joris-Winge"),
        new Val( 189,  13856508, 345088979, 345088979, "Steenweg op Sint-Joris-Winge"),
        new Val( 190,  13856508, 102853233, 102853233, null),
        new Val( 191,  13856508, 112565995, 112565995, "Steenweg op Sint-Joris-Winge"),
        new Val( 192,  13856508, 345088986, 345088986, "Steenweg op Sint-Joris-Winge"),
        new Val( 193,  13856508, 345152123, 345152123, "Steenweg op Sint-Joris-Winge"),
        new Val( 194,  13856508, 102853195, 102853195, "Steenweg op Sint-Joris-Winge"),
        new Val( 195,  13856508, 181252214, 181252214, "Steenweg op Sint-Joris-Winge"),
        new Val( 196,  13856508, 602060323, 602060323, "Steenweg op Sint-Joris-Winge"),
        new Val( 197,  13856508, 345152132, 345152132, "Steenweg op Sint-Joris-Winge"),
        new Val( 198,  13856508, 412810127, 412810127, "Steenweg op Sint-Joris-Winge"),
        new Val( 199,  13856508, 345152127, 345152127, "Steenweg op Sint-Joris-Winge"),
        new Val("Rijksweg Aarschot-Winge - Steenweg op Sint-Joris-Winge (305;306;310)",

                 200,  13856508, 345152127, 125101583, "Leuvensesteenweg"),
        new Val( 201, 125101583, 188863432, 188863432, "Leuvensesteenweg"),
        new Val("Leuvensesteenweg (37;305;306;310;334;335;513)",

                 202, 125101583, 188863432, 125101584, "Leuvensesteenweg"),
        new Val( 203, 125101584, 412810122, 412810122, "Leuvensesteenweg"),
        new Val( 204, 125101584,  13892080,  13892080, "Albertlaan"),
        new Val("Leuvensesteenweg - Albertlaan (37;310;334;335;513)",

                 205, 125101584,  13892080, 125101554, "Albertlaan"),
        new Val("Albertlaan (37;220;305;306;310;334;335;513)",

                 206, 125101554, 125101554,  41405911, "Statiestraat"),
        new Val( 207,  41405911, 101376793, 101376793, "Statiestraat"),
        new Val( 208,  41405911, 125101590, 125101590, "Statieplein"),
        new Val("Statiestraat - Statieplein (35;36;37;160;161;220;221;305;306;310;334;335;390;392;491;492;530;532;590)",

                 209,  41405911, 125101590, 100289852, "Statieplein"),
        new Val("Statieplein (35;36;37;160;161;305;306;310;334;335;390;392;491;492;530;532;590)",

                 210, 100289852, 100289852, 100289892, "Statieplein"),
        new Val("Statieplein (35;36;37;305;306;310;334;335;390;392;491;492;530;532;590)",

                 211, 100289892, 100289892, 100289859, "Statieplein"),
        new Val("Statieplein (35;36;37;305;306;310;334;335;390;392;491;492;590)",

                 212, 100289859, 100289859, 100289896, "Statieplein"),
        new Val("Statieplein (35;36;37;310;334;335;390;392;590)",

                 213, 100289896,  13856192,         0, "Statieplein")
    );

    testPtLine(expectedValues, PT_ROUTE_BUS_310_TO_AARSCHOT_STATION);

        // ***********************************************************
        // Line 433 is an express bus
        // During interactive testing it causes an Index out of bounds exception
        // for the variant that goes from Tremelo to Leuven
        // Line 333 has the same issue, but 433 is a bit shorter
        // ***********************************************************

    expectedValues = Arrays.asList(
        new Val(  17, 520502050,         0, 520502050, null),
        new Val(  18, 520502050, 255018553, 255018553, null),
        new Val(  19, 520502050, 845727172, 845727172, null),
        new Val(  20, 520502050, 255018560, 255018560, null),
        new Val(  21, 520502050,  26614159,  26614159, null),
        new Val(  22, 520502050,  20402695,  20402695, "Astridstraat"),
        new Val("Astridstraat (333;433;520;527;530)",

                  23, 520502050,  20402695, 116911330, "Schrieksebaan"),
        new Val(  24, 116911330, 348402887, 348402887, "Schrieksebaan"),
        new Val(  25, 116911330,  79057162,  79057162, "Schrieksebaan"),
        new Val("Schrieksebaan (333;433;520;527;530)",

                  26, 116911330,  79057162, 244077928, "Schrieksebaan"),
        new Val("Schrieksebaan (333;433;520;527;530;532)",

                  27, 244077928, 244077928,  25706520, "Schrieksebaan"),
        new Val(  28,  25706520, 106526457, 106526457, null),
        new Val(  29,  25706520,  79057173,  79057173, null),
        new Val("Schrieksebaan (333;433;520;527;532)",

                  30,  25706520,  79057173, 106526331, null),
        new Val("(333;433;520;527;530;532)",

                  31, 106526331, 106526331, 106526391, "Werchtersebaan"),
        new Val(  32, 106526391, 106526409, 106526409, "Werchtersebaan"),
        new Val(  33, 106526391, 116911314, 116911314, "Werchtersebaan"),
        new Val(  34, 106526391, 116911310, 116911310, "Werchtersebaan"),
        new Val(  35, 106526391, 116911322, 116911322, "Werchtersebaan"),
        new Val(  36, 106526391, 116911317, 116911317, "Werchtersebaan"),
        new Val(  37, 106526391,  22080665,  22080665, "Werchtersebaan"),
        new Val(  38, 106526391,  22080651,  22080651, "Tremelobaan"),
        new Val(  39, 106526391, 126264637, 126264637, "Tremelobaan"),
        new Val(  40, 106526391,  25706405,  25706405, "Tremelobaan"),
        new Val(  41, 106526391, 349648270, 349648270, "Tremelobaan"),
        new Val(  42, 106526391, 349648273, 349648273, "Tremelobaan"),
        new Val(  43, 106526391, 349648272, 349648272, "Tremelobaan"),
        new Val(  44, 106526391,  25706363,  25706363, "Tremelobaan"),
        new Val(  45, 106526391, 349648271, 349648271, "Tremelobaan"),
        new Val("Werchtersebaan - Tremelobaan (333;433;532)",

                  46, 106526391, 349648271,  32823731, "Tremelobaan"),
        new Val(  47,  32823731, 116704290, 116704290, "Tremelobaan"),
        new Val(  48,  32823731,  49577807,  49577807, "Sint-Jansstraat"),
        new Val(  49,  32823731, 205303320, 205303320, "Sint-Jansstraat"),
        new Val(  50,  32823731, 205303322, 205303322, "Sint-Jansstraat"),
        new Val(  51,  32823731, 205303321, 205303321, "Sint-Jansstraat"),
        new Val(  52,  32823731, 116911351, 116911351, "Sint-Jansstraat"),
        new Val(  53,  32823731,  21355673,  21355673, "Sint-Jansstraat"),
        new Val(  54,  32823731,  26591703,  26591703, "Werchterplein"),
        new Val(  55,  32823731,  24037032,  24037032, "Hoekje"),
        new Val(  56,  32823731,  13858894,  13858894, "Hoekje"),
        new Val(  57,  32823731, 292820652, 292820652, "Nieuwebaan"),
        new Val(  58,  32823731,  98918943,  98918943, "Nieuwebaan"),
        new Val(  59,  32823731, 292670109, 292670109, "Nieuwebaan"),
        new Val(  60,  32823731, 292670115, 292670115, "Nieuwebaan"),
        new Val(  61,  32823731, 292670113, 292670113, "Nieuwebaan"),
        new Val(  62,  32823731, 292670113, 292670110, "Nieuwebaan"),
        new Val(  63,  32823731, 292670113, 292670112, "Nieuwebaan"),
        new Val(  64,  32823731, 292670113,  24041208, "Provinciebaan"),
        new Val("Tremelobaan - Nieuwebaan (333;433)",

                  65,  32823731, 292670113, 100086271, "Provinciebaan"),
        new Val(  66, 100086271, 116535777, 116535777, "Provinciebaan"),
        new Val(  67, 100086271,  37712785,  37712785, "Provinciebaan"),
        new Val(  68, 100086271,  25706276,  25706276, "Provinciebaan"),
        new Val(  69, 100086271,  25706275,  25706275, "Provinciebaan"),
        new Val(  70, 100086271, 100083559, 100083559, "Provinciebaan"),
        new Val(  71, 100086271, 521082436, 521082436, "Provinciebaan"),
        new Val(  72, 100086271, 116535766, 116535766, "Provinciebaan"),
        new Val(  73, 100086271, 116535756, 116535756, "Provinciebaan"),
        new Val(  74, 100086271,  13899067,  13899067, "Provinciebaan"),
        new Val("Provinciebaan (333;433;512;513)",

                  75, 100086271,  13899067,  15220439, "Stationsstraat"),
        new Val(  76,  15220439,  23024090,  23024090, "Stationsstraat"),
        new Val(  77,  15220439,  23024091,  23024091, "Stationsstraat"),
        new Val(  78,  15220439,  98825487,  98825487, "Stationsstraat"),
        new Val(  79,  15220439, 663909838, 663909838, "Stationsstraat"),
        new Val(  80,  15220439,  23781189,  23781189, "Stationsstraat"),
        new Val("Stationsstraat (333;335;433;513)",

                  81,  15220439,  23781189,  16201055, "Aarschotsesteenweg"),
        new Val(  82,  16201055,  14099966,  14099966, "Aarschotsesteenweg"),
        new Val(  83,  16201055,  14099896,  14099896, "Aarschotsesteenweg"),
        new Val(  84,  16201055, 441677446, 441677446, "Aarschotsesteenweg"),
        new Val(  85,  16201055, 439397679, 439397679, "Aarschotsesteenweg"),
        new Val(  86,  16201055, 440732693, 440732693, "Aarschotsesteenweg"),
        new Val(  87,  16201055, 663909831, 663909831, "Aarschotsesteenweg"),
        new Val(  88,  16201055,  16201617,  16201617, "Aarschotsesteenweg"),
        new Val(  89,  16201055,  38159437,  38159437, "Aarschotsesteenweg"),
        new Val(  90,  16201055, 780600573, 780600573, "Aarschotsesteenweg"),
        new Val(  91,  16201055, 780600572, 780600572, "Aarschotsesteenweg"),
        new Val(  92,  16201055,  38159440,  38159440, "Aarschotsesteenweg"),
        new Val(  93,  16201055, 345381434, 345381434, "Aarschotsesteenweg"),
        new Val("Aarschotsesteenweg (333;334;335;433;512;513)",

                  94,  16201055, 345381434,  79299306, "Aarschotsesteenweg"),
        new Val(  95,  79299306, 345381435, 345381435, "Aarschotsesteenweg"),
        new Val(  96,  79299306, 863731643, 863731643, "Aarschotsesteenweg"),
        new Val(  97,  79299306, 631979428, 631979428, "Aarschotsesteenweg"),
        new Val(  98,  79299306,  37713149,  37713149, "Aarschotsesteenweg"),
        new Val(  99,  79299306, 377918608, 377918608, "Aarschotsesteenweg"),
        new Val( 100,  79299306, 377918607, 377918607, "Aarschotsesteenweg"),
        new Val( 101,  79299306, 345381437, 345381437, "Aarschotsesteenweg"),
        new Val( 102,  79299306, 440732691, 440732691, "Aarschotsesteenweg"),
        new Val( 103,  79299306, 351000218, 351000218, "Aarschotsesteenweg"),
        new Val( 104,  79299306,  13882237,  13882237, "Aarschotsesteenweg"),
        new Val("Aarschotsesteenweg (333;334;335;433;512;513;630)",

                 105,  79299306,  13882237,   4004855, "Vuntcomplex"),
        new Val( 106,   4004855, 345381453, 345381453, "Vuntcomplex"),
        new Val( 107,   4004855, 345381452, 345381452, "Vuntcomplex"),
        new Val("Vuntcomplex (333;433;512)",

                 108,   4004855, 345381452, 225497053, "Vuntcomplex"),
        new Val( 109, 225497053, 345381449, 345381449, "Vuntcomplex"),
        new Val("Vuntcomplex (179;306;333;433;512)",

                 110, 225497053, 345381449, 315666515, "Vuntcomplex"),
        new Val("Vuntcomplex (179;306;333;433;512)",

                 111, 315666515, 315666515, 315666514, "Vuntcomplex"),
        new Val( 112, 315666514,  17540932,  17540932, "Vuntcomplex"),
        new Val( 113, 315666514,  17540907,  17540907, "Vuntcomplex"),
        new Val( 114, 315666514, 149117671, 149117671, "Duchesnelaan"),
        new Val( 115, 315666514, 192559635, 192559635, "Duchesnelaan"),
        new Val("Vuntcomplex - Duchesnelaan (179;306;333;433;512)",

                 116, 315666514, 192559635, 112565989, "Kesseldallaan"),
        new Val( 117, 112565989,  13823565,  13823565, "Kesseldallaan"),
        new Val( 118, 112565989, 440732696, 440732696, "Kesseldallaan"),
        new Val( 119, 112565989, 112566010, 112566010, "Kesseldallaan"),
        new Val( 120, 112565989, 440732694, 440732694, "Kesseldallaan"),
        new Val( 121, 112565989, 440732695, 440732695, "Kesseldallaan"),
        new Val( 122, 112565989, 112566011, 112566011, "Kesseldallaan"),
        new Val( 123, 112565989, 125835557, 125835557, "Kesseldallaan"),
        new Val( 124, 112565989, 242622344, 242622344, "Kesseldallaan"),
        new Val("Kesseldallaan (2;179;306;310;333;433;512)",

                 125, 112565989, 242622344, 112566018, "Eénmeilaan"),
        new Val( 126, 112566018,  80284458,  80284458, "Eénmeilaan"),
        new Val( 127, 112566018,  38245753,  38245753, "Eénmeilaan"),
        new Val( 128, 112566018,  38245752,  38245752, "Eénmeilaan"),
        new Val( 129, 112566018, 138017385, 138017385, "Eénmeilaan"),
        new Val( 130, 112566018, 138017383, 138017383, "Eénmeilaan"),
        new Val( 131, 112566018, 608715591, 608715591, "Eénmeilaan"),
        new Val( 132, 112566018, 608715592, 608715592, "Eénmeilaan"),
        new Val( 133, 112566018,  22476094,  22476094, "Eénmeilaan"),
        new Val( 134, 112566018, 138017384, 138017384, "Eénmeilaan"),
        new Val( 135, 112566018, 138017386, 138017386, "Eénmeilaan"),
        new Val( 136, 112566018, 125835547, 125835547, "Eénmeilaan"),
        new Val("Eénmeilaan (179;306;310;333;433;512)",

                 137, 112566018, 125835547, 125835500, "Baron August de Becker Remyplein"),
        new Val( 138, 125835500,  35633068,  35633068, "Baron August de Becker Remyplein"),
        new Val( 139, 125835500, 608715602, 608715602, "Baron August de Becker Remyplein"),
        new Val( 140, 125835500, 608715601, 608715601, "Baron August de Becker Remyplein"),
        new Val( 141, 125835500, 608715604, 608715604, "Baron August de Becker Remyplein"),
        new Val( 142, 125835500, 608715603, 608715603, "Baron August de Becker Remyplein"),
        new Val( 143, 125835500, 429571921, 429571921, "Baron August de Becker Remyplein"),
        new Val( 144, 125835500, 125835568, 125835568, "Leuvensestraat"),
        new Val("Baron August de Becker Remyplein - Leuvensestraat (2;179;306;310;333;433;512;520)",

                 145, 125835500, 125835568,  23707244, "Diestsesteenweg"),
        new Val( 146,  23707244, 125835524, 125835524, "Diestsesteenweg"),
        new Val( 147,  23707244,   8109264,   8109264, "Diestsepoort"),
        new Val("Diestsesteenweg - Diestsepoort (2;3;179;306;310;333;370;371;373;374;433;475;485;512;520;524;525)",

                 148,  23707244,   8109264,  61556877, "Diestsepoort"),
        new Val( 149,  61556877, 663770966, 663770966, "Diestsepoort"),
        new Val( 150,  61556877, 584356749, 584356749, "Diestsepoort"),
        new Val( 151,  61556877, 584356745, 584356745, "Diestsepoort"),
        new Val( 152,  61556877, 198559166, 198559166, "Diestsepoort"),
        new Val( 153,  61556877, 584356751, 584356751, "Diestsepoort"),
        new Val( 154,  61556877, 451873773, 451873773, "Diestsepoort"),
        new Val("Diestsepoort (2;3;178;179;306;310;333;370;371;373;374;433;475;485;512;520;524;525)",

                 155,  61556877, 451873773, 584356742, "Diestsepoort"),
        new Val( 156, 584356742, 451873774, 451873774, "Diestsepoort"),
        new Val("Diestsepoort (2;3;178;179;306;310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525)",

                 157, 584356742, 451873774, 116797180, "Diestsepoort"),
        new Val("Diestsepoort (306;310;318;410;433;475;485)",

                 158, 116797180, 116797180, 116797179, "Diestsepoort"),
        new Val("Diestsepoort (306;310;318;410;433)",

                 159, 116797179, 116797179,   4884707, "Tiensevest"),
        new Val("Tiensevest (284;285;306;310;315;316;317;395;433;475;485)",

                 160,   4884707,   4884707,  19793164, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;651;652;658)",

                 161,  19793164,  19793164,  80458208, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;651;652;658)",

                 162,  80458208,  80458208,  79175435, "Tiensevest"),
        new Val("Tiensevest (284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658)",

                 163,  79175435,  79175435,  79211472, "Tiensevest"),
        new Val("Tiensevest (1;2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658)",

                 164,  79211472,  79211472,  79211473, "Tiensevest"),
        new Val("Tiensevest (1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658)",

                 165,  79211473,  79211473,  79596986, null),
        new Val("(305;306;310;318;358;410;433;475;485;601;658)",

                 166,  79596986,  79596986, 377814547, null),
        new Val("perron 1 & 2 (1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658)",

                 167, 377814547,  78579065,         0, null)
    );

    testPtLine(expectedValues, PT_ROUTE_BUS_433_TO_LEUVEN_STATION);

//    generateListOfExpectedValues(PT_ROUTE_BUS_310_TO_AARSCHOT_STATION);

    }

    public void testPtLine(List<Val> expectedValues, Relation busRouteRelation) {
        ArrayList<RelationMember> segmentRelationsList = new ArrayList<>();
        ArrayList<Integer> indicesToRemoveList = new ArrayList<>();
        assertNotNull(busRouteRelation);

        Relation clonedRelation = new Relation(busRouteRelation);

        RouteSegmentToExtract segment = new RouteSegmentToExtract(busRouteRelation);
        segment.setActiveDataSet(DATASET_BUSES_BEFORE_SPLITTING);

        assertEquals(clonedRelation.get("ref"), segment.getLineIdentifiersSignature());
        assertEquals(clonedRelation.get("colour"), segment.getColoursSignature());

        assertNull(segment.extractToRelation(Collections.emptyList(),
            false, false));

        assertEquals("", segment.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment.getWayMembers());
        System.out.printf("***** %s *****\n\n     ", clonedRelation.get("name"));

        RouteSegmentToExtract previousSegment = segment;

        for (Val v: expectedValues) {
            segment = previousSegment.addPTWayMember(v.index);
            if (segment != null) {
                Relation extractedRelation = previousSegment.extractToRelation(Arrays.asList("type", "route"),
                    false, false);
                segmentRelationsList.add(new RelationMember("", extractedRelation));
                final String note = extractedRelation.get("note");
                System.out.println(v.index + " " + note);
                assertEquals(String.format("%d first way not correct %s%s\n%s%s\n", v.index, rc, v.iDOfFirstWay, rc, extractedRelation.firstMember().getWay().getId()), v.iDOfFirstWay, extractedRelation.firstMember().getWay().getId());
                assertEquals(String.format("%d last way not correct %s%s\n%s%s\n", v.index, rc, v.iDOfLastWay, rc, extractedRelation.lastMember().getWay().getId()), v.iDOfLastWay, extractedRelation.lastMember().getWay().getId());
                assertEquals(String.format("\nnote differs on segment\n%s%s,way%s", rc, v.iDOfFirstWay, v.iDOfLastWay), v.note, previousSegment.getNote());
                assertEquals(String.format("\nnote differs on relation\n%s%s,way%s", rc, v.iDOfFirstWay, v.iDOfLastWay), v.note, note);

                // newSegment should have the last way that was added to this segment
                if (v.iDOfNextWay != 0) {
                    assertNotNull(String.format("No new segment was created for way\n%s%s at position %d", rc, v.iDOfNextWay, v.index), segment);
                    final long wayId = segment.getWayMembers().get(0).getWay().getId();
                    assertEquals(String.format("%d name of last added way not correct %s%s\n ", v.index, rc, wayId), v.nameOfNextWay, segment.getWayMembers().get(0).getWay().get("name"));
                    assertEquals(String.format("%d id of first way not correct  %s%s\n", v.index, rc, wayId), v.iDOfNextWay, wayId);
                }
                previousSegment = segment;
            }
            if (v.index < clonedRelation.getMembersCount()) {
                indicesToRemoveList.add(0, v.index);
            }
        }

        for (Integer integer : indicesToRemoveList) {
            clonedRelation.removeMember(integer);
        }
        segmentRelationsList.forEach(clonedRelation::addMember);
        UndoRedoHandler.getInstance().add(new ChangeCommand(busRouteRelation, clonedRelation));

        System.out.printf("%s\n", busRouteRelation.getMembersCount());
        System.out.print("\n\n");
    }

    /**
     * This prints a list of known values to be used in unit tests
     *
     * @param relation route relation
     */
    @SuppressWarnings("unused")
    public void generateListOfExpectedValues(Relation relation) {
        List<RelationMember> members = relation.getMembers();
        RouteSegmentToExtract segment = new RouteSegmentToExtract(relation);
        List<RelationMember> wayMembers;
        String listVal = "";
        String className = "";
        Way way;
        String wayName = null;
        String note;
        StringBuilder output = new StringBuilder();
        output.append(String.format("    %sexpectedValues = Arrays.asList(\n", listVal));
        for (int i = 0; i < members.size() - 1; i++) {
            RelationMember member = members.get(i);
            if (member.isWay() && RouteUtils.isPTWay(member)) {
                way = member.getWay();
                wayName = (way != null && way.get("name") != null) ? "\"" + way.get("name") + "\"" : "null";
                note = segment.getNote();
                RouteSegmentToExtract ns = segment.addPTWayMember(i);
                wayMembers = segment.getWayMembers();
                if (ns != null) {
                    output.append(String.format("        new Val(\"%s\",\n\n                % 4d,% 10d,% 10d,% 10d, %s),\n",
                        note,
                        i,
                        wayMembers.get(0).getMember().getId(),
                        wayMembers.get(wayMembers.size() - 1).getMember().getId(),
                        (way != null && way.getId() != 0) ? way.getId() : 0,
                        wayName
                    ));
                    segment = ns;
                } else {
                    output.append(String.format("        new Val(% 4d,% 10d,% 10d,% 10d, %s),\n",
                        i,
                        (wayMembers.size() > 0) ? wayMembers.get(0).getMember().getId() : 0,
                        (wayMembers.size() > 1) ? wayMembers.get(wayMembers.size() - 1).getMember().getId() : 0,
                        (way != null && way.getId() != 0) ? way.getId() : 0,
                        wayName
                    ));
                }
            }
        }
        List<RelationMember> lastSegmentMembers = segment.getWayMembers();
        output.append(String.format("        new Val(\"%s\",\n\n                % 4d,% 10d,% 10d,% 10d, %s)\n    );\n\n",
            segment.getNote(),
            members.size() - 1,
            lastSegmentMembers.get(0).getMember().getId(),
            members.get(members.size() - 1).getMember().getId(),
            0,
            wayName
        ));

        StringBuilder lineName = new StringBuilder(relation.get("route"));
        if (relation.hasKey("ref")) lineName.append("_").append(relation.get("ref"));
        if (relation.hasKey("to")) lineName.append("_TO_").append(relation.get("to"));
        if (relation.hasKey("via")) {
            String via = relation.get("via");
            if (!via.contains("-")) lineName.append("_VIA_").append(via);
        }

        output.append(String.format("    testPtLine(expectedValues, PT_ROUTE_%s);\n", lineName.toString()
            .toUpperCase(Locale.ROOT)
            .replaceAll("\\sPERRON\\s\\d+", "")
            .replaceAll("\\s+", " ")
            .replaceAll(" ", "_")));

        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(output.toString()), null);

        System.out.print(output);
    }
}
