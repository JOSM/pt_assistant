package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRelation;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.tools.ImageProvider;

public class LineGridRouteActions extends JPanel {
    public LineGridRouteActions(OsmDataLayer layer, LineRelation lineRelation) {
        final Relation originalRelation = lineRelation.getRelation().getRelation();
        JButton button = new JButton(new JosmAction("", new ImageProvider("dialogs", "edit"),
            tr("Open the route relation editor for this other route"), null, false, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogUtils.showRelationEditor(RelationEditor.getEditor(
                    layer,
                    originalRelation,
                    Collections.emptyList()
                ));
            }
        });
        button.setMargin(new Insets(0, 0, 0, 0));
        if (originalRelation == null) {
            button.setEnabled(false);
        }
        add(button, BorderLayout.WEST);
    }
}
