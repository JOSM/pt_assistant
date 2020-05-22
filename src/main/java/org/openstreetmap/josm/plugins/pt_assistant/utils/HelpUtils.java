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

/**
 * Help messages that are displayed on the bottom of the map
 *
 * @author sudhanshu2
 */
public class HelpUtils extends JPanel {

    private static final HelpUtils ClassInstance = new HelpUtils();
    private static final BooleanProperty doNotShow = new BooleanProperty("message.ptassistant.helputil", false);

    private HelpUtils() {
        /* Empty to prevent initialization */
    }

    public void addToJOSM(String message) {
        if (doNotShow.get() == Boolean.FALSE) {
            Font font = getFont().deriveFont(Font.PLAIN, 14.0f);
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
                removeSnackbar();
                if (doNotShowBox.isSelected()) {
                    doNotShow.put(Boolean.TRUE);
                }
            });

            setLayout(new GridBagLayout());
            add(snackBarLabel, GBC.std(1, 1).fill());
            add(closeBtn, GBC.std(2, 1).span(1, 1).anchor(GBC.EAST));
            setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
            setBackground(new Color(224, 236, 249));

            MapFrame map = MainApplication.getMap();
            map.addTopPanel(this);
        }
    }

    /**
     * Removes this snackbar instance
     */
    public void removeSnackbar() {
        MapFrame map = MainApplication.getMap();
        map.removeTopPanel(this.getClass());
    }
}
