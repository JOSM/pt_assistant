package org.openstreetmap.josm.plugins.pt_assistant.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.AbstractMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.Pair;

public class RainbowMapRenderer extends AbstractMapRenderer {
    public RainbowMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    public void drawNode(INode n, Color color, int size, boolean fill) { }

    @Override
    public void render(OsmData<?, ?, ?, ?> data, boolean renderVirtualNodes, Bounds bbox) {
        data.getRelations().stream()
            .filter(it -> "route".equals(it.get("type")))
            .flatMap(r -> {
                final Color color = getRouteColorOf(r);
                if (color != null) {
                    return r.findRelationMembers("").stream().filter(m -> m instanceof Way).map(w -> Pair.create((Way) w, color));
                }
                return Stream.empty();
            })
            .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toSet())))
            .forEach((way, colors) -> {
                final List<Color> colorList = new ArrayList<>(colors);
                for (int i = 0; way.getNodesCount() >= 2 && i < colorList.size(); i++) {
                    g.setStroke(new BasicStroke((colors.size() - i) * 6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                    g.setColor(colorList.get(i));
                    final Path2D path = new Path2D.Double();
                    final Point p1 = nc.getPoint(way.firstNode());
                    path.moveTo(p1.x, p1.y);
                    for (int n = 1; n < way.getNodesCount(); n++) {
                        final Point p = nc.getPoint(way.getNode(n));
                        path.lineTo(p.x, p.y);
                    }
                    g.draw(path);
                }
            });
    }

    private static Color getRouteColorOf(final IRelation r) {
        if ("route".equals(r.get("type"))) {
            String colourString = r.get("colour");
            if (colourString == null) {
                //  In case the colours are only defined in the routeMaster relation
                final List<? extends IPrimitive> routeMasters = r.getReferrers().stream()
                    .filter(rel -> rel.hasTag("type", "routeMaster"))
                    .collect(Collectors.toList());
                if (routeMasters != null && routeMasters.size() > 0) {
                    // normally there is only a single routeMaster
                    colourString = routeMasters.get(0).get("colour");
                }
            }
            if (colourString != null) {
                final Matcher matcher = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})").matcher(colourString);
                if (matcher.matches()) {
                    return new Color(Integer.valueOf(matcher.group(1), 16), Integer.valueOf(matcher.group(2), 16), Integer.valueOf(matcher.group(3), 16));
                }
            }
        }
        return null;
    }
}
