package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.junit.BeforeClass;
import org.junit.Test;
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

        final int W_158_perron1and2terminus_78579065_B = 78579065;
        final int W_157_perron1and2_377814547_A = 377814547;

        final int W_156_TiensevestToPerron1_79596986_A = 79596986;

        RouteSegmentToExtract returnValueNull;
        for (int n = 158; n >= 157; n--) {returnValueNull =segment1.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment2 = segment1.addPTWayMember(156);

        final Way veryLastWay = segment1.getWayMembers().get(1).getWay();
        assertEquals("perron 1 & 2", veryLastWay.get("ref"));
        assertEquals(W_158_perron1and2terminus_78579065_B, veryLastWay.getId());
        final Way beforeLastWay = segment1.getWayMembers().get(0).getWay();
        assertEquals("perron 1 & 2", beforeLastWay.get("ref"));
        assertEquals(W_157_perron1and2_377814547_A, beforeLastWay.getId());

        // segment2 was created, extract segment1 to its own relation
        extractAndAssertValues(156, segment1, segment2, cloneOfBus601RouteRelation,
            W_157_perron1and2_377814547_A, W_158_perron1and2terminus_78579065_B,
            W_156_TiensevestToPerron1_79596986_A, null,
            "#1199DD;#229922;#771133;#77CCAA;#8899AA;#991199;#995511;#BB0022;#BBDD00;#C5AA77;#DD0077;#DD5555;#FF88AA;#FFCC11",
            "1;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658");

        final int W_155_Tiensevest_5_79211473_A = 79211473;

        RouteSegmentToExtract segment3 = segment2.addPTWayMember(155);
        extractAndAssertValues(155, segment2, segment3, cloneOfBus601RouteRelation,
            W_156_TiensevestToPerron1_79596986_A, W_156_TiensevestToPerron1_79596986_A,
            W_155_Tiensevest_5_79211473_A, "Tiensevest",
            null,
            "305;306;310;318;358;410;433;475;485;601;658");

        final int W_154_Tiensevest_4_79211472_A = 79211472;

        RouteSegmentToExtract segment4 = segment3.addPTWayMember(154);
        extractAndAssertValues(154, segment3, segment4, cloneOfBus601RouteRelation,
            W_155_Tiensevest_5_79211473_A, W_155_Tiensevest_5_79211473_A,
            W_154_Tiensevest_4_79211472_A, "Tiensevest",
            null,
            "1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658");

        final int W_153_Tiensevest_3_79175435_A = 79175435;

        RouteSegmentToExtract segment5 = segment4.addPTWayMember(153);
        extractAndAssertValues(153, segment4, segment5, cloneOfBus601RouteRelation,
            W_154_Tiensevest_4_79211472_A, W_154_Tiensevest_4_79211472_A,
            W_153_Tiensevest_3_79175435_A, "Tiensevest",
            null,
            "2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658");

        final int W_152_Tiensevest_2_80458208_A = 80458208;

        RouteSegmentToExtract segment6 = segment5.addPTWayMember(152);
        extractAndAssertValues(152, segment5, segment6, cloneOfBus601RouteRelation,
            W_153_Tiensevest_3_79175435_A, W_153_Tiensevest_3_79175435_A,
            W_152_Tiensevest_2_80458208_A, "Tiensevest",
            null,
            "284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658");

        final int W_33_151_Tiensevest_1_19793164_A = 19793164;

        RouteSegmentToExtract segment7 = segment6.addPTWayMember(151);
        extractAndAssertValues(151, segment6, segment7, cloneOfBus601RouteRelation,
            W_152_Tiensevest_2_80458208_A, W_152_Tiensevest_2_80458208_A,
            W_33_151_Tiensevest_1_19793164_A, "Tiensevest",
            null,
            "284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658");

        final int W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D = 6184898;
        final int W_29_147_Diestsevest_1_8133608_A = 8133608;

        RouteSegmentToExtract segment8 = segment7.addPTWayMember(150);
        Relation rel8 = extractAndAssertValues(150, segment7, segment8, cloneOfBus601RouteRelation,
            W_33_151_Tiensevest_1_19793164_A, W_33_151_Tiensevest_1_19793164_A,
            W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D, "Diestsevest",
            null,
            "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658");

        final int W_28_146_Artoisplein_2_254800931_A = 254800931;

        final int W_145_Artoisplein_1_23691158_E = 23691158;
        final int W_141_Lüdenscheidsingel_2_109267417_A = 109267417;

        for (int n = 149; n >= 147; n--) {returnValueNull =segment8.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment9 = segment8.addPTWayMember(146);
        Relation rel9 = extractAndAssertValues(146, segment8, segment9, cloneOfBus601RouteRelation,
            W_29_147_Diestsevest_1_8133608_A, W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D,
            W_28_146_Artoisplein_2_254800931_A, "Joanna-Maria Artoisplein",
            null,
            "305;318;334;335;358;410;513;601;630;651;652;658");

        RouteSegmentToExtract segment10 = segment9.addPTWayMember(145);
        Relation rel10 = extractAndAssertValues(145, segment9, segment10, cloneOfBus601RouteRelation,
            W_28_146_Artoisplein_2_254800931_A, W_28_146_Artoisplein_2_254800931_A,
            W_145_Artoisplein_1_23691158_E, "Joanna-Maria Artoisplein",
            null,
            "178;305;318;334;335;358;410;513;601;630;651;652;658");

        final int W_140_Lüdenscheidsingel_1_3993387_E = 3993387;
        final int W_136_Den_Boschsingel_2_23837544_A = 23837544;

        for (int n = 144; n >= 141; n--) {returnValueNull =segment10.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment11 = segment10.addPTWayMember(140);
        extractAndAssertValues(140, segment10, segment11, cloneOfBus601RouteRelation,
            W_141_Lüdenscheidsingel_2_109267417_A, W_145_Artoisplein_1_23691158_E,
            W_140_Lüdenscheidsingel_1_3993387_E, "Lüdenscheidsingel",
            null,
            "178;305;318;358;410;601;651;652;658");

        final int W_135_Den_Boschsingel_3_225605630_E = 225605630;
        final int W_131_Rennes_Singel_1_249333185_A = 249333185;

        for (int n = 139; n >= 136; n--) {returnValueNull =segment11.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment12 = segment11.addPTWayMember(135);
        extractAndAssertValues(135, segment11, segment12, cloneOfBus601RouteRelation,
            W_136_Den_Boschsingel_2_23837544_A, W_140_Lüdenscheidsingel_1_3993387_E,
            W_135_Den_Boschsingel_3_225605630_E, "Den Boschsingel",
            null,
            "318;358;410;601;651;658");
        final int W_99_130_Herestraat_1_249333184_AC = 249333184;
        // W_100_129_Herestraat_2_813970231_BB = 813970231;
        final int W_128_Herestraat_11_681081951_A = 681081951;

        for (int n = 134; n >= 131; n--) {returnValueNull =segment12.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment13 = segment12.addPTWayMember(130);
        extractAndAssertValues(130, segment12, segment13, cloneOfBus601RouteRelation,
            W_131_Rennes_Singel_1_249333185_A, W_135_Den_Boschsingel_3_225605630_E,
            W_99_130_Herestraat_1_249333184_AC, "Herestraat",
            null,
            "318;410;601");

        final int W_127_Herestraat_10_813970227_C = 813970227;
        final int W_125_Herestraat_8_8079995_A = 8079995;

        for (int n = 129; n >= 128; n--) {returnValueNull =segment13.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment14 = segment13.addPTWayMember(127);
        extractAndAssertValues(127, segment13, segment14, cloneOfBus601RouteRelation,
            W_128_Herestraat_11_681081951_A, W_99_130_Herestraat_1_249333184_AC,
            W_127_Herestraat_10_813970227_C, "Herestraat",
            null,
            "410;601");

        final int W_124_Rotonde_Het_Teken_41403538_B = 41403538;
        final int W_123_Ring_Zuid_79340950_A = 79340950;

        for (int n = 126; n >= 125; n--) {returnValueNull =segment14.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment15 = segment14.addPTWayMember(124);
        extractAndAssertValues(124, segment14, segment15, cloneOfBus601RouteRelation,
            W_125_Herestraat_8_8079995_A, W_127_Herestraat_10_813970227_C,
            W_124_Rotonde_Het_Teken_41403538_B, "Rotonde Het Teken",
            null,
            "410;600;601");

        final int W_122_Ring_Zuid_11369123_A = 11369123;

        returnValueNull =segment15.addPTWayMember(123); assertNull(returnValueNull);
        RouteSegmentToExtract segment16 = segment15.addPTWayMember(122);
        extractAndAssertValues(122, segment15, segment16, cloneOfBus601RouteRelation,
            W_123_Ring_Zuid_79340950_A, W_124_Rotonde_Het_Teken_41403538_B,
            W_122_Ring_Zuid_11369123_A, "Ring Zuid",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_121__159949154_A = 159949154;
        final int W_120__332258104_B = 332258104;

        final int W_119_GHB_ingang_78852604_A = 78852604;

        RouteSegmentToExtract segment17 = segment16.addPTWayMember(121);
        extractAndAssertValues(121, segment16, segment17, cloneOfBus601RouteRelation,
            W_122_Ring_Zuid_11369123_A, W_122_Ring_Zuid_11369123_A,
            W_121__159949154_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_118_GHB_p5_377918641_B = 377918641;
        final int W_117_GHB_p5_14508736_A = 14508736;

        RouteSegmentToExtract segment18 = segment17.addPTWayMember(120);
        extractAndAssertValues(120, segment17, segment18, cloneOfBus601RouteRelation,
            W_121__159949154_A, W_121__159949154_A,
            W_120__332258104_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_116_Ring_Zuid_p45_109267436_A = 109267436;

        returnValueNull =segment18.addPTWayMember(119); assertNull(returnValueNull);
        RouteSegmentToExtract segment19 = segment18.addPTWayMember(118);
        extractAndAssertValues(118, segment18, segment19, cloneOfBus601RouteRelation,
            W_119_GHB_ingang_78852604_A, W_120__332258104_B,
            W_118_GHB_p5_377918641_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_115_Ring_Zuid_p3_14508739_A = 14508739;

        returnValueNull =segment19.addPTWayMember(117); assertNull(returnValueNull);
        RouteSegmentToExtract segment20 = segment19.addPTWayMember(116);
        extractAndAssertValues(116, segment19, segment20, cloneOfBus601RouteRelation,
            W_117_GHB_p5_14508736_A, W_118_GHB_p5_377918641_B,
            W_116_Ring_Zuid_p45_109267436_A, "Ring Zuid",
            null,
            "3;317;395;410;601");

        final int W_114_Ring_Zuid_p2_14508740_A = 14508740;

        RouteSegmentToExtract segment21 = segment20.addPTWayMember(115);
        extractAndAssertValues(115, segment20, segment21, cloneOfBus601RouteRelation,
            W_116_Ring_Zuid_p45_109267436_A, W_116_Ring_Zuid_p45_109267436_A,
            W_115_Ring_Zuid_p3_14508739_A, "Ring Zuid",
            null,
            "3;317;395;410;600;601");

        final int W_113_Ring_Zuid_p1_502328838_D = 502328838;
        final int W_110_Rotonde_Het_Teken_78568660_A = 78568660;

        RouteSegmentToExtract segment22 = segment21.addPTWayMember(114);
        extractAndAssertValues(114, segment21, segment22, cloneOfBus601RouteRelation,
            W_115_Ring_Zuid_p3_14508739_A, W_115_Ring_Zuid_p3_14508739_A,
            W_114_Ring_Zuid_p2_14508740_A, "Ring Zuid",
            null,
            "3;317;334;335;395;410;600;601");

        RouteSegmentToExtract segment23 = segment22.addPTWayMember(113);
        extractAndAssertValues(113, segment22, segment23, cloneOfBus601RouteRelation,
            W_114_Ring_Zuid_p2_14508740_A, W_114_Ring_Zuid_p2_14508740_A,
            W_113_Ring_Zuid_p1_502328838_D, "Ring Zuid",
            null,
            "3;317;334;335;380;395;410;600;601");

        final int W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A = 78873921;

        for (int n = 112; n >= 110; n--) {returnValueNull =segment23.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment24 = segment23.addPTWayMember(109);
        extractAndAssertValues(109, segment23, segment24, cloneOfBus601RouteRelation,
            W_110_Rotonde_Het_Teken_78568660_A, W_113_Ring_Zuid_p1_502328838_D,
            W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_108_Rotonde_Het_Teken_3752557_A = 3752557;

        RouteSegmentToExtract segment25 = segment24.addPTWayMember(108);
        extractAndAssertValues(108, segment24, segment25, cloneOfBus601RouteRelation,
            W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A, W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A,
            W_108_Rotonde_Het_Teken_3752557_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_107_Rotonde_Het_Teken_VWBln_249333188_A = 249333188;

        RouteSegmentToExtract segment26 = segment25.addPTWayMember(107);
        extractAndAssertValues(107, segment25, segment26, cloneOfBus601RouteRelation,
            W_108_Rotonde_Het_Teken_3752557_A, W_108_Rotonde_Het_Teken_3752557_A,
            W_107_Rotonde_Het_Teken_VWBln_249333188_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_106_Rotonde_Het_Teken_249333187_A = 249333187;

        RouteSegmentToExtract segment27 = segment26.addPTWayMember(106);
        extractAndAssertValues(106, segment26, segment27, cloneOfBus601RouteRelation,
            W_107_Rotonde_Het_Teken_VWBln_249333188_A, W_107_Rotonde_Het_Teken_VWBln_249333188_A,
            W_106_Rotonde_Het_Teken_249333187_A, "Rotonde Het Teken",
            null,
            "3;410;600;601");

        final int W_105_Herestraat_7_13067134_D = 13067134;
        final int W_102_Herestraat_4_813970228_A = 813970228;

        RouteSegmentToExtract segment28 = segment27.addPTWayMember(105);
        extractAndAssertValues(105, segment27, segment28, cloneOfBus601RouteRelation,
            W_106_Rotonde_Het_Teken_249333187_A, W_106_Rotonde_Het_Teken_249333187_A,
            W_105_Herestraat_7_13067134_D, "Herestraat",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_101_Herestraat_3_813970229_C = 813970229;
        // 99 and 100 are defined as 129 and 130

        for (int n = 104; n >= 102; n--) {returnValueNull =segment28.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment29 = segment28.addPTWayMember(101);
        extractAndAssertValues(101, segment28, segment29, cloneOfBus601RouteRelation,
            W_102_Herestraat_4_813970228_A, W_105_Herestraat_7_13067134_D,
            W_101_Herestraat_3_813970229_C, "Herestraat",
            null,
            "410;600;601");

        final int W_98_Rennes_Singel_249333186_H = 249333186;
        final int W_91_Rennes_Singel_429706864_A = 429706864;

        for (int n = 100; n >= 99; n--) {returnValueNull =segment29.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment30 = segment29.addPTWayMember(98);
        extractAndAssertValues(98, segment29, segment30, cloneOfBus601RouteRelation,
            W_99_130_Herestraat_1_249333184_AC, W_101_Herestraat_3_813970229_C,
            W_98_Rennes_Singel_249333186_H, "Rennes-Singel",
            null,
            "601");

        final int W_90_Tervuursevest_99583853_K = 99583853;
        final int W_80_Tervuursevest_88361317_A = 88361317;

        for (int n = 97; n >= 91; n--) {returnValueNull =segment30.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment31 = segment30.addPTWayMember(90);
        extractAndAssertValues(90, segment30, segment31, cloneOfBus601RouteRelation,
            W_91_Rennes_Singel_429706864_A, W_98_Rennes_Singel_249333186_H,
            W_90_Tervuursevest_99583853_K, "Tervuursevest",
            null,
            "318;601");

        final int W_79_Tervuursevest_Kapucijnenvoer_461159345_A = 461159345;

        for (int n = 89; n >= 80; n--) {returnValueNull =segment31.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment32 = segment31.addPTWayMember(79);
        extractAndAssertValues(79, segment31, segment32, cloneOfBus601RouteRelation,
            W_80_Tervuursevest_88361317_A, W_90_Tervuursevest_99583853_K,
            W_79_Tervuursevest_Kapucijnenvoer_461159345_A, "Tervuursevest",
            null,
            "601");

        final int W_78_Tervuursevest_461159362_K = 461159362;
        final int W_68_Tervuursevest_3677330_A = 3677330;

        RouteSegmentToExtract segment33 = segment32.addPTWayMember(78);
        extractAndAssertValues(78, segment32, segment33, cloneOfBus601RouteRelation,
            W_79_Tervuursevest_Kapucijnenvoer_461159345_A, W_79_Tervuursevest_Kapucijnenvoer_461159345_A,
            W_78_Tervuursevest_461159362_K, "Tervuursevest",
            null,
            "178;179;306 (student);601");

        final int W_67_Naamsevest_86164005_G = 86164005;
        final int W_61_Geldenaaksevest_24905257_A = 24905257;

        final int W_60_Geldenaaksevest_608715605_H = 608715605;
        final int W_53_Geldenaaksevest_8130906_A = 8130906;

        for (int n = 77; n >= 68; n--) {returnValueNull =segment33.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment34 = segment33.addPTWayMember(67);
        extractAndAssertValues(67, segment33, segment34, cloneOfBus601RouteRelation,
            W_68_Tervuursevest_3677330_A, W_78_Tervuursevest_461159362_K,
            W_67_Naamsevest_86164005_G, "Naamsevest",
            null,
            "178;179;306 (student);520;524;525;537;601");

        final int W_52_Tiensepoort_16775171_D = 16775171;
        final int W_49_Tiensevest_Oostertunnel_8590231_A = 8590231;

        for (int n = 66; n >= 61; n--) {returnValueNull =segment34.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment35 = segment34.addPTWayMember(60);
        extractAndAssertValues(60, segment34, segment35, cloneOfBus601RouteRelation,
            W_61_Geldenaaksevest_24905257_A, W_67_Naamsevest_86164005_G,
            W_60_Geldenaaksevest_608715605_H, "Geldenaaksevest",
            null,
            "18;178;179;306 (student);337;601;616");

        for (int n = 60; n >= 53; n--) {returnValueNull =segment35.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment36 = segment35.addPTWayMember(52);
        extractAndAssertValues(52, segment35, segment36, cloneOfBus601RouteRelation,
            W_53_Geldenaaksevest_8130906_A, W_60_Geldenaaksevest_608715605_H,
            W_52_Tiensepoort_16775171_D, "Tiensepoort",
            null,
            "18;178;179;306 (student);337;601;616;630");

        final int W_48_Tiensevest_185988814_A = 185988814;

        for (int n = 51; n >= 49; n--) {returnValueNull =segment36.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment37 = segment36.addPTWayMember(48);
        extractAndAssertValues(48, segment36, segment37, cloneOfBus601RouteRelation,
            W_49_Tiensevest_Oostertunnel_8590231_A, W_52_Tiensepoort_16775171_D,
            W_48_Tiensevest_185988814_A, "Tiensevest",
            null,
            "7;8;9;18;178;179;306 (student);337;380;527;601;616;630");

        final int W_47_Martelarenplein_76856823_D = 76856823;
        final int W_44__78815533_A = 78815533;

        RouteSegmentToExtract segment38 = segment37.addPTWayMember(47);
        extractAndAssertValues(47, segment37, segment38, cloneOfBus601RouteRelation,
            W_48_Tiensevest_185988814_A, W_48_Tiensevest_185988814_A,
            W_47_Martelarenplein_76856823_D, "Martelarenplein",
            null,
            "1;4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;616;630");

        final int W_43__79264899_A = 79264899;

        for (int n = 46; n >= 44; n--) {returnValueNull =segment38.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment39 = segment38.addPTWayMember(43);
        extractAndAssertValues(43, segment38, segment39, cloneOfBus601RouteRelation,
            W_44__78815533_A, W_47_Martelarenplein_76856823_D,
            W_43__79264899_A, null,
            null,
            "4;5;6;7;8;9;18;178;179;306 (student);337;380;527;601;630");

        final int W_42__377918635_A = 377918635;

        RouteSegmentToExtract segment40 = segment39.addPTWayMember(42);
        extractAndAssertValues(42, segment39, segment40, cloneOfBus601RouteRelation,
            W_43__79264899_A, W_43__79264899_A,
            W_42__377918635_A, null,
            null,
            "4;5;6;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601;630");
        final int W_41__79264888_A = 79264888;

        RouteSegmentToExtract segment41 = segment40.addPTWayMember(41);
        extractAndAssertValues(41, segment40, segment41, cloneOfBus601RouteRelation,
            W_42__377918635_A, W_42__377918635_A,
            W_41__79264888_A, null,
            null,
            "3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601");

        final int W_40__79264897_B = 79264897;
        final int W_39__71754927_A = 71754927;

        RouteSegmentToExtract segment42 = segment41.addPTWayMember(40);
        extractAndAssertValues(40, segment41, segment42, cloneOfBus601RouteRelation,
            W_41__79264888_A, W_41__79264888_A,
            W_40__79264897_B, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601");

        final int W_38__377918638_A_TEC18 = 377918638;

        returnValueNull =segment42.addPTWayMember(39); assertNull(returnValueNull);
        RouteSegmentToExtract segment44 = segment42.addPTWayMember(38);
        extractAndAssertValues(38, segment42, segment44, cloneOfBus601RouteRelation,
            W_39__71754927_A, W_40__79264897_B,
            W_38__377918638_A_TEC18, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601");

        final int W_37__79264891_A = 79264891;

        RouteSegmentToExtract segment45 = segment44.addPTWayMember(37);
        extractAndAssertValues(37, segment44, segment45, cloneOfBus601RouteRelation,
            W_38__377918638_A_TEC18, W_38__377918638_A_TEC18,
            W_37__79264891_A, null,
            null,
            "18;601");
        final int W_36_Tiensevest_78568409_A = 78568409;

        RouteSegmentToExtract segment46 = segment45.addPTWayMember(36);
        extractAndAssertValues(36, segment45, segment46, cloneOfBus601RouteRelation,
            W_37__79264891_A, W_37__79264891_A,
            W_36_Tiensevest_78568409_A, "Tiensevest",
            null,
            "18;601");
        final int W_35_Tiensevest_79193579_A = 79193579;

        RouteSegmentToExtract segment47 = segment46.addPTWayMember(35);
        extractAndAssertValues(35, segment46, segment47, cloneOfBus601RouteRelation,
            W_36_Tiensevest_78568409_A, W_36_Tiensevest_78568409_A,
            W_35_Tiensevest_79193579_A, "Tiensevest",
            null,
            "4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;334;335;337;380;601;616;658");
        final int W_34_Bend_19793394_A = 19793394;
        // ways 28 to 33 are the same as 146 - 151
        final int W_27_Zoutstraat_3992548_A = 3992548;

        RouteSegmentToExtract segment48 = segment47.addPTWayMember(34);
        extractAndAssertValues(34, segment47, segment48, cloneOfBus601RouteRelation,
            W_35_Tiensevest_79193579_A, W_35_Tiensevest_79193579_A,
            W_34_Bend_19793394_A, null,
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658");

        final int W_26_Havenkant_510790349_M = 510790349;
        final int W_14_Engels_Plein_338057820_A = 338057820;

        RouteSegmentToExtract segment49 = segment48.addPTWayMember(33);
        extractAndAssertValues(33, segment48, segment49, cloneOfBus601RouteRelation,
            W_34_Bend_19793394_A, W_34_Bend_19793394_A,
            W_33_151_Tiensevest_1_19793164_A, "Tiensevest",
            null,
            "334;335;513;601");

        RouteSegmentToExtract segment50 = segment49.addPTWayMember(32);
        Relation rel50 = extractAndAssertValues(32, segment49, segment50, cloneOfBus601RouteRelation,
            W_33_151_Tiensevest_1_19793164_A, W_33_151_Tiensevest_1_19793164_A,
            W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D, "Diestsevest",
            null,
            "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658");
        assertEquals(rel8.getId(), rel50.getId());

        for (int n = 31; n >= 29; n--) {returnValueNull =segment50.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment51 = segment50.addPTWayMember(28);
        Relation rel51 = extractAndAssertValues(28, segment50, segment51, cloneOfBus601RouteRelation,
            W_29_147_Diestsevest_1_8133608_A, W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D,
            W_28_146_Artoisplein_2_254800931_A, "Joanna-Maria Artoisplein",
            null,
            "305;318;334;335;358;410;513;601;630;651;652;658");
        assertEquals(rel9.getId(), rel51.getId());

        RouteSegmentToExtract segment52 = segment51.addPTWayMember(27);
        Relation rel52 = extractAndAssertValues(27, segment51, segment52, cloneOfBus601RouteRelation,
            W_28_146_Artoisplein_2_254800931_A, W_28_146_Artoisplein_2_254800931_A,
            W_27_Zoutstraat_3992548_A, "Zoutstraat",
            null,
            "178;305;318;334;335;358;410;513;601;630;651;652;658");
        assertEquals(rel10.getId(), rel52.getId());

        RouteSegmentToExtract segment53 = segment52.addPTWayMember(26);
        extractAndAssertValues(26, segment52, segment53, cloneOfBus601RouteRelation,
            W_27_Zoutstraat_3992548_A, W_27_Zoutstraat_3992548_A,
            W_26_Havenkant_510790349_M, "Havenkant",
            null,
            "334;335;513;601;630");

        for (int n = 25; n >= 14; n--) {returnValueNull =segment53.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus601RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment54 = segment53.addPTWayMember(13);
        extractAndAssertValues(13, segment53, segment54, cloneOfBus601RouteRelation,
            W_14_Engels_Plein_338057820_A, W_26_Havenkant_510790349_M,
            0, null,
            null,
            "601");


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

        final int W_169_Engels_Plein_608715622_O = 608715622;
        final int W_155_Havenkant_29283599_A = 29283599;

        final int W_154_Havenkant_304241968_B = 304241968;
        final int W_153_Aarschotsesteenweg_304241967_A = 304241967;

        for (int n = 169; n >= 155; n--) {returnValueNull =segment101.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment102 = segment101.addPTWayMember(154);
        extractAndAssertValues(154, segment101, segment102, cloneOfBus600RouteRelation,
            W_155_Havenkant_29283599_A, W_169_Engels_Plein_608715622_O,
            W_154_Havenkant_304241968_B, "Havenkant",
            null,
            "600");

        final int W_152_Redersstraat_340265961_B = 340265961;
        final int W_151_Redersstraat_318825613_A = 318825613;

        returnValueNull =segment102.addPTWayMember(153); assertNull(returnValueNull);
        RouteSegmentToExtract segment103 = segment102.addPTWayMember(152);
        extractAndAssertValues(152, segment102, segment103, cloneOfBus600RouteRelation,
            W_153_Aarschotsesteenweg_304241967_A, W_154_Havenkant_304241968_B,
            W_152_Redersstraat_340265961_B, "Redersstraat",
            null,
            "334;335;513;600;630");

        final int W_150_Redersstraat_340265962_A = 340265962;

        returnValueNull =segment103.addPTWayMember(151); assertNull(returnValueNull);
        RouteSegmentToExtract segment104 = segment103.addPTWayMember(150);
        extractAndAssertValues(150, segment103, segment104, cloneOfBus600RouteRelation,
            W_151_Redersstraat_318825613_A, W_152_Redersstraat_340265961_B,
            W_150_Redersstraat_340265962_A, "Redersstraat",
            null,
            "600");

        final int W_149_Joanna_Maria_Artoisplein_254801390_B = 254801390;
        final int W_148_Joanna_Maria_Artoisplein_61540068_A = 61540068;

        RouteSegmentToExtract segment105 = segment104.addPTWayMember(149);
        extractAndAssertValues(149, segment104, segment105, cloneOfBus600RouteRelation,
            W_150_Redersstraat_340265962_A, W_150_Redersstraat_340265962_A,
            W_149_Joanna_Maria_Artoisplein_254801390_B, "Joanna-Maria Artoisplein",
            null,
            "333;334;335;513;600;630");

        final int W_147_Vuurkruisenlaan_23691160_C = 23691160;
        final int W_145_Diestsepoort_8109264_A = 8109264;

        returnValueNull =segment105.addPTWayMember(148); assertNull(returnValueNull);
        RouteSegmentToExtract segment106 = segment105.addPTWayMember(147);
        extractAndAssertValues(147, segment105, segment106, cloneOfBus600RouteRelation,
            W_148_Joanna_Maria_Artoisplein_61540068_A, W_149_Joanna_Maria_Artoisplein_254801390_B,
            W_147_Vuurkruisenlaan_23691160_C, "Vuurkruisenlaan",
            null,
            "178;305;318;333;334;335;410;513;600;630;651;652;658");

        final int W_144_Diestsepoort_61556877_D = 61556877;
        final int W_141_Diestsepoort_584356745_A = 584356745;

        for (int n = 146; n >= 145; n--) {returnValueNull =segment106.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment107 = segment106.addPTWayMember(144);
        extractAndAssertValues(144, segment106, segment107, cloneOfBus600RouteRelation,
            W_145_Diestsepoort_8109264_A, W_147_Vuurkruisenlaan_23691160_C,
            W_144_Diestsepoort_61556877_D, "Diestsepoort",
            null,
            "305;333;334;335;513;600;630;651;652;658");

        final int W_140_Diestsepoort_198559166_E = 198559166;
        final int W_136_Diestsepoort_451873774_A = 451873774;

        for (int n = 143; n >= 141; n--) {returnValueNull =segment107.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment108 = segment107.addPTWayMember(140);
        extractAndAssertValues(140, segment107, segment108, cloneOfBus600RouteRelation,
            W_141_Diestsepoort_584356745_A, W_144_Diestsepoort_61556877_D,
            W_140_Diestsepoort_198559166_E, "Diestsepoort",
            null,
            "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658");

        final int W_135__76867049_C = 76867049;
        // W_134__79264890_B = 79264890;
        final int W_133__79596965_A = 79596965;

        for (int n = 139; n >= 136; n--) {returnValueNull =segment108.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment109 = segment108.addPTWayMember(135);
        extractAndAssertValues(135, segment108, segment109, cloneOfBus600RouteRelation,
            W_136_Diestsepoort_451873774_A, W_140_Diestsepoort_198559166_E,
            W_135__76867049_C, null,
            null,
            "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658");

        final int W_132__79596974_B = 79596974;
        final int W_131__79596982_A = 79596982;

        for (int n = 134; n >= 133; n--) {returnValueNull =segment109.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment110 = segment109.addPTWayMember(132);
        extractAndAssertValues(132, segment109, segment110, cloneOfBus600RouteRelation,
            W_133__79596965_A, W_135__76867049_C,
            W_132__79596974_B, null,
            null,
            "2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630");

        final int W_130__79596987_C = 79596987;
        final int W_128__79596980_A = 79596980;

        returnValueNull =segment110.addPTWayMember(131); assertNull(returnValueNull);
        RouteSegmentToExtract segment111 = segment110.addPTWayMember(130);
        extractAndAssertValues(130, segment110, segment111, cloneOfBus600RouteRelation,
            W_131__79596982_A, W_132__79596974_B,
            W_130__79596987_C, null,
            null,
            "3;333;334;335;433;600;630");
        final int W_127_Tiensevest_79193579_A = 79193579;

        for (int n = 129; n >= 128; n--) {returnValueNull =segment111.addPTWayMember(n); assertNull(returnValueNull);}
        RouteSegmentToExtract segment112 = segment111.addPTWayMember(127);
        extractAndAssertValues(127, segment111, segment112, cloneOfBus600RouteRelation,
            W_128__79596980_A, W_130__79596987_C,
            W_127_Tiensevest_79193579_A, "Tiensevest",
            null,
            "3;333;334;335;513;600;630");

        final int W_126_Tiensevest_258936980_A = 258936980;

        RouteSegmentToExtract segment113 = segment112.addPTWayMember(126);
        extractAndAssertValues(126, segment112, segment113, cloneOfBus600RouteRelation,
            W_127_Tiensevest_79193579_A, W_127_Tiensevest_79193579_A,
            W_126_Tiensevest_258936980_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658");

        final int W_125_Tiensevest_79193580_A = 79193580;

        RouteSegmentToExtract segment114 = segment113.addPTWayMember(125);
        extractAndAssertValues(125, segment113, segment114, cloneOfBus600RouteRelation,
            W_126_Tiensevest_258936980_A, W_126_Tiensevest_258936980_A,
            W_125_Tiensevest_79193580_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658");

        final int W_124_Tiensevest_79193581_A = 79193581;

        RouteSegmentToExtract segment115 = segment114.addPTWayMember(124);
        extractAndAssertValues(124, segment114, segment115, cloneOfBus600RouteRelation,
            W_125_Tiensevest_79193580_A, W_125_Tiensevest_79193580_A,
            W_124_Tiensevest_79193581_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        final int W_123_Tiensevest_78815505_A = 78815505;

        RouteSegmentToExtract segment116 = segment115.addPTWayMember(123);
        extractAndAssertValues(123, segment115, segment116, cloneOfBus600RouteRelation,
            W_124_Tiensevest_79193581_A, W_124_Tiensevest_79193581_A,
            W_123_Tiensevest_78815505_A, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        final int W_122_Tiensevest_84696751_B = 84696751;
        final int W_121_Tiensevest_79265237_A = 79265237;

        RouteSegmentToExtract segment117 = segment116.addPTWayMember(122);
        extractAndAssertValues(122, segment116, segment117, cloneOfBus600RouteRelation,
            W_123_Tiensevest_78815505_A, W_123_Tiensevest_78815505_A,
            W_122_Tiensevest_84696751_B, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        final int W_120_Tiensevest_89574079_B = 89574079;
        final int W_119_Tiensevest_81522744_A = 81522744;

        final int W_118_Tiensevest_19793223_B = 19793223;
        final int W_117_Tiensevest_185988814_A = 185988814;

        for (int n = 121; n >= 121; n--) {returnValueNull =segment117.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment118 = segment117.addPTWayMember(120);
        extractAndAssertValues(120, segment117, segment118, cloneOfBus600RouteRelation,
            W_121_Tiensevest_79265237_A, W_122_Tiensevest_84696751_B,
            W_120_Tiensevest_89574079_B, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630");

        final int W_116_Tiensevest_185988816_B = 185988816;
        final int W_115_Tiensevest_15083398_A = 15083398;

        for (int n = 119; n >= 119; n--) {returnValueNull =segment118.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment119a = segment118.addPTWayMember(118);
        extractAndAssertValues(118, segment118, segment119a, cloneOfBus600RouteRelation,
            W_119_Tiensevest_81522744_A, W_120_Tiensevest_89574079_B,
            W_118_Tiensevest_19793223_B, "Tiensevest",
            null,
            "1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630");

        for (int n = 117; n >= 117; n--) {returnValueNull =segment119a.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment119b = segment119a.addPTWayMember(116);
        extractAndAssertValues(116, segment119a, segment119b, cloneOfBus600RouteRelation,
            W_117_Tiensevest_185988814_A, W_118_Tiensevest_19793223_B,
            W_116_Tiensevest_185988816_B, "Tiensevest",
            null,
            "1;4;5;6;7;8;9;18;179;306 (student);337;380;600;616;630");

        final int W_114_Tiensevest_8154434_E = 8154434;
        final int W_110_Tiensevest_521193611_A = 521193611;

        returnValueNull =segment119b.addPTWayMember(115); assertNull(returnValueNull);
        RouteSegmentToExtract segment120 = segment119b.addPTWayMember(114);
        extractAndAssertValues(114, segment119b, segment120, cloneOfBus600RouteRelation,
            W_115_Tiensevest_15083398_A, W_116_Tiensevest_185988816_B,
            W_114_Tiensevest_8154434_E, "Tiensevest",
            null,
            "7;8;9;18;179;306 (student);337;380;600;616;630");

        final int W_109_Tiensepoort_4003928_G = 4003928;
        final int W_103_Geldenaaksevest_199381120_A = 199381120;

        for (int n = 113; n >= 110; n--) {returnValueNull =segment120.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment121 = segment120.addPTWayMember(109);
        extractAndAssertValues(109, segment120, segment121, cloneOfBus600RouteRelation,
            W_110_Tiensevest_521193611_A, W_114_Tiensevest_8154434_E,
            W_109_Tiensepoort_4003928_G, "Tiensepoort",
            null,
            "7;8;9;18;178;179;306 (student);337;380;600;616;630");

        final int W_102_Geldenaaksevest_3991775_E = 3991775;
        final int W_99_Erasme_Ruelensvest_608715579_A = 608715579;

        for (int n = 108; n >= 103; n--) {returnValueNull =segment121.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment122 = segment121.addPTWayMember(102);
        extractAndAssertValues(102, segment121, segment122, cloneOfBus600RouteRelation,
            W_103_Geldenaaksevest_199381120_A, W_109_Tiensepoort_4003928_G,
            W_102_Geldenaaksevest_3991775_E, "Geldenaaksevest",
            null,
            "18;178;179;306 (student);337;600;616;630");

        final int W_98_Erasme_Ruelensvest_3677822_B = 3677822;
        final int W_97_Tervuursevest_120086003_A = 120086003;

        for (int n = 100; n >= 99; n--) {returnValueNull =segment122.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment123 = segment122.addPTWayMember(98);
        extractAndAssertValues(98, segment122, segment123, cloneOfBus600RouteRelation,
            W_99_Erasme_Ruelensvest_608715579_A, W_102_Geldenaaksevest_3991775_E,
            W_98_Erasme_Ruelensvest_3677822_B, "Erasme Ruelensvest",
            null,
            "18;178;179;306 (student);337;600;616");

        final int W_96_Tervuursevest_90168774_F = 90168774;
        final int W_92_Tervuursevest_85044922_A = 85044922;

        final int W_91_Tervuursevest_16771611_A = 16771611;

        returnValueNull =segment123.addPTWayMember(97); assertNull(String.format("%d %s%s\n", 97, rc, cloneOfBus600RouteRelation.getMember(97).getMember().getId()), returnValueNull);
        RouteSegmentToExtract segment125 = segment123.addPTWayMember(96);
        extractAndAssertValues(96, segment123, segment125, cloneOfBus600RouteRelation,
            W_97_Tervuursevest_120086003_A, W_98_Erasme_Ruelensvest_3677822_B,
            W_96_Tervuursevest_90168774_F, "Tervuursevest",
            null,
            "178;179;306 (student);600");

        for (int n = 95; n >= 92; n--) {returnValueNull =segment125.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment127 = segment125.addPTWayMember(91);
        extractAndAssertValues(91, segment125, segment127, cloneOfBus600RouteRelation,
            W_92_Tervuursevest_85044922_A, W_96_Tervuursevest_90168774_F,
            W_91_Tervuursevest_16771611_A, "Tervuursevest",
            null,
            "178;179;306 (student);520;524;525;537;586;600");

        final int W_90_Tervuursevest_13246158_A = 13246158;

        final int W_89_Tervuursevest_260405216_B = 260405216;
        final int W_88_Tervuursevest_16771609_A = 16771609;

        RouteSegmentToExtract segment128 = segment127.addPTWayMember(90);
        extractAndAssertValues(90, segment127, segment128, cloneOfBus600RouteRelation,
            W_91_Tervuursevest_16771611_A, W_91_Tervuursevest_16771611_A,
            W_90_Tervuursevest_13246158_A, "Tervuursevest",
            null,
            "178;520;524;525;537;586;600");

        final int W_87_Tervuursevest_608715518_J = 608715518;
        final int W_78_Tervuursevest_3677945_A = 3677945;

        RouteSegmentToExtract segment129 = segment128.addPTWayMember(89);
        extractAndAssertValues(89, segment128, segment129, cloneOfBus600RouteRelation,
            W_90_Tervuursevest_13246158_A, W_90_Tervuursevest_13246158_A,
            W_89_Tervuursevest_260405216_B, "Tervuursevest",
            null,
            "7;8;9;178;520;524;525;527;537;586;600");

        for (int n = 88; n >= 88; n--) {returnValueNull =segment129.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment130 = segment129.addPTWayMember(87);
        extractAndAssertValues(87, segment129, segment130, cloneOfBus600RouteRelation,
            W_88_Tervuursevest_16771609_A, W_89_Tervuursevest_260405216_B,
            W_87_Tervuursevest_608715518_J, "Tervuursevest",
            null,
            "7;8;9;178;527;600");

        final int W_77_Rennes_Singel_192559627_E = 192559627;
        final int W_73_Rennes_Singel_28982660_A = 28982660;

        final int W_72_Herestraat_78568455_A = 78568455;

        for (int n = 86; n >= 78; n--) {returnValueNull =segment130.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment131 = segment130.addPTWayMember(77);
        extractAndAssertValues(77, segment130, segment131, cloneOfBus600RouteRelation,
            W_78_Tervuursevest_3677945_A, W_87_Tervuursevest_608715518_J,
            W_77_Rennes_Singel_192559627_E, "Rennes-Singel",
            null,
            "178;600");

        final int W_71_Herestraat_813970227_C = 813970227;
        final int W_69_Herestraat_8079995_A = 8079995;

        for (int n = 76; n >= 73; n--) {returnValueNull =segment131.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment132 = segment131.addPTWayMember(72);
        extractAndAssertValues(72, segment131, segment132, cloneOfBus600RouteRelation,
            W_73_Rennes_Singel_28982660_A, W_77_Rennes_Singel_192559627_E,
            W_72_Herestraat_78568455_A, "Herestraat",
            null,
            "178;318;600");

        final int W_68_Rotonde_Het_Teken_41403538_B = 41403538;
        final int W_67_Ring_Zuid_79340950_A = 79340950;

        RouteSegmentToExtract segment133 = segment132.addPTWayMember(71);
        extractAndAssertValues(71, segment132, segment133, cloneOfBus600RouteRelation,
            W_72_Herestraat_78568455_A, W_72_Herestraat_78568455_A,
            W_71_Herestraat_813970227_C, "Herestraat",
            null,
            "600");

        for (int n = 70; n >= 69; n--) {returnValueNull =segment133.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, cloneOfBus600RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment134 = segment133.addPTWayMember(68);
        extractAndAssertValues(68, segment133, segment134, cloneOfBus600RouteRelation,
            W_69_Herestraat_8079995_A, W_71_Herestraat_813970227_C,
            W_68_Rotonde_Het_Teken_41403538_B, "Rotonde Het Teken",
            null,
            "410;600;601");

        final int W_66_Ring_Zuid_11369123_A = 11369123;

        for (int n = 67; n >= 67; n--) {returnValueNull =segment134.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment135 = segment134.addPTWayMember(66);
        extractAndAssertValues(66, segment134, segment135, cloneOfBus600RouteRelation,
            W_67_Ring_Zuid_79340950_A, W_68_Rotonde_Het_Teken_41403538_B,
            W_66_Ring_Zuid_11369123_A, "Ring Zuid",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_65__159949154_A = 159949154;

        final int W_64__332258104_B = 332258104;
        final int W_63__78852604_A = 78852604;

        RouteSegmentToExtract segment136 = segment135.addPTWayMember(65);
        extractAndAssertValues(65, segment135, segment136, cloneOfBus600RouteRelation,
            W_66_Ring_Zuid_11369123_A, W_66_Ring_Zuid_11369123_A,
            W_65__159949154_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment137 = segment136.addPTWayMember(64);
        extractAndAssertValues(64, segment136, segment137, cloneOfBus600RouteRelation,
            W_65__159949154_A, W_65__159949154_A,
            W_64__332258104_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_62__14508735_A = 14508735;

        for (int n = 63; n >= 63; n--) {returnValueNull =segment137.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment138 = segment137.addPTWayMember(62);
        extractAndAssertValues(62, segment137, segment138, cloneOfBus600RouteRelation,
            W_63__78852604_A, W_64__332258104_B,
            W_62__14508735_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_61__318878531_B = 318878531;
        final int W_60__14506241_A = 14506241;

        RouteSegmentToExtract segment139 = segment138.addPTWayMember(61);
        extractAndAssertValues(61, segment138, segment139, cloneOfBus600RouteRelation,
            W_62__14508735_A, W_62__14508735_A,
            W_61__318878531_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600");

        final int W_59_Ring_Zuid_109267436_A = 109267436;

        for (int n = 60; n >= 60; n--) {returnValueNull =segment139.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment142 = segment139.addPTWayMember(59);
        extractAndAssertValues(59, segment139, segment142, cloneOfBus600RouteRelation,
            W_60__14506241_A, W_61__318878531_B,
            W_59_Ring_Zuid_109267436_A, "Ring Zuid",
            null,
            "3;317;395;410;600");

        final int W_58_Ring_Zuid_14508739_A = 14508739;

        RouteSegmentToExtract segment143 = segment142.addPTWayMember(58);
        extractAndAssertValues(58, segment142, segment143, cloneOfBus600RouteRelation,
            W_59_Ring_Zuid_109267436_A, W_59_Ring_Zuid_109267436_A,
            W_58_Ring_Zuid_14508739_A, "Ring Zuid",
            null,
            "3;317;395;410;600;601");

        final int W_57_Ring_Zuid_14508740_A = 14508740;

        RouteSegmentToExtract segment144 = segment143.addPTWayMember(57);
        extractAndAssertValues(57, segment143, segment144, cloneOfBus600RouteRelation,
            W_58_Ring_Zuid_14508739_A, W_58_Ring_Zuid_14508739_A,
            W_57_Ring_Zuid_14508740_A, "Ring Zuid",
            null,
            "3;317;334;335;395;410;600;601");

        final int W_56_Ring_Zuid_502328838_D = 502328838;
        final int W_53_Rotonde_Het_Teken_78568660_A = 78568660;

        RouteSegmentToExtract segment145 = segment144.addPTWayMember(56);
        extractAndAssertValues(56, segment144, segment145, cloneOfBus600RouteRelation,
            W_57_Ring_Zuid_14508740_A, W_57_Ring_Zuid_14508740_A,
            W_56_Ring_Zuid_502328838_D, "Ring Zuid",
            null,
            "3;317;334;335;380;395;410;600;601");

        final int W_52_Rotonde_Het_Teken_78873921_A = 78873921;

        for (int n = 55; n >= 53; n--) {returnValueNull =segment145.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment146 = segment145.addPTWayMember(52);
        extractAndAssertValues(52, segment145, segment146, cloneOfBus600RouteRelation,
            W_53_Rotonde_Het_Teken_78568660_A, W_56_Ring_Zuid_502328838_D,
            W_52_Rotonde_Het_Teken_78873921_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_51_Rotonde_Het_Teken_3752557_A = 3752557;

        RouteSegmentToExtract segment148 = segment146.addPTWayMember(51);
        extractAndAssertValues(51, segment146, segment148, cloneOfBus600RouteRelation,
            W_52_Rotonde_Het_Teken_78873921_A, W_52_Rotonde_Het_Teken_78873921_A,
            W_51_Rotonde_Het_Teken_3752557_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_50_Rotonde_Het_Teken_249333188_A = 249333188;

        RouteSegmentToExtract segment149 = segment148.addPTWayMember(50);
        extractAndAssertValues(50, segment148, segment149, cloneOfBus600RouteRelation,
            W_51_Rotonde_Het_Teken_3752557_A, W_51_Rotonde_Het_Teken_3752557_A,
            W_50_Rotonde_Het_Teken_249333188_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        final int W_49_Rotonde_Het_Teken_249333187_A = 249333187;

        RouteSegmentToExtract segment150 = segment149.addPTWayMember(49);
        extractAndAssertValues(49, segment149, segment150, cloneOfBus600RouteRelation,
            W_50_Rotonde_Het_Teken_249333188_A, W_50_Rotonde_Het_Teken_249333188_A,
            W_49_Rotonde_Het_Teken_249333187_A, "Rotonde Het Teken",
            null,
            "3;410;600;601");

        final int W_48_Herestraat_13067134_D = 13067134;
        final int W_45_Herestraat_813970228_A = 813970228;

        RouteSegmentToExtract segment151 = segment150.addPTWayMember(48);
        extractAndAssertValues(48, segment150, segment151, cloneOfBus600RouteRelation,
            W_49_Rotonde_Het_Teken_249333187_A, W_49_Rotonde_Het_Teken_249333187_A,
            W_48_Herestraat_13067134_D, "Herestraat",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        final int W_44_Herestraat_270181177_A = 270181177;

        for (int n = 47; n >= 45; n--) {returnValueNull =segment151.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment152 = segment151.addPTWayMember(44);
        extractAndAssertValues(44, segment151, segment152, cloneOfBus600RouteRelation,
            W_45_Herestraat_813970228_A, W_48_Herestraat_13067134_D,
            W_44_Herestraat_270181177_A, "Herestraat",
            null,
            "410;600;601");

        final int W_43_Rennes_Singel_78568454_B = 78568454;
        final int W_42_Rennes_Singel_225605633_A = 225605633;

        RouteSegmentToExtract segment153 = segment152.addPTWayMember(43);
        extractAndAssertValues(43, segment152, segment153, cloneOfBus600RouteRelation,
            W_44_Herestraat_270181177_A, W_44_Herestraat_270181177_A,
            W_43_Rennes_Singel_78568454_B, "Rennes-Singel",
            null,
            "410;600");

        final int W_41_Rennes_Singel_8131123_D = 8131123;
        final int W_38_Den_Boschsingel_23837543_A = 23837543;

        for (int n = 42; n >= 42; n--) {returnValueNull =segment153.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment154 = segment153.addPTWayMember(41);
        extractAndAssertValues(41, segment153, segment154, cloneOfBus600RouteRelation,
            W_42_Rennes_Singel_225605633_A, W_43_Rennes_Singel_78568454_B,
            W_41_Rennes_Singel_8131123_D, "Rennes-Singel",
            null,
            "178;318;410;600");

        final int W_37_Den_Boschsingel_146171867_D = 146171867;
        final int W_34_Lüdenscheidsingel_85048201_A = 85048201;

        for (int n = 40; n >= 38; n--) {returnValueNull =segment154.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment155 = segment154.addPTWayMember(37);
        extractAndAssertValues(37, segment154, segment155, cloneOfBus600RouteRelation,
            W_38_Den_Boschsingel_23837543_A, W_41_Rennes_Singel_8131123_D,
            W_37_Den_Boschsingel_146171867_D, "Den Boschsingel",
            null,
            "318;410;600");

        final int W_33_Lüdenscheidsingel_12891213_D = 12891213;
        final int W_30_Joanna_Maria_Artoisplein_61540098_A = 61540098;

        for (int n = 36; n >= 34; n--) {returnValueNull =segment155.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment156 = segment155.addPTWayMember(33);
        extractAndAssertValues(33, segment155, segment156, cloneOfBus600RouteRelation,
            W_34_Lüdenscheidsingel_85048201_A, W_37_Den_Boschsingel_146171867_D,
            W_33_Lüdenscheidsingel_12891213_D, "Lüdenscheidsingel",
            null,
            "318;410;600;651;658");

        final int W_29_Joanna_Maria_Artoisplein_254801390_B = 254801390;
        final int W_28_Joanna_Maria_Artoisplein_61540068_A = 61540068;

        for (int n = 32; n >= 30; n--) {returnValueNull =segment156.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment157 = segment156.addPTWayMember(29);
        extractAndAssertValues(29, segment156, segment157, cloneOfBus600RouteRelation,
            W_30_Joanna_Maria_Artoisplein_61540098_A, W_33_Lüdenscheidsingel_12891213_D,
            W_29_Joanna_Maria_Artoisplein_254801390_B, "Joanna-Maria Artoisplein",
            null,
            "178;305;318;410;600;651;652;658");

        final int W_27_Vuurkruisenlaan_23691160_C = 23691160;
        final int W_25_Diestsepoort_8109264_A = 8109264;

        for (int n = 28; n >= 28; n--) {returnValueNull =segment157.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment158 = segment157.addPTWayMember(27);
        extractAndAssertValues(27, segment157, segment158, cloneOfBus600RouteRelation,
            W_28_Joanna_Maria_Artoisplein_61540068_A, W_29_Joanna_Maria_Artoisplein_254801390_B,
            W_27_Vuurkruisenlaan_23691160_C, "Vuurkruisenlaan",
            null,
            "178;305;318;333;334;335;410;513;600;630;651;652;658");

        final int W_24_Diestsepoort_61556877_D = 61556877;
        final int W_21_Diestsepoort_584356745_A = 584356745;

        for (int n = 26; n >= 25; n--) {returnValueNull =segment158.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment159 = segment158.addPTWayMember(24);
        extractAndAssertValues(24, segment158, segment159, cloneOfBus600RouteRelation,
            W_25_Diestsepoort_8109264_A, W_27_Vuurkruisenlaan_23691160_C,
            W_24_Diestsepoort_61556877_D, "Diestsepoort",
            null,
            "305;333;334;335;513;600;630;651;652;658");

        final int W_20_Diestsepoort_198559166_E = 198559166;
        final int W_16_Diestsepoort_451873774_A = 451873774;

        for (int n = 23; n >= 21; n--) {returnValueNull =segment159.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment160 = segment159.addPTWayMember(20);
        extractAndAssertValues(20, segment159, segment160, cloneOfBus600RouteRelation,
            W_21_Diestsepoort_584356745_A, W_24_Diestsepoort_61556877_D,
            W_20_Diestsepoort_198559166_E, "Diestsepoort",
            null,
            "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658");

        final int W_15_Diestsepoort_116797180_B = 116797180;
        final int W_14_Diestsepoort_23691157_A = 23691157;

        for (int n = 19; n >= 16; n--) {returnValueNull =segment160.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment161 = segment160.addPTWayMember(15);
        extractAndAssertValues(15, segment160, segment161, cloneOfBus600RouteRelation,
            W_16_Diestsepoort_451873774_A, W_20_Diestsepoort_198559166_E,
            W_15_Diestsepoort_116797180_B, "Diestsepoort",
            null,
            "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658");

        final int W_13_p12_to_Diestsepoort_78815527_B = 78815527;
        final int W_12_perron_12_377918658_A = 377918658;

        for (int n = 15; n >= 14; n--) {returnValueNull =segment161.addPTWayMember(n); assertNull(n + " http://127.0.0.1:8111/zoom?left=8&right=8&top=48&bottom=48&select=way" + cloneOfBus600RouteRelation.getMember(n).getMember().getId() + "\n", returnValueNull);}
        RouteSegmentToExtract segment162 = segment161.addPTWayMember(13);
        extractAndAssertValues(13, segment161, segment162, cloneOfBus600RouteRelation,
            W_14_Diestsepoort_23691157_A, W_15_Diestsepoort_116797180_B,
            W_13_p12_to_Diestsepoort_78815527_B, null,
            null,
            "4;5;6;7;8;9;179;306 (student);334;335;337;380;600;616;651;652;658");

        RouteSegmentToExtract segment163 = segment162.addPTWayMember(12);
        extractAndAssertValues(12, segment162, segment163, cloneOfBus600RouteRelation,
            W_12_perron_12_377918658_A, W_13_p12_to_Diestsepoort_78815527_B,
            0, null,
            null,
            "395;600;651;652");

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

        final int W_194_Dorpskring_27684829_D = 27684829;
        final int W_191_Dorpskring_112917099_A = 112917099;

        final int W_190_Dorpskring_125835538_A = 125835538;

        final int W_189_Bollenberg_81197019_Z = 81197019;
        final int W_179_Kapelstraat_16377612_A = 16377612;

        final int W_178_Lostraat_40189518_Z = 40189518;
        final int W_130_Oude_Diestsesteenweg_10230617_A = 10230617;

        final int W_129_Diestsesteenweg_23707243_A = 23707243;

        final int W_128_Diestsesteenweg_23707244_B = 23707244;
        final int W_127_Diestsesteenweg_12715116_A = 12715116;

        final int W_126_Diestsepoort_61556877_D = 61556877;

        for (int n = 194; n >= 191; n--) {returnValueNull =segment201.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment202 = segment201.addPTWayMember(190);
        extractAndAssertValues(190, segment201, segment202, bus3_GHB_Lubbeek_RouteRelation,
            W_191_Dorpskring_112917099_A, W_194_Dorpskring_27684829_D,
            W_190_Dorpskring_125835538_A, "Dorpskring",
            null,
            "3");

        RouteSegmentToExtract segment203 = segment202.addPTWayMember(189);
        extractAndAssertValues(189, segment202, segment203, bus3_GHB_Lubbeek_RouteRelation,
            W_190_Dorpskring_125835538_A, W_190_Dorpskring_125835538_A,
            W_189_Bollenberg_81197019_Z, "Bollenberg",
            null,
            "3;373;485");

        for (int n = 188; n >= 179; n--) {returnValueNull =segment203.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment204 = segment203.addPTWayMember(178);
        extractAndAssertValues(178, segment203, segment204, bus3_GHB_Lubbeek_RouteRelation,
            W_179_Kapelstraat_16377612_A, W_189_Bollenberg_81197019_Z,
            W_178_Lostraat_40189518_Z, "Lostraat",
            null,
            "3");

        for (int n = 177; n >= 130; n--) {returnValueNull =segment204.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment205 = segment204.addPTWayMember(129);
        extractAndAssertValues(129, segment204, segment205, bus3_GHB_Lubbeek_RouteRelation,
            W_130_Oude_Diestsesteenweg_10230617_A, W_178_Lostraat_40189518_Z,
            W_129_Diestsesteenweg_23707243_A, "Diestsesteenweg",
            null,
            "3");

        RouteSegmentToExtract segment206 = segment205.addPTWayMember(128);
        extractAndAssertValues(128, segment205, segment206, bus3_GHB_Lubbeek_RouteRelation,
            W_129_Diestsesteenweg_23707243_A, W_129_Diestsesteenweg_23707243_A,
            W_128_Diestsesteenweg_23707244_B, "Diestsesteenweg",
            null,
            "3;370;371;373;374;475;485;524;525");

        for (int n = 127; n >= 127; n--) {returnValueNull =segment206.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment207 = segment206.addPTWayMember(126);
        extractAndAssertValues(126, segment206, segment207, bus3_GHB_Lubbeek_RouteRelation,
            W_127_Diestsesteenweg_12715116_A, W_128_Diestsesteenweg_23707244_B,
            W_126_Diestsepoort_61556877_D, "Diestsepoort",
            null,
            "2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525");

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

        final int W_217_Dorpskring_27684829_D = 27684829;
        final int W_214_Dorpskring_112917099_A = 112917099;

        final int W_213_Dorpskring_125835538_A = 125835538;

        final int W_212_Bollenberg_81197019_Z = 81197019;
        final int W_202_Kapelstraat_16377612_A = 16377612;

        final int W_201_Ganzendries_112917224_F = 112917224;
        final int W_196__847554912_A = 847554912;

        final int W_195__27686453_G = 27686453;
        final int W_189__112917225_A = 112917225;

        final int W_188__847554912_F = 847554912;
        final int W_183_Ganzendries_112917224_A = 112917224;

        final int W_182_Lostraat_40189518_Z = 40189518;
        final int W_134_Oude_Diestsesteenweg_10230617_A = 10230617;

        final int W_133_Diestsesteenweg_23707243_A = 23707243;

        final int W_132_Diestsesteenweg_23707244_B = 23707244;
        final int W_131_Diestsesteenweg_12715116_A = 12715116;

        final int W_130_Diestsepoort_61556877_D = 61556877;
        final int W_127_Diestsepoort_584356745_A = 584356745;

        final int W_126_Diestsepoort_198559166_E = 198559166;
        final int W_122_Diestsepoort_451873774_A = 451873774;

        final int W_121__76867049_C = 76867049;
        final int W_119__79596965_A = 79596965;

        final int W_118__79596974_B = 79596974;
        final int W_117__79596982_A = 79596982;

        final int W_116__79596987_C = 79596987;
        final int W_114__79596980_A = 79596980;

        final int W_113_Tiensevest_79193579_A = 79193579;

        final int W_112_Tiensevest_258936980_A = 258936980;

        final int W_111_Tiensevest_79193580_A = 79193580;

        final int W_110_Tiensevest_79193581_A = 79193581;

        final int W_109_Tiensevest_78815505_A = 78815505;

        final int W_108_Tiensevest_84696751_B = 84696751;
        final int W_107_Tiensevest_79265237_A = 79265237;

        final int W_106_Tiensevest_89574079_B = 89574079;
        final int W_105_Tiensevest_81522744_A = 81522744;

        final int W_104_Bondgenotenlaan_305434579_H = 305434579;
        final int W_97_Rector_De_Somerplein_521211977_A = 521211977;

        final int W_96_Rector_De_Somerplein_521211976_B = 521211976;
        final int W_95_Rector_De_Somerplein_16771741_A = 16771741;

        final int W_94_Margarethaplein_3991635_M = 3991635;
        final int W_82_Brouwersstraat_284664268_A = 284664268;

        final int W_81_Brouwersstraat_608715545_E = 608715545;
        final int W_77_Brouwersstraat_284664272_A = 284664272;

        final int W_76_Tessenstraat___Fonteinstraat_147856945_B = 147856945;
        final int W_75_Kapucijnenvoer_123929547_A = 123929547;

        final int W_74_Biezenstraat_3358673_E = 3358673;
        final int W_70_Sint_Hubertusstraat_123929615_A = 123929615;

        final int W_69_Monseigneur_Van_Waeyenberghlaan_189453003_G = 189453003;
        final int W_63_Monseigneur_Van_Waeyenberghlaan_249333181_A = 249333181;

        final int W_62_Rotonde_Het_Teken_249333187_A = 249333187;
        final int W_61_Rotonde_Het_Teken_813970230_B = 813970230;

        final int W_60_Rotonde_Het_Teken_41403540_A = 41403540;
        final int W_59_Rotonde_Het_Teken_41403538_B = 41403538;

        final int W_58_Ring_Zuid_79340950_A = 79340950;

        final int W_57_Ring_Zuid_11369123_A = 11369123;

        final int W_56__159949154_A = 159949154;

        final int W_55__332258104_B = 332258104;
        final int W_54__78852604_A = 78852604;

        final int W_53__14508735_A = 14508735;

        final int W_52__109267438_A = 109267438;

        final int W_51__318878532_A = 318878532;

        final int W_50__100687528_A = 100687528;

        final int W_49_Ring_Zuid_14508739_A = 14508739;

        final int W_48_Ring_Zuid_14508740_A = 14508740;

        final int W_47_Ring_Zuid_502328838_D = 502328838;
        final int W_44_Rotonde_Het_Teken_78568660_A = 78568660;

        final int W_43_Ring_Noord_15945426_G = 15945426;
        final int W_37_Ring_Noord_377918625_A = 377918625;

        for (int n = 217; n >= 214; n--) {returnValueNull =segment301.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment302 = segment301.addPTWayMember(213);
        extractAndAssertValues(213, segment301, segment302, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_214_Dorpskring_112917099_A, W_217_Dorpskring_27684829_D,
            W_213_Dorpskring_125835538_A, "Dorpskring",
            null,
            "3");

        RouteSegmentToExtract segment303 = segment302.addPTWayMember(212);
        extractAndAssertValues(212, segment302, segment303, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_213_Dorpskring_125835538_A, W_213_Dorpskring_125835538_A,
            W_212_Bollenberg_81197019_Z, "Bollenberg",
            null,
            "3;373;485");

        for (int n = 211; n >= 202; n--) {returnValueNull =segment303.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment304 = segment303.addPTWayMember(201);
        extractAndAssertValues(201, segment303, segment304, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_202_Kapelstraat_16377612_A, W_212_Bollenberg_81197019_Z,
            W_201_Ganzendries_112917224_F, "Ganzendries",
            null,
            "3");

        for (int n = 200; n >= 196; n--) {returnValueNull =segment304.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment305 = segment304.addPTWayMember(195);
        extractAndAssertValues(195, segment304, segment305, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_196__847554912_A, W_201_Ganzendries_112917224_F,
            W_195__27686453_G, null,
            null,
            "3");

        for (int n = 194; n >= 189; n--) {returnValueNull =segment305.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment306 = segment305.addPTWayMember(188);
        extractAndAssertValues(188, segment305, segment306, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_189__112917225_A, W_195__27686453_G,
            W_188__847554912_F, null,
            null,
            "3");

        for (int n = 187; n >= 183; n--) {returnValueNull =segment306.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment307 = segment306.addPTWayMember(182);
        extractAndAssertValues(182, segment306, segment307, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_183_Ganzendries_112917224_A, W_188__847554912_F,
            W_182_Lostraat_40189518_Z, "Lostraat",
            null,
            "3");

        for (int n = 181; n >= 134; n--) {returnValueNull =segment307.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment308 = segment307.addPTWayMember(133);
        extractAndAssertValues(133, segment307, segment308, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_134_Oude_Diestsesteenweg_10230617_A, W_182_Lostraat_40189518_Z,
            W_133_Diestsesteenweg_23707243_A, "Diestsesteenweg",
            null,
            "3");

        RouteSegmentToExtract segment309 = segment308.addPTWayMember(132);
        extractAndAssertValues(132, segment308, segment309, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_133_Diestsesteenweg_23707243_A, W_133_Diestsesteenweg_23707243_A,
            W_132_Diestsesteenweg_23707244_B, "Diestsesteenweg",
            null,
            "3;370;371;373;374;475;485;524;525");

        for (int n = 131; n >= 131; n--) {returnValueNull =segment309.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment310 = segment309.addPTWayMember(130);
        extractAndAssertValues(130, segment309, segment310, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_131_Diestsesteenweg_12715116_A, W_132_Diestsesteenweg_23707244_B,
            W_130_Diestsepoort_61556877_D, "Diestsepoort",
            null,
            "2;3;179;306;306 (student);310;370;371;373;374;433;475;485;520;524;525");

        for (int n = 129; n >= 127; n--) {returnValueNull =segment310.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment311 = segment310.addPTWayMember(126);
        extractAndAssertValues(126, segment310, segment311, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_127_Diestsepoort_584356745_A, W_130_Diestsepoort_61556877_D,
            W_126_Diestsepoort_198559166_E, "Diestsepoort",
            null,
            "2;3;179;305;306;306 (student);310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630;651;652;658");

        for (int n = 125; n >= 122; n--) {returnValueNull =segment311.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment312 = segment311.addPTWayMember(121);
        extractAndAssertValues(121, segment311, segment312, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_122_Diestsepoort_451873774_A, W_126_Diestsepoort_198559166_E,
            W_121__76867049_C, null,
            null,
            "2;3;179;306 (student);310;333;334;335;337;370;371;373;374;433;475;485;513;520;524;525;600;616;630;651;652;658");

        for (int n = 120; n >= 119; n--) {returnValueNull =segment312.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment313 = segment312.addPTWayMember(118);
        extractAndAssertValues(118, segment312, segment313, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_119__79596965_A, W_121__76867049_C,
            W_118__79596974_B, null,
            null,
            "2;3;310;333;334;335;370;371;373;374;433;475;485;513;520;524;525;600;630");

        for (int n = 117; n >= 117; n--) {returnValueNull =segment313.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment314 = segment313.addPTWayMember(116);
        extractAndAssertValues(116, segment313, segment314, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_117__79596982_A, W_118__79596974_B,
            W_116__79596987_C, null,
            null,
            "3;333;334;335;433;600;630");

        for (int n = 115; n >= 114; n--) {returnValueNull =segment314.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment315 = segment314.addPTWayMember(113);
        extractAndAssertValues(113, segment314, segment315, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_114__79596980_A, W_116__79596987_C,
            W_113_Tiensevest_79193579_A, "Tiensevest",
            null,
            "3;333;334;335;513;600;630");

        RouteSegmentToExtract segment316 = segment315.addPTWayMember(112);
        extractAndAssertValues(112, segment315, segment316, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_113_Tiensevest_79193579_A, W_113_Tiensevest_79193579_A,
            W_112_Tiensevest_258936980_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658");

        RouteSegmentToExtract segment317 = segment316.addPTWayMember(111);
        extractAndAssertValues(111, segment316, segment317, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_112_Tiensevest_258936980_A, W_112_Tiensevest_258936980_A,
            W_111_Tiensevest_79193580_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630;658");

        RouteSegmentToExtract segment318 = segment317.addPTWayMember(110);
        extractAndAssertValues(110, segment317, segment318, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_111_Tiensevest_79193580_A, W_111_Tiensevest_79193580_A,
            W_110_Tiensevest_79193581_A, "Tiensevest",
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        RouteSegmentToExtract segment319 = segment318.addPTWayMember(109);
        extractAndAssertValues(109, segment318, segment319, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_110_Tiensevest_79193581_A, W_110_Tiensevest_79193581_A,
            W_109_Tiensevest_78815505_A, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        RouteSegmentToExtract segment320 = segment319.addPTWayMember(108);
        extractAndAssertValues(108, segment319, segment320, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_109_Tiensevest_78815505_A, W_109_Tiensevest_78815505_A,
            W_108_Tiensevest_84696751_B, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;600;616;630");

        for (int n = 107; n >= 107; n--) {returnValueNull =segment320.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment321 = segment320.addPTWayMember(106);
        extractAndAssertValues(106, segment320, segment321, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_107_Tiensevest_79265237_A, W_108_Tiensevest_84696751_B,
            W_106_Tiensevest_89574079_B, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;370;371;373;374;380;513;520;524;525;527;600;616;630");

        for (int n = 105; n >= 105; n--) {returnValueNull =segment321.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment322 = segment321.addPTWayMember(104);
        extractAndAssertValues(104, segment321, segment322, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_105_Tiensevest_81522744_A, W_106_Tiensevest_89574079_B,
            W_104_Bondgenotenlaan_305434579_H, "Bondgenotenlaan",
            null,
            "1;2;3;4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;333;334;335;337;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539;600;616;630");

        for (int n = 103; n >= 97; n--) {returnValueNull =segment322.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment323 = segment322.addPTWayMember(96);
        extractAndAssertValues(96, segment322, segment323, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_97_Rector_De_Somerplein_521211977_A, W_104_Bondgenotenlaan_305434579_H,
            W_96_Rector_De_Somerplein_521211976_B, "Rector De Somerplein",
            null,
            "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539");

        for (int n = 95; n >= 95; n--) {returnValueNull =segment323.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment324 = segment323.addPTWayMember(94);
        extractAndAssertValues(94, segment323, segment324, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_95_Rector_De_Somerplein_16771741_A, W_96_Rector_De_Somerplein_521211976_B,
            W_94_Margarethaplein_3991635_M, "Margarethaplein",
            null,
            "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537;539");

        for (int n = 93; n >= 82; n--) {returnValueNull =segment324.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment325 = segment324.addPTWayMember(81);
        extractAndAssertValues(81, segment324, segment325, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_82_Brouwersstraat_284664268_A, W_94_Margarethaplein_3991635_M,
            W_81_Brouwersstraat_608715545_E, "Brouwersstraat",
            null,
            "3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537");

        for (int n = 80; n >= 77; n--) {returnValueNull =segment325.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment326 = segment325.addPTWayMember(76);
        extractAndAssertValues(76, segment325, segment326, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_77_Brouwersstraat_284664272_A, W_81_Brouwersstraat_608715545_E,
            W_76_Tessenstraat___Fonteinstraat_147856945_B, "Tessenstraat - Fonteinstraat",
            null,
            "3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537");

        for (int n = 75; n >= 75; n--) {returnValueNull =segment326.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment327 = segment326.addPTWayMember(74);
        extractAndAssertValues(74, segment326, segment327, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_75_Kapucijnenvoer_123929547_A, W_76_Tessenstraat___Fonteinstraat_147856945_B,
            W_74_Biezenstraat_3358673_E, "Biezenstraat",
            null,
            "3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513;520;524;525;527;537");

        for (int n = 73; n >= 70; n--) {returnValueNull =segment327.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment328 = segment327.addPTWayMember(69);
        extractAndAssertValues(69, segment327, segment328, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_70_Sint_Hubertusstraat_123929615_A, W_74_Biezenstraat_3358673_E,
            W_69_Monseigneur_Van_Waeyenberghlaan_189453003_G, "Monseigneur Van Waeyenberghlaan",
            null,
            "3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;513");

        for (int n = 68; n >= 63; n--) {returnValueNull =segment328.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment329 = segment328.addPTWayMember(62);
        extractAndAssertValues(62, segment328, segment329, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_63_Monseigneur_Van_Waeyenberghlaan_249333181_A, W_69_Monseigneur_Van_Waeyenberghlaan_189453003_G,
            W_62_Rotonde_Het_Teken_249333187_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;513");

        RouteSegmentToExtract segment330 = segment329.addPTWayMember(61);
        extractAndAssertValues(61, segment329, segment330, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_62_Rotonde_Het_Teken_249333187_A, W_62_Rotonde_Het_Teken_249333187_A,
            W_61_Rotonde_Het_Teken_813970230_B, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        for (int n = 60; n >= 60; n--) {returnValueNull =segment330.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment331 = segment330.addPTWayMember(59);
        extractAndAssertValues(59, segment330, segment331, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_60_Rotonde_Het_Teken_41403540_A, W_61_Rotonde_Het_Teken_813970230_B,
            W_59_Rotonde_Het_Teken_41403538_B, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;513");

        for (int n = 58; n >= 58; n--) {returnValueNull =segment331.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment332 = segment331.addPTWayMember(57);
        extractAndAssertValues(57, segment331, segment332, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_58_Ring_Zuid_79340950_A, W_59_Rotonde_Het_Teken_41403538_B,
            W_57_Ring_Zuid_11369123_A, "Ring Zuid",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment333 = segment332.addPTWayMember(56);
        extractAndAssertValues(56, segment332, segment333, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_57_Ring_Zuid_11369123_A, W_57_Ring_Zuid_11369123_A,
            W_56__159949154_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment334 = segment333.addPTWayMember(55);
        extractAndAssertValues(55, segment333, segment334, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_56__159949154_A, W_56__159949154_A,
            W_55__332258104_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        for (int n = 54; n >= 54; n--) {returnValueNull =segment334.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment335 = segment334.addPTWayMember(53);
        extractAndAssertValues(53, segment334, segment335, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_54__78852604_A, W_55__332258104_B,
            W_53__14508735_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment336 = segment335.addPTWayMember(52);
        extractAndAssertValues(52, segment335, segment336, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_53__14508735_A, W_53__14508735_A,
            W_52__109267438_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600");

        RouteSegmentToExtract segment337 = segment336.addPTWayMember(51);
        extractAndAssertValues(51, segment336, segment337, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_52__109267438_A, W_52__109267438_A,
            W_51__318878532_A, null,
            null,
            "3;333;334;335;370;371;373;374;380;513");

        RouteSegmentToExtract segment338 = segment337.addPTWayMember(50);
        extractAndAssertValues(50, segment337, segment338, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_51__318878532_A, W_51__318878532_A,
            W_50__100687528_A, null,
            null,
            "3;333;334;335;513");

        RouteSegmentToExtract segment339 = segment338.addPTWayMember(49);
        extractAndAssertValues(49, segment338, segment339, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_50__100687528_A, W_50__100687528_A,
            W_49_Ring_Zuid_14508739_A, "Ring Zuid",
            null,
            "3;334;335");

        RouteSegmentToExtract segment340 = segment339.addPTWayMember(48);
        extractAndAssertValues(48, segment339, segment340, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_49_Ring_Zuid_14508739_A, W_49_Ring_Zuid_14508739_A,
            W_48_Ring_Zuid_14508740_A, "Ring Zuid",
            null,
            "3;317;334;335;395;410;600;601");

        RouteSegmentToExtract segment341 = segment340.addPTWayMember(47);
        extractAndAssertValues(47, segment340, segment341, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_48_Ring_Zuid_14508740_A, W_48_Ring_Zuid_14508740_A,
            W_47_Ring_Zuid_502328838_D, "Ring Zuid",
            null,
            "3;317;334;335;380;395;410;600;601");

        for (int n = 46; n >= 44; n--) {returnValueNull =segment341.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment342 = segment341.addPTWayMember(43);
        extractAndAssertValues(43, segment341, segment342, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_44_Rotonde_Het_Teken_78568660_A, W_47_Ring_Zuid_502328838_D,
            W_43_Ring_Noord_15945426_G, "Ring Noord",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        for (int n = 42; n >= 37; n--) {returnValueNull =segment342.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_GHB_Pellenberg_Lubbeek_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment343 = segment342.addPTWayMember(36);
        extractAndAssertValues(36, segment342, segment343, bus3_GHB_Pellenberg_Lubbeek_RouteRelation,
            W_37_Ring_Noord_377918625_A, W_43_Ring_Noord_15945426_G,
            0, null,
            null,
            "3");

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
        final int W_221_Ring_Noord_377918626_I = 377918626;
        final int W_213_Ring_Noord_112917239_A = 112917239;
        final int W_212_Rotonde_Het_Teken_3752557_A = 3752557;
        final int W_211_Rotonde_Het_Teken_249333188_A = 249333188;
        final int W_210_Rotonde_Het_Teken_249333187_A = 249333187;
        final int W_209_Rotonde_Het_Teken_813970230_B = 813970230;
        final int W_208_Rotonde_Het_Teken_41403540_A = 41403540;
        final int W_207_Rotonde_Het_Teken_41403538_B = 41403538;
        final int W_206_Ring_Zuid_79340950_A = 79340950;
        final int W_205_Ring_Zuid_11369123_A = 11369123;
        final int W_204__159949154_A = 159949154;
        final int W_203__332258104_B = 332258104;
        final int W_202__78852604_A = 78852604;
        final int W_201__377918641_B = 377918641;
        final int W_200__14508736_A = 14508736;
        final int W_199_Ring_Zuid_109267436_A = 109267436;
        final int W_198_Ring_Zuid_14508739_A = 14508739;
        final int W_197_Ring_Zuid_14508740_A = 14508740;
        final int W_196_Ring_Zuid_502328838_D = 502328838;
        final int W_193_Rotonde_Het_Teken_78568660_A = 78568660;
        final int W_192_Rotonde_Het_Teken_78873921_A = 78873921;
        final int W_191_Rotonde_Het_Teken_3752557_A = 3752557;
        final int W_190_Monseigneur_Van_Waeyenberghlaan_249333183_G = 249333183;
        final int W_184_Monseigneur_Van_Waeyenberghlaan_189453003_A = 189453003;
        final int W_183_Sint_Hubertusstraat_123929615_E = 123929615;
        final int W_179_Biezenstraat_3358673_A = 3358673;
        final int W_178_Kapucijnenvoer_123929547_B = 123929547;
        final int W_177_Tessenstraat___Fonteinstraat_147856945_A = 147856945;
        final int W_176_Brouwersstraat_284664272_E = 284664272;
        final int W_172_Brouwersstraat_608715545_A = 608715545;
        final int W_171_Brouwersstraat_284664268_M = 284664268;
        final int W_159_Margarethaplein_3991635_A = 3991635;
        final int W_158_Rector_De_Somerplein_16771741_J = 16771741;
        final int W_149_Bondgenotenlaan_305434579_A = 305434579;
        final int W_148_Tiensevest_81522744_B = 81522744;
        final int W_147_Tiensevest_89574079_A = 89574079;
        final int W_146_Tiensevest_79265237_A = 79265237;
        final int W_145_Tiensevest_79211473_A = 79211473;
        final int W_144__79605527_F = 79605527;
        final int W_139__79596971_A = 79596971;
        final int W_138__377918635_A = 377918635;
        final int W_137__79264888_A = 79264888;
        final int W_136__79264897_B = 79264897;
        final int W_135__71754927_A = 71754927;
        final int W_134__79596965_C = 79596965;
        final int W_132__76867049_A = 76867049;
        final int W_131_Diestsepoort_451873774_B = 451873774;
        final int W_130_Diestsepoort_584356742_A = 584356742;
        final int W_129_Diestsepoort_451873773_G = 451873773;
        final int W_123_Diestsepoort_61556877_A = 61556877;
        final int W_122_Diestsepoort_8109264_C = 8109264;
        final int W_120_Diestsesteenweg_23707244_A = 23707244;
        final int W_119_Diestsesteenweg_23707243_A = 23707243;
        final int W_118_Oude_Diestsesteenweg_10230617_Z = 10230617;
        final int W_70_Lostraat_40189518_A = 40189518;
        final int W_69_Ganzendries_112917224_F = 112917224;
        final int W_64__847554912_A = 847554912;
        final int W_63__27686453_G = 27686453;
        final int W_57_Sint_Barbaradreef_112917225_A = 112917225;
        final int W_56__847554912_F = 847554912;
        final int W_51_Ganzendries_112917224_A = 112917224;
        final int W_50_Kapelstraat_16377612_K = 16377612;
        final int W_40_Bollenberg_81197019_A = 81197019;
        final int W_39_Dorpskring_125835541_A = 125835541;
        final int W_38_Dorpskring_81083332_C = 81083332;
        final int W_37_Dorpskring_125835543_B = 125835543;
        final int W_36_Dorpskring_608754701_A = 608754701;

        for (int n = 221; n >= 213; n--) {returnValueNull =segment401.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment402 = segment401.addPTWayMember(212);
        extractAndAssertValues(212, segment401, segment402, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_213_Ring_Noord_112917239_A, W_221_Ring_Noord_377918626_I,
            W_212_Rotonde_Het_Teken_3752557_A, "Rotonde Het Teken",
            null,
            "3");

        RouteSegmentToExtract segment403 = segment402.addPTWayMember(211);
        extractAndAssertValues(211, segment402, segment403, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_212_Rotonde_Het_Teken_3752557_A, W_212_Rotonde_Het_Teken_3752557_A,
            W_211_Rotonde_Het_Teken_249333188_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        RouteSegmentToExtract segment404 = segment403.addPTWayMember(210);
        extractAndAssertValues(210, segment403, segment404, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_211_Rotonde_Het_Teken_249333188_A, W_211_Rotonde_Het_Teken_249333188_A,
            W_210_Rotonde_Het_Teken_249333187_A, "Rotonde Het Teken",
            null,
            "3;410;600;601");

        RouteSegmentToExtract segment405 = segment404.addPTWayMember(209);
        extractAndAssertValues(209, segment404, segment405, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_210_Rotonde_Het_Teken_249333187_A, W_210_Rotonde_Het_Teken_249333187_A,
            W_209_Rotonde_Het_Teken_813970230_B, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        for (int n = 208; n >= 208; n--) {returnValueNull =segment405.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment406 = segment405.addPTWayMember(207);
        extractAndAssertValues(207, segment405, segment406, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_208_Rotonde_Het_Teken_41403540_A, W_209_Rotonde_Het_Teken_813970230_B,
            W_207_Rotonde_Het_Teken_41403538_B, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;513");

        for (int n = 206; n >= 206; n--) {returnValueNull =segment406.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment407 = segment406.addPTWayMember(205);
        extractAndAssertValues(205, segment406, segment407, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_206_Ring_Zuid_79340950_A, W_207_Rotonde_Het_Teken_41403538_B,
            W_205_Ring_Zuid_11369123_A, "Ring Zuid",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment408 = segment407.addPTWayMember(204);
        extractAndAssertValues(204, segment407, segment408, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_205_Ring_Zuid_11369123_A, W_205_Ring_Zuid_11369123_A,
            W_204__159949154_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        RouteSegmentToExtract segment409 = segment408.addPTWayMember(203);
        extractAndAssertValues(203, segment408, segment409, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_204__159949154_A, W_204__159949154_A,
            W_203__332258104_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        for (int n = 202; n >= 202; n--) {returnValueNull =segment409.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment410 = segment409.addPTWayMember(201);
        extractAndAssertValues(201, segment409, segment410, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_202__78852604_A, W_203__332258104_B,
            W_201__377918641_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601");

        for (int n = 200; n >= 200; n--) {returnValueNull =segment410.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment411 = segment410.addPTWayMember(199);
        extractAndAssertValues(199, segment410, segment411, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_200__14508736_A, W_201__377918641_B,
            W_199_Ring_Zuid_109267436_A, "Ring Zuid",
            null,
            "3;317;395;410;601");

        RouteSegmentToExtract segment412 = segment411.addPTWayMember(198);
        extractAndAssertValues(198, segment411, segment412, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_199_Ring_Zuid_109267436_A, W_199_Ring_Zuid_109267436_A,
            W_198_Ring_Zuid_14508739_A, "Ring Zuid",
            null,
            "3;317;395;410;600;601");

        RouteSegmentToExtract segment413 = segment412.addPTWayMember(197);
        extractAndAssertValues(197, segment412, segment413, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_198_Ring_Zuid_14508739_A, W_198_Ring_Zuid_14508739_A,
            W_197_Ring_Zuid_14508740_A, "Ring Zuid",
            null,
            "3;317;334;335;395;410;600;601");

        RouteSegmentToExtract segment414 = segment413.addPTWayMember(196);
        extractAndAssertValues(196, segment413, segment414, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_197_Ring_Zuid_14508740_A, W_197_Ring_Zuid_14508740_A,
            W_196_Ring_Zuid_502328838_D, "Ring Zuid",
            null,
            "3;317;334;335;380;395;410;600;601");

        for (int n = 195; n >= 193; n--) {returnValueNull =segment414.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment415 = segment414.addPTWayMember(192);
        extractAndAssertValues(192, segment414, segment415, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_193_Rotonde_Het_Teken_78568660_A, W_196_Ring_Zuid_502328838_D,
            W_192_Rotonde_Het_Teken_78873921_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        RouteSegmentToExtract segment416 = segment415.addPTWayMember(191);
        extractAndAssertValues(191, segment415, segment416, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_192_Rotonde_Het_Teken_78873921_A, W_192_Rotonde_Het_Teken_78873921_A,
            W_191_Rotonde_Het_Teken_3752557_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        RouteSegmentToExtract segment417 = segment416.addPTWayMember(190);
        extractAndAssertValues(190, segment416, segment417, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_191_Rotonde_Het_Teken_3752557_A, W_191_Rotonde_Het_Teken_3752557_A,
            W_190_Monseigneur_Van_Waeyenberghlaan_249333183_G, "Monseigneur Van Waeyenberghlaan",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601");

        for (int n = 189; n >= 184; n--) {returnValueNull =segment417.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment418 = segment417.addPTWayMember(183);
        extractAndAssertValues(183, segment417, segment418, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_184_Monseigneur_Van_Waeyenberghlaan_189453003_A, W_190_Monseigneur_Van_Waeyenberghlaan_249333183_G,
            W_183_Sint_Hubertusstraat_123929615_E, "Sint-Hubertusstraat",
            null,
            "3;317;333;334;335;370;371;373;374;380;395");

        for (int n = 182; n >= 179; n--) {returnValueNull =segment418.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment419 = segment418.addPTWayMember(178);
        extractAndAssertValues(178, segment418, segment419, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_179_Biezenstraat_3358673_A, W_183_Sint_Hubertusstraat_123929615_E,
            W_178_Kapucijnenvoer_123929547_B, "Kapucijnenvoer",
            null,
            "3;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395");

        for (int n = 177; n >= 177; n--) {returnValueNull =segment419.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment420 = segment419.addPTWayMember(176);
        extractAndAssertValues(176, segment419, segment420, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_177_Tessenstraat___Fonteinstraat_147856945_A, W_178_Kapucijnenvoer_123929547_B,
            W_176_Brouwersstraat_284664272_E, "Brouwersstraat",
            null,
            "3;7;8;9;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586");

        for (int n = 175; n >= 172; n--) {returnValueNull =segment420.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment421 = segment420.addPTWayMember(171);
        extractAndAssertValues(171, segment420, segment421, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_172_Brouwersstraat_608715545_A, W_176_Brouwersstraat_284664272_E,
            W_171_Brouwersstraat_284664268_M, "Brouwersstraat",
            null,
            "3;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586");

        for (int n = 170; n >= 159; n--) {returnValueNull =segment421.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment422 = segment421.addPTWayMember(158);
        extractAndAssertValues(158, segment421, segment422, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_159_Margarethaplein_3991635_A, W_171_Brouwersstraat_284664268_M,
            W_158_Rector_De_Somerplein_16771741_J, "Rector De Somerplein",
            null,
            "3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;586");

        for (int n = 157; n >= 149; n--) {returnValueNull =segment422.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment423 = segment422.addPTWayMember(148);
        extractAndAssertValues(148, segment422, segment423, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_149_Bondgenotenlaan_305434579_A, W_158_Rector_De_Somerplein_16771741_J,
            W_148_Tiensevest_81522744_B, "Tiensevest",
            null,
            "2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586");

        for (int n = 147; n >= 147; n--) {returnValueNull =segment423.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment424 = segment423.addPTWayMember(146);
        extractAndAssertValues(146, segment423, segment424, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_147_Tiensevest_89574079_A, W_148_Tiensevest_81522744_B,
            W_146_Tiensevest_79265237_A, "Tiensevest",
            null,
            "1;2;3;4;5;6;7;8;9;284;285;315;316;317;333;334;335;351;352;358;370;371;373;374;380;395;520;524;525;537;539;586;616");

        RouteSegmentToExtract segment425 = segment424.addPTWayMember(145);
        extractAndAssertValues(145, segment424, segment425, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_146_Tiensevest_79265237_A, W_146_Tiensevest_79265237_A,
            W_145_Tiensevest_79211473_A, "Tiensevest",
            null,
            "1;2;3;4;5;6;7;8;9;284;285;315;316;317;351;352;358;395;520;524;525;537;539;586;616");

        RouteSegmentToExtract segment426 = segment425.addPTWayMember(144);
        extractAndAssertValues(144, segment425, segment426, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_145_Tiensevest_79211473_A, W_145_Tiensevest_79211473_A,
            W_144__79605527_F, null,
            null,
            "1;2;3;4;5;6;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;616;658");

        for (int n = 143; n >= 139; n--) {returnValueNull =segment426.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment428 = segment426.addPTWayMember(138);
        extractAndAssertValues(138, segment426, segment428, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_139__79596971_A, W_144__79605527_F,
            W_138__377918635_A, null,
            null,
            "3;4;5;6;7;8;9");

        RouteSegmentToExtract segment429 = segment428.addPTWayMember(137);
        extractAndAssertValues(137, segment428, segment429, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_138__377918635_A, W_138__377918635_A,
            W_137__79264888_A, null,
            null,
            "3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601");

        RouteSegmentToExtract segment430 = segment429.addPTWayMember(136);
        extractAndAssertValues(136, segment429, segment430, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_137__79264888_A, W_137__79264888_A,
            W_136__79264897_B, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601");

        for (int n = 135; n >= 135; n--) {returnValueNull =segment430.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment432 = segment430.addPTWayMember(134);
        extractAndAssertValues(134, segment430, segment432, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_135__71754927_A, W_136__79264897_B,
            W_134__79596965_C, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601");

        for (int n = 133; n >= 132; n--) {returnValueNull =segment432.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment433 = segment432.addPTWayMember(131);
        extractAndAssertValues(131, segment432, segment433, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_132__76867049_A, W_134__79596965_C,
            W_131_Diestsepoort_451873774_B, "Diestsepoort",
            null,
            "2;3;4;5;6;7;8;9;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525");

        for (int n = 130; n >= 130; n--) {returnValueNull =segment433.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment434 = segment433.addPTWayMember(129);
        extractAndAssertValues(129, segment433, segment434, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_130_Diestsepoort_584356742_A, W_131_Diestsepoort_451873774_B,
            W_129_Diestsepoort_451873773_G, "Diestsepoort",
            null,
            "2;3;178;179;306;306 (student);310;318;333;337;370;371;373;374;410;433;475;485;512;520;524;525");

        for (int n = 128; n >= 123; n--) {returnValueNull =segment434.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment436 = segment434.addPTWayMember(122);
        extractAndAssertValues(122, segment434, segment436, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_123_Diestsepoort_61556877_A, W_129_Diestsepoort_451873773_G,
            W_122_Diestsepoort_8109264_C, "Diestsepoort",
            null,
            "2;3;178;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525");

        for (int n = 121; n >= 120; n--) {returnValueNull =segment436.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment437 = segment436.addPTWayMember(119);
        extractAndAssertValues(119, segment436, segment437, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_120_Diestsesteenweg_23707244_A, W_122_Diestsepoort_8109264_C,
            W_119_Diestsesteenweg_23707243_A, "Diestsesteenweg",
            null,
            "2;3;179;306;306 (student);310;333;370;371;373;374;433;475;485;512;520;524;525");

        RouteSegmentToExtract segment438 = segment437.addPTWayMember(118);
        extractAndAssertValues(118, segment437, segment438, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_119_Diestsesteenweg_23707243_A, W_119_Diestsesteenweg_23707243_A,
            W_118_Oude_Diestsesteenweg_10230617_Z, "Oude Diestsesteenweg",
            null,
            "3;370;371;373;374;475;485;524;525");

        for (int n = 117; n >= 70; n--) {returnValueNull =segment438.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment439 = segment438.addPTWayMember(69);
        extractAndAssertValues(69, segment438, segment439, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_70_Lostraat_40189518_A, W_118_Oude_Diestsesteenweg_10230617_Z,
            W_69_Ganzendries_112917224_F, "Ganzendries",
            null,
            "3");

        for (int n = 68; n >= 64; n--) {returnValueNull =segment439.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment440 = segment439.addPTWayMember(63);
        extractAndAssertValues(63, segment439, segment440, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_64__847554912_A, W_69_Ganzendries_112917224_F,
            W_63__27686453_G, null,
            null,
            "3");

        for (int n = 62; n >= 57; n--) {returnValueNull =segment440.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment441 = segment440.addPTWayMember(56);
        extractAndAssertValues(56, segment440, segment441, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_57_Sint_Barbaradreef_112917225_A, W_63__27686453_G,
            W_56__847554912_F, null,
            null,
            "3");

        for (int n = 55; n >= 51; n--) {returnValueNull =segment441.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment442 = segment441.addPTWayMember(50);
        extractAndAssertValues(50, segment441, segment442, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_51_Ganzendries_112917224_A, W_56__847554912_F,
            W_50_Kapelstraat_16377612_K, "Kapelstraat",
            null,
            "3");

        for (int n = 49; n >= 40; n--) {returnValueNull =segment442.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment443 = segment442.addPTWayMember(39);
        extractAndAssertValues(39, segment442, segment443, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_40_Bollenberg_81197019_A, W_50_Kapelstraat_16377612_K,
            W_39_Dorpskring_125835541_A, "Dorpskring",
            null,
            "3");

        RouteSegmentToExtract segment444 = segment443.addPTWayMember(38);
        extractAndAssertValues(38, segment443, segment444, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_39_Dorpskring_125835541_A, W_39_Dorpskring_125835541_A,
            W_38_Dorpskring_81083332_C, "Dorpskring",
            null,
            "3;373;485");

        // todo This is not the behaviour I wanted. It shouldn't actually split at index 36
        for (int n = 37; n >= 37; n--) {returnValueNull =segment444.addPTWayMember(n); assertNull(String.format("%d %s%s\n", n, rc, bus3_Lubbeek_Pellenberg_GHB_RouteRelation.getMember(n).getMember().getId()), returnValueNull);}
        RouteSegmentToExtract segment445 = segment444.addPTWayMember(36);
        extractAndAssertValues(36, segment444, segment445, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_37_Dorpskring_125835543_B, W_38_Dorpskring_81083332_C,
            W_36_Dorpskring_608754701_A, "Dorpskring",
            null,
            "3;373;485");

        RouteSegmentToExtract segment446 = segment445.addPTWayMember(35);
        extractAndAssertValues(35, segment445, segment446, bus3_Lubbeek_Pellenberg_GHB_RouteRelation,
            W_36_Dorpskring_608754701_A, W_36_Dorpskring_608754701_A,
            0, null,
            null,
            "3;373;485");
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

        assertEquals(String.format("%d relation id not correct\n", index), extractedRelation.getId(), superRouteRelation.getMember(index+1).getMember().getId());
        // newSegment should have the last way we tried to add to this segment
        if (firstWayIdForNewSegment != 0) {
            assertNotNull(String.format("No new segment was created for way\n%s%s at position %d", rc, firstWayIdForNewSegment, index), newSegment);
            final long wayId = newSegment.getWayMembers().get(0).getWay().getId();
            assertEquals(String.format("%d name of last added way not correct %s%s\n ", index, rc, wayId), nameOfNewWay, newSegment.getWayMembers().get(0).getWay().get("name"));
            assertEquals(String.format("%d id of first way not correct  %s%s\n", index, rc, wayId), firstWayIdForNewSegment, wayId);
        }
        return extractedRelation;
    }

    /**
     * This creates a list of variable definitions to be used in unit tests
     *
     * @param relation the route relation to print a list of ways with their ids for
     */
    @SuppressWarnings("unused")
    public void printListOfWays(Relation relation) {
        List<RelationMember> members = relation.getMembers();
        for (int i = members.size() - 1; i >= 0; i--) {
            RelationMember member = members.get(i);
            if (member.isWay() && RouteUtils.isPTWay(member)) {
                Way way = member.getWay();
                String id = String.valueOf(way.getId());
                String name = "";
                if (way.hasKey("name")) name = way.get("name");
                name = name.replace("-", "_");
                name = name.replace(" ", "_");
                name += "_";
                System.out.printf("final int W_%s_%s%s_ = %s;%n", i, name, id, id);
            }
        }
    }
}
