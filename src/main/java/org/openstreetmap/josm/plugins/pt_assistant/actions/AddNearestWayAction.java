package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.tools.ImageProvider;

public class AddNearestWayAction extends AbstractRelationEditorAction {

    private Relation relation = null;
    private GenericRelationEditor editor = null;
    boolean setEnable = true;

    public AddNearestWayAction(IRelationEditorActionAccess editorAccess){
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(SHORT_DESCRIPTION, tr("Add Nearest Way"));
        new ImageProvider("dialogs/relation", "routing_assistance.svg").getResource().attachImageIcon(this, true);
        updateEnabledState();
        editor = (GenericRelationEditor) editorAccess.getEditor();
        this.relation = editor.getRelation();
        editor.addWindowListener(new RoutingAction.WindowEventHandler());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Relation newRel = new Relation(editor.getRelation());
        editor.apply();

        for (RelationMember rm : newRel.getMembers()) {
            if (rm.getType() == OsmPrimitiveType.WAY && rm.getRole().equals("stop")) {
                DataSet data = rm.getNode().getDataSet();
                Collection<Way> ways = data.getWays();

                // add each way that is adjacent and not already in the relation
                ways.stream()
                    .filter(w -> w.containsNode(rm.getNode()) && !newRel.getMembers().contains(w))
                    .forEach(w -> newRel.addMember(new RelationMember("way", w)));
            }
        }
        UndoRedoHandler.getInstance().add(new ChangeCommand(editor.getRelation(), newRel));
        editor.reloadDataFromRelation();
    }
}
