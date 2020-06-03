// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.data.osm.Way;

import java.util.HashMap;
import java.util.List;

/**
 *
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
     * @param wayListColoring
     */
    void setWayListColoring(HashMap<Character, List<Way>> wayListColoring);

}
