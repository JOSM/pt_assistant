package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;

/**
 * Adds or removes the tab for the linear relation
 */
public class LineRelationTabManager {
    private final JTabbedPane tabPanel;
    private final Supplier<Relation> relationGetter;
    private JScrollPane tabContent = null;



    public LineRelationTabManager(IRelationEditorActionAccess editorAccess) {
        Container editorComponent = ((JDialog) editorAccess.getEditor()).getContentPane();

        this.tabPanel = Stream.of(editorComponent.getComponents())
            .filter(it -> it instanceof JTabbedPane)
            .map(it -> (JTabbedPane) it)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Relation editor did not have a tab panel"));

        this.relationGetter = editorAccess.getEditor()::getRelation;

        updateTab();
        editorComponent.addPropertyChangeListener(RelationEditor.RELATION_PROP, __ -> updateTab());
        editorAccess.getMemberTableModel().addTableModelListener(__ -> updateTab());
        editorAccess.getTagModel().addPropertyChangeListener(__ -> updateTab());
    }

    private void updateTab() {
        Relation relation = relationGetter.get();
        PublicTransportLinePanel linePanel = PublicTransportLinePanel.forRelation(relation);
        boolean shouldDisplayTab = linePanel != null;

        if (shouldDisplayTab) {
            if (tabContent == null) {
                tabContent = new JScrollPane();
                tabPanel.add(tr("route"), tabContent);
            }

            tabContent.getViewport().setView(linePanel);
        } else {
            if (tabContent != null) {
                tabPanel.remove(tabContent);
                tabContent = null;
            }
        }
    }
}
