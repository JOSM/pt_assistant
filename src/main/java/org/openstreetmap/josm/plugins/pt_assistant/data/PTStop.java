// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;

/**
 * Model a stop with one or two elements (platform and/or stop_position)
 *
 * @author darya
 *
 */
public class PTStop extends RelationMember implements ILatLon {

    /* stop_position element of this stop */
    private Node stopPosition = null;

    /* platform element of this stop */
    private OsmPrimitive platform = null;
    private RelationMember stopPositionRM = null;
    private RelationMember platformRM = null;

    /* the name of this stop */
    private String name = "";

    /*check flag */
    public boolean flag = false;

    /*stop will serve this particular way*/
    public Way serveWay;

    /**
     * Constructor
     *
     * @param other other relation member
     * @throws IllegalArgumentException if the given relation member does not fit to
     *                                  the data model used in the plugin
     */

    public PTStop(RelationMember other) {

        super(other);
        String role = "";
        if (other.getRole().contains("_exit_only")) {
            role = "_exit_only";
        } else if (other.getRole().contains("_entry_only")) {
            role = "_entry_only";
        }

        if (isPTStopPosition(other)) {
            stopPosition = other.getNode();
            setName(stopPosition.get("name"));
            stopPositionRM = new RelationMember("stop" + role, other.getMember());
        } else if (isPTPlatform(other)) {
            platform = other.getMember();
            setName(platform.get("name"));
            platformRM = new RelationMember("platform" + role, other.getMember());
        } else {
            throw new IllegalArgumentException(
                    "The RelationMember type does not match its role " + other.getMember().getName());
        }
    }

    /**
     * Adds the given element to the stop after a check
     *
     * @param member Element to add
     * @return true if added successfully, false otherwise. A false value indicates
     *         either that the OsmPrimitiveType of the given RelationMember does not
     *         match its role or that this PTStop already has an attribute with that
     *         role.
     */
    public boolean addStopElement(RelationMember member) {

        String role = "";
        if (member.getRole().contains("_exit_only")) {
            role = "_exit_only";
        } else if (member.getRole().contains("_entry_only")) {
            role = "_entry_only";
        }

        if (stopPosition == null && isPTStopPosition(member)) {
            this.stopPosition = member.getNode();
            stopPositionRM = new RelationMember("stop" + role, member.getMember());
            return true;
        } else if (platform == null && isPTPlatform(member)) {
            platform = member.getMember();
            platformRM = new RelationMember("platform" + role, member.getMember());
            return true;
        }

        return false;
    }

    /**
     * Returns the stop_position for this PTstop. If the stop_position is not
     * available directly, the method searches for a stop_area relation
     *
     * @return the stop_position for this PTstop
     */
    public Node getStopPosition() {

        return this.stopPosition;
    }

    /**
     * Returns platform (including platform_entry_only and platform_exit_only)
     *
     * @return platform (including platform_entry_only and platform_exit_only)
     */
    public OsmPrimitive getPlatform() {
        return this.platform;
    }

    /**
     * Sets the name for this stop
     *
     * @param name name for this stop
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this stop
     *
     * @return the name of this stop, never null.
     */
    public String getName() {
        return Optional.ofNullable(this.name).orElse("");
    }

    /**
     * Sets the stop_position for this stop to the given node
     *
     * @param newStopPosition the stop_position for this stop to the given node
     */
    public void setStopPosition(Node newStopPosition) {

        this.stopPosition = newStopPosition;

    }

    /**
     * Finds potential stop_positions of the platform of this PTStop. It only makes
     * sense to call this method if the stop_position attribute is null. The
     * stop_positions are potential because they may refer to a different route,
     * which this method does not check.
     *
     * @return List of potential stop_positions for this PTStop
     */
    public List<Node> findPotentialStopPositions() {
        ArrayList<Node> potentialStopPositions = new ArrayList<>();
        if (platform == null) {
            return potentialStopPositions;
        }
        // Look for a stop position within 0.001 degrees (around 50 m) of this
        // platform:
        LatLon platformCenter = platform.getBBox().getCenter();
        double ax = platformCenter.getX() - 0.001;
        double bx = platformCenter.getX() + 0.001;
        double ay = platformCenter.getY() - 0.001;
        double by = platformCenter.getY() + 0.001;
        BBox platformBBox = new BBox(ax, ay, bx, by);

        Collection<Node> allNodes = platform.getDataSet().getNodes();
        for (Node currentNode : allNodes) {
            if (checkNodeBelongstoWayorNot(platformBBox, currentNode)) {
                potentialStopPositions.add(currentNode);
            }
        }
        return potentialStopPositions;
    }

