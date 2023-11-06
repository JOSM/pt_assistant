// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTProperties;

/**
 * Unit tests of {@link SplitRoundaboutAction}.
 */
class SplitRoundaboutTest {
    private DataSet ds, ds1, ds2;
    private OsmDataLayer layer;
    private SplitRoundaboutAction action;
    private Way r1, r2, r3, r4, r5;

    @BeforeEach
    public void beforeEach() {
        PTProperties.ROUNDABOUT_SPLITTER_ALIGN_ALWAYS.put(true);
    }

    public void init() throws IllegalDataException {
        ds = OsmReader.parseDataSet(TestFiles.ROUNDABOUT(), null);
        layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);

        action = new SplitRoundaboutAction();
        r1 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(293302077L, OsmPrimitiveType.WAY));
        r2 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(205833435L, OsmPrimitiveType.WAY));
        r3 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(25739002L, OsmPrimitiveType.WAY));
        r4 = (Way) ds.getPrimitives(p -> p.hasTag("name", "r4")).iterator().next();
    }

    public void init1() throws IllegalDataException {
        ds1 = OsmReader.parseDataSet(TestFiles.ROUNDABOUT1_BEFORE(), null);
        layer = new OsmDataLayer(ds1, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);

        action = new SplitRoundaboutAction();
        r5 = (Way) ds1.getPrimitiveById(new SimplePrimitiveId(97417157, OsmPrimitiveType.WAY));
    }

    private Collection<Way> splitWay(Way w) {
        Map<Relation, List<Integer>> savedPositions = action.getSavedPositions(w);
        action.getRemoveRoundaboutFromRelationsCommand(w).executeCommand();
        List<Node> splitNodes = action.getSplitNodes(w);
        SplitWayCommand result = SplitWayCommand.split(w, splitNodes, Collections.emptyList());
        result.executeCommand();
        Collection<Way> splitWays = result.getNewWays();
        splitWays.add(result.getOriginalWay());
        action.getUpdateRelationsCommand(savedPositions, splitNodes, splitWays).executeCommand();
        return splitWays;
    }

    @Test
    void testSplitWay1() throws IllegalDataException {
    		init();
        Collection<Way> sw1 = splitWay(r1);
        assertEquals(4, sw1.size());
        sw1.forEach(w -> {
            if (w.firstNode().getUniqueId() == 267843779L && w.lastNode().getUniqueId() == 2968718407L)
                assertEquals(5, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 2968718407L && w.lastNode().getUniqueId() == 2383688231L)
                assertEquals(0, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 2383688231L && w.lastNode().getUniqueId() == 267843741L)
                assertEquals(5, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 267843741L && w.lastNode().getUniqueId() == 267843779L)
                assertEquals(0, w.getReferrers().size());
            else
                fail();
        });
    }

    @Test
    void testSplitWay2() throws IllegalDataException {
    		init();
        Collection<Way> sw2 = splitWay(r2);
        assertEquals(4, sw2.size());
        sw2.forEach(w -> {
            if (w.firstNode().getUniqueId() == 2158181809L && w.lastNode().getUniqueId() == 2158181798L)
                assertEquals(8, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 2158181798L && w.lastNode().getUniqueId() == 2158181789L)
                assertEquals(0, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 2158181789L && w.lastNode().getUniqueId() == 2158181803L)
                assertEquals(8, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 2158181803L && w.lastNode().getUniqueId() == 2158181809L)
                assertEquals(0, w.getReferrers().size());
            else
                fail();
        });
    }

    @Test
    void testSplitWay3() throws IllegalDataException {
    		init();
        Collection<Way> sw3 = splitWay(r3);
        assertEquals(4, sw3.size());
        sw3.forEach(w -> {
            if (w.firstNode().getUniqueId() == 280697532L && w.lastNode().getUniqueId() == 280697452L)
                assertEquals(0, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 280697452L && w.lastNode().getUniqueId() == 280697591L)
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 280697591L && w.lastNode().getUniqueId() == 280697534L)
                assertEquals(0, w.getReferrers().size());
            else if (w.firstNode().getUniqueId() == 280697534L && w.lastNode().getUniqueId() == 280697532L)
                assertEquals(1, w.getReferrers().size());
            else
                fail();
        });
    }

    @Test
    void testSplitWay4() throws IllegalDataException {
    		init();
        Collection<Way> sw4 = splitWay(r4);
        assertEquals(10, sw4.size());
        Node entry11 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry1-1")).iterator().next();
        Node exit11 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit1-1")).iterator().next();
        Node entry12 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry1-2")).iterator().next();
        Node exit12 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit1-2")).iterator().next();
        Node entry21 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry2-1")).iterator().next();
        Node exit21 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit2-1")).iterator().next();
        Node entry22 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry2-2")).iterator().next();
        Node exit22 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit2-2")).iterator().next();
        Node entry3 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nentry3")).iterator().next();
        Node exit3 = (Node) ds.getPrimitives(p -> p.hasTag("name", "nexit3")).iterator().next();

        sw4.forEach(w -> {
            if (w.firstNode().equals(entry11) && w.lastNode().equals(exit22))
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().equals(exit22) && w.lastNode().equals(entry21))
                assertEquals(1, w.getReferrers().size());
            else if (w.firstNode().equals(entry21) && w.lastNode().equals(exit11))
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().equals(exit11) && w.lastNode().equals(entry12))
                assertEquals(1, w.getReferrers().size());
            else if (w.firstNode().equals(entry12) && w.lastNode().equals(entry3))
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().equals(entry3) && w.lastNode().equals(exit21))
                assertEquals(3, w.getReferrers().size());
            else if (w.firstNode().equals(exit21) && w.lastNode().equals(entry22))
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().equals(entry22) && w.lastNode().equals(exit3))
                assertEquals(3, w.getReferrers().size());
            else if (w.firstNode().equals(exit3) && w.lastNode().equals(exit12))
                assertEquals(2, w.getReferrers().size());
            else if (w.firstNode().equals(exit12) && w.lastNode().equals(entry11))
                assertEquals(1, w.getReferrers().size());
            else
                fail();
        });
    }

    @Test
    void testRoundabout1After() throws IllegalDataException {
    		init1();
    		Collection<Way> sw5 = splitWay(r5);
        ds2 = OsmReader.parseDataSet(TestFiles.ROUNDABOUT1_AFTER(), null);
    		for (Relation d1 : ds1.getRelations()) {
    			for (Relation d2 : ds2.getRelations()) {
    				if (d2.getId() == d1.getId()) {
    					assertEquals(d1.getMembers().size(), d2.getMembers().size());
    				}
    			}
    		}
    }
}
