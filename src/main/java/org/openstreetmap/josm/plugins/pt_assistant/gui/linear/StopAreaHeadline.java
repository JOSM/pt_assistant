package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;

public class StopAreaHeadline extends JPanel {
    public StopAreaHeadline(RelationAccess relation) {
        super(new BorderLayout());
        String name = relation.get("name");
        String title = tr("Routes stopping at {0}", name == null ? "" : name);
        add(new UnBoldLabel("<html><h2>" + UnBoldLabel.safeHtml(title) + "</h2></html>"));

        add(new JButton(new JosmAction(tr("Download routes"), "download", null, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadUtils.downloadUsingOverpass(
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadStopArea.query.txt",
                    line -> line
                        .replace("##NODEIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.NODE))
                        .replace("##WAYIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.WAY)));
            }
        }), BorderLayout.EAST);
    }

}
