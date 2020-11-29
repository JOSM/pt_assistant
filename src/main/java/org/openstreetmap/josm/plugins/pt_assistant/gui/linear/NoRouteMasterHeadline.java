package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel.safeHtml;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.tools.GBC;

public class NoRouteMasterHeadline extends JPanel {
    public NoRouteMasterHeadline(RelationAccess relation, OsmDataLayer layer) {
        super(new BorderLayout());

        add(new JLabel(tr("This route is not in a route master relation or the route master was not downloaded.")));

        JPanel actions = new JPanel();
        actions.add(new JButton(new JosmAction(tr("Add to route master"), null, tr("Adds this route to a route master relation."), null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAddToRouteMaster();
            }

            private void startAddToRouteMaster() {
                ExtendedDialog dialog = new ExtendedDialog(
                    NoRouteMasterHeadline.this.getTopLevelAncestor(),
                    tr("Add to route master"), tr("Set master route"), tr("Cancel"));
                dialog.setButtonIcons("dialogs/add", "cancel");
                dialog.setDefaultButton(1);

                JPanel content = new JPanel();
                content.setLayout(new GridBagLayout());
                content.add(new JLabel("<html>"
                    // Make link localizable!
                    + tr("This adds the current route relation to a <a href=\"https://wiki.openstreetmap.org/wiki/Relation:route_master\">route master</a>.")
                    + "</html>"), GBC.std(0, 0).span(2).fill(GBC.HORIZONTAL));

                content.add(new JLabel("<html>"
                    + tr("Please ensure that you have downloaded possible master relations before doing this step.")
                    + "</html>"), GBC.std(0, 1).fill(GBC.HORIZONTAL));

                content.add(new JButton(new JosmAction(tr("Download possible relations"), "download", null, null, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(false);
                        DownloadUtils.downloadUsingOverpass(
                            "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadAllRouteMasters.query.txt",
                            line -> line
                                .replace("##RELATIONID##",
                                    relation.getRelation() != null ? relation.getRelation().getId() + "" : Long.MAX_VALUE + "")
                                .replace("##NODEIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.NODE))
                                .replace("##WAYIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.WAY))
                                .replace("##PTTYPE##",
                                    DownloadUtils.escape(relation.get(OSMTags.KEY_ROUTE))),
                            () -> startAddToRouteMaster()
                        );
                    }
                }), GBC.std(1, 1));

                JComboBox<Relation> comboBox = new JComboBox<>(layer.getDataSet()
                    .getRelations()
                    .stream()
                    .filter(PublicTransportLinePanel::isRouteMaster)
                    .sorted(Comparator.comparing(it -> it.get("ref")))
                    .toArray(Relation[]::new));

                comboBox.setRenderer(new ListCellRenderer<Relation>() {
                    @Override
                    public Component getListCellRendererComponent(JList<? extends Relation> list, Relation value,
                                                                  int index, boolean isSelected, boolean cellHasFocus) {
                        return new UnBoldLabel("<html>"
                          +  "<b><font bgcolor=\"" + safeHtml(value.get("colour")) + "\">" + safeHtml(value.get("ref")) + "</font></b> "
                            + safeHtml(value.get("name"))
                            + "<br/>"
                            + tr("Operator")
                            + ": "
                            + safeHtml(value.get("operator"))
                            + " "
                            + tr("Network")
                            + ": "
                            + safeHtml(value.get("network"))
                        + "</html>");
                    }
                });
                content.add(comboBox, GBC.std(0, 2).fill(GridBagConstraints.HORIZONTAL).span(2).insets(0, 10, 0, 0));

                content.setBorder(new EmptyBorder(10, 10, 10, 10));
                content.setMaximumSize(new Dimension(500, 800));
                content.setPreferredSize(new Dimension(500, 300));
                dialog.setContent(content);
                dialog.showDialog();

                if (dialog.getValue() != 1) {
                    // canceled
                    return;
                }

                System.out.println("Set to parent: " + (Relation) comboBox.getSelectedItem());
            }
        }));

        actions.add(new JButton(new JosmAction(tr("Download route"), "download", null, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadUtils.downloadUsingOverpass(
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadRoute.query.txt",
                    line -> line
                        .replace("##NODEIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.NODE))
                        .replace("##WAYIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.WAY))
                        .replace("##RELATIONIDS##", DownloadUtils.collectMemberIds(relation, OsmPrimitiveType.RELATION))
                        .replace("##RELATIONID##", relation.getRelation() != null ? relation.getRelation().getId() + "" : Long.MAX_VALUE + ""));
            }
        }));
        add(actions, BorderLayout.EAST);
    }
}
