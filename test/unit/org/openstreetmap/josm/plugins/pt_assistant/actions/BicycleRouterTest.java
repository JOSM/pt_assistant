//// License: GPL. For details, see LICENSE file.
//package org.openstreetmap.josm.plugins.pt_assistant.actions;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.Test;
//import org.openstreetmap.josm.data.osm.DataSet;
//import org.openstreetmap.josm.data.osm.Node;
//import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
//import org.openstreetmap.josm.data.osm.Relation;
//import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
//import org.openstreetmap.josm.data.osm.Way;
//import org.openstreetmap.josm.gui.MainApplication;
//import org.openstreetmap.josm.gui.layer.OsmDataLayer;
//import org.openstreetmap.josm.io.IllegalDataException;
//import org.openstreetmap.josm.io.OsmReader;
//import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;
//import org.openstreetmap.josm.plugins.pt_assistant.utils.PTProperties;
//
///**
// * Unit tests of {@link SplitRoundaboutAction}.
// */
//public class BicycleRouterTest extends AbstractTest {
//
//
//    private DataSet ds, ds1, ds2;
//    private OsmDataLayer layer;
//    private Bicycle action;
//    private Way r1, r2, r3, r4, r5;
//    private Node node1,node2,node3;
//
//    public void init() throws FileNotFoundException, IllegalDataException {
//        ds = OsmReader.parseDataSet(new FileInputStream(AbstractTest.PATH_TO_BICYCLE_LOOP_BACK_TEST), null);
//        layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
//        MainApplication.getLayerManager().addLayer(layer);
//
//        PTProperties.ROUNDABOUT_SPLITTER_ALIGN_ALWAYS.put(true);
//        action = new SplitRoundaboutAction();
//        r1 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(298212681L, OsmPrimitiveType.WAY));
//        r2 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(221395600L, OsmPrimitiveType.WAY));
//        r3 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(221267344L, OsmPrimitiveType.WAY));
//        r4 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(427476309L, OsmPrimitiveType.WAY));
//        r5 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(427476318L, OsmPrimitiveType.WAY));
//
//        node1 = (Node) ds.getPrimitiveById(new SimplePrimitiveId(3021148495L, OsmPrimitiveType.NODE));
//        node2 = (Node) ds.getPrimitiveById(new SimplePrimitiveId(1792367842L, OsmPrimitiveType.NODE));
//
//    }
//
//    private List<Way> findingNextWays(Way w,Node node){
//      List<Way> ways =action.findNextWay(w,node);
//      return ways;
//    }
//
//    @Test
//    public void test1() throws FileNotFoundException, IllegalDataException {
//    		init();
//        List<Way> ways = findingNextWays(r1,node1);
//        assertEquals(2, ways.size());
//        assertEquals(r4,ways.get(0).getUniqueId());
//        assertEquals(r5,ways.get(1).getUniqueId());
//        assertEquals(node2,ways.get(1).lastNode.getUniqueId());
//    }
//    // @Test
//    // public void test4() throws FileNotFoundException, IllegalDataException {
//    // 		init();
//    //     Collection<Way> sw4 = splitWay(r4);
//    //     assertEquals(10, sw4.size());
//    //     Node entry11 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry1-1")).iterator().next();
//    //     Node exit11 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit1-1")).iterator().next();
//    //     Node entry12 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry1-2")).iterator().next();
//    //     Node exit12 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit1-2")).iterator().next();
//    //     Node entry21 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry2-1")).iterator().next();
//    //     Node exit21 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit2-1")).iterator().next();
//    //     Node entry22 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry2-2")).iterator().next();
//    //     Node exit22 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit2-2")).iterator().next();
//    //     Node entry3 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry3")).iterator().next();
//    //     Node exit3 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit3")).iterator().next();
//    //
//    //     sw4.forEach(w -> {
//    //         if (w.firstNode().equals(entry11) && w.lastNode().equals(exit22))
//    //             assertEquals(2, w.getReferrers().size());
//    //         else if (w.firstNode().equals(exit22) && w.lastNode().equals(entry21))
//    //             assertEquals(1, w.getReferrers().size());
//    //         else if (w.firstNode().equals(entry21) && w.lastNode().equals(exit11))
//    //             assertEquals(2, w.getReferrers().size());
//    //         else if (w.firstNode().equals(exit11) && w.lastNode().equals(entry12))
//    //             assertEquals(1, w.getReferrers().size());
//    //         else if (w.firstNode().equals(entry12) && w.lastNode().equals(entry3))
//    //             assertEquals(2, w.getReferrers().size());
//    //         else if (w.firstNode().equals(entry3) && w.lastNode().equals(exit21))
//    //             assertEquals(3, w.getReferrers().size());
//    //         else if (w.firstNode().equals(exit21) && w.lastNode().equals(entry22))
//    //             assertEquals(2, w.getReferrers().size());
//    //         else if (w.firstNode().equals(entry22) && w.lastNode().equals(exit3))
//    //             assertEquals(3, w.getReferrers().size());
//    //         else if (w.firstNode().equals(exit3) && w.lastNode().equals(exit12))
//    //             assertEquals(2, w.getReferrers().size());
//    //         else if (w.firstNode().equals(exit12) && w.lastNode().equals(entry11))
//    //             assertEquals(1, w.getReferrers().size());
//    //         else
//    //             fail();
//    //     });
//    // }
//    //
//    // @Test
//    // public void test5() throws FileNotFoundException, IllegalDataException {
//    // 		init1();
//    // 		Collection<Way> sw5 = splitWay(r5);
//    // 		ds2 = OsmReader.parseDataSet(new FileInputStream(AbstractTest.PATH_TO_ROUNDABOUT1_AFTER), null);
//    // 		for (Relation d1 : ds1.getRelations()) {
//    // 			for (Relation d2 : ds2.getRelations()) {
//    // 				if (d2.getId() == d1.getId()) {
//    // 					assertEquals(d1.getMembers().size(), d2.getMembers().size());
//    // 				}
//    // 			}
//    // 		}
//    // }
//}
