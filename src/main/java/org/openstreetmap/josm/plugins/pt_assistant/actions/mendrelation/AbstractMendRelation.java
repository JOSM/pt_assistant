package org.openstreetmap.josm.plugins.pt_assistant.actions.mendrelation;

import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;

/**
 * Abstract class that needs to be implements for developing mend relation action methods
 *
 * @author sudhanshu2
 */
public abstract class AbstractMendRelation extends AbstractRelationEditorAction {
    protected boolean shorterRoutes = false;

    protected AbstractMendRelation(IRelationEditorActionAccess editorAccess, IRelationEditorUpdateOn... updateOn) {
        super(editorAccess, updateOn);
    }
}
