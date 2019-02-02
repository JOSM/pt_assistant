// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sorts the members of a PT route. It orders first the ways, then the stops
 * according to the assigned ways
 *
 * @author giacomo
 *
 */
public class SortPTRouteMembersMenuBar extends JosmAction {

    /**
     * Creates a new SortPTRouteMembersAction
     */
    public SortPTRouteMembersMenuBar() {
        super(tr("Sort PT Route Members"), "icons/sortptroutemembers", tr("Sort PT Route Members"), null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Relation rel : getLayerManager().getEditDataSet().getSelectedRelations()) {
            if (rel.hasIncompleteMembers()) {
                if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
                        tr("The relation has incomplete members. Do you want to download them and continue with the sorting?"),
                        tr("Incomplete Members"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, null, null)) {

                    List<Relation> incomplete = Collections.singletonList(rel);
                    Future<?> future = MainApplication.worker.submit(new DownloadRelationMemberTask(
                            incomplete,
                            Utils.filteredCollection(
                                    DownloadSelectedIncompleteMembersAction.buildSetOfIncompleteMembers(
                                    Collections.singletonList(rel)), OsmPrimitive.class),
                            MainApplication.getLayerManager().getEditLayer()));

                    MainApplication.worker.submit(() -> {
                        try {
                            future.get();
                            continueAfterDownload(rel);
                        } catch (InterruptedException | ExecutionException e1) {
                            Logging.error(e1);
                        }
                    });
                } else {
                    return;
                }
            } else {
                continueAfterDownload(rel);
            }
        }
    }

    private void continueAfterDownload(Relation rel) {
        Relation newRel = new Relation(rel);
        SortPTRouteMembersAction.sortPTRouteMembers(newRel);
        UndoRedoHandler.getInstance().add(new ChangeCommand(rel, newRel));
    }

    @Override
    protected void updateEnabledState() {
        super.updateEnabledState();
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(final Collection<? extends OsmPrimitive> selection) {
        setEnabled(
            selection != null &&
            !selection.isEmpty() &&
            selection.stream().allMatch(it -> it instanceof Relation && RouteUtils.isPTRoute((Relation) it))
        );
    }
}
