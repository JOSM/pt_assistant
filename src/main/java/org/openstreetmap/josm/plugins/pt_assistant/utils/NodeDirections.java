//package org.openstreetmap.josm.plugins.pt_assistant.utils;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import org.openstreetmap.josm.data.coor.LatLon;
//import org.openstreetmap.josm.data.osm.Node;
//import org.openstreetmap.josm.data.osm.Relation;
//import org.openstreetmap.josm.data.osm.Way;
//import org.openstreetmap.josm.plugins.pt_assistant.data.PTRouteDataManager;
//import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
//
//public class NodeDirections {
//    HashMap<Way, ArrayList<PTStop>> RightSideStops = new HashMap<>();
//    HashMap<Way, ArrayList<PTStop>> LeftSideStops = new HashMap<>();
//    HashMap<Way, Integer> wayAlreadyThere = new HashMap<>();
//    HashMap<PTStop, Integer> StopHasBeenChecked = new HashMap<>();
//    // HashMap<Way,Integer> wayAlreadyThere = new HashMap<>();
//    Way prev1 = null;
//    Way curr1 = null;
//    Node strt = null;
//    Node endn = null;
//    Node tempend = null;
//    Node tempstrt = null;
//
//    public NodeDirections() {
//    }
//    public static void assignDirections(List<Way> ways,Relation relat) {
//        PTRouteDataManager route = new PTRouteDataManager(relat);
//        for (int in = 0; in < ways.size(); in++) {
//            Way w = ways.get(in);
//            wayAlreadyThere.put(w, 0);
//            if (prev1 == null) {
//                Way nex = ways.get(in + 1);
//                if (w.firstNode().equals(nex.firstNode()) || w.firstNode().equals(nex.lastNode())) {
//                    strt = w.lastNode();
//                    endn = w.firstNode();
//                    tempstrt = w.lastNode();
//                    tempend = w.firstNode();
//                } else {
//                    strt = w.firstNode();
//                    endn = w.lastNode();
//                    tempstrt = w.firstNode();
//                    tempend = w.lastNode();
//                }
//            } else {
//                strt = endn;
//                endn = route.getOtherNode(w, strt);
//                tempstrt = strt;
//                tempend = endn;
//            }
//            if (wayStop.containsKey(w)) {
//                curr1 = w;
//                for (PTStop pts : wayStop.get(w)) {
//                    Node node3 = pts.getStopNode();
//                    if (w.getNodes().get(0).equals(endn)) {
//                        for (int i = 0; i < w.getNodes().size() - 1; i++) {
//                            Node node1 = w.getNodes().get(i);
//                            Node node2 = w.getNodes().get(i + 1);
//
//                            LatLon coord1 = new LatLon(node1.lat(), node1.lon());
//                            LatLon coord2 = new LatLon(node2.lat(), node2.lon());
//                            LatLon coord3 = new LatLon(node3.lat(), node3.lon());
//
//                            if (route.checkAcuteAngles(coord3, coord2, coord1) && route.checkAcuteAngles(coord3, coord1, coord2)) {
//                                tempend = node1;
//                                tempstrt = node2;
//                                break;
//                            }
//                        }
//                    } else {
//                        for (int i = w.getNodes().size() - 1; i > 0; i--) {
//                            Node node1 = w.getNodes().get(i);
//                            Node node2 = w.getNodes().get(i - 1);
//
//                            LatLon coord1 = new LatLon(node1.lat(), node1.lon());
//                            LatLon coord2 = new LatLon(node2.lat(), node2.lon());
//                            LatLon coord3 = new LatLon(node3.lat(), node3.lon());
//
//                            if (route.checkAcuteAngles(coord3, coord2, coord1) && route.checkAcuteAngles(coord3, coord1, coord2)) {
//                                tempstrt = node2;
//                                tempend = node1;
//                                break;
//                            }
//                        }
//                    }
//                    if (route.CrossProduct(tempstrt, tempend, pts)) {
//                        if (!RightSideStops.containsKey(w)) {
//                            RightSideStops.put(w, new ArrayList<PTStop>());
//                        }
//                        if (StopHasBeenChecked.get(pts) == null) {
//                            RightSideStops.get(w).add(pts);
//                        }
//                    } else {
//                        if (!LeftSideStops.containsKey(w)) {
//                            LeftSideStops.put(w, new ArrayList<PTStop>());
//                        }
//                        if (StopHasBeenChecked.get(pts) == null) {
//                            LeftSideStops.get(w).add(pts);
//                        }
//                    }
//                    StopHasBeenChecked.put(pts, 1);
//                }
//            }
//            prev1 = w;
//        }
//        for (Way w : ways) {
//            if (RightSideStops.get(w) != null) {
//                for (PTStop pt : RightSideStops.get(w)) {
//                    System.out.println("Way Id is " + w.getUniqueId() + " right stop " + pt.getUniqueId());
//                }
//            }
//            if (LeftSideStops.get(w) != null) {
//                for (PTStop pt : LeftSideStops.get(w)) {
//                    System.out.println("Way Id is " + w.getUniqueId() + " left stop " + pt.getUniqueId());
//                }
//            }
//        }
//    }
//}