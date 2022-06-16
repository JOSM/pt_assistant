package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.TestUtil;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class WayUtilsTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    private final Map<Long, Node> NODES = new HashMap<>();


    @Test
    public void testIsTouchingOtherWay() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Way WAY1 = createWay(1, 1, 2, 3, 4);
        final Way WAY2 = createWay(2, 4, 5, 6, 1);
        final Way WAY3 = createWay(3, 1, 7, 8, 9);
        final Way WAY4 = createWay(4, 4, 10, 11);
        final Way WAY5 = createWay(5, 12, 13, 14, 15, 4);
        final Way WAY6 = createWay(6, 16, 2, 3, 17);
        final Way WAY7 = createWay(7);
        final Way WAY8 = createWay(8, 1);
        final Way WAY9 = createWay(9, 18);

        // if one parameter is null → false
        assertFalse(WayUtils.isTouchingOtherWay(null, null));
        assertFalse(WayUtils.isTouchingOtherWay(null, WAY1));
        assertFalse(WayUtils.isTouchingOtherWay(WAY1, null));

        // both endnodes match → true
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY1));
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY2));
        assertTrue(WayUtils.isTouchingOtherWay(WAY2, WAY1));
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY3));
        assertTrue(WayUtils.isTouchingOtherWay(WAY3, WAY1));
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY4));
        assertTrue(WayUtils.isTouchingOtherWay(WAY4, WAY1));
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY5));
        assertTrue(WayUtils.isTouchingOtherWay(WAY5, WAY1));

        // only middle nodes match → false
        assertFalse(WayUtils.isTouchingOtherWay(WAY1, WAY6));
        assertFalse(WayUtils.isTouchingOtherWay(WAY6, WAY1));

        // empty way → false
        assertFalse(WayUtils.isTouchingOtherWay(WAY1, WAY7));
        assertFalse(WayUtils.isTouchingOtherWay(WAY7, WAY1));
        assertFalse(WayUtils.isTouchingOtherWay(WAY7, WAY7));

        // one-node-way → true, if end node
        assertTrue(WayUtils.isTouchingOtherWay(WAY1, WAY8));
        assertTrue(WayUtils.isTouchingOtherWay(WAY8, WAY1));
        assertFalse(WayUtils.isTouchingOtherWay(WAY1, WAY9));
        assertFalse(WayUtils.isTouchingOtherWay(WAY9, WAY1));

    }

    private Way createWay(final long wayId, final long... nodeIds) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Way w = new Way(wayId);
        w.setNodes(LongStream.of(nodeIds).mapToObj(nodeId -> {
            final Node nodeFromCache = NODES.get(nodeId);
            if (nodeFromCache != null) {
                return nodeFromCache;
            } else {
                final Node newNode = new Node(nodeId);
                newNode.setChangesetId((int) nodeId);
                newNode.setUser(User.createOsmUser(nodeId, "User" + nodeId));
                newNode.setInstant(Instant.now());
                newNode.setCoor(new LatLon(nodeId, nodeId));
                NODES.put(nodeId, newNode);
                return newNode;
            }
        }).collect(Collectors.toList()));
        TestUtil.invokeHiddenMethod(w, OsmPrimitive.class, "setIncomplete", void.class, new Class<?>[]{ boolean.class }, false);
        return w;
    }
}
