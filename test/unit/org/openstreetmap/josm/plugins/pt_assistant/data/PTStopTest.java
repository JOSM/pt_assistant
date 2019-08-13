// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
* Unit tests of {@link StopToWayAssigner}.
*/
public class PTStopTest extends AbstractTest {

    private DataSet ds;

    @Before
    public void init() throws FileNotFoundException, IllegalDataException {
        ds = OsmReader.parseDataSet(new FileInputStream(AbstractTest.PATH_TO_SORT_PT_STOPS), null);
    }

    List<PTStop> findAllStops() {
        Collection<Node> allNodes = ds.getNodes();
        List<PTStop> allStops = new ArrayList<>();
        List<Node> potentialStops = new ArrayList<>();
        for (Node currentNode : allNodes) {
            if (StopUtils.isHighwayOrRailwayStopPosition(currentNode) || StopUtils.isStopPosition(currentNode)
                    || StopUtils.verifyStopAreaPlatform(currentNode)
                    || StopUtils.verifyIfMemberOfStopArea(currentNode)) {
                potentialStops.add(currentNode);
            }
        }
        for (Node node : potentialStops) {
            if (CheckItIsPTStopOrNot(node) != null) {
                PTStop pts = CheckItIsPTStopOrNot(node);
                allStops.add(pts);
            }
        }
        return allStops;
    }

    public PTStop CheckItIsPTStopOrNot(Node stop) {
        List<OsmPrimitive> referrers = stop.getReferrers();
        for (OsmPrimitive referredPrimitive : referrers) {
            if (referredPrimitive.getType().equals(OsmPrimitiveType.RELATION)) {
                Relation referredRelation = (Relation) referredPrimitive;
                if (checkRelationContainsStop(referredRelation, stop) != null) {
                    PTStop pts = new PTStop(checkRelationContainsStop(referredRelation, stop));
                    return pts;
                }
            }
        }
        return null;
    }

    RelationMember checkRelationContainsStop(Relation rel, Node node) {
        for (RelationMember rm : rel.getMembers()) {
            if (rm.getUniqueId() == node.getUniqueId()) {
                return rm;
            }
        }
        return null;
    }

    @Test
    public void test() {
        List<PTStop> pts = findAllStops();
        List<Way> associatedWay = new ArrayList<>();
        System.out.println(pts.size());
        for (PTStop pt : pts) {
            Way w = pt.findServingWays(pt);
            if (pt.getUniqueId() == 4155886899L) {
                assertEquals(504377140L, w.getUniqueId());
            } else if (pt.getUniqueId() == 4155886904L) {
                assertEquals(441957165L, w.getUniqueId());
            }
            if (w.getUniqueId() > 0) {
                associatedWay.add(w);
                System.out.println(pt.getUniqueId() + " serves " + w.getUniqueId());
            }
        }
    }
}
