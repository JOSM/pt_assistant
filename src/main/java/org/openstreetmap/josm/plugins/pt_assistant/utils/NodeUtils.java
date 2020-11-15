// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;

public final class NodeUtils {
    private NodeUtils() {
        // Private constructor to avoid instantiation
    }

    public static void moveOntoNearestWay(final Node node) {
        if (node.getParentWays().isEmpty()) {
            MainApplication.getLayerManager().getEditDataSet().setSelected(node);
            JoinNodeWayAction.createMoveNodeOntoWayAction().actionPerformed(null);
        }
    }
}
