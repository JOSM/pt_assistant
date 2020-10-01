package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static org.junit.Assert.*;
import static org.openstreetmap.josm.io.OsmReader.parseDataSet;

@SuppressWarnings("NonAsciiCharacters")
public class RouteSegmentToExtractTest extends AbstractTest{

    private static DataSet ds;
    private static Collection<Relation> allRelations;

    @BeforeClass
    public static void init() throws FileNotFoundException, IllegalDataException {
        ds = parseDataSet(new FileInputStream(PATH_TO_PT_BEFORE_SPLITTING_TEST), null);
        allRelations = ds.getRelations();
    }

    @Test
    public void isItineraryInSameDirectionTest() {
        Relation bus601RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3612781)
            .findFirst().orElse(null);

        Relation bus370NightRouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 5240367)
            .findFirst().orElse(null);

        Relation bus358RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 6695469)
            .findFirst().orElse(null);

        Relation bus371RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 1606056)
            .findFirst().orElse(null);

        WaySequence<Way, Way, Way, Way> waysInParentRouteOf601 = new WaySequence<>(
            bus601RouteRelation.getMembers().get(156).getWay(),
            bus601RouteRelation.getMembers().get(157).getWay(),
            bus601RouteRelation.getMembers().get(158).getWay());
//        WaySequence<Way, Way, Way, Way> waysInParentRouteOf370Night = new WaySequence<>(
//            bus370NightRouteRelation.getMembers().get(152).getWay(),
//            bus370NightRouteRelation.getMembers().get(153).getWay(),
//            null);
        WaySequence<Way, Way, Way, Way> waysInParentRouteOf358 = new WaySequence<>(
            bus358RouteRelation.getMembers().get(114).getWay(),
            bus358RouteRelation.getMembers().get(115).getWay(),
            bus358RouteRelation.getMembers().get(116).getWay());
        WaySequence<Way, Way, Way, Way> waysInParentRouteOf371 = new WaySequence<>(
            bus371RouteRelation.getMembers().get(132).getWay(),
            bus371RouteRelation.getMembers().get(133).getWay(),
            bus371RouteRelation.getMembers().get(134).getWay());
        RouteSegmentToExtract segment601_1 = new RouteSegmentToExtract(bus601RouteRelation, ds);
//        assertTrue(segment601_1.isItineraryInSameDirection(waysInParentRouteOf601, waysInParentRouteOf370Night));
        assertTrue(segment601_1.isItineraryInSameDirection(waysInParentRouteOf601, waysInParentRouteOf358));
        assertFalse(segment601_1.isItineraryInSameDirection(waysInParentRouteOf601, waysInParentRouteOf371));
    }


    @Test
    public void bus601_600Test() {
        Relation bus601RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 3612781)
            .findFirst().orElse(null);

        assertNotNull(bus601RouteRelation);
        Relation cloneOfBus601RouteRelation = new Relation(bus601RouteRelation);
        assertNotNull(cloneOfBus601RouteRelation);
        RouteSegmentToExtract segment1 = new RouteSegmentToExtract(cloneOfBus601RouteRelation, ds);
        assertEquals(cloneOfBus601RouteRelation.get("ref"), segment1.getLineIdentifiersSignature());
        assertEquals(cloneOfBus601RouteRelation.get("colour"), segment1.getColoursSignature());

        assertNull(segment1.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment1.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment1.getWayMembers());

        // The following code serves to create list of
        // ways in the route relation to test with
