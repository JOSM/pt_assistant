package org.openstreetmap.josm.plugins.pt_assistant.actions.mendaction;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DownloadArea {

    protected static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();
    protected int downloadCounter;

    /**
     *
     */
    protected void incompleteRelations() {
        List<Relation> parents = Collections.singletonList(relation);
        Future<?> future = MainApplication.worker.submit(new DownloadRelationMemberTask(parents,
            Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
                .buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
            MainApplication.getLayerManager().getEditLayer()));

        MainApplication.worker.submit(() -> {
            try {
                NotificationUtils.downloadWithNotifications(future, tr("Incomplete relations"));
                initialise();
            } catch (InterruptedException | ExecutionException e1) {
                Logging.error(e1);
            }
        });
    }

    /**
     *
     * @param wayList
     * @param Int
     */
    void downloadAreaBeforeRemovalOption(List<Way> wayList, List<Integer> Int) {
        if (abort) {
            return;
        }

        if (!onFly) {
            displayWaysToRemove(Int);
        }

        downloadCounter = 0;

        DownloadOsmTask task = new DownloadOsmTask();

        BoundsUtils.createBoundsWithPadding(wayList, .4).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area before removal"));
                    displayWaysToRemove(Int);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    }

    /**
     *
     */
    void downloadEntireArea() {
        if (abort) {
            return;
        }

        DownloadOsmTask task = new DownloadOsmTask();
        List<Way> wayList = getListOfAllWays();

        if (wayList.isEmpty()) {
            callNextWay(currentIndex);
            return;
        }

        String typeRoute = "   [\"highway\"][\"highway\"!=\"footway\"][\"highway\"!=\"path\"][\"highway\"!=\"cycleway\"];\n";
        String query = getQuery(typeRoute);
        Logging.debug(query);

        if (aroundStops || aroundGaps) {
            BoundsUtils.createBoundsWithPadding(wayList, .1).ifPresent(area -> {
                final Future<?> future = task.download(
                    new OverpassDownloadReader(area, OverpassDownloadReader.OVERPASS_SERVER.get(), query),
                    DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Entire area"));
                        callNextWay(currentIndex);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            callNextWay(currentIndex);
        }
    }


}
