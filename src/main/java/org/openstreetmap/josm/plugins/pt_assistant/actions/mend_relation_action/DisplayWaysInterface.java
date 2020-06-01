// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation_action;

import org.openstreetmap.josm.data.osm.Way;

import java.util.HashMap;

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
     * @param shorterRoutes
     */
    void setShorterRoutes(boolean shorterRoutes);

    /**
     *
     */
    void setWayColoring(HashMap<Way, Character> wayColoring);

    /**
     *
     */
    void removeTemporarylayers();
}
