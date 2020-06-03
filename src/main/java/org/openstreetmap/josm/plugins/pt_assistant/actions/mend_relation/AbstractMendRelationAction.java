// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;

public abstract class AbstractMendRelationAction extends AbstractRelationEditorAction {

    protected AbstractMendRelationAction(IRelationEditorActionAccess editorAccess, String addOneWay) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION));
    }
}
