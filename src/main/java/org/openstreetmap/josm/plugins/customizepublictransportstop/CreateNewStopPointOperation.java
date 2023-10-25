// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.customizepublictransportstop;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWaySegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Operation of creation of new stop point
 *
 * @author Rodion Scherbakov
 */
public class CreateNewStopPointOperation extends StopAreaOperationBase {

    /**
     * Constructor of operation object
     *
     * @param currentDataSet Current Josm data set
     */
    public CreateNewStopPointOperation(DataSet currentDataSet) {
        super(currentDataSet);
    }

    /**
     * The *result* does not depend on the current map selection state, neither does
     * the result *order*. It solely depends on the distance to point p.
     * <p>
     * This code is coped from JOSM code
     *
     * @return a sorted map with the keys representing the distance of their
     *         associated nodes to point p.
     */
    private Map<Double, List<Node>> getNearestNodesImpl(Point p) {
        TreeMap<Double, List<Node>> nearestMap = new TreeMap<>();
        DataSet ds = getCurrentDataSet();

        if (ds != null) {
            double dist;
            double snapDistanceSq = 200;
            snapDistanceSq *= snapDistanceSq;

            for (Node n : ds.searchNodes(getBBox(p, 200))) {
                if ((dist = MainApplication.getMap().mapView.getPoint2D(n).distanceSq(p)) < snapDistanceSq) {
                    List<Node> nlist;
                    if (nearestMap.containsKey(dist)) {
                        nlist = nearestMap.get(dist);
                    } else {
                        nlist = new LinkedList<>();
                        nearestMap.put(dist, nlist);
                    }
                    nlist.add(n);
                }
            }
        }

        return nearestMap;
    }

    /**
     * Selection of area for search of roads
     *
     * @param p Current point
     * @param snapDistance Distance for search
     * @return Area
     */
    private static BBox getBBox(Point p, int snapDistance) {
        MapView mapView = MainApplication.getMap().mapView;
        return new BBox(mapView.getLatLon(p.x - snapDistance, p.y - snapDistance),
                mapView.getLatLon(p.x + snapDistance, p.y + snapDistance));
    }

