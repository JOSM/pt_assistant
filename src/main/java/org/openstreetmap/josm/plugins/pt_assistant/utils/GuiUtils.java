package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class GuiUtils {
    private GuiUtils() {
        // Private constructor to avoid instantiation
    }

    public static JPanel createJPanel(final FlowLayout layout, final Component... components) {
        final JPanel panel = createJPanel(layout);
        for (final Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    public static JPanel createJPanel(final LayoutManager layoutManager) {
        final JPanel panel = new JPanel(layoutManager);
        panel.setOpaque(false);
        return panel;
    }

    public static JButton createJButton(final String text, final Runnable action) {
        return createJButton(text, __ -> action.run());
    }

    public static JButton createJButton(final String text, final ActionListener action) {
        return new JButton(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        });
    }

    public static JLabel createJLabel() {
        return createJLabel(false);
    }

    public static JLabel createJLabel(final boolean bold) {
        final JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(bold ? label.getFont().getStyle() | Font.BOLD : label.getFont().getStyle() & ~Font.BOLD));
        return label;
    }
}
