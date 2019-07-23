// //// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;

/**
* @author AshishSingh
*/

public class EdgeDataManager {
    public List<Relation> PTRelationList = new ArrayList<>();
    public HashMap<PTWay, Integer> NumberOfRoutesfromWay = new HashMap<>();
    public Map<PTWay, ArrayList<Relation>> relationsToWay = new HashMap<>();
    public Map<Relation, ArrayList<Edge>> edgesToRelation = new HashMap<>();
    public Map<Relation,ArrayList<PTStop>> orderedStops = new HashMap<>();
    Collection<Relation> allRelations = null;

    public EdgeDataManager() {
      if (MainApplication.getLayerManager().getEditLayer() != null
              && MainApplication.getLayerManager().getEditLayer().getDataSet() != null){
                allRelations = MainApplication.getLayerManager().getEditLayer().getDataSet().getRelations();
              }
        for (Relation r : allRelations) {
            if (r.hasTag("route", "bus")) {
                PTRelationList.add(r);
                List<RelationMember> m = r.getMembers();
                for (RelationMember rm : m) {
                    if (rm.isWay()) {
                        PTWay way =  new PTWay(rm);
                        NumberOfRoutesfromWay.put(way, 0);
                    }
                }
            }
        }
    }

    public int findNumberofParents(PTWay w) {
        return NumberOfRoutesfromWay.get(w);
    }

    public void IterationOnAllExistingRoutes() {
        for (Relation rel : PTRelationList) {
            List<RelationMember> memb = rel.getMembers();
            for (RelationMember rm : memb) {
                if (rm.isWay()) {
                    PTWay way =  new PTWay(rm);
                    ArrayList<Relation> lis = relationsToWay.get(way);
                    if (lis == null) {
                        lis = new ArrayList<>();
                    }
                    lis.add(rel);
                    relationsToWay.put(way, lis);
                    NumberOfRoutesfromWay.put(way, NumberOfRoutesfromWay.get(way) + 1);
                }
            }
        }
    }

    public void makingEdgesForRelations() {
      IterationOnAllExistingRoutes();
        for (Relation rel : PTRelationList) {
            List<RelationMember> memb = rel.getMembers();
            PTWay prev = null;
            Edge edge = new Edge();
            ArrayList<Edge> listOfEdges = new ArrayList<>();
            for (RelationMember rm : memb) {
                if (rm.isWay()) {
                    PTWay way =  new PTWay(rm);
                    if (prev != null) {
                        if (NumberOfRoutesfromWay.get(way) == NumberOfRoutesfromWay.get(prev)) {
                            if (checkEqualityOfEdges(way, prev)) {
                                listOfEdges.get(listOfEdges.size() - 1).addWayToEdge(way);
                            } else {
                                edge = new Edge();
                                edge.addWayToEdge(way);
                                listOfEdges.add(edge);
                            }
                        } else {
                            edge = new Edge();
                            edge.addWayToEdge(way);
                            listOfEdges.add(edge);
                        }
                    } else {
                        edge.addWayToEdge(way);
                        listOfEdges.add(edge);
                    }
                    prev = way;
                }
            }
            edgesToRelation.put(rel, listOfEdges);
        }
    }

    List<Edge> getEdgeListOfRelation(Relation rel) {
        return edgesToRelation.get(rel);
    }

    public void printPTRelationList() {
        makingEdgesForRelations();
        for(Relation r:PTRelationList) {
         System.out.println("printing the relation: "+r.getName());
         for(Edge edge:getEdgeListOfRelation(r)){
           System.out.println("edge printing ");
           for(PTWay w:edge.getAllWays()){
             System.out.println("edge ways:"+w.getUniqueId());
           }
         }
        }

    }

    public boolean checkEqualityOfEdges(PTWay w1, PTWay w2) {
        List<Relation> lis1 = relationsToWay.get(w1);
        List<Relation> lis2 = relationsToWay.get(w2);
        for (Relation rel : lis1) {
            if (!lis2.contains(rel)) {
                return false;
            }
        }
        return true;
    }
}