    /**
     * Search of nearest points on ways
     *
     * @param platformCoord Platform coordinates
     * @param stopArea Stop area object
     * @return Dictionary of founded points and distances from platform
     */
    public AbstractMap.SimpleEntry<Double, Node> getNearestNode(LatLon platformCoord, StopArea stopArea) {
        Point p = MainApplication.getMap().mapView.getPoint(platformCoord);
        Map<Double, List<Node>> distNodes = getNearestNodesImpl(p);
        Double[] distances = distNodes.keySet().toArray(new Double[0]);
        Arrays.sort(distances);
        int distanceIndex = -1;
        while (++distanceIndex < distances.length) {
            List<Node> nodes = distNodes.get(distances[distanceIndex]);
            for (Node node : nodes) {
                for (Way way : getCurrentDataSet().getWays()) {
                    if (way.getNodes().contains(node) && testWay(way, stopArea)) {
                        return new AbstractMap.SimpleEntry<>(distances[distanceIndex], node);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Selection of ways for stop position by type of way and type of stop
     *
     * @param way The way
     * @param stopArea Stop area
     * @return true, if way can contain stop position
     */
    public boolean testWay(final Way way, final StopArea stopArea) {
        if (stopArea.isTrainStation || stopArea.isTrainStop) {
            return OSMTags.RAIL_TAG_VALUE.equals(way.getKeys().get(OSMTags.RAILWAY_TAG)) &&
                OSMTags.MAIN_TAG_VALUE.equals(way.getKeys().get(OSMTags.USAGE_TAG));
        }

        if (stopArea.isTram) {
            return OSMTags.TRAM_TAG_VALUE.equals(way.getKeys().get(OSMTags.RAILWAY_TAG));
        }

        if (stopArea.isBus || stopArea.isTrolleybus || stopArea.isShareTaxi) {
            return Optional.ofNullable(way.getKeys().get(OSMTags.HIGHWAY_TAG))
                .map(OSMTags.TAG_VALUES_HIGHWAY::contains)
                .orElse(false);
        }
        return false;
    }

    /**
     * The *result* does not depend on the current map selection state, neither does
     * the result *order*. It solely depends on the distance to point p.
     * <p>
     * This code is coped from JOSM code
     *
     * @return a sorted map with the keys representing the perpendicular distance of
     *         their associated way segments to point p.
     */
    private Map<Double, List<IWaySegment<Node, Way>>> getNearestWaySegmentsImpl(Point p) {
        Map<Double, List<IWaySegment<Node, Way>>> nearestMap = new TreeMap<>();
        DataSet ds = getCurrentDataSet();

        if (ds != null) {
            double snapDistanceSq = Config.getPref().getInt("mappaint.segment.snap-distance", 200);
            snapDistanceSq *= snapDistanceSq;

            for (Way w : ds.searchWays(getBBox(p, Config.getPref().getInt("mappaint.segment.snap-distance", 200)))) {
                Node lastN = null;
                int i = -2;
                for (Node n : w.getNodes()) {
                    i++;
                    if (n.isDeleted() || n.isIncomplete()) { // FIXME: This shouldn't happen, raise exception?
                        continue;
                    }
                    if (lastN == null) {
                        lastN = n;
                        continue;
                    }

                    Point2D pointA = MainApplication.getMap().mapView.getPoint2D(lastN);
                    Point2D pointB = MainApplication.getMap().mapView.getPoint2D(n);
                    double c = pointA.distanceSq(pointB);
                    double a = p.distanceSq(pointB);
                    double b = p.distanceSq(pointA);

                    /*
                     * perpendicular distance squared loose some precision to account for possible
                     * deviations in the calculation above e.g. if identical (A and B) come about
                     * reversed in another way, values may differ -- zero out least significant 32
                     * dual digits of mantissa.
                     */
                    double perDistSq = Double.longBitsToDouble(
                            // resolution in numbers with large exponent not needed here..
                            Double.doubleToLongBits(a - (a - b + c) * (a - b + c) / 4 / c) >> 32 << 32);

                    if (perDistSq < snapDistanceSq && a < c + snapDistanceSq && b < c + snapDistanceSq) {
                        List<IWaySegment<Node, Way>> wslist;
                        if (nearestMap.containsKey(perDistSq)) {
                            wslist = nearestMap.get(perDistSq);
                        } else {
                            wslist = new LinkedList<>();
                            nearestMap.put(perDistSq, wslist);
                        }
                        wslist.add(new IWaySegment<>(w, i));
                    }

                    lastN = n;
                }
            }
        }

        return nearestMap;
    }

    /**
     * Selection of nearest way for stop position
     *
     * @param platformCoord Platform coordinates
     * @param stopArea Stop area
     * @return Nearest way segment
     */
    protected NearestWaySegment getNearestWaySegment(LatLon platformCoord, StopArea stopArea) {
        MapView mapView = MainApplication.getMap().mapView;
        Point p = mapView.getPoint(platformCoord);
        Map<Double, List<IWaySegment<Node, Way>>> distWaySegments = getNearestWaySegmentsImpl(p);
        for (Map.Entry<Double, List<IWaySegment<Node, Way>>> entry : distWaySegments.entrySet()) {
            for (IWaySegment<Node, Way> waySegment : entry.getValue()) {
                if (testWay(waySegment.getWay(), stopArea)) {
                    INode n = waySegment.getFirstNode();
                    INode lastN = waySegment.getSecondNode();

                    EastNorth newPosition = Geometry.closestPointToSegment(n.getEastNorth(), lastN.getEastNorth(),
                            ProjectionRegistry.getProjection().latlon2eastNorth(platformCoord));
                    LatLon newNodePosition = ProjectionRegistry.getProjection().eastNorth2latlon(newPosition);
                    Point2D lastN2D = mapView.getPoint2D(lastN);
                    Point2D n2D = mapView.getPoint2D(n);
                    Point2D newNodePosition2D = mapView.getPoint2D(newNodePosition);
                    double distCurrenNodes = lastN2D.distance(n2D);
                    if ((newNodePosition2D.distance(lastN2D) < distCurrenNodes)
                            && (newNodePosition2D.distance(n2D) < distCurrenNodes)) {
                        return new NearestWaySegment(entry.getKey(), waySegment, new Node(newNodePosition));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creation of stop position node on nearest way
     *
     * @param newStopNode New stop position node
     * @param waySegment Way segment including stop position node
     * @return Stop position node
     */
    protected Node createNodeOnWay(Node newStopNode, IWaySegment<?, Way> waySegment) {
        UndoRedoHandler.getInstance().add(new AddCommand(MainApplication.getLayerManager().getEditDataSet(), newStopNode));
        List<Node> wayNodes = waySegment.getWay().getNodes();
        wayNodes.add(waySegment.getUpperIndex(), newStopNode);
        Way newWay = new Way(waySegment.getWay());
        newWay.setNodes(wayNodes);
        UndoRedoHandler.getInstance().add(new ChangeCommand(waySegment.getWay(), newWay));
        return newStopNode;
    }

    /**
     * Creation of stop position
     *
     * @param stopArea Stop Area
     */
    @Override
    public StopArea performCustomizing(StopArea stopArea) {
        LatLon platformCoord = null;
        if (stopArea.selectedObject instanceof Node) {
            platformCoord = ((Node) stopArea.selectedObject).getCoor();
        } else
            platformCoord = getCenterOfWay(stopArea.selectedObject);
        if (platformCoord == null)
            return stopArea;
        AbstractMap.SimpleEntry<Double, Node> nearestNode = getNearestNode(platformCoord, stopArea);
        NearestWaySegment nearestWaySegment = getNearestWaySegment(platformCoord, stopArea);
        Node newStopPointNode = null;
        if (nearestNode != null && nearestWaySegment != null) {
            MapView mapView = MainApplication.getMap().mapView;
            double segmentDist = mapView.getPoint2D(platformCoord)
                    .distanceSq(mapView.getPoint2D(nearestWaySegment.newNode));
            Double nodeDistSq = nearestNode.getKey();
            // nodeDistSq *= nodeDistSq - 2;
            if (segmentDist < nodeDistSq - 2) {
                // MessageBox.ok("new stop node v2 " + segmentDist.toString() + " " +
                // nodeDistSq.toString());
                newStopPointNode = createNodeOnWay(nearestWaySegment.newNode, nearestWaySegment.waySegment);
            } else {
                // MessageBox.ok("new stop node v3 " + segmentDist.toString() + " " +
                // nodeDistSq.toString());
                newStopPointNode = nearestNode.getValue();
            }
        } else if (nearestNode != null && nearestWaySegment == null) {
            newStopPointNode = nearestNode.getValue();
        } else if (nearestNode == null && nearestWaySegment != null) {
            // MessageBox.ok("new stop node2");
            newStopPointNode = createNodeOnWay(nearestWaySegment.newNode, nearestWaySegment.waySegment);
        }
        if (newStopPointNode != null) {
            stopArea.stopPoints.add(newStopPointNode);
        }
        return stopArea;
    }

}
