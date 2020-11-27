package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.AbstractTabManager;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class StopVicinityTabManager extends AbstractTabManager {
    public StopVicinityTabManager(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    @Override
    protected TabAndDisplay getTabToShow(Relation relation) {
        return new TabAndDisplay() {
            @Override
            public boolean shouldDisplay() {
                return StopUtils.isStopArea(relation);
            }

            @Override
            public JPanel getTabContent() {
                return new StopVicinityPanel(relation);
            }

            @Override
            public String getTitle() {
                return tr("Vicinity");
            }
        };
    }
}
