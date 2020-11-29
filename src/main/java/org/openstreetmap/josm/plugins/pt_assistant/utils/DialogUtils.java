// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;

public final class DialogUtils {
    private DialogUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Shows a warning dialog with only an "OK" option
     */
    public static void showOkWarning(final String title, final String message) {
        JOptionPane.showConfirmDialog(
            MainApplication.getMainFrame(),
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * @param title the window title of the displayed dialog
     * @param question the question that is displayed in the dialog
     * @return {@code true}, iff the user clicked yes. In all other cases, {@code false} is returned.
     */
    public static boolean showYesNoQuestion(final Component parentComponent, final String title, final String question) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            parentComponent,
            question,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
    }

    /**
     * Show or focus that relation editor
     * @param editor The editor
     */
    public static void showRelationEditor(RelationEditor editor) {
        if (editor.isVisible()) {
            EventQueue.invokeLater(() -> {
                editor.setAlwaysOnTop(true);
                editor.toFront();
                editor.requestFocus();
                editor.setAlwaysOnTop(false);
            });
        } else {
            editor.setVisible(true);
        }
    }
}
