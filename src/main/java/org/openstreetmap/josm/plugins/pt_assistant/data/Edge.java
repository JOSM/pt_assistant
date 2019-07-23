package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;

public class Edge {

    private Color color;
    private List<PTWay> containPTWays = new ArrayList<>();
    private List<PTStop> allPTStops = new ArrayList<>();
    private List<PTStop> allLeftStops = new ArrayList<>();
    private List<PTStop> allRightStops = new ArrayList<>();

    public Edge(List<PTWay> ways) {
        containPTWays = ways;
        color = color;
    }

    public Edge() {
        color = Color.BLUE;
    }

    public List<PTWay> getAllWays() {
        return containPTWays;
    }

    public List<PTStop> getAllStops() {
        for (PTWay w : containPTWays) {
            for (Way w1 : w.getWays()) {
                allPTStops.addAll(w.getAllStops(w1));
            }
        }
        return allPTStops;
    }

    public List<PTStop> getAllLeftStops() {
        for (PTWay w : containPTWays) {
            for (Way w1 : w.getWays()) {
                allLeftStops.addAll(w.getLeftStops(w1));
            }
        }
        return allLeftStops;
    }

    public List<PTStop> getAllRightStops() {
        for (PTWay w : containPTWays) {
            for (Way w1 : w.getWays()) {
                allRightStops.addAll(w.getRightStops(w1));
            }
        }
        return allRightStops;
    }

    public Color getEdgeColor() {
        return color;
    }

    public void addWayToEdge(PTWay w) {
        containPTWays.add(w);
    }

}