//        Relation bus600RouteRelation = allRelations.stream()
//            .filter(relation -> relation.getId() == 955908)
//            .findFirst().orElse(null);
//        List<RelationMember> members = bus600RouteRelation.getMembers();
//        for (int i = members.size() - 1; i >= 0; i--) {
//            RelationMember member = members.get(i);
//            if (member.isWay() && RouteUtils.isPTWay(member)) {
//                Way way = member.getWay();
//                String id = String.valueOf(way.getId());
//                String name = "";
//                if (way.hasKey("name")) name = way.get("name");
//                name = name.replace("-","_");
//                name = name.replace(" ","_");
//                name += "_";
//                System.out.println(String.format("final int W_%s_%s%s_ = %s;",
//                    i, name, id, id));
//            }
//        }

        final int W_158_perron1and2terminus_78579065_B = 78579065;
        final int W_157_perron1and2_377814547_A = 377814547;

        final int W_156_TiensevestToPerron1_79596986_A = 79596986;

        RouteSegmentToExtract returnValueNull = segment1.addPTWayMember(158);
        assertNull(returnValueNull);
        returnValueNull = segment1.addPTWayMember(157);
        assertNull(returnValueNull);
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
            "284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;537;539;601;658"
        );

        final int W_155_Tiensevest_5_79211473_A = 79211473;
        RouteSegmentToExtract segment3 = segment2.addPTWayMember(155);

        // segment3 was created, verify segment2
        extractAndAssertValues(155, segment2, segment3, cloneOfBus601RouteRelation,
            W_156_TiensevestToPerron1_79596986_A, W_156_TiensevestToPerron1_79596986_A,
            W_155_Tiensevest_5_79211473_A, "Tiensevest",
            null,
            "305;306;310;318;358;410;433;475;485;601;658"
        );

        final int W_154_Tiensevest_4_79211472_A = 79211472;
        RouteSegmentToExtract segment4 = segment3.addPTWayMember(154);

        extractAndAssertValues(154, segment3, segment4, cloneOfBus601RouteRelation,
            W_155_Tiensevest_5_79211473_A, W_155_Tiensevest_5_79211473_A,
            W_154_Tiensevest_4_79211472_A, "Tiensevest",
            null,
            "1;2;3;7;8;9;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658"
        );

        final int W_153_Tiensevest_3_79211472_A = 79175435;
        RouteSegmentToExtract segment5 = segment4.addPTWayMember(153);

        extractAndAssertValues(153, segment4, segment5, cloneOfBus601RouteRelation,
            W_154_Tiensevest_4_79211472_A, W_154_Tiensevest_4_79211472_A,
            W_153_Tiensevest_3_79211472_A, "Tiensevest",
            null,
            "1;2;284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;520;524;525;537;539;586;601;658"
        );

        final int W_152_Tiensevest_2_80458208_A = 80458208;
        RouteSegmentToExtract segment6 = segment5.addPTWayMember(152);

        extractAndAssertValues(152, segment5, segment6, cloneOfBus601RouteRelation,
            W_153_Tiensevest_3_79211472_A, W_153_Tiensevest_3_79211472_A,
            W_152_Tiensevest_2_80458208_A, "Tiensevest",
            null,
            "284;285;305;306;310;315;316;317;318;351;352;358;395;410;433;475;485;524;537;601;651;652;658"
        );

        final int W_33_151_Tiensevest_1_19793164_A = 19793164;
        RouteSegmentToExtract segment7 = segment6.addPTWayMember(151);

        extractAndAssertValues(151, segment6, segment7, cloneOfBus601RouteRelation,
            W_152_Tiensevest_2_80458208_A, W_152_Tiensevest_2_80458208_A,
            W_33_151_Tiensevest_1_19793164_A, "Tiensevest",
            null,
            "284;285;305;306;310;315;316;317;318;358;395;410;433;475;485;601;630;651;652;658"
        );

        final int W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D = 6184898;
        final int W_31_149_Diestsevest_3_81457878_C = 81457878;
        final int W_30_148_Diestsevest_2_4003924_B = 4003924;
        final int W_29_147_Diestsevest_1_8133608_A = 8133608;

        final int W_28_146_Artoisplein_2_254800931_A = 254800931;

        RouteSegmentToExtract segment8 = segment7.addPTWayMember(150);
        Relation rel8 = extractAndAssertValues(150, segment7, segment8, cloneOfBus601RouteRelation,
            W_33_151_Tiensevest_1_19793164_A, W_33_151_Tiensevest_1_19793164_A,
            W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D, "Diestsevest",
            null,
            "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658"
        );

        for (int n = 149; n >= 147; n--) returnValueNull = segment8.addPTWayMember(n); assertNull(returnValueNull);

        final int W_145_Artoisplein_1_23691158_E = 23691158;
//        final int W_144_Lüdenscheidsingel_5_254800932_D = 254800932;
//        final int W_143_Lüdenscheidsingel_4_44932921_C = 44932921;
//        final int W_142_Lüdenscheidsingel_3_3993388_B = 3993388;
        final int W_141_Lüdenscheidsingel_2_109267417_A = 109267417;
        RouteSegmentToExtract segment9 = segment8.addPTWayMember(146);

        Relation rel9 = extractAndAssertValues(146, segment8, segment9, cloneOfBus601RouteRelation,
            W_29_147_Diestsevest_1_8133608_A, W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D,
            W_28_146_Artoisplein_2_254800931_A, "Joanna-Maria Artoisplein",
            null,
            "305;318;334;335;358;410;513;601;630;651;652;658"
        );

        RouteSegmentToExtract segment10 = segment9.addPTWayMember(145);
        Relation rel10 = extractAndAssertValues(145, segment9, segment10, cloneOfBus601RouteRelation,
            W_28_146_Artoisplein_2_254800931_A, W_28_146_Artoisplein_2_254800931_A,
            W_145_Artoisplein_1_23691158_E, "Joanna-Maria Artoisplein",
            null,
            "178;305;318;334;335;358;410;513;601;630;651;652;658"
        );
        for (int n = 144; n >= 141; n--) returnValueNull = segment10.addPTWayMember(n); assertNull(returnValueNull);

        final int W_140_Lüdenscheidsingel_1_3993387_E = 3993387;
//        final int W_139_R23_2_8131125_D = 8131125;
//        final int W_138_R23_1_3877105_C = 3877105;
//        final int W_137_Den_Boschsingel_1_146171871_B = 146171871;
        final int W_136_Den_Boschsingel_2_23837544_A = 23837544;
        final int W_135_Den_Boschsingel_3_225605630_E = 225605630;
        RouteSegmentToExtract segment11 = segment10.addPTWayMember(140);
        extractAndAssertValues(140, segment10, segment11, cloneOfBus601RouteRelation,
            W_141_Lüdenscheidsingel_2_109267417_A, W_145_Artoisplein_1_23691158_E,
            W_140_Lüdenscheidsingel_1_3993387_E, "Lüdenscheidsingel",
            null,
            "178;305;318;358;410;601;651;652;658"
        );
        for (int n = 139; n >= 136; n--) returnValueNull = segment11.addPTWayMember(n); assertNull(returnValueNull);

        RouteSegmentToExtract segment12 = segment11.addPTWayMember(135);

        extractAndAssertValues(135, segment11, segment12, cloneOfBus601RouteRelation,
            W_136_Den_Boschsingel_2_23837544_A, W_140_Lüdenscheidsingel_1_3993387_E,
            W_135_Den_Boschsingel_3_225605630_E, "Den Boschsingel",
            null,
            "318;358;410;601;651;658"
        );
        for (int n = 134; n >= 131; n--) returnValueNull = segment12.addPTWayMember(n); assertNull(returnValueNull);

