package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel.safeHtml;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.utils.ColorPalette;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.tools.ColorHelper;

public class RouteMasterHeadlinePanel extends JPanel {
    public RouteMasterHeadlinePanel(RelationAccess masterRelation) {
        setLayout(new BorderLayout());
        String color = masterRelation.get("colour");
        if (color == null) {
            color = "#666666";
        }
        String font = ColorHelper.color2html(ColorPalette.fontColor(ColorHelper.html2color(color)));
        String headline = MessageFormat.format("<font bgcolor=\"{0}\" color=\"{1}\">{2}</font> {3}",
            safeHtml(color), font,
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
