package org.openstreetmap.josm.plugins.pt_assistant.gui.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;

/**
 * Allows adding / removing a tab from the Relation window.
 */
public abstract class AbstractTabManager<T extends Component> {
    private final JTabbedPane tabPanel;
    private final IRelationEditorActionAccess editorAccess;
    private JScrollPane tabContent = null;
    private ChangeListener tabListener;

    public AbstractTabManager(IRelationEditorActionAccess editorAccess) {
        JDialog dialog = (JDialog) editorAccess.getEditor();
        Container editorComponent = dialog.getContentPane();

        this.tabPanel = Stream.of(editorComponent.getComponents())
            .filter(it -> it instanceof JTabbedPane)
            .map(it -> (JTabbedPane) it)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Relation editor did not have a tab panel"));

        this.editorAccess = editorAccess;

        updateTab();
        editorComponent.addPropertyChangeListener(RelationEditor.RELATION_PROP, __ -> updateTab());
        editorAccess.getMemberTableModel().addTableModelListener(__ -> updateTab());
        editorAccess.getTagModel().addPropertyChangeListener(__ -> updateTab());
        editorAccess.getTagModel().addTableModelListener(__ -> updateTab());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                possiblyDispose();
            }
        });
    }

    private void updateTab() {
        TabAndDisplay<T> toShow = getTabToShow(editorAccess);

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

    private void showIfVisible(TabAndDisplay<T> toShow) {
        possiblyDispose();
        if (tabPanel.getSelectedComponent() == tabContent
                && this.tabContent.getViewport().getView() == null) {
            T newContent = toShow.getTabContent();
            Objects.requireNonNull(newContent, "newContent");
            this.tabContent.getViewport().setView(newContent);
        }
    }

    @SuppressWarnings("unchecked")
    private void possiblyDispose() {
        if (this.tabContent != null) {
            JViewport viewport = this.tabContent.getViewport();
            if (viewport.getView() != null) {
                dispose((T) viewport.getView());
                viewport.setView(null);
            }
        }
    }

    protected void dispose(T view) {
        // Nop
    }

    protected abstract TabAndDisplay<T> getTabToShow(IRelationEditorActionAccess editorAccess);

    public interface TabAndDisplay<T extends Component> {
        boolean shouldDisplay();
        T getTabContent();
        String getTitle();
    }
}
