package org.openstreetmap.josm.plugins.pt_assistant.actions.mendaction;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

public interface MendRelationInterface {

    /**
     *
     */
    void initialise();

    /**
     *
     * @param parentWays
     * @param node
     * @param way
     * @return
     */
    List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way);

    /**
     *
     * @param currentIndex
     */
    void callNextWay(int currentIndex);

    /**
     *
     * @param way
     * @param node1
     * @param node2
     */
    void findNextWayAfterDownload(Way way, Node node1, Node node2);

    /**
     * This is a helper method for {@code searchWayFromOtherRelations} method
     * @param way
     * @return
     */
    boolean isOneWayOrRoundabout(Way way);

    /**
     *
     * @param current
     * @param next
     * @return
     */
    List<List<Way>> getDirectRouteBetweenWays(Way current, Way next);

    /**
     *
     * @param way
     * @param index
     */
    void backTrack(Way way, int index);

    /**
     *
     */
    void backtrackCurrentEdge();

    /**
     *
     * @param way
     */
    void getNextWayAfterBackTrackSelection(Way way);

    /**
     *
     * @param ways
     */
    void getNextWayAfterSelection(List<Way> ways);

    /**
     *
     * @param way
     * @param node
     * @return
     */
    boolean checkOneWaySatisfiability(Way way, Node node);

    /**
     *
     * @param wayIndices
     * @param ch
     */
    void removeWayAfterSelection(List<Integer> wayIndices, char ch);

    /**
     *
     * @param way
     * @return
     */
    Way findWayAfterChunk(Way way);

    /**
     *
     * @param ways
     * @param index
     */
    void addNewWays(List<Way> ways, int index);

    /**
     *
     * @param way
     * @param index
     */
    void deleteWayAfterIndex(Way way, int index);

    /**
     *
     */
    void removeCurrentEdge();
}
