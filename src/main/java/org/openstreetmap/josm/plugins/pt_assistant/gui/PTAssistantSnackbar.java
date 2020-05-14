// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Snackbars (or useful messages) that are displayed on the bottom of the map
 *
 * @author sudhanshu2
 */
public class PTAssistantSnackbar extends JPanel {

    /**
     * Creates a snackbar
     *
     * @param message the plain text string to be displayed
     * @param delay time the message should be displayed for, set it to -1 for infinite (persist message)
     */
    protected void showSnackbar(String message, long delay) {
        Font font = getFont().deriveFont(Font.PLAIN, 14.0f);
        JMultilineLabel snackBarLabel = new JMultilineLabel(tr(message));
        snackBarLabel.setFont(font);
        snackBarLabel.setForeground(Color.BLACK);

        // todo : replace the text with the close icon
        JButton closeButton = new JButton("x");

        // set button to transparent, rollover-able, and no border
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setBorderPainted(false);

        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText(tr("Close this message"));

        closeButton.addActionListener(e -> {
            // todo : remove the message
        });
    }
}
