package org.openstreetmap.josm.plugins.pt_assistant.utils;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;

public class DialogUtils {
    private DialogUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Shows a warning dialog with only an "OK" option
     */
    public static int showOkWarning(final String title, final String message) {
        return JOptionPane.showConfirmDialog(
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
    public static boolean showYesNoQuestion(final String title, final String question) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            MainApplication.getMainFrame(),
            question,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
    }
}
