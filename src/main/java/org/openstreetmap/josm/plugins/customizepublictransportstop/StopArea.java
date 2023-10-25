// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.customizepublictransportstop;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Stop area settings
 *
 * @author Rodion Scherbakov
 */
public class StopArea {

    /**
     * Name of stop area
     */
    public String name;
    /**
     * English name of stop area
     */
    public String nameEn;
    /**
     * Operator of stop area
     */
    public String operator;
    /**
     * Network name
     */
    public String network;
    /**
     * Level of network including this stop area
     */
    public String service;
    /**
     * Flag of bus stop area
     */
    public boolean isBus;
    /**
     * Flag of trolleybus stop area
     */
    public boolean isTrolleybus;
    /**
     * Flag of share taxi stop area
     */
    public boolean isShareTaxi;
    /**
     * Flag of bus station stop area
     */
    public boolean isBusStation;
    /**
     * Flag of tram stop area
     */
    public boolean isTram;
    /**
     * Flag of railway stop area
     */
    public boolean isTrainStop;
    /**
     * Flag of railway station
     */
    public boolean isTrainStation;
    /**
     * Flag of bench on selected platform
     */
    public boolean isAssignTransportType;
    /**
     * Flag of bench near platform
     */
    public boolean isBench;
    /**
     * Flag of covered platform
     */
    public boolean isCovered;
    /**
     * Flag of shelter on selected platform
     */
    public boolean isShelter;
    /**
     * Relation of stop area
     */
    public Relation stopAreaRelation;
    /**
     * Flag of existing of stop position
     */
    public boolean isStopPointExists;
    /**
     * Flag of area platform
     */
    public boolean isArea;
    /**
     * Separate node of bus stop or bus station
     */
    public Node separateBusStopNode;

    /**
     * List of nodes of stop positions
     */
    public final List<Node> stopPoints = new ArrayList<>();
    /**
     * List of josm objects of platforms
     */
    public final List<OsmPrimitive> platforms = new ArrayList<>();
    /**
     * List of non stop positions or platform stop area members
     */
    public final List<OsmPrimitive> otherMembers = new ArrayList<>();

    /**
     * Selected josm objects. Must be a platform
     */
    public OsmPrimitive selectedObject;

    /**
     * Constructor of stop area object
     */
    public StopArea() {
    }

    /**
     * Constructor of stop area object from selected object
     *
     * @param selectedObject Selected object
     */
    public StopArea(OsmPrimitive selectedObject) {
        this.selectedObject = selectedObject;
    }

    /**
     * Get selected in editor node
     *
     * @return Selected node or null
     */
    public Node getSelectedNode() {
        if (selectedObject instanceof Node)
            return (Node) selectedObject;
        return null;
    }

    /**
     * Get selected way
     *
     * @return Selected way or null
     */
    public Way getSelectedWay() {
        if (selectedObject instanceof Way)
            return (Way) selectedObject;
        return null;
    }
}
