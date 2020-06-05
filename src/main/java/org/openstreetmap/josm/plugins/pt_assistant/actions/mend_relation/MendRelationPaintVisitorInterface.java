// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import java.util.HashMap;
import java.util.List;

/**
 * Interface required to be implemented in order to use {@code MendRelationPaintVisitor}
 *
 * @author sudhanshu2
 */
public interface MendRelationPaintVisitorInterface {

    /**
     *
     * @return
     */
    String getOneWayString();

    /**
     *
     * @return
     */
    boolean getShowOption0();

    /**
     *
     * @return
     */
    boolean getShorterRoutes();

    /**
     *
     * @return
     */
    HashMap<Way, Character> getWayColoring();

    /**
     *
     * @param wayColoring
     */
    void setWayColoring(HashMap<Way, Character> wayColoring);

    /**
     *
     * @return
     */
    HashMap<Character, List<Way>> getWayListColoring();

    /**
     *
     * @return
     */
    Way getCurrentWay();

    /**
     *
     * @return
     */
    Way getNextWay();

    /**
     *
     * @return
     */
    String getNotice();

    /**
     *
     * @param index
     * @return
     */
    int getPreviousWayIndex(int index);

    /**
     *
     * @return
     */
    int getCurrentIndex();

    /**
     *
     * @return
     */
    List<RelationMember> getMembersList();

    /**
     *
     * @param wayListColoring
     */
    void setWayListColoring(HashMap<Character, List<Way>> wayListColoring);

}
