package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.pt_assistant.TestUtil;
import org.openstreetmap.josm.tools.Pair;

public final class GeometryUtilsTest {

    private static final ILatLon P_0_0 = new LatLon(0, 0);
    private static final ILatLon P_2_0 = new LatLon(2, 0);
    private static final ILatLon P_0_2 = new LatLon(0, 2);
    private static final ILatLon P_3_5 = new LatLon(3, 5);
    private static final ILatLon P_7_M3 = new LatLon(7, -3);
    private static final ILatLon P_M4_M2 = new LatLon(-4, -2);
    private static final ILatLon P_M2_3 = new LatLon(-2, 3);
    private static final ILatLon P_10_173_7_924 = new LatLon(10.173, 7.924);

    @Test
    public void testIsUtilityClass() {
        TestUtil.testUtilityClass(GeometryUtils.class);
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void testDirection() {
        assertEquals(0, GeometryUtils.direction(Pair.create(P_0_0, P_0_0), P_0_0), 1e-12);
        assertEquals(0, GeometryUtils.direction(Pair.create(P_M4_M2, P_M4_M2), P_3_5), 1e-12); // special case, line has no direction → 0

        assertPointsOnSidesOfLine(
            Pair.create(P_0_0, P_2_0),
            Arrays.asList(P_M2_3, P_0_2, P_3_5, P_10_173_7_924), Arrays.asList(P_0_0, P_2_0), Arrays.asList(P_M4_M2, P_7_M3)
        );
        assertPointsOnSidesOfLine(
            Pair.create(P_M4_M2, P_3_5),
            Arrays.asList(P_M2_3), Arrays.asList(P_M4_M2, P_0_2, P_3_5), Arrays.asList(P_0_0, P_2_0, P_10_173_7_924, P_7_M3)
        );
        assertPointsOnSidesOfLine(
            Pair.create(P_10_173_7_924, P_M4_M2),
            Arrays.asList(P_0_0, P_2_0, P_7_M3), Arrays.asList(P_M4_M2, P_10_173_7_924), Arrays.asList(P_0_2, P_3_5, P_M2_3)
        );
    }

    private void assertPointsOnSidesOfLine(
        final Pair<ILatLon, ILatLon> line,
        final Iterable<? extends ILatLon> toTheLeft,
        final Iterable<? extends ILatLon> onTheLine,
        final Iterable<? extends ILatLon> toTheRight
    ) {
        final Pair<ILatLon, ILatLon> invertedLine = Pair.create(line.b, line.a);
        System.out.println("Checking direction of points in relation to the line from (" + line.a.lon() + '/' + line.a.lat() + ") to (" + line.b.lon() + '/' + line.b.lat() + ')');
        toTheLeft.forEach(l -> {
            System.out.print("  to the left:  (" + l.lon() + '/' + l.lat() + ')');
            System.out.flush();
            assertTrue(GeometryUtils.direction(line, l) < 0);
            assertTrue(GeometryUtils.direction(invertedLine, l) > 0);
            System.out.println(" ✔️️️");
        });
        onTheLine.forEach(on -> {
            System.out.print("  on the line:  (" + on.lon() + '/' + on.lat() + ')');
            System.out.flush();
            assertEquals(0, GeometryUtils.direction(line, on), 1e-12);
            assertEquals(0, GeometryUtils.direction(invertedLine, on), 1e-12);
            System.out.println(" ✔️️️");
        });
        toTheRight.forEach(r -> {
            System.out.print("  to the right: (" + r.lon() + '/' + r.lat() + ')');
            System.out.flush();
            assertTrue(GeometryUtils.direction(line, r) > 0);
            assertTrue(GeometryUtils.direction(invertedLine, r) < 0);
            System.out.println(" ✔️️️");
        });
    }

    @Test
    public void testDistancePointToLineSegment() {
        assertDistancePointToLineSegment(0, Pair.create(P_0_0, P_0_0), P_0_0);
        assertDistancePointToLineSegment(0, Pair.create(P_M4_M2, P_3_5), P_0_2);
        assertDistancePointToLineSegment(0, Pair.create(P_M4_M2, P_3_5), P_M4_M2);
        assertDistancePointToLineSegment(0, Pair.create(P_M4_M2, P_3_5), P_3_5);

        assertDistancePointToLineSegment(Math.sqrt(2), Pair.create(P_0_2, P_2_0), P_0_0);
        assertDistancePointToLineSegment(Math.sqrt(72), Pair.create(P_M4_M2, P_3_5), P_7_M3);
        assertDistancePointToLineSegment(Math.sqrt(34), Pair.create(P_0_0, P_2_0), P_7_M3);
        assertDistancePointToLineSegment(Math.sqrt(20), Pair.create(P_0_0, P_2_0), P_M4_M2);

        assertDistancePointToLineSegment(Math.sqrt(361/13.0), Pair.create(P_M2_3, P_7_M3), P_M4_M2);
        assertDistancePointToLineSegment(Math.sqrt(1/13.0), Pair.create(P_M2_3, P_7_M3), P_0_2);
        assertDistancePointToLineSegment(Math.sqrt(256/13.0), Pair.create(P_M2_3, P_7_M3), P_3_5);
        assertDistancePointToLineSegment(Math.sqrt(382554481/3250000.0), Pair.create(P_M2_3, P_7_M3), P_10_173_7_924);
    }

    private void assertDistancePointToLineSegment(
        final double expectedDistance,
        final Pair<ILatLon, ILatLon> segment,
        final ILatLon point
    ) {
        System.out.print("Distance between (" + point.lon() + '/' + point.lat() + ") and the line from (" + segment.a.lon() + '/' + segment.a.lat() + ") to (" + segment.b.lon() + '/' + segment.b.lat() + ") is expected to be " + expectedDistance);
        System.out.flush();
        assertEquals(expectedDistance, GeometryUtils.distPointToSegment(segment, point), 1e-12);
        assertEquals(expectedDistance, GeometryUtils.distPointToSegment(Pair.create(segment.b, segment.a), point), 1e-12);
        System.out.println(" ✔️️️");
    }
}
