// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import java.util.HashMap;
import java.util.List;

/**
 * Interface required to be implemented in order to use {@code DisplayWays}
 *
 * @author sudhanshu2
 */
public interface DisplayWaysInterface {

    /**
     *
     * @return
     */
    boolean getShowOption0();

    /**
     *
     * @return
     */
    boolean getAbort();

    /**
     *
     * @return
     */
    boolean getShorterRoutes();

    /**
     *
     * @param downloadCounter
     */
    void setDownloadCounter(int downloadCounter);

    /**
     *
     * @param setEnable
     */
    void setSetEnable(boolean setEnable);

    /**
     *
     * @param nextIndex
     */
    void setNextIndex(boolean nextIndex);

    /**
     *
     * @param halt
     */
    void setHalt(boolean halt);

    /**
     *
     * @param shorterRoutes
     */
    void setShorterRoutes(boolean shorterRoutes);

    /**
     *
     */
    void setWayColoring(HashMap<Way, Character> wayColoring);

    /**
     *
     * @return
     */
    int getCurrentIndex();

    /**
     *
     */
    void removeCurrentEdge();

    /**
     *
     */
    void backtrackCurrentEdge();

    /**
     *
     * @param ways
     */
    void getNextWayAfterSelection(List<Way> ways);

    /**
     *
     */
    void removeTemporaryLayers();

    /**
     *
     * @param index
     */
    void callNextWay(int index);

    /**
     *
     * @param newValue
     */
    void setEnabled(boolean newValue);

    /**
     *
     * @param way
     * @param index
     */
    void backTrack(Way way, int index);

    /**
     *
     * @return
     */
    Way getCurrentWay();

    /**
     *
     * @param way
     * @return
     */
    Way findWayAfterChunk(Way way);

    /**
     *
     * @param way
     */
    void getNextWayAfterBackTrackSelection(Way way);

    /**
     *
     * @return
     */
    List<RelationMember> getMembers();

    /**
     *
     * @return
     */
    String getNotice();

    /**
     *
     * @param wayIndices
     * @param ch
     */
    void removeWayAfterSelection(List<Integer> wayIndices, char ch);
}
