package org.openstreetmap.josm.plugins.pt_assistant.gui.utils;

import java.awt.Dimension;

import javax.swing.JLabel;

public class UnBoldLabel extends JLabel {
    public UnBoldLabel(String text) {
        super(text);
        setHorizontalAlignment(LEFT);
        setFont(getFont().deriveFont(0));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, super.getMaximumSize().height);
    }

    public static String safeHtml(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

}
