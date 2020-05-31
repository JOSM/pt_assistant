package org.openstreetmap.josm.plugins.pt_assistant.actions;

import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.validation.PaintVisitor;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openstreetmap.josm.tools.I18n.tr;

public class PTMendRelationSupportClasses {

    public static class PTMendRelationPaintVisitor extends PaintVisitor {
        protected final String I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY;
        protected final String I18N_CLOSE_OPTIONS = I18n.marktr("Close the options");
        protected final String I18N_NOT_REMOVE_WAYS = I18n.marktr("Do not remove ways");
        protected final String I18N_REMOVE_CURRENT_EDGE = I18n.marktr("Remove current edge (white)");
        protected final String I18N_REMOVE_WAYS = I18n.marktr("Remove ways");
        protected final String I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY = I18n.marktr("Remove ways along with previous way");
        protected final String I18N_SKIP = I18n.marktr("Skip");
        protected final String I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS = I18n.marktr("solutions based on other route relations");
        protected final String I18N_TURN_BY_TURN_NEXT_INTERSECTION = I18n.marktr("turn-by-turn at next intersection");
        protected final String I18N_BACKTRACK_WHITE_EDGE = I18n.marktr("Split white edge");

        protected final Color[] FIVE_COLOR_PALETTE = {
            new Color(0, 255, 0, 150),
            new Color(255, 0, 0, 150),
            new Color(0, 0, 255, 150),
            new Color(255, 255, 0, 150),
            new Color(0, 255, 255, 150)
        };

        protected final Color CURRENT_WAY_COLOR = new Color(255, 255, 255, 190);
        protected final Color NEXT_WAY_COLOR = new Color(169, 169, 169, 210);

        protected static final Map<Character, Color> CHARACTER_COLOR_MAP = new HashMap<>();
        static {
            CHARACTER_COLOR_MAP.put('A', new Color(0, 255, 0, 200));
            CHARACTER_COLOR_MAP.put('B', new Color(255, 0, 0, 200));
            CHARACTER_COLOR_MAP.put('C', new Color(0, 0, 255, 200));
            CHARACTER_COLOR_MAP.put('D', new Color(255, 255, 0, 200));
            CHARACTER_COLOR_MAP.put('E', new Color(0, 255, 255, 200));
            CHARACTER_COLOR_MAP.put('1', new Color(0, 255, 0, 200));
            CHARACTER_COLOR_MAP.put('2', new Color(255, 0, 0, 200));
            CHARACTER_COLOR_MAP.put('3', new Color(0, 0, 255, 200));
            CHARACTER_COLOR_MAP.put('4', new Color(255, 255, 0, 200));
            CHARACTER_COLOR_MAP.put('5', new Color(0, 255, 255, 200));
        }


        /** The graphics */
        private final Graphics g;
        /** The MapView */
        private final MapView mv;
        private HashMap<Way, java.util.List<Character>> waysColoring;

        /**
         *
         * @param g
         * @param mv
         * @param PTorNot
         */
        public PTMendRelationPaintVisitor(Graphics2D g, MapView mv, boolean PTorNot) {
            super(g, mv);
            this.g = g;
            this.mv = mv;

            if (PTorNot) {
                I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY = I18n.marktr("Add oneway:bus=no to way");
            } else {
                I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY = I18n.marktr("Add oneway:bicycle=no to way");
            }
        }

        /*
         * Functions in this class are directly taken from PTAssistantPaintVisitor with
         * some slight modification
         */

