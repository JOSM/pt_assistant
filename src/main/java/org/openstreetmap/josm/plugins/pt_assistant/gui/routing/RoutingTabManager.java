package org.openstreetmap.josm.plugins.pt_assistant.gui.routing;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractTabManager;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;

public class RoutingTabManager extends AbstractTabManager<RoutingPanel> {
    private final ZoomSaver zoomSaver = new ZoomSaver();

    public RoutingTabManager(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    @Override
    protected TabAndDisplay<RoutingPanel> getTabToShow(IRelationEditorActionAccess editorAccess) {
        return new TabAndDisplay<RoutingPanel>() {
            @Override
            public boolean shouldDisplay() {
                return RelationAccess.of(editorAccess).hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.KEY_ROUTE);
            }

            @Override
            public RoutingPanel getTabContent() {
                return new RoutingPanel(editorAccess, zoomSaver);
            }

            @Override
            public String getTitle() {
                return tr("Map");
            }
        };
    }
}
