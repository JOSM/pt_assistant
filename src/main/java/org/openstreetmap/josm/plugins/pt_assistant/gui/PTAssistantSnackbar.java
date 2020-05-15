// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

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
    public PTAssistantSnackbar(String message, long delay) {
        Font font = getFont().deriveFont(Font.PLAIN, 14.0f);
        JMultilineLabel snackBarLabel = new JMultilineLabel(tr(message));
        snackBarLabel.setFont(font);
        snackBarLabel.setForeground(Color.BLACK);

        JButton closeBtn = new JButton(ImageProvider.get("misc", "black_x"));

        // set button to transparent, rollover-able, and no border
        closeBtn.setContentAreaFilled(false);
        closeBtn.setRolloverEnabled(true);
        closeBtn.setBorderPainted(false);

        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText(tr("Close this message"));

        closeBtn.addActionListener(e -> {
            // todo : remove the message
        });

        setLayout(new GridBagLayout());
        add(snackBarLabel, GBC.std(1, 1).fill());
        add(closeBtn, GBC.std(2 , 1).span(1, 1).anchor(GBC.EAST));
        setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
        setBackground(new Color(224, 236, 249));

        MapFrame map = MainApplication.getMap();
        map.addTopPanel(this);
    }


}