        /**
         *
         */
        void drawVariants(boolean showOption0, boolean shorterRoutes, HashMap<Way, Character> wayColoring) {
            drawFixVariantsWithParallelLines(true, wayColoring);

            Color[] colors = { new Color(0, 255, 150), new Color(255, 0, 0, 150), new Color(0, 0, 255, 150),
                new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
            Character chr = 'A';
            if (numeric)
                chr = '1';

            if (showOption0 && numeric) {
                if (!shorterRoutes)
                    drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                        letterY, 25);
                else
                    drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                        letterY, 25);
                letterY = letterY + 60;
            } else if (showOption0) {
                if (!shorterRoutes)
                    drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                        letterY, 25);
                else
                    drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                        letterY, 25);
                letterY = letterY + 60;
            }

            for (int i = 0; i < 5; i++) {
                if (wayColoring.containsValue(chr)) {
                    drawFixVariantLetter(chr.toString(), colors[i], letterX, letterY, 35);
                    letterY = letterY + 60;
                }
                chr++;
            }

            /* display the "Esc", "Skip" label: */
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);

            letterY = letterY + 60;
            if (numeric) {
                drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("8 : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            } else {
                drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("V : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            }
        }

        /**
         *
         */
        public void drawOptionsToRemoveWays(String notice) {
            drawFixVariantsWithParallelLines(false);
            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            if (notice != null) {
                drawFixVariantLetter("Error:  " + notice, Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
            }
            if (numeric) {
                drawFixVariantLetter("1 : " + tr(I18N_REMOVE_WAYS), FIVE_COLOR_PALETTE[0], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("2 : " + tr(I18N_NOT_REMOVE_WAYS), FIVE_COLOR_PALETTE[1], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("3 : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), FIVE_COLOR_PALETTE[4], letterX,
                    letterY, 25);
                letterY = letterY + 60;
                if (notice.equals("vehicle travels against oneway restriction")) {
                    drawFixVariantLetter("4 : " + tr(I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY), FIVE_COLOR_PALETTE[3], letterX,
                        letterY, 25);
                }
            } else {
                drawFixVariantLetter("A : " + tr(I18N_REMOVE_WAYS), FIVE_COLOR_PALETTE[0], letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("B : " + tr(I18N_NOT_REMOVE_WAYS), FIVE_COLOR_PALETTE[1], letterX, letterY, 25);
                letterY = letterY + 60;
                if (notice.equals("vehicle travels against oneway restriction")) {
                    drawFixVariantLetter("C : " + tr(I18N_ADD_ONEWAY_VEHICLE_NO_TWO_WAY), FIVE_COLOR_PALETTE[3], letterX,
                        letterY, 25);
                    letterY = letterY + 60;
                }
                drawFixVariantLetter("R : " + tr(I18N_REMOVE_WAYS_WITH_PREVIOUS_WAY), FIVE_COLOR_PALETTE[4], letterX,
                    letterY, 25);
            }

            letterY = letterY + 60;
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 30);
        }

        /**
         *
         * @param fixVariants
         */
        void drawMultipleVariants(HashMap<Character, java.util.List<Way>> fixVariants, boolean showOption0, boolean shorterRoutes) {
            waysColoring = new HashMap<>();
            addFixVariants(fixVariants);
            drawFixVariantsWithParallelLines(waysColoring);

            int colorIndex = 0;

            double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
            double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

            boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

            if (showOption0 && numeric) {
                if (!shorterRoutes)
                    drawFixVariantLetter("0 : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                        letterY, 25);
                else
                    drawFixVariantLetter("0 : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                        letterY, 25);
                letterY = letterY + 60;
            } else if (showOption0) {
                if (!shorterRoutes)
                    drawFixVariantLetter("W : " + tr(I18N_TURN_BY_TURN_NEXT_INTERSECTION), Color.ORANGE, letterX,
                        letterY, 25);
                else
                    drawFixVariantLetter("W : " + tr(I18N_SOLUTIONS_BASED_ON_OTHER_RELATIONS), Color.PINK, letterX,
                        letterY, 25);
                letterY = letterY + 60;
            }

            for (Map.Entry<Character, java.util.List<Way>> entry : fixVariants.entrySet()) {
                Character c = entry.getKey();
                if (fixVariants.get(c) != null) {
                    drawFixVariantLetter(c.toString(), FIVE_COLOR_PALETTE[colorIndex % 5], letterX, letterY, 35);
                    colorIndex++;
                    letterY = letterY + 60;
                }
            }

            // display the "Esc", "Skip" label:
            drawFixVariantLetter("Esc : " + tr(I18N_CLOSE_OPTIONS), Color.WHITE, letterX, letterY, 25);
            letterY = letterY + 60;
            if (numeric) {
                drawFixVariantLetter("7 : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("8 : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("9 : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            } else {
                drawFixVariantLetter("S : " + tr(I18N_SKIP), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("V : " + tr(I18N_BACKTRACK_WHITE_EDGE), Color.WHITE, letterX, letterY, 25);
                letterY = letterY + 60;
                drawFixVariantLetter("Q : " + tr(I18N_REMOVE_CURRENT_EDGE), Color.WHITE, letterX, letterY, 25);
            }

        }

        /**
         *
         * @param drawNextWay
         */
        protected void drawFixVariantsWithParallelLines(boolean drawNextWay, HashMap<Way, Character> wayColoring) {
            wayColoring.entrySet().stream()
                // Create pairs of a color and an associated pair of nodes
                .flatMap(entry -> entry.getKey().getNodePairs(false).stream()
                    .map(it -> Pair.create(CHARACTER_COLOR_MAP.get(entry.getValue()), it)))
                // Grouping by color: groups stream into a map, each map entry has a color and all associated pairs of nodes
                .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toList())))
                .forEach((color, nodePairs) -> drawSegmentsWithParallelLines(nodePairs, color));
            drawSegmentsWithParallelLines(currentWay.getNodePairs(false), CURRENT_WAY_COLOR);
            if (drawNextWay) {
                drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);
            }
        }

        /**
         *
         * @param waysColoring
         */
        protected void drawFixVariantsWithParallelLines(Map<Way, java.util.List<Character>> waysColoring) {
            for (final Map.Entry<Way, java.util.List<Character>> entry : waysColoring.entrySet()) {
                final java.util.List<Color> wayColors = entry.getValue().stream().map(CHARACTER_COLOR_MAP::get)
                    .collect(Collectors.toList());
                for (final Pair<Node, Node> nodePair : entry.getKey().getNodePairs(false)) {
                    drawSegmentWithParallelLines(nodePair.a, nodePair.b, wayColors);
                }
            }

            drawSegmentsWithParallelLines(findCurrentEdge().stream().flatMap(it -> it.getNodePairs(false).stream())
                .collect(Collectors.toList()), CURRENT_WAY_COLOR);

            drawSegmentsWithParallelLines(nextWay.getNodePairs(false), NEXT_WAY_COLOR);

        }

        /**
         * Convenience method for {@link #drawSegmentWithParallelLines(Node, Node, java.util.List)}.
         */
        private void drawSegmentsWithParallelLines(java.util.List<Pair<Node, Node>> nodePairs, final Color color) {
            final java.util.List<Color> colorList = Collections.singletonList(color);
            nodePairs.forEach(it -> drawSegmentWithParallelLines(it.a, it.b, colorList));
        }

        /**
         *
         * @param n1
         * @param n2
         * @param colors
         */
        void drawSegmentWithParallelLines(Node n1, Node n2, java.util.List<Color> colors) {
            if (!n1.isDrawable() || !n2.isDrawable() || !isSegmentVisible(n1, n2)) {
                return;
            }

            Point p1 = mv.getPoint(n1);
            Point p2 = mv.getPoint(n2);
            double t = Math.atan2((double) p2.x - p1.x, (double) p2.y - p1.y);
            double cosT = 9 * Math.cos(t);
            double sinT = 9 * Math.sin(t);
            double heightCosT = 9 * Math.cos(t);
            double heightSinT = 9 * Math.sin(t);

            double prevPointX = p1.x;
            double prevPointY = p1.y;
            double nextPointX = p1.x + heightSinT;
            double nextPointY = p1.y + heightCosT;

            Color currentColor = colors.get(0);
            int i = 0;
            g.setColor(currentColor);
            g.fillOval(p1.x - 9, p1.y - 9, 18, 18);

            if (colors.size() == 1) {
                int[] xPoints = { (int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
                int[] yPoints = { (int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
                g.setColor(currentColor);
                g.fillPolygon(xPoints, yPoints, 4);
            } else if (colors.size() > 1) {
                boolean iterate = true;
                while (iterate) {
                    currentColor = colors.get(i % colors.size());

                    int[] xPoints = { (int) (prevPointX + cosT), (int) (nextPointX + cosT), (int) (nextPointX - cosT),
                        (int) (prevPointX - cosT) };
                    int[] yPoints = { (int) (prevPointY - sinT), (int) (nextPointY - sinT), (int) (nextPointY + sinT),
                        (int) (prevPointY + sinT) };
                    g.setColor(currentColor);
                    g.fillPolygon(xPoints, yPoints, 4);

                    prevPointX = prevPointX + heightSinT;
                    prevPointY = prevPointY + heightCosT;
                    nextPointX = nextPointX + heightSinT;
                    nextPointY = nextPointY + heightCosT;
                    i++;
                    if ((p1.x < p2.x && nextPointX >= p2.x) || (p1.x >= p2.x && nextPointX <= p2.x)) {
                        iterate = false;
                    }
                }

                int[] lastXPoints = { (int) (prevPointX + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT),
                    (int) (prevPointX - cosT) };
                int[] lastYPoints = { (int) (prevPointY - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT),
                    (int) (prevPointY + sinT) };
                g.setColor(currentColor);
                g.fillPolygon(lastXPoints, lastYPoints, 4);
            }

            g.setColor(currentColor);
            g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
        }

        /**
         *
         * @param fixVariants
         */
        void addFixVariants(HashMap<Character, java.util.List<Way>> fixVariants) {
            for (Map.Entry<Character, java.util.List<Way>> entry : fixVariants.entrySet()) {
                Character currentFixVariantLetter = entry.getKey();
                java.util.List<Way> fixVariant = entry.getValue();
                for (Way way : fixVariant) {
                    if (waysColoring.containsKey(way)) {
                        if (!waysColoring.get(way).contains(currentFixVariantLetter)) {
                            waysColoring.get(way).add(currentFixVariantLetter);
                        }
                    } else {
                        List<Character> letterList = new ArrayList<>();
                        letterList.add(currentFixVariantLetter);
                        waysColoring.put(way, letterList);
                    }
                }
            }
        }

        /**
         * Visualizes the letters for each fix variant
         * @param letter letter to draw
         * @param color text color
         * @param letterX X coordinate
         * @param letterY Y coordinate
         * @param size font size
         */
        private void drawFixVariantLetter(String letter, Color color, double letterX, double letterY, int size) {
            g.setColor(color);
            Font stringFont = new Font("SansSerif", Font.PLAIN, size);
            g.setFont(stringFont);
            try {
                g.drawString(letter, (int) letterX, (int) letterY);
                g.drawString(letter, (int) letterX, (int) letterY);
            } catch (NullPointerException ex) {
                // do nothing
                Logging.trace(ex);
            }

        }
    }

    /**
     *
     */
    protected static class PTMendRelationRemoveLayer extends AbstractMapViewPaintable {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            PTMendRelationSupportClasses.PTMendRelationPaintVisitor paintVisitor
                = new PTMendRelationSupportClasses.PTMendRelationPaintVisitor(g, mv);
            paintVisitor.drawOptionsToRemoveWays();
        }
    }

    protected class MendRelationAddLayer extends AbstractMapViewPaintable {

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            PTMendRelationPaintVisitor paintVisitor = new PTMendRelationPaintVisitor(g, mv);
            paintVisitor.drawVariants();
        }
    }
}
