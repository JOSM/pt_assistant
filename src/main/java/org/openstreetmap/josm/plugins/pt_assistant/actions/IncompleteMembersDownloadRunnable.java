// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Runnable that downloads incomplete relation members while pausing the rest of testing
 * @author darya
 *
 */
public class IncompleteMembersDownloadRunnable implements Runnable {

    /**
     * Default constructor
     */
    public IncompleteMembersDownloadRunnable() {
        super();
    }

    @Override
    public void run() {

        synchronized (this) {

            ArrayList<PrimitiveId> list = new ArrayList<>();

            // if there are selected routes, try adding them first:
            for (Relation currentSelectedRelation : MainApplication.getLayerManager().getEditDataSet().getSelectedRelations()) {
                if (RouteUtils.isVersionTwoPTRoute(currentSelectedRelation)) {
                    list.add(currentSelectedRelation);
                }
            }

            if (list.isEmpty()) {
                // add all route relations that are of public_transport
                // version 2:
                Collection<Relation> allRelations = MainApplication.getLayerManager().getEditDataSet().getRelations();
                for (Relation currentRelation : allRelations) {
                    if (RouteUtils.isVersionTwoPTRoute(currentRelation)) {
                        list.add(currentRelation);
                    }
                }
            }

            // add all stop_positions:
            Collection<Node> allNodes = MainApplication.getLayerManager().getEditDataSet().getNodes();
            for (Node currentNode : allNodes) {
                if (StopUtils.isStopPosition(currentNode)) {
                    List<OsmPrimitive> referrers = currentNode.getReferrers();
                    boolean parentWayExists = false;
                    for (OsmPrimitive referrer : referrers) {
                        if (referrer.getType() == OsmPrimitiveType.WAY) {
                            parentWayExists = true;
                            break;
                        }
                    }
                    if (!parentWayExists) {
                        list.add(currentNode);

                    }

                }
            }

            DownloadPrimitivesWithReferrersTask task = new DownloadPrimitivesWithReferrersTask(false, list, false, true,
                null, null);
            task.run();
        }
    }
}
