package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;

import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops.EntryExit;

/**
 * A cell in the line grid.
 * Displays a stop, one or multiple lines and other symbols
 */
public class LineGridCell extends JPanel {
    private static final int COL_WIDTH = 20;
    private List<Consumer<Graphics2D>> toPaint = new ArrayList<>();
    private int colCount = 0;
    private Color color;

    public LineGridCell(Color color) {
        this.color = color;
    }

    public void addDownwardsStop(int colIndex, EntryExit entryExit) {
        addStop(colIndex, entryExit, 1);
    }
    public void addUpwardsStop(int colIndex, EntryExit entryExit) {
        addStop(colIndex, entryExit, -1);
    }
    private void addStop(int colIndex, EntryExit entryExit, int dir) {
        addToPaint(colIndex, true, (graphics2D, xMin, xMax, yMin, yMax) -> {
            double cx = (xMax + xMin) / 2.0;
            double cy = (yMax + yMin) / 2.0;
            Path2D tri = new Path2D.Double();
            tri.moveTo(cx - 8 * dir, cy - 3 * dir);
            tri.lineTo(cx + 8 * dir, cy - 3 * dir);
            tri.lineTo(cx, cy + 5 * dir);
            tri.closePath();
            graphics2D.setColor(Color.WHITE);
            graphics2D.fill(tri);
            graphics2D.setStroke(new BasicStroke(2));
            graphics2D.setColor(entryExit == EntryExit.ENTRY ? Color.GREEN
                : entryExit == EntryExit.EXIT ? Color.RED : Color.BLACK);
            graphics2D.draw(tri);
        });
    }

    public void addToPaint(int colIndex, boolean fg, SymbolToPaint symbol) {
        toPaint.add(fg ? toPaint.size() : 0, graphics2D -> symbol.paintAt(graphics2D, COL_WIDTH * colIndex, COL_WIDTH * (colIndex + 1),
            0, getHeight()));
        colCount = Math.max(colIndex + 1, colCount);
    }

    public void addDownwardsU(int col) {
        addU(col, true);
    }
    public void addUpwardsU(int col) {
        addU(col, false);
    }

    private void addU(int col, boolean downwards) {
        addToPaint(col, false, (graphics2D, xMin, xMax, yMin, yMax) -> {
            int x1 = (xMin + xMax) / 2;
            int x2 = (3 * xMax - xMin) / 2;
            int cy = (yMax + yMin) / 2;
            int y = downwards ? yMax : yMin;
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x1, y);
            path.quadTo(x1, cy, xMax, cy);
            path.quadTo(x2, cy, x2, y);
            setLinePaint(graphics2D);
            graphics2D.draw(path);
        });
    }

    public void addDownwardsConnection(int col) {
        addConnection(col, 0.5, 1);
    }

    public void addUpwardsConnection(int col) {
        addConnection(col, 0, 0.5);
    }

    public void addThroughConnection(int col) {
        addConnection(col, 0, 1);
    }

    private void addConnection(int col, double start, double end) {
        addToPaint(col, false, (graphics2D, xMin, xMax, yMin, yMax) -> {
            setLinePaint(graphics2D);
            int cx = (xMax + xMin) / 2;
            graphics2D.drawLine(cx, (int) (yMin * (1 - start) + yMax * start),
                cx, (int) (yMin * (1 - end) + yMax * end));
        });
    }

    public void addContinuityUp(int colIndex) {
        addContinuity(colIndex, false);
    }

    public void addContinuityDown(int colIndex) {
        addContinuity(colIndex, true);
    }

    private void addContinuity(int colIndex, boolean down) {
        addToPaint(colIndex, false, (graphics2D, xMin, xMax, yMin, yMax) -> {
            setLinePaint(graphics2D);
            Path2D.Double path = new Path2D.Double();
            int y = down ? yMin : yMax;
            int cx = (xMax + xMin) / 2;
            path.moveTo(cx - 5, y);
            path.lineTo(cx + 5, y);
            path.lineTo(cx, y + 8 * (down ? 1 : -1));
            path.closePath();
            graphics2D.fill(path);
        });
    }

    private void setLinePaint(Graphics2D graphics2D) {
        graphics2D.setStroke(new BasicStroke(4));
        graphics2D.setColor(color);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(COL_WIDTH * colCount, toPaint.isEmpty() ? 0 : 20);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        toPaint.forEach(it -> it.accept((Graphics2D) g));
    }

    private interface SymbolToPaint {
        void paintAt(Graphics2D graphics2D, int xMin, int xMax, int yMin, int yMax);
    }
}
