package org.openstreetmap.josm.plugins.pt_assistant.gtfs.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerPositionStrategy;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.OnLineStrategy;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.GtfsFile;
import org.openstreetmap.josm.plugins.pt_assistant.gtfs.data.StopStation;

public class GtfsLayer extends Layer {
    public static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final GtfsFile file;

    private GtfsLayerTextColor textColor = GtfsLayerTextColor.WHITE;
    /**
     * Services are per week
     */
    private int minServicesRequired = 7 * 5;

    public GtfsLayer(GtfsFile file) {
        super(file.getFileName());
        this.file = file;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return tr("GTFS data loaded from file {0}", file.getFileName());
    }

    @Override
    public void mergeFrom(Layer from) {
        throw new UnsupportedOperationException("Cannot merge GTFS files");
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {

    }

    @Override
    public Object getInfoComponent() {
        return null;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[0];
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        throw new UnsupportedOperationException("No legacy painting");
    }

    @Override
    public LayerPainter attachToMapView(MapViewEvent event) {
        return new LayerPainter() {
            @Override
            public void paint(MapViewGraphics graphics) {
                Graphics2D g = graphics.getDefaultGraphics();
                MapViewState state = graphics.getMapView().getState();
                Bounds bounds = graphics.getClipBounds().getLatLonBoundsBox();

                // paint all trips
                List<Runnable> labels = new ArrayList<>();
                file.getMergedTrips()
                    .stream()
                    .filter(mergedTrip -> mergedTrip.getServicesNextWeek() >= minServicesRequired)
                    .filter(mergedTrip -> mergedTrip.getBounds().intersects(bounds))
                    .forEach(mergedTrip -> {
                        Color color = mergedTrip.getRoute().getColorSafe();
                        g.setColor(color);
                        g.setStroke(new BasicStroke(2));

                        MapViewPath path = new MapViewPath(state);
                        path.append(mergedTrip.getSampleTrip().getPoints(), false);
                        g.draw(path);

                        // Scheduled for later => we need to paint all lines first.
                        labels.add(() -> {
                            // Draw number of line along the line somewhere
                            FontMetrics fontMetrics = g.getFontMetrics(FONT);
                            String shortName = mergedTrip.getRoute().getShortName();
                            Rectangle2D nb = fontMetrics.getStringBounds(shortName, g);
                            MapViewPositionAndRotation placement = OnLineStrategy.INSTANCE.findLabelPlacement(path, nb);
                            if (placement != null) {
                                g.setFont(FONT);
                                drawText(g, placement, nb, shortName, mergedTrip.getRoute().getColorSafe(), mergedTrip.getRoute().getTextColorSafe());
                            }
                        });
                    });

                labels.forEach(Runnable::run);

                // Paint all stops
                file.getStops()
                    .values()
                    .forEach(stop -> {
                        if (!bounds.contains(stop.getLatLon())) {
                            return;
                        }
                        MapViewState.MapViewPoint p = state.getPointFor(stop.getLatLon());
                        int r = stop instanceof StopStation ? 7 : 5;
                        boolean isStationParent = stop.getMostGenericStop() == stop;

                        if (isStationParent || state.getScale() < 2.5) {
                            int x = (int) (p.getInViewX() - r / 2);
                            int y = (int) (p.getInViewY() - r / 2);
                            g.setColor(Color.WHITE);
                            g.fillOval(x, y, r, r);
                            g.setColor(Color.BLACK);
                            g.drawOval(x, y, r, r);
                        }

                        if (isStationParent && state.getScale() < 10 && textColor.getColor() != null) {
                            g.setColor(textColor.getColor());
                            g.drawString(stop.getName(), (int) p.getInViewX() + 6, (int) p.getInViewY() + 5);
                        }
                    });

            }

            // Same as org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.displayText(org.openstreetmap.josm.data.osm.IPrimitive, org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel, java.lang.String, java.awt.geom.Rectangle2D, org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation)
            private void drawText(Graphics2D g, MapViewPositionAndRotation placement, Rectangle2D nb, String shortName,
                                  Color background, Color text) {
                AffineTransform at = new AffineTransform();
                if (Math.abs(placement.getRotation()) < .01) {
                    // Explicitly no rotation: move to full pixels.
                    at.setToTranslation(
                        Math.round(placement.getPoint().getInViewX() - nb.getCenterX()),
                        Math.round(placement.getPoint().getInViewY() - nb.getCenterY()));
                } else {
                    at.setToTranslation(
                        placement.getPoint().getInViewX(),
                        placement.getPoint().getInViewY());
                    at.rotate(placement.getRotation());
                    at.translate(-nb.getCenterX(), -nb.getCenterY());
                }

                AffineTransform defaultTransform = g.getTransform();
                g.transform(at);
                g.setColor(background);
                g.fill(nb);
                g.setColor(text);
                g.drawString(shortName, 0, 0);
                g.setTransform(defaultTransform);
            }

            @Override
            public void detachFromMapView(MapViewEvent event) {
                // NOP
            }
        };
    }

    @Override
    public LayerPositionStrategy getDefaultLayerPosition() {
        // In default sorting, this should be behind the data but before the background.
        return LayerPositionStrategy.BEFORE_FIRST_BACKGROUND_LAYER;
    }

    public GtfsLayerTextColor getTextColor() {
        return textColor;
    }

    public void setTextColor(GtfsLayerTextColor textColor) {
        this.textColor = Objects.requireNonNull(textColor, "textColor");
        invalidate();
    }

    public int getMinServicesRequired() {
        return minServicesRequired;
    }

    public void setMinServicesRequired(int minServicesRequired) {
        if (minServicesRequired < 0) {
            throw new IllegalArgumentException("Services too low: " + minServicesRequired);
        }
        this.minServicesRequired = minServicesRequired;
        invalidate();
    }
}
