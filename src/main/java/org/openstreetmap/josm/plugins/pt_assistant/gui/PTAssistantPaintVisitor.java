// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.layer.validation.PaintVisitor;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTWay;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Visits the primitives to be visualized in the pt_assistant layer
 *
 * @author darya
 *
 */
public class PTAssistantPaintVisitor extends PaintVisitor {
    private static final String[] PUBLIC_TRANSPORT_NODE_ROLES = {"stop", "stop_entry_only", "stop_exit_only",
            "platform", "platform_entry_only", "platform_exit_only"};
    /** The graphics */
    private final Graphics g;
    /** The MapView */
    private final MapView mv;

    /**
     * Constructor
     *
     * @param g
     *            graphics
     * @param mv
     *            map view
     */
    public PTAssistantPaintVisitor(Graphics g, MapView mv) {
        super((Graphics2D) g, mv);
        this.g = g;
        this.mv = mv;
    }

    @Override
    public void visit(Relation r) {
        if (RouteUtils.isBicycleRoute(r) || RouteUtils.isFootRoute(r) || RouteUtils.isHorseRoute(r)) {
            drawCycleRoute(r);
            return;
        }
        List<RelationMember> rmList = new ArrayList<>();
        List<RelationMember> revisitedWayList = new ArrayList<>();
        // first, draw primitives:
        for (RelationMember rm : r.getMembers()) {

            if (PTStop.isPTStopPosition(rm)) {
                drawStop(rm.getMember(), true);
            } else if (PTStop.isPTPlatform(rm)) {
                drawStop(rm.getMember(), false);
            } else if (RouteUtils.isPTWay(rm)) {
                if (rm.isWay()) {
                    if (rmList.contains(rm)) {
                        if (!revisitedWayList.contains(rm)) {
                            visit(rm.getWay(), true);
                            revisitedWayList.add(rm);
                        }
                    } else {
                        visit(rm.getWay(), false);
                    }
                } else if (rm.isRelation()) {
                    visit(rm.getRelation());
                }
                rmList.add(rm);
            }
        }

        // show refs for all the relations in the layer
        showRefs(r);
    }

    private void showRefs(Relation r) {
        // in the end, draw labels of the given route r:
        HashMap<Long, String> stopOrderMap = new HashMap<>();
        int stopCount = 1;

        for (RelationMember rm : r.getMembers()) {
            if (PTStop.isPTStop(rm)
                    || (rm.getMember().isIncomplete() && (rm.isNode() || rm.hasRole(PUBLIC_TRANSPORT_NODE_ROLES)))) {

                StringBuilder sb = new StringBuilder();

                final Integer innerCount = stopCount;
                stopOrderMap.computeIfPresent(rm.getUniqueId(),
                        (x, y) -> sb.append(y).append(";").append(innerCount).toString());
                stopOrderMap.computeIfAbsent(rm.getUniqueId(), y -> sb
                        .append(r.hasKey("ref") ? r.get("ref") : (r.hasKey("name") ? r.get("name") : I18n.tr("NA")))
                        .append("-").append(innerCount).toString());

                if (PTStop.isPTStopPosition(rm)) {
                    drawStopLabelWithRefsOfRoutes(rm.getMember(), sb.toString(), false);
                } else if (PTStop.isPTPlatform(rm)) {
                    drawStopLabelWithRefsOfRoutes(rm.getMember(), sb.toString(), true);
                }

                stopCount++;
            }
        }

        // show the magenta ref value of all the routes

        Collection<Relation> allRelations = null;
        if (MainApplication.getLayerManager().getEditLayer() != null
                && MainApplication.getLayerManager().getEditLayer().getDataSet() != null)
            allRelations = MainApplication.getLayerManager().getEditLayer().getDataSet().getRelations();
        double scale = MainApplication.getMap().mapView.getScale();

        if (allRelations != null && scale < 0.7) {
            for (Relation rel : allRelations) {
                for (RelationMember rm : rel.getMembers()) {
                    if (PTStop.isPTStop(rm) || (rm.getMember().isIncomplete()
                            && (rm.isNode() || rm.hasRole(PUBLIC_TRANSPORT_NODE_ROLES)))) {

                        if (PTStop.isPTStopPosition(rm)) {
                            drawStopLabel(rm.getMember());
                        } else if (PTStop.isPTPlatform(rm)) {
                            drawStopLabel(rm.getMember());
                        }
                    }

                }
            }
        }
    }

