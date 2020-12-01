package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.RepaintManager;

public class Rotate90DegreesPanel extends JPanel {
    public Rotate90DegreesPanel(Component child) {
        super(new BorderLayout());
        add(child);
    }

    @Override
    protected void paintChildren(Graphics g) {
        synchronized(getTreeLock()) {
            for (int i = 0; i < getComponentCount(); i++) {
                Component component = getComponent(i);
                Graphics2D g2 = (Graphics2D) g.create(0, 0, getWidth(), getHeight());
                g2.setColor(component.getForeground());
                g2.setBackground(component.getBackground());

                g2.rotate(-Math.PI / 2);
                g2.translate(-getHeight(), 0);
                g2.setClip(0, 0, getHeight(), getWidth());

                Rectangle clipBounds = g.getClipBounds();
                if (clipBounds == null) {
                    clipBounds = new Rectangle(0, 0, getWidth(),
                        getHeight());
                }
                //noinspection SuspiciousNameCombination
                Rectangle rect = new Rectangle(getHeight() - clipBounds.y - clipBounds.height,
                    clipBounds.x, clipBounds.height, clipBounds.width);
                g2.setClip(rect);

                // Need to disable repaint manager => it would trac the paint rect
                RepaintManager cm = RepaintManager.currentManager(this);
                RepaintManager.setCurrentManager(null);
                component.paint(g2);
                RepaintManager.setCurrentManager(cm);
            }
        }
    }

    @Override
    public void doLayout() {
        for (int i = 0; i < getComponentCount(); i++) {
            getComponent(i).setSize(rot(getSize()));
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return rot(super.getMinimumSize());
    }

    @Override
    public Dimension getPreferredSize() {
        return rot(super.getPreferredSize());
    }

    @Override
    public Dimension getMaximumSize() {
        return rot(super.getMaximumSize());
    }

    private Dimension rot(Dimension size) {
        //noinspection SuspiciousNameCombination
        return new Dimension(size.height, size.width);
    }
}
