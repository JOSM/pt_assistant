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
}
