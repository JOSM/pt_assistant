// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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
            Optional.ofNullable(MainApplication.getLayerManager().getEditDataSet()).ifPresent(dataset -> {
                final List<Relation> ptv2Relations =
                    Stream.of(
                        dataset.getSelectedRelations(), // if there are selected routes, try adding them first
                        dataset.getRelations() // fallback in case there are no selected ptv2 routes: use all ptv2 relations in the edit dataset
                    )
                        // keep only PTv2 relations in each relation list
                        .map(it -> it.stream().filter(RouteUtils::isVersionTwoPTRoute).collect(Collectors.toList()))
                        // take the first non-empty relation list
                        .filter(it -> !it.isEmpty()).findFirst()
                        // fallback: empty list
                        .orElse(Collections.emptyList());

                // add all stop_positions:
                final List<Node> stopPositions = dataset.getNodes().stream()
                    .filter(StopUtils::isStopPosition)
                    // only keep those nodes that do not have a referer way
                    .filter(node -> node.getReferrers().stream().noneMatch(referer -> referer.getType() == OsmPrimitiveType.WAY))
                    .collect(Collectors.toList());

                DownloadPrimitivesWithReferrersTask task = new DownloadPrimitivesWithReferrersTask(
                    false,
                    Stream.of(ptv2Relations, stopPositions)
                        .flatMap(Collection::stream)
                        .filter(it -> !it.isNew()) // https://josm.openstreetmap.de/ticket/23640 (only download not-new elements)
                        .collect(Collectors.toList()),
                    false,
                    true,
                    null,
                    null
                );
                task.run();
            });
        }
    }
}
