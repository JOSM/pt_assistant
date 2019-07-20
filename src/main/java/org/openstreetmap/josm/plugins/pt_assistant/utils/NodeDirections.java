//package org.openstreetmap.josm.plugins.pt_assistant.utils;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.openstreetmap.josm.data.osm.Node;
//import org.openstreetmap.josm.data.osm.Relation;
//import org.openstreetmap.josm.data.osm.Way;
//import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
//import org.openstreetmap.josm.tools.Pair;
//
//public class NodeDirections {
//   Relation relation = null;
//   List<Way> ways = new ArrayList<>();
//   List<PTStop> ptstops = new ArrayList<>();
//   HashMap<Way, ArrayList<PTStop>> RightSideStops = new HashMap<>();
//   HashMap<Way, ArrayList<PTStop>> LeftSideStops = new HashMap<>();
//   HashMap<Way, Integer> wayAlreadyThere = new HashMap<>();
//   HashMap<PTStop, Integer> StopHasBeenChecked = new HashMap<>();
//   // HashMap<Way,Integer> wayAlreadyThere = new HashMap<>();
//   Way prev1 = null;
//   Way curr1 = null;
//   Node strt = null;
//   Node endn = null;
//   Node tempend = null;
//   Node tempstrt = null;
//
//   //ways should be in sorted order
//   public NodeDirections(List<Way> ways, List<PTStop> ptstops) {
//       this.ways = ways;
//       this.ptstops = ptstops;
//   }
//
//   void gettingNearestWay(){
//     StopToWay assigner2 = new StopToWay(ways);
//     List<PTStop> ptstops = new ArrayList<>();
//
//     stopsByName.values().forEach(ptstops::addAll);
//
//     Map<Way, List<PTStop>> wayStop = new HashMap<>();
//     PTRouteDataManager route = new PTRouteDataManager(rel);
//
//     ptstops.forEach(stop -> {
//         Way way = assigner2.get(stop);
//         if (way == null) {
//             addStopToRelation(rel, stop);
//         }
//         if (!wayStop.containsKey(way))
//             wayStop.put(way, new ArrayList<PTStop>());
//         wayStop.get(way).add(stop);
//     });
//
//     unnamed.forEach(stop -> {
//         Way way = assigner2.get(stop);
//         if (way == null) {
//             addStopToRelation(rel, stop);
//         }
//         if (!wayStop.containsKey(way))
//             wayStop.put(way, new ArrayList<PTStop>());
//         wayStop.get(way).add(stop);
//     });
//   }
//  public void RightLeftStops(){
//     HashMap<Way, ArrayList<PTStop>> RightSideStops = new HashMap<>();
//     HashMap<Way, ArrayList<PTStop>> LeftSideStops = new HashMap<>();
//     HashMap<Way, Integer> wayAlreadyThere = new HashMap<>();
//     HashMap<PTStop, Integer> StopHasBeenChecked = new HashMap<>();
//     // HashMap<Way,Integer> wayAlreadyThere = new HashMap<>();
//     Way prev1 = null;
//     Way curr1 = null;
//     Node strt = null;
//     Node endn = null;
//     Node tempstrt = null;
//     Node tempend = null;
//     for (int in = 0; in < ways.size(); in++) {
//         Way w = ways.get(in);
//         wayAlreadyThere.put(w, 0);
//         if (prev1 == null) {
//             Way nex = ways.get(in + 1);
//             if (w.firstNode().equals(nex.firstNode()) || w.firstNode().equals(nex.lastNode())) {
//                 strt = w.lastNode();
//                 endn = w.firstNode();
//                 tempstrt = w.lastNode();
//                 tempend = w.firstNode();
//             } else {
//                 strt = w.firstNode();
//                 endn = w.lastNode();
//                 tempstrt = w.firstNode();
//                 tempend = w.lastNode();
//             }
//         } else {
//             strt = endn;
//             endn = getOtherNode(w, strt);
//             tempstrt = strt;
//             tempend = endn;
//         }
//         if (wayStop.containsKey(w)) {
//             curr1 = w;
//             for (PTStop pts : wayStop.get(w)) {
//                 Node node3 = pts.getNode();
//                 Pair<Node, Node> segment = assigner.calculateNearestSegment(node3, w);
//                 Node node1 = segment.a;
//                 Node node2 = segment.b;
//                 //if the endn(it is not a link at this point) is the starting point of the way nodes
//                 if (w.getNodes().get(0).equals(endn)) {
//                     for (int i = 0; i < w.getNodes().size() - 1; i++) {
//                         if (w.getNodes().get(i) == node1 && w.getNodes().get(i + 1) == node2) {
//                             tempend = node1;
//                             tempstrt = node2;
//                         } else if (w.getNodes().get(i) == node2 && w.getNodes().get(i) == node1) {
//                             tempend = node2;
//                             tempstrt = node1;
//                         }
//                     }
//                 } else {
//                     for (int i = w.getNodes().size() - 1; i > 0; i--) {
//                         if (w.getNodes().get(i) == node1 && w.getNodes().get(i - 1) == node2) {
//                             tempend = node1;
//                             tempstrt = node2;
//                         } else if (w.getNodes().get(i) == node2 && w.getNodes().get(i - 1) == node1) {
//                             tempend = node2;
//                             tempstrt = node1;
//                         }
//                     }
//                 }
//                 if (route.CrossProduct(tempstrt, tempend, pts)) {
//                     if (!RightSideStops.containsKey(w)) {
//                         RightSideStops.put(w, new ArrayList<PTStop>());
//                     }
//                     if (StopHasBeenChecked.get(pts) == null) {
//                         RightSideStops.get(w).add(pts);
//                     }
//                 } else {
//                     if (!LeftSideStops.containsKey(w)) {
//                         LeftSideStops.put(w, new ArrayList<PTStop>());
//                     }
//                     if (StopHasBeenChecked.get(pts) == null) {
//                         LeftSideStops.get(w).add(pts);
//                     }
//                 }
//                 StopHasBeenChecked.put(pts, 1);
//             }
//         }
//         prev1 = w;
//     }
//   }
//   public void printWays(List<Way> ways){
//     for (Way w : ways) {
//         if (RightSideStops.get(w) != null) {
//             for (PTStop pt : RightSideStops.get(w)) {
//                 System.out.println("Way Id is " + w.getUniqueId() + " right stop " + pt.getUniqueId());
//             }
//         }
//         if (LeftSideStops.get(w) != null) {
//             for (PTStop pt : LeftSideStops.get(w)) {
//                 System.out.println("Way Id is " + w.getUniqueId() + " left stop " + pt.getUniqueId());
//             }
//         }
//     }
//   }
//
//}