//        final int W_134_Den_Boschsingel_4_8131121_D = 8131121;
//        final int W_133_Rennes_Singel_3_3680456_C = 3680456;
//        final int W_132_Rennes_Singel_2_3994257_B = 3994257;
        final int W_131_Rennes_Singel_1_249333185_A = 249333185;

        final int W_99_130_Herestraat_1_249333184_AC = 249333184;
//        final int W_100_129_Herestraat_2_813970231_BB = 813970231;
        final int W_128_Herestraat_11_681081951_A = 681081951;
        RouteSegmentToExtract segment13 = segment12.addPTWayMember(130);

        extractAndAssertValues(130, segment12, segment13, cloneOfBus601RouteRelation,
            W_131_Rennes_Singel_1_249333185_A, W_135_Den_Boschsingel_3_225605630_E,
            W_99_130_Herestraat_1_249333184_AC, "Herestraat",
            null,
            "318;410;601"
        );

        returnValueNull = segment13.addPTWayMember(129);
        assertNull(returnValueNull);
        returnValueNull = segment13.addPTWayMember(128);
        assertNull(returnValueNull);
        final int W_127_Herestraat_10_813970227_C = 813970227;
//        final int W_126_Herestraat_9_41403544_B = 41403544;
        final int W_125_Herestraat_8_8079995_A = 8079995;
        RouteSegmentToExtract segment14 = segment13.addPTWayMember(127);

        extractAndAssertValues(127, segment13, segment14, cloneOfBus601RouteRelation,
            W_128_Herestraat_11_681081951_A, W_99_130_Herestraat_1_249333184_AC,
            W_127_Herestraat_10_813970227_C, "Herestraat",
            null,
            "410;601"
        );
        for (int n = 126; n >= 125; n--) returnValueNull = segment14.addPTWayMember(n); assertNull(returnValueNull);

        final int W_124_Rotonde_Het_Teken_41403538_B = 41403538;
        final int W_123_Ring_Zuid_79340950_A = 79340950;
        RouteSegmentToExtract segment15 = segment14.addPTWayMember(124);

        extractAndAssertValues(124, segment14, segment15, cloneOfBus601RouteRelation,
            W_125_Herestraat_8_8079995_A, W_127_Herestraat_10_813970227_C,
            W_124_Rotonde_Het_Teken_41403538_B, "Rotonde Het Teken",
            null,
            "410;600;601"
        );

        returnValueNull = segment15.addPTWayMember(123); assertNull(returnValueNull);

        final int W_122_Ring_Zuid_11369123_A = 11369123;
        RouteSegmentToExtract segment16 = segment15.addPTWayMember(122);

        extractAndAssertValues(122, segment15, segment16, cloneOfBus601RouteRelation,
            W_123_Ring_Zuid_79340950_A, W_124_Rotonde_Het_Teken_41403538_B,
            W_122_Ring_Zuid_11369123_A, "Ring Zuid",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"
        );

        final int W_121__159949154_A = 159949154;
        final int W_120__332258104_B = 332258104;
        final int W_119_GHB_ingang_78852604_A = 78852604;
        RouteSegmentToExtract segment17 = segment16.addPTWayMember(121);

        extractAndAssertValues(121, segment16, segment17, cloneOfBus601RouteRelation,
            W_122_Ring_Zuid_11369123_A, W_122_Ring_Zuid_11369123_A,
            W_121__159949154_A, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"
        );

        final int W_118_GHB_p5_377918641_B = 377918641;
        final int W_117_GHB_p5_14508736_A = 14508736;
        RouteSegmentToExtract segment18 = segment17.addPTWayMember(120);

        extractAndAssertValues(120, segment17, segment18, cloneOfBus601RouteRelation,
            W_121__159949154_A, W_121__159949154_A,
            W_120__332258104_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"
        );

        returnValueNull = segment18.addPTWayMember(119); assertNull(returnValueNull);

        final int W_116_Ring_Zuid_p45_109267436_A = 109267436;
        RouteSegmentToExtract segment19 = segment18.addPTWayMember(118);

        extractAndAssertValues(118, segment18, segment19, cloneOfBus601RouteRelation,
            W_119_GHB_ingang_78852604_A, W_120__332258104_B,
            W_118_GHB_p5_377918641_B, null,
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"
        );
        returnValueNull = segment19.addPTWayMember(117); assertNull(returnValueNull);

        final int W_115_Ring_Zuid_p3_14508739_A = 14508739;
        RouteSegmentToExtract segment20 = segment19.addPTWayMember(116);

        extractAndAssertValues(116, segment19, segment20, cloneOfBus601RouteRelation,
            W_117_GHB_p5_14508736_A, W_118_GHB_p5_377918641_B,
            W_116_Ring_Zuid_p45_109267436_A, "Ring Zuid",
            null,
            "3;317;395;410;601"
        );
        final int W_114_Ring_Zuid_p2_14508740_A = 14508740;
        RouteSegmentToExtract segment21 = segment20.addPTWayMember(115);

        extractAndAssertValues(115, segment20, segment21, cloneOfBus601RouteRelation,
            W_116_Ring_Zuid_p45_109267436_A, W_116_Ring_Zuid_p45_109267436_A,
            W_115_Ring_Zuid_p3_14508739_A, "Ring Zuid",
            null,
            "3;317;395;410;600;601"
        );

        final int W_113_Ring_Zuid_p1_502328838_D = 502328838;
        final int W_112_Ring_Zuid_502328837_C = 502328837;
        final int W_111_Ring_Zuid_8080023_B = 8080023;
        final int W_110_Rotonde_Het_Teken_78568660_A = 78568660;

        RouteSegmentToExtract segment22 = segment21.addPTWayMember(114);
        extractAndAssertValues(114, segment21, segment22, cloneOfBus601RouteRelation,
            W_115_Ring_Zuid_p3_14508739_A, W_115_Ring_Zuid_p3_14508739_A,
            W_114_Ring_Zuid_p2_14508740_A, "Ring Zuid",
            null,
            "3;317;334;335;395;410;600;601"
        );

        RouteSegmentToExtract segment23 = segment22.addPTWayMember(113);
        extractAndAssertValues(113, segment22, segment23, cloneOfBus601RouteRelation,
            W_114_Ring_Zuid_p2_14508740_A, W_114_Ring_Zuid_p2_14508740_A,
            W_113_Ring_Zuid_p1_502328838_D, "Ring Zuid",
            null,
            "3;317;334;335;380;395;410;600;601"
        );
        for (int n = 112; n >= 110; n--) returnValueNull = segment23.addPTWayMember(n); assertNull(returnValueNull);

        final int W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A = 78873921;

        RouteSegmentToExtract segment24 = segment23.addPTWayMember(109);
        extractAndAssertValues(109, segment23, segment24, cloneOfBus601RouteRelation,
            W_110_Rotonde_Het_Teken_78568660_A, W_113_Ring_Zuid_p1_502328838_D,
            W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601"
        );

        final int W_108_Rotonde_Het_Teken_3752557_A = 3752557;

        RouteSegmentToExtract segment25 = segment24.addPTWayMember(108);
        extractAndAssertValues(108, segment24, segment25, cloneOfBus601RouteRelation,
            W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A, W_109_Rotonde_Het_Teken_Ring_Noord_bus3_78873921_A,
            W_108_Rotonde_Het_Teken_3752557_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601"
        );

        final int W_107_Rotonde_Het_Teken_VWBln_249333188_A = 249333188;

        RouteSegmentToExtract segment26 = segment25.addPTWayMember(107);
        extractAndAssertValues(107, segment25, segment26, cloneOfBus601RouteRelation,
            W_108_Rotonde_Het_Teken_3752557_A, W_108_Rotonde_Het_Teken_3752557_A,
            W_107_Rotonde_Het_Teken_VWBln_249333188_A, "Rotonde Het Teken",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;600;601"
        );
        final int W_106_Rotonde_Het_Teken_249333187_A = 249333187;

        RouteSegmentToExtract segment27 = segment26.addPTWayMember(106);
        extractAndAssertValues(106, segment26, segment27, cloneOfBus601RouteRelation,
            W_107_Rotonde_Het_Teken_VWBln_249333188_A, W_107_Rotonde_Het_Teken_VWBln_249333188_A,
            W_106_Rotonde_Het_Teken_249333187_A, "Rotonde Het Teken",
            null,
            "3;410;600;601"
        );

        final int W_105_Herestraat_7_13067134_D = 13067134;
        final int W_104_Herestraat_6_813970232_C = 813970232;
        final int W_103_Herestraat_5_813970226_B = 813970226;
        final int W_102_Herestraat_4_813970228_A = 813970228;

        RouteSegmentToExtract segment28 = segment27.addPTWayMember(105);
        extractAndAssertValues(105, segment27, segment28, cloneOfBus601RouteRelation,
            W_106_Rotonde_Het_Teken_249333187_A, W_106_Rotonde_Het_Teken_249333187_A,
            W_105_Herestraat_7_13067134_D, "Herestraat",
            null,
            "3;317;333;334;335;370;371;373;374;380;395;410;513;600;601"
        );

        final int W_101_Herestraat_3_813970229_C = 813970229;
        // 99 and 100 are defined as 129 and 130

        for (int n = 104; n >= 102; n--) returnValueNull = segment28.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment29 = segment28.addPTWayMember(101);
        extractAndAssertValues(101, segment28, segment29, cloneOfBus601RouteRelation,
            W_102_Herestraat_4_813970228_A, W_105_Herestraat_7_13067134_D,
            W_101_Herestraat_3_813970229_C, "Herestraat",
            null,
            "410;600;601"
        );

        final int W_98_Rennes_Singel_249333186_H = 249333186;
        final int W_97_Rennes_Singel_192559628_G = 192559628;
        final int W_96_Rennes_Singel_161166589_F = 161166589;
        final int W_95_Rennes_Singel_813979470_E = 813979470;
        final int W_94_Rennes_Singel_79289746_D = 79289746;
        final int W_93_Rennes_Singel_813979472_C = 813979472;
        final int W_92_Rennes_Singel_8131120_B = 8131120;
        final int W_91_Rennes_Singel_429706864_A = 429706864;

        for (int n = 100; n >= 99; n--) returnValueNull = segment29.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment30 = segment29.addPTWayMember(98);
        extractAndAssertValues(98, segment29, segment30, cloneOfBus601RouteRelation,
            W_99_130_Herestraat_1_249333184_AC, W_101_Herestraat_3_813970229_C,
            W_98_Rennes_Singel_249333186_H, "Rennes-Singel",
            null,
            "601"
        );

        final int W_90_Tervuursevest_99583853_K = 99583853;
        final int W_89_Tervuursevest_521193379_J = 521193379;
        final int W_88_Tervuursevest_521193380_I = 521193380;
        final int W_87_Tervuursevest_813979465_H = 813979465;
        final int W_86_Tervuursevest_174338458_G = 174338458;
        final int W_85_Tervuursevest_608715520_F = 608715520;
        final int W_84_Tervuursevest_608715521_E = 608715521;
        final int W_83_Tervuursevest_3677944_D = 3677944;
        final int W_82_Tervuursevest_174338459_C = 174338459;
        final int W_81_Tervuursevest_429706866_B = 429706866;
        final int W_80_Tervuursevest_88361317_A = 88361317;
        for (int n = 97; n >= 91; n--) returnValueNull = segment30.addPTWayMember(n); assertNull(returnValueNull);

        RouteSegmentToExtract segment31 = segment30.addPTWayMember(90);

        extractAndAssertValues(90, segment30, segment31, cloneOfBus601RouteRelation,
            W_91_Rennes_Singel_429706864_A, W_98_Rennes_Singel_249333186_H,
            W_90_Tervuursevest_99583853_K, "Tervuursevest",
            null,
            "318;601"
        );
        final int W_79_Tervuursevest_Kapucijnenvoer_461159345_A = 461159345;
        for (int n = 89; n >= 80; n--) returnValueNull = segment31.addPTWayMember(n); assertNull(returnValueNull);

        RouteSegmentToExtract segment32 = segment31.addPTWayMember(79);

        extractAndAssertValues(79, segment31, segment32, cloneOfBus601RouteRelation,
            W_80_Tervuursevest_88361317_A, W_90_Tervuursevest_99583853_K,
            W_79_Tervuursevest_Kapucijnenvoer_461159345_A, "Tervuursevest",
            null,
            "601"
        );
        final int W_78_Tervuursevest_461159362_K = 461159362;
        final int W_77_Tervuursevest_344507822_J = 344507822;
        final int W_76_Tervuursevest_461159367_I = 461159367;
        final int W_75_Tervuursevest_3677335_H = 3677335;
        final int W_74_Tervuursevest_31474001_G = 31474001;
        final int W_73_Tervuursevest_23237288_F = 23237288;
        final int W_72_Tervuursevest_90168773_E = 90168773;
        final int W_71_Tervuursevest_23237287_D = 23237287;
        final int W_70_Tervuursevest_608715561_C = 608715561;
        final int W_69_Tervuursevest_608715562_B = 608715562;
        final int W_68_Tervuursevest_3677330_A = 3677330;
        RouteSegmentToExtract segment33 = segment32.addPTWayMember(78);

        extractAndAssertValues(78, segment32, segment33, cloneOfBus601RouteRelation,
            W_79_Tervuursevest_Kapucijnenvoer_461159345_A, W_79_Tervuursevest_Kapucijnenvoer_461159345_A,
            W_78_Tervuursevest_461159362_K, "Tervuursevest",
            null,
            "178;179;306 (student);601"
        );

        final int W_67_Naamsevest_86164005_G = 86164005;
        final int W_66_Naamsevest_655251293_F = 655251293;
        final int W_65_Naamsevest_131571763_E = 131571763;
        final int W_64_Naamsevest_661733369_D = 661733369;
        final int W_63_Naamsevest_655251292_C = 655251292;
        final int W_62_Naamsevest_3677823_B = 3677823;
        final int W_61_Geldenaaksevest_24905257_A = 24905257;

        final int W_60_Geldenaaksevest_608715605_H = 608715605;
        final int W_59_Geldenaaksevest_608715606_G = 608715606;
        final int W_58_Geldenaaksevest_79299303_F = 79299303;
        final int W_57_Geldenaaksevest_10296368_E = 10296368;
        final int W_56_Geldenaaksevest_521193607_D = 521193607;
        final int W_55_Geldenaaksevest_94585453_C = 94585453;
        final int W_54_Geldenaaksevest_586268893_B = 586268893;
        final int W_53_Geldenaaksevest_8130906_A = 8130906;

        for (int n = 77; n >= 68; n--) returnValueNull = segment33.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment34 = segment33.addPTWayMember(67);
        extractAndAssertValues(67, segment33, segment34, cloneOfBus601RouteRelation,
            W_68_Tervuursevest_3677330_A, W_78_Tervuursevest_461159362_K,
            W_67_Naamsevest_86164005_G, "Naamsevest",
            null,
            "178;179;306 (student);520;524;525;537;601"
        );

        final int W_52_Tiensepoort_16775171_D = 16775171;
        final int W_51_Tiensevest_8131717_C = 8131717;
        final int W_50_Tiensevest_12712557_B = 12712557;
        final int W_49_Tiensevest_Oostertunnel_8590231_A = 8590231;
        for (int n = 66; n >= 62; n--) returnValueNull = segment34.addPTWayMember(n); assertNull(returnValueNull);

        RouteSegmentToExtract segment35 = segment34.addPTWayMember(61);
        extractAndAssertValues(61, segment34, segment35, cloneOfBus601RouteRelation,
            W_62_Naamsevest_3677823_B, W_67_Naamsevest_86164005_G,
            W_61_Geldenaaksevest_24905257_A, "Geldenaaksevest",
            null,
            "18;178;179;306 (student);337;601;616"
        );
        for (int n = 60; n >= 53; n--) returnValueNull = segment35.addPTWayMember(n); assertNull(returnValueNull);

        RouteSegmentToExtract segment36 = segment35.addPTWayMember(52);

        extractAndAssertValues(52, segment35, segment36, cloneOfBus601RouteRelation,
            W_53_Geldenaaksevest_8130906_A, W_61_Geldenaaksevest_24905257_A,
            W_52_Tiensepoort_16775171_D, "Tiensepoort",
            null,
            "18;178;179;306 (student);337;601;616;630"
        );
        final int W_48_Tiensevest_185988814_A = 185988814;
        for (int n = 51; n >= 49; n--) returnValueNull = segment36.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment37 = segment36.addPTWayMember(48);

        extractAndAssertValues(48, segment36, segment37, cloneOfBus601RouteRelation,
            W_49_Tiensevest_Oostertunnel_8590231_A, W_52_Tiensepoort_16775171_D,
            W_48_Tiensevest_185988814_A, "Tiensevest",
            null,
            "7;8;9;18;178;179;306 (student);337;380;527;601;616;630"
        );

        final int W_47_Martelarenplein_76856823_D = 76856823;
        final int W_46_Martelarenplein_459446598_C = 459446598;
        final int W_45_Martelarenplein_459446600_B = 459446600;
        final int W_44__78815533_A = 78815533;

        RouteSegmentToExtract segment38 = segment37.addPTWayMember(47);
        extractAndAssertValues(47, segment37, segment38, cloneOfBus601RouteRelation,
            W_48_Tiensevest_185988814_A, W_48_Tiensevest_185988814_A,
            W_47_Martelarenplein_76856823_D, "Martelarenplein",
            null,
            "1;4;5;6;7;8;9;18;178;179;306 (student);337;380;527;600;601;616;630"
        );

        final int W_43__79264899_A = 79264899;

        for (int n = 46; n >= 44; n--) returnValueNull = segment38.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment39 = segment38.addPTWayMember(43);
        extractAndAssertValues(43, segment38, segment39, cloneOfBus601RouteRelation,
            W_44__78815533_A, W_47_Martelarenplein_76856823_D,
            W_43__79264899_A, null,
            null,
            "7;8;9;18;178;179;306 (student);337;380;527;601;630"
        );
        final int W_42__377918635_A = 377918635;

        RouteSegmentToExtract segment40 = segment39.addPTWayMember(42);
        extractAndAssertValues(42, segment39, segment40, cloneOfBus601RouteRelation,
            W_43__79264899_A, W_43__79264899_A,
            W_42__377918635_A, null,
            null,
            "18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601;630"
        );
        final int W_41__79264888_A = 79264888;

        RouteSegmentToExtract segment41 = segment40.addPTWayMember(41);
        extractAndAssertValues(41, segment40, segment41, cloneOfBus601RouteRelation,
            W_42__377918635_A, W_42__377918635_A,
            W_41__79264888_A, null,
            null,
            "3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;601"
        );
        final int W_40__79264897_A = 79264897;

        RouteSegmentToExtract segment42 = segment41.addPTWayMember(40);
        extractAndAssertValues(40, segment41, segment42, cloneOfBus601RouteRelation,
            W_41__79264888_A, W_41__79264888_A,
            W_40__79264897_A, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;525;601"
        );
        final int W_39__71754927_A = 71754927;

        RouteSegmentToExtract segment43 = segment42.addPTWayMember(39);
        extractAndAssertValues(39, segment42, segment43, cloneOfBus601RouteRelation,
            W_40__79264897_A, W_40__79264897_A,
            W_39__71754927_A, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;512;520;524;525;601"
        );
        final int W_38__377918638_A_TEC18 = 377918638;

        RouteSegmentToExtract segment44 = segment43.addPTWayMember(38);
        extractAndAssertValues(38, segment43, segment44, cloneOfBus601RouteRelation,
            W_39__71754927_A, W_39__71754927_A,
            W_38__377918638_A_TEC18, null,
            null,
            "2;3;4;5;6;7;8;9;18;178;179;306 (student);333;334;335;337;370;371;373;374;380;475;485;512;513;520;524;525;601"
        );
        final int W_37__79264891_A = 79264891;

        RouteSegmentToExtract segment45 = segment44.addPTWayMember(37);
        extractAndAssertValues(37, segment44, segment45, cloneOfBus601RouteRelation,
            W_38__377918638_A_TEC18, W_38__377918638_A_TEC18,
            W_37__79264891_A, null,
            null,
            "601" // TODO, here line 18 has its first way in common with 601
        );
        final int W_36_Tiensevest_78568409_A = 78568409;

        RouteSegmentToExtract segment46 = segment45.addPTWayMember(36);
        extractAndAssertValues(36, segment45, segment46, cloneOfBus601RouteRelation,
            W_37__79264891_A, W_37__79264891_A,
            W_36_Tiensevest_78568409_A, "Tiensevest",
            null,
            "18;601"
        );
        final int W_35_Tiensevest_79193579_A = 79193579;

        RouteSegmentToExtract segment47 = segment46.addPTWayMember(35);
        extractAndAssertValues(35, segment46, segment47, cloneOfBus601RouteRelation,
            W_36_Tiensevest_78568409_A, W_36_Tiensevest_78568409_A,
            W_35_Tiensevest_79193579_A, "Tiensevest",
            null,
            "4;5;6;7;8;9;18;179;284;285;306 (student);315;316;317;334;335;337;380;601;616;658"
        );
        final int W_34_Bend_19793394_A = 19793394;
        // ways 28 to 33 are the same as 146 - 151
        final int W_27_Zoutstraat_3992548_A = 3992548;

        RouteSegmentToExtract segment48 = segment47.addPTWayMember(34);

        extractAndAssertValues(34, segment47, segment48, cloneOfBus601RouteRelation,
            W_35_Tiensevest_79193579_A, W_35_Tiensevest_79193579_A,
            W_34_Bend_19793394_A, null,
            null,
            "3;4;5;6;7;8;9;18;179;306 (student);333;334;335;337;380;513;600;601;616;630;658"
        );
        final int W_26_Havenkant_510790349_M = 510790349;
        final int W_25_Havenkant_510790348_L = 510790348;
        final int W_24_Havenkant_314635787_K = 314635787;
        final int W_23_Havenkant_843534478_J = 843534478;
        final int W_22_Havenkant_406205781_I = 406205781;
        final int W_21_Havenkant_270181176_H = 270181176;
        final int W_20_Havenkant_330300725_G = 330300725;
        final int W_19_Burchtstraat_3869822_F = 3869822;
        final int W_18_Achter_de_latten_330300723_E = 330300723;
        final int W_17_Wolvengang_659297690_D = 659297690;
        final int W_16_Wolvengang_25928482_C = 25928482;
        final int W_15_Engels_Plein_3869812_B = 3869812;
        final int W_14_Engels_Plein_338057820_A = 338057820;

        RouteSegmentToExtract segment49 = segment48.addPTWayMember(33);
        extractAndAssertValues(33, segment48, segment49, cloneOfBus601RouteRelation,
            W_34_Bend_19793394_A, W_34_Bend_19793394_A,
            W_33_151_Tiensevest_1_19793164_A, "Tiensevest",
            null,
            "334;335;513;601"
        );

        RouteSegmentToExtract segment50 = segment49.addPTWayMember(32);
        Relation rel50 = extractAndAssertValues(32, segment49, segment50, cloneOfBus601RouteRelation,
            W_33_151_Tiensevest_1_19793164_A, W_33_151_Tiensevest_1_19793164_A,
            W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D, "Diestsevest",
            null,
            "284;285;305;306;310;315;316;317;318;334;335;358;395;410;433;475;485;513;601;630;651;652;658"
        );
        assertEquals(rel8.getId(), rel50.getId());

        for (int n = 31; n >= 29; n--) returnValueNull = segment50.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment51 = segment50.addPTWayMember(28);
        Relation rel51 = extractAndAssertValues(28, segment50, segment51, cloneOfBus601RouteRelation,
            W_29_147_Diestsevest_1_8133608_A, W_32_150_Diestsevest_4_NextToOostertunnel_6184898_D,
            W_28_146_Artoisplein_2_254800931_A, "Joanna-Maria Artoisplein",
            null,
            "305;318;334;335;358;410;513;601;630;651;652;658"
        );
        assertEquals(rel9.getId(), rel51.getId());

        RouteSegmentToExtract segment52 = segment51.addPTWayMember(27);
        Relation rel52 = extractAndAssertValues(27, segment51, segment52, cloneOfBus601RouteRelation,
            W_28_146_Artoisplein_2_254800931_A, W_28_146_Artoisplein_2_254800931_A,
            W_27_Zoutstraat_3992548_A, "Zoutstraat",
            null,
            "178;305;318;334;335;358;410;513;601;630;651;652;658"
        );
        assertEquals(rel10.getId(), rel52.getId());

        RouteSegmentToExtract segment53 = segment52.addPTWayMember(26);
        extractAndAssertValues(26, segment52, segment53, cloneOfBus601RouteRelation,
            W_27_Zoutstraat_3992548_A, W_27_Zoutstraat_3992548_A,
            W_26_Havenkant_510790349_M, "Havenkant",
            null,
            "334;335;513;601;630"
        );

        for (int n = 25; n >= 14; n--) returnValueNull = segment53.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment54 = segment53.addPTWayMember(13);
        extractAndAssertValues(13, segment53, segment54, cloneOfBus601RouteRelation,
            W_14_Engels_Plein_338057820_A, W_26_Havenkant_510790349_M,
            0, null,
            null,
            "601"
        );


        // ***********************************************************
        // ***********************************************************
        // ***********************************************************


        Relation bus600RouteRelation = allRelations.stream()
            .filter(relation -> relation.getId() == 955908)
            .findFirst().orElse(null);

        assertNotNull(bus600RouteRelation);
        Relation cloneOfBus600RouteRelation = new Relation(bus600RouteRelation);
        assertNotNull(cloneOfBus600RouteRelation);
        RouteSegmentToExtract segment101 = new RouteSegmentToExtract(cloneOfBus600RouteRelation, ds);
        assertEquals(cloneOfBus600RouteRelation.get("ref"), segment101.getLineIdentifiersSignature());
        assertEquals(cloneOfBus600RouteRelation.get("colour"), segment101.getColoursSignature());

        assertNull(segment101.extractToRelation(Collections.emptyList(), false));

        assertEquals("", segment101.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment101.getWayMembers());

        final int W_169_Engels_Plein_608715622_O = 608715622;
        final int W_168_Engels_Plein_338057819_N = 338057819;
        final int W_167_Engels_Plein_305316104_M = 305316104;
        final int W_166_Engels_Plein_3869812_L = 3869812;
        final int W_165_Wolvengang_25928482_K = 25928482;
        final int W_164_Wolvengang_659297690_J = 659297690;
        final int W_163_Achter_de_latten_330300723_I = 330300723;
        final int W_162_Burchtstraat_3869822_H = 3869822;
        final int W_161_Havenkant_330300725_G = 330300725;
        final int W_160_Havenkant_270181176_F = 270181176;
        final int W_159_Havenkant_406205781_E = 406205781;
        final int W_158_Havenkant_843534478_D = 843534478;
        final int W_157_Havenkant_314635787_C = 314635787;
        final int W_156_Havenkant_510790348_B = 510790348;
        final int W_155_Havenkant_29283599_A = 29283599;

        final int W_154_Havenkant_304241968_B = 304241968;
        final int W_153_Aarschotsesteenweg_304241967_A = 304241967;

        for (int n = 169; n >= 155; n--) returnValueNull = segment101.addPTWayMember(n); assertNull(returnValueNull);
        RouteSegmentToExtract segment102 = segment101.addPTWayMember(154);
        extractAndAssertValues(154, segment101, segment102, cloneOfBus600RouteRelation,
            W_155_Havenkant_29283599_A, W_169_Engels_Plein_608715622_O,
            W_154_Havenkant_304241968_B, "Havenkant",
            null,
            "600"
        );

        final int W_152_Redersstraat_340265961_B = 340265961;
        final int W_151_Redersstraat_318825613_A = 318825613;

        returnValueNull = segment102.addPTWayMember(153); assertNull(returnValueNull);
        RouteSegmentToExtract segment103 = segment102.addPTWayMember(152);
        extractAndAssertValues(152, segment102, segment103, cloneOfBus600RouteRelation,
            W_153_Aarschotsesteenweg_304241967_A, W_154_Havenkant_304241968_B,
            W_152_Redersstraat_340265961_B, "Redersstraat",
            null,
            "334;335;513;600;630"
        );

        final int W_150_Redersstraat_340265962_A = 340265962;

        returnValueNull = segment103.addPTWayMember(151); assertNull(returnValueNull);
        RouteSegmentToExtract segment104 = segment103.addPTWayMember(150);
        extractAndAssertValues(150, segment103, segment104, cloneOfBus600RouteRelation,
            W_151_Redersstraat_318825613_A, W_152_Redersstraat_340265961_B,
            W_150_Redersstraat_340265962_A, "Redersstraat",
            null,
            "600"
        );

    }

    public Relation extractAndAssertValues(int index, RouteSegmentToExtract createdSegment, RouteSegmentToExtract newSegment,
                                       Relation superRouteRelation, int firstWayId, int lastWayId, int firstWayIdForNewSegment,
                                       String nameOfNewWay,
                                       String expectedColours, String expectedRouteRef) {
        Relation extractedRelation = createdSegment.extractToRelation(Arrays.asList("type", "route"), true);
        System.out.println(index + " " + extractedRelation.get("note"));
        assertEquals("first way not correct", firstWayId, extractedRelation.firstMember().getWay().getId());
        assertEquals("last way not correct", lastWayId, extractedRelation.lastMember().getWay().getId());
        if (expectedColours != null) assertEquals(expectedColours, createdSegment.getColoursSignature());
        assertEquals(expectedRouteRef, createdSegment.getLineIdentifiersSignature());

        assertEquals("relation id not correct", extractedRelation.getId(), superRouteRelation.getMember(index+1).getMember().getId());
        // newSegment should have the last way we tried to add to this segment
        if (firstWayIdForNewSegment != 0) {
            assertEquals("name of last added way not correct", nameOfNewWay, newSegment.getWayMembers().get(0).getWay().get("name"));
            assertEquals("id of first way not correct", firstWayIdForNewSegment, newSegment.getWayMembers().get(0).getWay().getId());
        }
        return extractedRelation;
    }
}