    private void drawCycleRoute(Relation r) {

        List<RelationMember> members = new ArrayList<>(r.getMembers());
        members.removeIf(m -> !m.isWay());
        WayConnectionTypeCalculator connectionTypeCalculator = new WayConnectionTypeCalculator();
        List<WayConnectionType> links = connectionTypeCalculator.updateLinks(members);

        for (int i = 0; i < links.size(); i++) {
            WayConnectionType link = links.get(i);
            Way way = members.get(i).getWay();
            if (!link.isOnewayLoopForwardPart && !link.isOnewayLoopBackwardPart) {
                if (way.isSelected()) {
                    drawWay(way, new Color(0, 255, 0, 100));
                } else {
                    drawWay(way, new Color(0, 255, 255, 100));
                }
            } else if (link.isOnewayLoopForwardPart) {
                if (way.isSelected()) {
                    drawWay(way, new Color(255, 255, 0, 100));
                } else {
                    drawWay(way, new Color(255, 0, 0, 100));
                }
            } else {
                if (way.isSelected()) {
                    drawWay(way, new Color(128, 0, 128, 100));
                } else {
                    drawWay(way, new Color(0, 0, 255, 100));
                }
            }
        }
    }

    private void drawWay(Way way, Color color) {
        List<Node> nodes = way.getNodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            drawSegment(nodes.get(i), nodes.get(i + 1), color, 1, false);
        }
    }

    public void visit(Way w, boolean revisit) {
        if (w == null) {
            return;
        }

        /*-
         * oneway values:
         * 0 two-way street
         * 1 oneway street in the way's direction
         * 2 oneway street in ways's direction but public transport allowed
         * -1 oneway street in reverse direction
         * -2 oneway street in reverse direction but public transport allowed
         */
        int oneway = 0;

        if (w.hasTag("junction", "roundabout") || w.hasTag("highway", "motorway")) {
            oneway = 1;
        } else if (w.hasTag("oneway", "1") || w.hasTag("oneway", "yes") || w.hasTag("oneway", "true")) {
            if (w.hasTag("busway", "lane") || w.hasTag("busway:left", "lane") || w.hasTag("busway:right", "lane")
                    || w.hasTag("oneway:bus", "no") || w.hasTag("busway", "opposite_lane")
                    || w.hasTag("oneway:psv", "no") || w.hasTag("trolley_wire", "backward")) {
                oneway = 2;
            } else {
                oneway = 1;
            }
        } else if (w.hasTag("oneway", "-1") || w.hasTag("oneway", "reverse")) {
            if (w.hasTag("busway", "lane") || w.hasTag("busway:left", "lane") || w.hasTag("busway:right", "lane")
                    || w.hasTag("oneway:bus", "no") || w.hasTag("busway", "opposite_lane")
                    || w.hasTag("oneway:psv", "no") || w.hasTag("trolley_wire", "backward")) {
                oneway = -2;
            } else {
                oneway = -1;
            }
        }

        visit(w.getNodes(), oneway, revisit);

    }

    /**
     * Variation of the visit method that allows a special visualization of oneway
     * roads
     *
     * @param nodes
     *            nodes
     * @param oneway
     *            oneway
     * @param revisit if true, draw 3 separate lines
     */
    public void visit(List<Node> nodes, int oneway, boolean revisit) {
        Node lastN = null;
        for (Node n : nodes) {
            if (lastN == null) {
                lastN = n;
                continue;
            }
            if (!revisit)
                drawSegment(lastN, n, new Color(128, 0, 128, 100), oneway, revisit);
            else
                drawSegment(lastN, n, new Color(0, 0, 0, 180), oneway, revisit);
            lastN = n;
        }
    }

    /**
     * Draw a small rectangle. White if selected (as always) or red otherwise.
     *
     * @param n
     *            The node to draw.
     */
    @Override
    public void visit(Node n) {
        if (n.isDrawable() && isNodeVisible(n)) {
            drawNode(n, Color.BLUE);
        }
    }

    /**
     * Draws a line around the segment
     *
     * @param n1
     *            The first node of segment
     * @param n2
     *            The second node of segment
     * @param color
     *            The color
     * @param oneway oneway values:<ul>
     *   <li>0 two-way street</li>
     *   <li>1 oneway street in the way's direction</li>
     *   <li>2 oneway street in ways's direction but public transport allowed</li>
     *   <li>-1 oneway street in reverse direction</li>
     *   <li>-2 oneway street in reverse direction but public transport allowed</li>
     *   </ul>
     * @param revisit if true, draw 3 separate lines
     */
    protected void drawSegment(Node n1, Node n2, Color color, int oneway, boolean revisit) {
        if (n1.isDrawable() && n2.isDrawable() && isSegmentVisible(n1, n2)) {
            try {
                drawSegment(mv.getPoint(n1), mv.getPoint(n2), color, oneway, revisit);
            } catch (NullPointerException ex) {
                // do nothing
                Logging.trace(ex);
            }

        }
    }

    /**
     * Draws a line around the segment
     *
     * @param p1
     *            The first point of segment
     * @param p2
     *            The second point of segment
     * @param color
     *            The color
     * @param oneway oneway values:<ul>
     *   <li>0 two-way street</li>
     *   <li>1 oneway street in the way's direction</li>
     *   <li>2 oneway street in ways's direction but public transport allowed</li>
     *   <li>-1 oneway street in reverse direction</li>
     *   <li>-2 oneway street in reverse direction but public transport allowed</li>
     *   </ul>
     * @param revisit if true, draw 3 separate lines
     */
    protected void drawSegment(Point p1, Point p2, Color color, int oneway, boolean revisit) {

        double t = Math.atan2((double) p2.x - p1.x, (double) p2.y - p1.y);
        double cosT = 9 * Math.cos(t);
        double sinT = 9 * Math.sin(t);

        if (revisit) {
            // draw 3 separate lines
            g.setColor(new Color(0, 0, 0, 140));
            int[] xPointsMiddle = {(int) (p1.x + 0.3 * cosT), (int) (p2.x + 0.3 * cosT), (int) (p2.x - 0.3 * cosT),
                    (int) (p1.x - 0.3 * cosT) };
            int[] yPointsMiddle = {(int) (p1.y - 0.3 * sinT), (int) (p2.y - 0.3 * sinT), (int) (p2.y + 0.3 * sinT),
                    (int) (p1.y + 0.3 * sinT) };
            g.fillPolygon(xPointsMiddle, yPointsMiddle, 4);

            g.setColor(color);

            int[] xPointsBottom = {(int) (p1.x - cosT + 0.2 * cosT), (int) (p2.x - cosT + 0.2 * cosT),
                    (int) (p2.x - 1.3 * cosT), (int) (p1.x - 1.3 * cosT) };
            int[] yPointsBottom = {(int) (p1.y + sinT - 0.2 * sinT), (int) (p2.y + sinT - 0.2 * sinT),
                    (int) (p2.y + 1.3 * sinT), (int) (p1.y + 1.3 * sinT) };
            g.fillPolygon(xPointsBottom, yPointsBottom, 4);

            int[] xPointsTop = {(int) (p1.x + 1.3 * cosT), (int) (p2.x + 1.3 * cosT), (int) (p2.x + cosT - 0.2 * cosT),
                    (int) (p1.x + cosT - 0.2 * cosT) };
            int[] yPointsTop = {(int) (p1.y - 1.3 * sinT), (int) (p2.y - 1.3 * sinT), (int) (p2.y - sinT + 0.2 * sinT),
                    (int) (p1.y - sinT + 0.2 * sinT) };
            g.fillPolygon(xPointsTop, yPointsTop, 4);

        } else {
            int[] xPoints = {(int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
            int[] yPoints = {(int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
            g.setColor(color);
            g.fillPolygon(xPoints, yPoints, 4);
            g.fillOval(p1.x - 9, p1.y - 9, 18, 18);
            g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
        }

        if (oneway != 0) {
            double middleX = (p1.x + p2.x) / 2.0;
            double middleY = (p1.y + p2.y) / 2.0;
            double cosTriangle = 6 * Math.cos(t);
            double sinTriangle = 6 * Math.sin(t);
            g.setColor(Color.WHITE);

            if (oneway > 0) {
                int[] xFillTriangle = {(int) (middleX + cosTriangle), (int) (middleX - cosTriangle),
                        (int) (middleX + 2 * sinTriangle) };
                int[] yFillTriangle = {(int) (middleY - sinTriangle), (int) (middleY + sinTriangle),
                        (int) (middleY + 2 * cosTriangle) };
                g.fillPolygon(xFillTriangle, yFillTriangle, 3);

                if (oneway == 2) {
                    int[] xDrawTriangle = {(int) (middleX + cosTriangle), (int) (middleX - cosTriangle),
                            (int) (middleX - 2 * sinTriangle) };
                    int[] yDrawTriangle = {(int) (middleY - sinTriangle), (int) (middleY + sinTriangle),
                            (int) (middleY - 2 * cosTriangle) };
                    g.drawPolygon(xDrawTriangle, yDrawTriangle, 3);
                }
            }

            if (oneway < 0) {
                int[] xFillTriangle = {(int) (middleX + cosTriangle), (int) (middleX - cosTriangle),
                        (int) (middleX - 2 * sinTriangle) };
                int[] yFillTriangle = {(int) (middleY - sinTriangle), (int) (middleY + sinTriangle),
                        (int) (middleY - 2 * cosTriangle) };
                g.fillPolygon(xFillTriangle, yFillTriangle, 3);

                if (oneway == -2) {
                    int[] xDrawTriangle = {(int) (middleX + cosTriangle), (int) (middleX - cosTriangle),
                            (int) (middleX + 2 * sinTriangle) };
                    int[] yDrawTriangle = {(int) (middleY - sinTriangle), (int) (middleY + sinTriangle),
                            (int) (middleY + 2 * cosTriangle) };
                    g.drawPolygon(xDrawTriangle, yDrawTriangle, 3);
                }
            }

        }

    }

    /**
     * Draws a circle around the node
     *
     * @param n
     *            The node
     * @param color
     *            The circle color
     */
    @Override
    protected void drawNode(Node n, Color color) {
        if (mv == null || g == null) {
            return;
        }
        Point p = mv.getPoint(n);
        if (p == null) {
            return;
        }
        g.setColor(color);
        g.drawOval(p.x - 5, p.y - 5, 10, 10);

    }

    /**
     * Draws s stop_position as a blue circle; draws a platform as a blue square
     *
     * @param primitive
     *            primitive
     * @param stopPosition whether to draw a circle (true) or square (false)
     */
    protected void drawStop(OsmPrimitive primitive, Boolean stopPosition) {

        // find the point to which the stop visualization will be linked:
        Node n = new Node(primitive.getBBox().getCenter());

        Point p = mv.getPoint(n);

        g.setColor(Color.BLUE);

        if (stopPosition) {
            g.fillOval(p.x - 8, p.y - 8, 16, 16);
        } else {
            g.fillRect(p.x - 8, p.y - 8, 16, 16);
        }

    }

    /**
     * Draws the labels for the stops, which include the ordered position of the
     * stop in the route and the ref numbers of other routes that use this stop
     *
     * @param primitive
     *     primitive
     */
    protected void drawStopLabel(OsmPrimitive primitive) {
        // find the point to which the stop visualization will be linked:
        Node n = new Node(primitive.getBBox().getCenter());

        Point p = mv.getPoint(n);

        // draw the ref values of all parent routes:
        List<String> parentsLabelList = new ArrayList<>();

        for (OsmPrimitive parent : primitive.getReferrers()) {
            if (parent.getType().equals(OsmPrimitiveType.RELATION)) {
                Relation relation = (Relation) parent;
                if (RouteUtils.isVersionTwoPTRoute(relation) && relation.get("ref") != null
                        && !relation.get("ref").equals("")) {

                    boolean stringFound = false;
                    for (String s : parentsLabelList) {
                        if (s.equals(relation.get("ref"))) {
                            stringFound = true;
                        }
                    }
                    if (!stringFound) {
                        parentsLabelList.add(relation.get("ref"));
                    }

                }
            }
        }

        parentsLabelList.sort(new RefTagComparator());

        StringBuilder sb = new StringBuilder();
        for (String s : parentsLabelList) {
            sb.append(s).append(";");
        }

        if (sb.length() > 0) {
            // remove the last semicolon:
            String parentsLabel = sb.substring(0, sb.length() - 1);

            g.setColor(new Color(192, 41, 86));
            Font parentLabelFont = new Font("SansSerif", Font.ITALIC, 20);
            g.setFont(parentLabelFont);
            g.drawString(parentsLabel, p.x + 20, p.y + 20);
        }

    }

    protected void drawStopLabelWithRefsOfRoutes(OsmPrimitive primitive, String label, Boolean platform) {
        // find the point to which the stop visualization will be linked:
        Node n = new Node(primitive.getBBox().getCenter());

        Point p = mv.getPoint(n);

        if (label != null && !label.isEmpty()) {
            final Font stringFont = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
            if (platform) {
                g.setColor(new Color(255, 255, 102));
                g.setFont(stringFont);
                g.drawString(label, p.x + 20, p.y - 40);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(stringFont);
                g.drawString(label, p.x + 20, p.y - 20);
            }
        }
    }

    /**
     * Compares route ref numbers
     */
    public static class RefTagComparator implements Comparator<String> {
        private static final Pattern LEADING_UNSIGNED_INT_PATTERN = Pattern.compile("^([0-9]+).*$");

        @Override
        public int compare(final String s1, final String s2) {

            final boolean s1Empty = s1 == null || s1.isEmpty();
            final boolean s2Empty = s2 == null || s2.isEmpty();

            if (s1Empty || s2Empty) {
                // only s1 empty: -1 (s1 < s2)
                // only s2 empty: 1 (s2 < s1)
                // both empty: 0 (s1 == s2)
                return (s1Empty ? -1 : 0) + (s2Empty ? 1 : 0);
            }
            final Integer s1Number = getLeadingInt(s1);
            final Integer s2Number = getLeadingInt(s2);
            if (s1Number != null && s2Number != null) { // both start with integer
                return Integer.compare(s1Number, s2Number);
            } else if (s1Number != null) { // only s1 starts with integer
                return -1;
            } else if (s2Number != null) { // only s2 starts with integer
                return 1;
            }
            return s1.compareTo(s2);
        }

        private Integer getLeadingInt(final CharSequence stringValue) {
            final Matcher matcher = LEADING_UNSIGNED_INT_PATTERN.matcher(stringValue);
            if (matcher.matches()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Visualizes the fix variants, assigns colors to them based on their order
     *
     * @param fixVariants
     *            fix variants
     * @param wayColoring Color codes (A to E)
     */
    protected void visitFixVariants(HashMap<Character, List<PTWay>> fixVariants,
            Map<Way, List<Character>> wayColoring) {

        drawFixVariantsWithParallelLines(wayColoring);

        Color[] colors = {new Color(255, 0, 0, 150), new Color(0, 255, 0, 150), new Color(0, 0, 255, 150),
                new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

        int colorIndex = 0;

        double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
        double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

        for (Entry<Character, List<PTWay>> entry : fixVariants.entrySet()) {
            Character c = entry.getKey();
            if (fixVariants.get(c) != null) {
                drawFixVariantLetter(c.toString(), colors[colorIndex % 5], letterX, letterY);
                colorIndex++;
                letterY = letterY + 60;
            }
        }

        // display the "Esc" label:
        if (!fixVariants.isEmpty()) {
            drawFixVariantLetter("Esc", Color.WHITE, letterX, letterY);
        }
    }

    @SuppressWarnings("unused")
    private void drawFixVariant(List<PTWay> fixVariant, Color color) {
        for (PTWay ptway : fixVariant) {
            for (Way way : ptway.getWays()) {
                for (Pair<Node, Node> nodePair : way.getNodePairs(false)) {
                    drawSegment(nodePair.a, nodePair.b, color, 0, false);
                }
            }
        }
    }

    protected void drawFixVariantsWithParallelLines(Map<Way, List<Character>> wayColoring) {

        HashMap<Character, Color> colors = new HashMap<>();
        colors.put('1', new Color(255, 0, 0, 200));
        colors.put('2', new Color(0, 255, 0, 200));
        colors.put('3', new Color(0, 0, 255, 200));
        colors.put('4', new Color(255, 255, 0, 200));
        colors.put('5', new Color(0, 255, 255, 200));

        for (Entry<Way, List<Character>> entry : wayColoring.entrySet()) {
            Way way = entry.getKey();
            List<Character> letterList = entry.getValue();
            List<Color> wayColors = new ArrayList<>();
            for (Character letter : letterList) {
                wayColors.add(colors.get(letter));
            }
            for (Pair<Node, Node> nodePair : way.getNodePairs(false)) {
                drawSegmentWithParallelLines(nodePair.a, nodePair.b, wayColors);
            }
        }
    }

    protected void drawSegmentWithParallelLines(Node n1, Node n2, List<Color> colors) {
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
            int[] xPoints = {(int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
            int[] yPoints = {(int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
            g.setColor(currentColor);
            g.fillPolygon(xPoints, yPoints, 4);
        } else {
            boolean iterate = true;
            while (iterate) {
                currentColor = colors.get(i % colors.size());

                int[] xPoints = {(int) (prevPointX + cosT), (int) (nextPointX + cosT), (int) (nextPointX - cosT),
                        (int) (prevPointX - cosT) };
                int[] yPoints = {(int) (prevPointY - sinT), (int) (nextPointY - sinT), (int) (nextPointY + sinT),
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

            int[] lastXPoints = {(int) (prevPointX + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT),
                    (int) (prevPointX - cosT) };
            int[] lastYPoints = {(int) (prevPointY - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT),
                    (int) (prevPointY + sinT) };
            g.setColor(currentColor);
            g.fillPolygon(lastXPoints, lastYPoints, 4);
        }

        g.setColor(currentColor);
        g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
    }

    /**
     * Visuallizes the letters for each fix variant
     *
     * @param letter
     *            letter
     * @param color
     *            color
     * @param letterX
     *            letter X
     * @param letterY
     *            letter Y
     */
    private void drawFixVariantLetter(String letter, Color color, double letterX, double letterY) {
        g.setColor(color);
        Font stringFont = new Font("SansSerif", Font.PLAIN, 50);
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