    /**
     * Finds the ways which can be served by a particular stop
     *
     * @return most probable way which will be served by a stop
     */
    public Way findServingWays(PTStop stop) {
        if (stop.flag) {
            return this.serveWay;
        }
        Way wayOfStopPosition = findWayForNode(stop.getStopPosition(), stop);
        if (wayOfStopPosition != null) {
            this.serveWay = wayOfStopPosition;
            return wayOfStopPosition;
        }
        // 3) Search if the stop has a stop_area:
        List<OsmPrimitive> stopElements = new ArrayList<>(2);
        if (stop.getStopPosition() != null) {
            stopElements.add(stop.getStopPosition());
        }
        if (stop.getPlatform() != null) {
            stopElements.add(stop.getPlatform());
        }
        Set<Relation> parents = Node.getParentRelations(stopElements);
        for (Relation parentRelation : parents) {
            if (StopUtils.isStopArea(parentRelation)) {
                for (RelationMember rm : parentRelation.getMembers()) {
                    if (StopUtils.isStopPosition(rm.getMember())) {
                        Way rmWay = this.findWayForNode(rm.getNode(), stop);
                        if (rmWay != null) {
                            this.serveWay = rmWay;
                            return rmWay;
                        }
                    }
                }
            }
        }
        if (stop.getPlatform() != null) {
            List<Node> potentialStopPositionList = stop.findPotentialStopPositions();
            Node closestStopPosition = null;
            double minDistanceSq = Double.MAX_VALUE;
            for (Node potentialStopPosition : potentialStopPositionList) {
                double distanceSq = potentialStopPosition.getCoor()
                        .distanceSq(stop.getPlatform().getBBox().getCenter());
                if (distanceSq < minDistanceSq) {
                    closestStopPosition = potentialStopPosition;
                    minDistanceSq = distanceSq;
                }
            }
            if (closestStopPosition != null) {
                Way closestWay = findWayForNode(closestStopPosition, stop);
                if (closestWay != null) {
                    this.serveWay = closestWay;
                    return closestWay;
                }
            }
        }

        double searchRadius = 0.001;

        while (searchRadius < 0.005) {

            Way foundWay = this.findNearestWayInRadius(stop.getPlatform(), searchRadius, stop);

            if (foundWay != null) {
                this.serveWay = foundWay;
                return foundWay;
            }

            foundWay = this.findNearestWayInRadius(stop.getStopPosition(), searchRadius, stop);

            if (foundWay != null) {
                this.serveWay = foundWay;
                return foundWay;
            }

            searchRadius = searchRadius + 0.001;
        }
        return null;
    }

    /**
     * Finds the nearest Way in particular radius around the stop
     *
     * @return most probable way which will be served by a stop
     */
    private Way findNearestWayInRadius(OsmPrimitive platform, double searchRadius, PTStop stop) {
        if (platform == null) {
            return null;
        }
        LatLon platformCenter = platform.getBBox().getCenter();
        double ax = platformCenter.getX() - searchRadius;
        double bx = platformCenter.getX() + searchRadius;
        double ay = platformCenter.getY() - searchRadius;
        double by = platformCenter.getY() + searchRadius;
        BBox platformBBox = new BBox(ax, ay, bx, by);

        Set<Way> potentialWays = new HashSet<>();

        Collection<Node> allNodes = platform.getDataSet().getNodes();
        for (Node currentNode : allNodes) {
            if (platformBBox.bounds(currentNode.getBBox())) {
                Way referredWay = findWayForNode(currentNode, stop);
                if (referredWay != null) {
                    potentialWays.add(referredWay);
                }
            }
        }
        Node platformNode;
        if (platform.getType().equals(OsmPrimitiveType.NODE)) {
            platformNode = (Node) platform;
        } else {
            platformNode = new Node(platform.getBBox().getCenter());
        }

        return potentialWays.stream().collect(WayUtils.nearestToPointCollector(platformNode)).orElse(null);
    }

