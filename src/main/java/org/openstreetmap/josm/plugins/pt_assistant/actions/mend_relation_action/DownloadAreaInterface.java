// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation_action;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

/**
 * Interface required to be implemented in order to use {@code DownloadArea}
 *
 * @author sudhanshu2
 */
public interface DownloadAreaInterface {
    /**
     *
     * @return
     */
    List<Way> getListOfAllWays();

    /**
     *
     * @param index
     */
    void callNextWay(int index);

    /**
     *
     * @return
     */
    boolean getAbort();

    /**
     *
     * @return
     */
    int getCurrentIndex();

    /**
     *
     * @param currentIndex
     */
    void setCurrentIndex(int currentIndex);

    /**
     *
     * @return
     */
    String getQuery();

    /**
     *
     * @return
     */
    boolean getAroundStops();

    /**
     *
     * @return
     */
    boolean getAroundGaps();

    /**
     *
     * @return
     */
    boolean getOnFly();

    /**
     *
     * @return
     */
    int getDownloadCounter();

    /**
     *
     * @param downloadCounter
     */
    void setDownloadCounter(int downloadCounter);

    /**
     *
     */
    void initialise();

    /**
     *
     * @return
     */
    Relation getRelation();

    /**
     *
     * @param way
     * @param node1
     * @param node2
     * @return
     */
    Way findNextWayAfterDownload(Way way, Node node1, Node node2);

    /**
     *
     * @param way
     * @param prevWay
     * @param wayList
     */
    void goToNextWays(Way way, Way prevWay, java.util.List<Way> wayList);

    /**
     *
     * @return
     */
    int getMemberSize();

    /**
     *
     */
    void deleteExtraWays();
}
