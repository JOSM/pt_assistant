// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTProperties;

public class BicycleMendRelationTest extends AbstractTest {

    private DataSet ds, ds1, ds2;
    private OsmDataLayer layer;
    private Way r1, r2, r3, r4, r5, r6;
    private Node node1, node2, node3;
    private BicycleMendRelation action;
    WayConnectionType calc;
    Relation relat;
    //    private static final IRelationEditorActionAccess editorAccess =null;

    public void init() throws FileNotFoundException, IllegalDataException {
        ds = OsmReader.parseDataSet(new FileInputStream(AbstractTest.PATH_TO_BICYCLE_LOOP_BACK_TEST), null);
        layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        PTProperties.ROUNDABOUT_SPLITTER_ALIGN_ALWAYS.put(true);
        r1 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(298212681L, OsmPrimitiveType.WAY));
        r2 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(221395600L, OsmPrimitiveType.WAY));
        r3 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(221267344L, OsmPrimitiveType.WAY));
        r4 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(427476309L, OsmPrimitiveType.WAY));
        r5 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(427476318L, OsmPrimitiveType.WAY));
        r6 = (Way) ds.getPrimitiveById(new SimplePrimitiveId(221267300L, OsmPrimitiveType.WAY));
        node1 = (Node) ds.getPrimitiveById(new SimplePrimitiveId(3021148495L, OsmPrimitiveType.NODE));
        node2 = (Node) ds.getPrimitiveById(new SimplePrimitiveId(1792367842L, OsmPrimitiveType.NODE));
        Collection<Relation> relist = ds.getRelations();
        relat = (Relation) ds.getPrimitiveById(new SimplePrimitiveId(9131355L, OsmPrimitiveType.RELATION));
    }

    private List<Way> findingNextWays(Way w, Node node) {
        action.nextWay = r6;
        action.IsWaythere.put(r1, 1);
        action.link = new WayConnectionType();
        action.prelink = new WayConnectionType();
        action.link.isOnewayLoopBackwardPart = false;
        action.prelink.isOnewayLoopBackwardPart = false;
        action.relation = relat;
        List<Way> ways = action.findNextWay(w, node);
        return ways;
    }

    @Test
    public void test1() throws FileNotFoundException, IllegalDataException {
        init();
        List<Way> ways = findingNextWays(r1, node1);
        assertEquals(2, ways.size());
        System.out.println(ways.get(0).getUniqueId());
        System.out.println(ways.get(1).getUniqueId());

        assertEquals(r4.getUniqueId(), ways.get(0).getUniqueId());
        assertEquals(r5.getUniqueId(), ways.get(1).getUniqueId());
        assertEquals(node2.getUniqueId(), ways.get(1).lastNode().getUniqueId());
    }
}
