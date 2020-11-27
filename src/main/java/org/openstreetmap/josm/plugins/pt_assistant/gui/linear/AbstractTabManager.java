package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.awt.Container;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;

/**
 * Allows adding / removing a tab from the Relation window.
 */
public abstract class AbstractTabManager {
    private final JTabbedPane tabPanel;
    private final Supplier<Relation> relationGetter;
    private JScrollPane tabContent = null;
    private ChangeListener tabListener;

    public AbstractTabManager(IRelationEditorActionAccess editorAccess) {
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
        TabAndDisplay toShow = getTabToShow(relation);

        if (toShow.shouldDisplay()) {
            if (tabContent == null) {
                tabContent = new JScrollPane();
                tabPanel.add(toShow.getTitle(), tabContent);
                tabListener = e -> {
                    showIfVisible(toShow);
                };
                tabPanel.addChangeListener(tabListener);
            }
            this.tabContent.getViewport().setView(null);
            // This makes adding the component lazy => complex layouts don't need to be computed immediately
            showIfVisible(toShow);
        } else {
            if (tabContent != null) {
                tabPanel.remove(tabContent);
                tabPanel.removeChangeListener(tabListener);
                tabContent = null;
            }
        }
    }

    private void showIfVisible(TabAndDisplay toShow) {
        if (tabPanel.getSelectedComponent() == tabContent
                && this.tabContent.getViewport().getView() == null) {
            JPanel newContent = toShow.getTabContent();
            Objects.requireNonNull(newContent, "newContent");
            this.tabContent.getViewport().setView(newContent);
        }
    }

    protected abstract TabAndDisplay getTabToShow(Relation relation);

    public interface TabAndDisplay {
        boolean shouldDisplay();
        JPanel getTabContent();
        String getTitle();
    }
}
