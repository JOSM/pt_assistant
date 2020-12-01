package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.StrokeBorder;

import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRefKey;

public class LineRefKeyPanel extends JPanel {
    public LineRefKeyPanel(LineRefKey lineRefKey) {
        super(new BorderLayout());
        setBorder(new CompoundBorder(
            new EmptyBorder(3, 3, 3, 3),
            new LineBorder(new Color(0x656565), 2)
        ));

        JLabel label = new JLabel(lineRefKey.getRef());
        label.setHorizontalAlignment(JLabel.CENTER);
        add(label);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(30, 25);
    }
}
