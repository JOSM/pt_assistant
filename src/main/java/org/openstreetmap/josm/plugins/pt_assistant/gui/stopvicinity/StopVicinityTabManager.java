package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.AbstractTabManager;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class StopVicinityTabManager extends AbstractTabManager {
    private final IRelationEditorActionAccess editorAccess;
    // Used so that zoom gets not lost when re-creating the view due to changes in the relation.
    private final ZoomSaver zoomSaver = new ZoomSaver();

    public StopVicinityTabManager(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        this.editorAccess = editorAccess;
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
                return new StopVicinityPanel(relation, editorAccess, zoomSaver);
            }

            @Override
            public String getTitle() {
                return tr("Vicinity");
            }
        };
    }
}
