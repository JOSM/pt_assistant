package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Used on the top of the line to display an arrow that points to a column
 */
public class LineGridHorizontalColumnArrow extends JPanel  {
    public LineGridHorizontalColumnArrow() {
        setMinimumSize(new Dimension(20, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.GRAY);

        double height = getHeight();
        double cy = height / 2.0;
        double offsetX = 15; // How much the arrow is offsetted from the left side
        double roundness = 4.0;

        Path2D.Double path = new Path2D.Double();
        path.moveTo(offsetX - 4, height - 4);
        path.lineTo(offsetX, height + 1);
        path.lineTo(offsetX + 4, height - 4);
        path.moveTo(offsetX, height - 2);
        path.lineTo(offsetX, cy + roundness);
        path.quadTo(offsetX, cy, offsetX + roundness, cy);
        path.lineTo(getWidth(), cy);
        g2d.draw(path);
    }
}
