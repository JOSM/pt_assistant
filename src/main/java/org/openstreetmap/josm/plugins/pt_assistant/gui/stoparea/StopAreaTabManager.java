package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractTabManager;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

public class StopAreaTabManager extends AbstractTabManager<StopAreaGroupPanel> {
    // Used so that zoom gets not lost when re-creating the view due to changes in the relation.
    private final ZoomSaver zoomSaver = new ZoomSaver();

    public StopAreaTabManager(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    @Override
    protected TabAndDisplay<StopAreaGroupPanel> getTabToShow(IRelationEditorActionAccess editorAccess) {
        return new TabAndDisplay<StopAreaGroupPanel>() {
            @Override
            public boolean shouldDisplay() {
                return StopUtils.isStopAreaGroup(RelationAccess.of(editorAccess));
            }

            @Override
            public StopAreaGroupPanel getTabContent() {
                return new StopAreaGroupPanel(editorAccess, zoomSaver);
            }

            @Override
            public String getTitle() {
                return tr("Areas");
            }
        };
    }

    @Override
    protected void dispose(StopAreaGroupPanel view) {
        view.dispose();
    }
}
