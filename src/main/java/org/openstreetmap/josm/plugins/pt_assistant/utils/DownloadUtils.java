package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.tools.Utils;

public class DownloadUtils {
    private DownloadUtils(){
    }

    public static void downloadUsingOverpass(String resource, Function<String, String> replacements) {
        downloadUsingOverpass(resource, replacements, () -> {});
    }
    public static void downloadUsingOverpass(String resource, Function<String, String> replacements, Runnable onFinish) {
        String query = new BufferedReader(new InputStreamReader(
            Utils.getResourceAsStream(DownloadUtils.class, resource), StandardCharsets.UTF_8))
            .lines()
            .map(replacements)
            .collect(Collectors.joining("\n"));


        Bounds area = new Bounds(0, 0, 0, 0);
        DownloadOsmTask task = new DownloadOsmTask();
        task.setZoomAfterDownload(false);
        task.download(
            new OverpassDownloadReader(area, OverpassDownloadReader.OVERPASS_SERVER.get(), query),
            new DownloadParams().withNewLayer(false), area, new PleaseWaitProgressMonitor(tr("Downloading data from overpass.")) {
                @Override
                public void doFinishTask() {
                    onFinish.run();
                }
            });
    }

    public static String collectMemberIds(RelationAccess relation, OsmPrimitiveType type) {
        // No need to filter => We can filter on overpass side.
        String ids = relation.getMembers()
            .stream()
            .map(RelationMember::getMember)
            .filter(it -> it.getType() == type)
            .filter(it -> !it.isNew())
            .map(it -> "" + it.getId())
            .collect(Collectors.joining(","));
        return ids.isEmpty() ? Long.MAX_VALUE + "": ids; // < a fake id, since Overpass requires at least one item
    }

    public static CharSequence escape(String s) {
        // TODO
        return s.replace("\"", "");
    }
}
