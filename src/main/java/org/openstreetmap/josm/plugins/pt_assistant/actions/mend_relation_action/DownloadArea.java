// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation_action;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.data.osm.Node;
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

/**
 * Used for downloading area while using the mend relation function in routing helper
 *
 * @author sudhanshu2
 */
public class DownloadArea {
    private static final DownloadParams DEFAULT_DOWNLOAD_PARAMS = new DownloadParams();
    DownloadAreaInterface download;

    /**
     *
     * @param download
     */
    public DownloadArea(DownloadAreaInterface download) {
        this.download = download;
    }

    /**
     *
     */
    protected void downloadEntireArea() {
        if (download.getAbort()) {
            return;
        }

        DownloadOsmTask task = new DownloadOsmTask();
        List<Way> wayList = download.getListOfAllWays();

        if (wayList.isEmpty()) {
            download.callNextWay(download.getCurrentIndex());
            return;
        }

        String query = download.getQuery();
        Logging.debug(query);

        if (download.getAroundStops() || download.getAroundGaps()) {
            BoundsUtils.createBoundsWithPadding(wayList, .1).ifPresent(area -> {
                final Future<?> future = task.download(
                    new OverpassDownloadReader(area, OverpassDownloadReader.OVERPASS_SERVER.get(), query),
                    DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Entire area"));
                        download.callNextWay(download.getCurrentIndex());
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            download.callNextWay(download.getCurrentIndex());
        }

    }

    /**
     *
     */
    protected void downloadIncompleteRelations() {

        List<Relation> parents = Collections.singletonList(download.getRelation());

        Future<?> future = MainApplication.worker
            .submit(new DownloadRelationMemberTask(parents,
                Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
                    .buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
                MainApplication.getLayerManager().getEditLayer()));

        MainApplication.worker.submit(() -> {
            try {
                NotificationUtils.downloadWithNotifications(future, tr("Incomplete relations"));
                download.initialise();
            } catch (InterruptedException | ExecutionException e1) {
                Logging.error(e1);
            }
        });
    }

    /**
     *
     * @param way
     * @param node1
     * @param node2
     */
    protected void downloadAreaAroundWay(Way way, Node node1, Node node2) {
        if (download.getAbort()) {
            return;
        }

        if ((download.getDownloadCounter() > 160 || way.isOutsideDownloadArea() || way.isNew()) && download.getOnFly()) {
            download.setDownloadCounter(0);

            DownloadOsmTask task = new DownloadOsmTask();

            BoundsUtils.createBoundsWithPadding(way.getBBox(), .4).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (1)");
                        download.findNextWayAfterDownload(way, node1, node2);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            download.findNextWayAfterDownload(way, node1, node2);
        }
    }

    /**
     *
     * @param way
     */
    protected void downloadAreaAroundWay(Way way) {
        if (download.getAbort()) {
            return;
        }

        if (download.getDownloadCounter() > 160 && download.getOnFly()) {
            download.setDownloadCounter(0);

            DownloadOsmTask task = new DownloadOsmTask();
            BoundsUtils.createBoundsWithPadding(way.getBBox(), .1).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (2)");
                        if (download.getCurrentIndex() >= download.getMemberSize() - 1) {
                            download.deleteExtraWays();
                        } else {
                            download.setCurrentIndex(download.getCurrentIndex() + 1);
                            download.callNextWay(download.getCurrentIndex());
                        }
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            if (download.getCurrentIndex() >= download.getMemberSize() - 1) {
                download.deleteExtraWays();
            } else {
                download.setCurrentIndex(download.getCurrentIndex() + 1);
                download.callNextWay(download.getCurrentIndex());
            }
        }
    }

    /**
     *
     * @param way
     * @param prevWay
     * @param ways
     */
    protected void downloadAreaAroundWay(Way way, Way prevWay, List<Way> ways) {
        if (download.getAbort()) {
            return;
        }

        if ((download.getDownloadCounter() > 160 || way.isOutsideDownloadArea() || way.isNew()) && download.getOnFly()) {
            download.setDownloadCounter(0);

            DownloadOsmTask task = new DownloadOsmTask();

            BoundsUtils.createBoundsWithPadding(way.getBBox(), .2).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (3)");
                        download.goToNextWays(way, prevWay, ways);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            download.goToNextWays(way, prevWay, ways);
        }
    }

    /**
     *
     * @param wayList
     * @param Int
     */
    protected void downloadAreaBeforeRemovalOption(java.util.List<Way> wayList, java.util.List<Integer> Int) {
        if (download.getAbort()) {
            return;
        }

        if (!download.getOnFly()) {
            new DisplayWays().displayWaysToRemove(Int);
        }

        download.setDownloadCounter(0);
        DownloadOsmTask task = new DownloadOsmTask();
        BoundsUtils.createBoundsWithPadding(wayList, .4).ifPresent(area -> {
            Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

            MainApplication.worker.submit(() -> {
                try {
                    NotificationUtils.downloadWithNotifications(future, tr("Area before removal"));
                    new DisplayWays().displayWaysToRemove(Int);
                } catch (InterruptedException | ExecutionException e1) {
                    Logging.error(e1);
                }
            });
        });
    }
}
