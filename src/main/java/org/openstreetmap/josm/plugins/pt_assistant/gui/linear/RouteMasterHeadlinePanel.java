package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.gui.linear.UnBoldLabel.safeHtml;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.download.OverpassDownloadSource;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.tools.Utils;

public class RouteMasterHeadlinePanel extends JPanel {
    public RouteMasterHeadlinePanel(RelationAccess masterRelation) {
        setLayout(new BorderLayout());
        String color = masterRelation.get("colour");
            if (color == null) {
                color = "#888888";
            }
        String headline = MessageFormat.format("<font bgcolor=\"{0}\">{1}</font> {2}", safeHtml(color),
            safeHtml(masterRelation.get("ref")), safeHtml(masterRelation.get("name")));
        String infos = safeHtml(masterRelation.get("operator")) + " " + safeHtml(masterRelation.get("network"));
        String routeMasterText = MessageFormat.format("<html><h2>{0}</h2><div>{1}</div></html>", headline, infos);
        add(new UnBoldLabel(routeMasterText));

        add(new JButton(new JosmAction(tr("Download routes"), "download", null, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadUtils.downloadUsingOverpass(
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadRouteMaster.query.txt",
                    line -> line
                        .replace("##ROUTEIDS##", DownloadUtils.collectMemberIds(masterRelation, OsmPrimitiveType.RELATION))
                        .replace("##MASTERID##", masterRelation.getRelation() != null ? masterRelation.getRelation().getId() + "" : Long.MAX_VALUE + ""));
            }
        }), BorderLayout.EAST);
    }

}
