package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;

/* TODO : https://github.com/JOSM/pt_assistant/pull/12#discussion_r428548822 */

/**
 * Help messages that are displayed on the bottom of the map
 *
 * @author sudhanshu2
 */
public class HelpUtils {
    private static final BooleanProperty doNotShow = new BooleanProperty("message.ptassistant.helputil", false);
    private static final JPanel currentPanel = new JPanel();

    private HelpUtils() {
        /* Empty to prevent initialization */
    }

    /**
     * Adds a help message on the top edge of the map window, persists and the message is updated if the UI element already exists
     * @param message the string to be displayed
     */
    public void addToJOSM(String message) {
        if (!doNotShow.get()) {
            removePanel();
            Font font = currentPanel.getFont().deriveFont(Font.PLAIN, 14.0f);
            JMultilineLabel snackBarLabel = new JMultilineLabel(message);
            snackBarLabel.setFont(font);
            snackBarLabel.setForeground(Color.BLACK);

            JCheckBox doNotShowBox = new JCheckBox(tr("Do not show tips"));
            doNotShowBox.setOpaque(false);
            doNotShowBox.setForeground(Color.BLACK);

            JButton closeBtn = new JButton(ImageProvider.get("misc", "black_x"));

            /* set button to transparent, rollover-able, and no border */
            closeBtn.setContentAreaFilled(false);
            closeBtn.setRolloverEnabled(true);
            closeBtn.setBorderPainted(false);

            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.setToolTipText(tr("Close this message"));

            closeBtn.addActionListener(e -> {
                removePanel();
                if (doNotShowBox.isSelected()) {
                    doNotShow.put(Boolean.TRUE);
                }
            });

            currentPanel.setLayout(new GridBagLayout());
            currentPanel.add(snackBarLabel, GBC.std(1, 1).fill());
            currentPanel.add(closeBtn, GBC.std(2, 1).span(1, 1).anchor(GBC.EAST));
            currentPanel.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
            currentPanel.setBackground(new Color(224, 236, 249));

            MapFrame map = MainApplication.getMap();
            map.addTopPanel(currentPanel);
        }
    }

    /**
     * Removes this message
     */
    public void removePanel() {
        MapFrame map = MainApplication.getMap();
        // map.removeTopPanel(currentPanel);
    }
}