    /**
    *check if node is inside the platform area and belongs to a way or not
    *
    * @param platformBBox bounding box
    *@param currentNode for which check has to be made
    * @return true if it belongs to way, false otherwise
    */
    public boolean checkNodeBelongstoWayorNot(BBox platformBBox, Node currentNode) {
        if (platformBBox.bounds(currentNode.getBBox())) {
            List<OsmPrimitive> referrers = currentNode.getReferrers();
            for (OsmPrimitive referrer : referrers) {
                if (referrer.getType().equals(OsmPrimitiveType.WAY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the PTWay of the given stop_position by looking at its referrer
     *
     * @param stopPosition stop position
     * @param stop PTstopwq
     *1- find all the referrer:
     *2-filter all referrer which are ways
     *3-find all relation of each way
     *4-in each relation check this stop is part of relation or Not
     *5-if stop is part of the relation then return
     * @return referred way which is part of relation
     */

    private Way findWayForNode(Node stopPosition, PTStop stop) {
        if (stopPosition == null) {
            return null;
        }
        StopToWayAssigner assigner = new StopToWayAssigner();
        List<OsmPrimitive> referrers = stopPosition.getReferrers();
        for (OsmPrimitive referredPrimitive : referrers) {
            if (referredPrimitive.getType().equals(OsmPrimitiveType.WAY)) {
                List<OsmPrimitive> ways = new ArrayList<>(1);
                Way referredWay = (Way) referredPrimitive;
                ways.add(referredWay);
                Set<Relation> parents = Way.getParentRelations(ways);
                for (Relation rel : parents) {
                    if (checkRelationContainsStop(rel, stop)) {
                        referredWay = assigner.findClosestWayFromRelation(rel, stop, stopPosition);
                        return referredWay;
                    }
                }
            }
        }
        return null;
    }

    /**
    *check if relation contains the given stop or not
    *
    *@param rel Relation
    *@param stop for which check has to be made
    * @return true if it belongs to relation, false otherwise
    */
    boolean checkRelationContainsStop(Relation rel, PTStop stop) {
        for (RelationMember rm : rel.getMembers()) {
            if (rm.getUniqueId() == stop.getUniqueId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this stop equals to other by comparing if they have the same
     * stop_position or a platform
     *
     * @param other PTStop to be compared
     * @return true if equal, false otherwise
     */
    public boolean equalsStop(PTStop other) {

        if (other == null) {
            return false;
        }

        if (this.stopPosition != null
                && (this.stopPosition == other.getStopPosition() || this.stopPosition == other.getPlatform())) {
            return true;
        }

        return this.platform != null
                && (this.platform == other.getPlatform() || this.platform == other.getStopPosition());
    }

    /**
     * Checks if the relation member refers to a stop in a public transport route.
     * Some stops can be modeled with ways.
     *
     * @param rm relation member to be checked
     * @return true if the relation member refers to a stop, false otherwise
     */
    public static boolean isPTStop(RelationMember rm) {
        return isPTStopPosition(rm) || isPTPlatform(rm);
    }

    /**
     * checks whether the given relation member matches a Stop Position or not
     *
     * @param rm member to check
     * @return true if it matches, false otherwise
     */
    public static boolean isPTStopPosition(RelationMember rm) {
        return StopUtils.isStopPosition(rm.getMember()) && rm.getType().equals(OsmPrimitiveType.NODE);
    }

    /**
     * checks whether the given relation member matches a Platform or not
     *
     * @param rm member to check
     * @return true if it matches, false otherwise
     */
    public static boolean isPTPlatform(RelationMember rm) {
        return rm.getMember().hasTag("highway", "bus_stop") || rm.getMember().hasTag("public_transport", "platform")
                || rm.getMember().hasTag("highway", "platform") || rm.getMember().hasTag("railway", "platform")
                || rm.getMember().hasTag("amenity", "bus_station");
    }

    public RelationMember getPlatformRM() {
        return platformRM;
    }

    public RelationMember getStopPositionRM() {
        return stopPositionRM;
    }

    @Override
    public double lon() {
        return getLatOrLon(ILatLon::lon);
    }

    @Override
    public double lat() {
        return getLatOrLon(ILatLon::lat);
    }

    /**
     * @param fun set this to the method reference of either {@link ILatLon#lat()} or {@link ILatLon#lon()} to make this return the latitude or longitude
     * @return the result of applying {@code fun} to the center of {@link #platform}.
     * Or if no platform is set, the result of applying {@code fun} to the {@link #stopPosition} is returned.
     * If that is also not set, {@link Double#NaN} is returned
     */
    private double getLatOrLon(final ToDoubleFunction<? super ILatLon> fun) {
        return Stream.of(
            Optional.ofNullable(platform).map(it -> it.getBBox().getCenter()).orElse(null),
            stopPosition
        )
            .filter(Objects::nonNull)
            .mapToDouble(fun)
            .findFirst()
            .orElse(Double.NaN);
    }
}
