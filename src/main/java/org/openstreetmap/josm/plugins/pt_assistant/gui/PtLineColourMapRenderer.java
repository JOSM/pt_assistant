package org.openstreetmap.josm.plugins.pt_assistant.gui;

import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.Pair;

public class PtLineColourMapRenderer extends AbstractMapRenderer {
    public PtLineColourMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
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
                final Color color = getLineColourOf(r);
                if (color != null) {
                    return r.findRelationMembers("").stream().filter(m -> m instanceof Way).map(w -> Pair.create((Way) w, color));
                }
                return Stream.empty();
            })
            .collect(Collectors.groupingBy(it -> it.a, Collectors.mapping(it -> it.b, Collectors.toSet())))
            .forEach((way, colors) -> {
                final List<Color> colorList = new ArrayList<>(colors);
                for (int i = 0; i < colorList.size(); i++) {
                    final StyledMapRenderer styledRenderer = new StyledMapRenderer(g, nc, isInactiveMode);
                    final float strokeWidth = 5.0f;
                    final BasicStroke line = new BasicStroke(strokeWidth, CAP_SQUARE, JOIN_MITER);
                    g.setStroke(line);
                    final int onewayForPublicTransport = RouteUtils.isOnewayForPublicTransport(way);
                    styledRenderer.drawWay(
                        way,
                        colorList.get(i),
                        line,
                        null,
                        null,
                        i * strokeWidth * -1,
                        false,
                        false,
                        onewayForPublicTransport == 1,
                        onewayForPublicTransport == -1
                    );
                    if (onewayForPublicTransport == 0) {
                        styledRenderer.drawWay(
                            way,
                            colorList.get(i),
                            line,
                            null,
                            null,
                            i * strokeWidth,
                            false,
                            false,
                            onewayForPublicTransport == 1,
                            onewayForPublicTransport == -1
                        );
                    }
                }
            });

    }

    private static Color getLineColourOf(final IRelation<?> r) {
        if ("route".equals(r.get("type"))) {
            String colourString = r.get("colour");
            if (colourString == null) {
                //  In case the colours are only defined in the route_master relation
                List<? extends IPrimitive> routeMasters = r.getReferrers();
                if (routeMasters != null && routeMasters.size() > 0) {
                    // normally there is only a single route_master
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
