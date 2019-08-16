package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

public class Edge {

    private Color color;
    private List<PTWay> containPTWays = new ArrayList<>();

    public Edge(List<PTWay> ways) {
        containPTWays = ways;
    }

    public Edge() {
        color = Color.BLUE;
    }

    public List<PTWay> getAllWays() {
        return containPTWays;
    }

    public List<PTStop> getAllStops() {
        List<PTStop> allPTStops = new ArrayList<>();
        for (PTWay w : containPTWays) {
            for (Way w1 : w.getWays()) {
                allPTStops.addAll(w.getAllStops(w1));
            }
        }
        return allPTStops;
    }

    public List<PTStop> getAllRightStops(boolean direc) {
        List<PTStop> allRightStops = new ArrayList<>();
        PTWay prev = null;
        PTWay next = null;
        for (int i = 0; i < containPTWays.size(); i++) {
            if (i > 0) {
                prev = containPTWays.get(i - 1);
            }
            if (i < containPTWays.size() - 1) {
                next = containPTWays.get(i + 1);
            }
            PTWay curr = containPTWays.get(i);
            Node[] endNodes = new Node[2];
            endNodes = curr.getEndNodes();
            if (prev != null) {
                Node[] preendNodes = new Node[2];
                preendNodes = prev.getEndNodes();
                if (endNodes[0].equals(preendNodes[0]) || endNodes[0].equals(preendNodes[1])) {
                    allRightStops.addAll(curr.getPTWayRightStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayLeftStops(curr);
                    Collections.reverse(lis);
                    allRightStops.addAll(lis);
                }
            } else if (next != null) {
                Node[] nexendNodes = new Node[2];
                nexendNodes = next.getEndNodes();
                if (endNodes[1].equals(nexendNodes[0]) || endNodes[1].equals(nexendNodes[1])) {
                    allRightStops.addAll(curr.getPTWayRightStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayLeftStops(curr);
                    Collections.reverse(lis);
                    allRightStops.addAll(lis);
                }
            } else {
                if (direc) {
                    allRightStops.addAll(curr.getPTWayRightStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayLeftStops(curr);
                    Collections.reverse(lis);
                    allRightStops.addAll(lis);
                }
            }
        }
        return allRightStops;
    }

    public List<PTStop> getAllLeftStops(boolean direc) {
        List<PTStop> allLeftStops = new ArrayList<>();
        PTWay prev = null;
        PTWay next = null;
        for (int i = 0; i < containPTWays.size(); i++) {
            if (i > 0) {
                prev = containPTWays.get(i - 1);
            }
            if (i < containPTWays.size() - 1) {
                next = containPTWays.get(i + 1);
            }
            PTWay curr = containPTWays.get(i);
            Node[] endNodes = new Node[2];
            endNodes = curr.getEndNodes();
            if (prev != null) {
                Node[] preendNodes = new Node[2];
                preendNodes = prev.getEndNodes();
                if (endNodes[0].equals(preendNodes[0]) || endNodes[0].equals(preendNodes[1])) {
                    allLeftStops.addAll(curr.getPTWayLeftStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayRightStops(curr);
                    Collections.reverse(lis);
                    allLeftStops.addAll(lis);
                }
            } else if (next != null) {
                Node[] nexendNodes = new Node[2];
                nexendNodes = next.getEndNodes();
                if (endNodes[1].equals(nexendNodes[0]) || endNodes[1].equals(nexendNodes[1])) {
                    allLeftStops.addAll(curr.getPTWayLeftStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayRightStops(curr);
                    Collections.reverse(lis);
                    allLeftStops.addAll(lis);
                }
            } else {
                if (direc) {
                    allLeftStops.addAll(curr.getPTWayLeftStops(curr));
                } else {
                    List<PTStop> lis = curr.getPTWayRightStops(curr);
                    Collections.reverse(lis);
                    allLeftStops.addAll(lis);
                }
            }
        }
        return allLeftStops;
    }

    public PTWay getFirstWay() {
        return containPTWays.get(0);
    }

    public PTWay getLastWay() {
        return containPTWays.get(containPTWays.size() - 1);
    }

    public Color getEdgeColor() {
        return color;
    }

    public void addWayToEdge(PTWay w) {
        containPTWays.add(w);
    }

}
