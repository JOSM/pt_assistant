// //// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;

//// ///**
//// //*
//// //* @author Ashish Singh
//// //*
//// //*/

public class EdgeDataManager{
	public List<IRelation> PTrelationList = new ArrayList<>();
  public HashMap<Way,Integer> NumberOfRoutesfromWay = new HashMap<>();
	Map<Way,ArrayList<IRelation>> relationsToWay = new HashMap<>();
	public Collection<IRelation<?>> selectedRelations = new ArrayList<>();
	public Map<IRelation,ArrayList<Edge>> edgesToRelation  = new HashMap<>();
	// List<Edge> edges = new ArrayList<>();
  EdgeDataManager(){
	   RelationListDialog relationdialog = new RelationListDialog();
	   relationdialog.showNotify();
	   List<IRelation<?>> relationList = relationdialog.model.getVisibleRelations();
	   for(IRelation r:relationList) {
		   if(r.hasTag("route","bus")) {
			      PTrelationList.add(r);
			      List<RelationMember> m = r.getMembers();
			      for(RelationMember rm:m) {
				          if(rm.isWay()) {
					           NumberOfRoutesfromWay.put(rm.getWay(), 0);
				          }
			      }
		  }
	   }
     selectedRelations = relationdialog.getSelectedRelations();
	}

	public int findNumberofParents(Way w) {
		return NumberOfRoutesfromWay.get(w);
	}

	public void IterationOnAllExistingRoutes() {
		for(IRelation rel:PTrelationList) {
			List<RelationMember> memb = rel.getMembers();
			for(RelationMember rm:memb) {
				if(rm.isWay()) {
					ArrayList<IRelation> lis= relationsToWay.get(rm.getWay());
					if(lis ==null){
						lis = new ArrayList<>();
					}
					lis.add(rel);
					relationsToWay.put(rm.getWay(),lis);
					NumberOfRoutesfromWay.put(rm.getWay(),NumberOfRoutesfromWay.get(rm.getWay())+1);
				}
			}
		}
	}

	public void makingEdgesForRelations(){
			for(IRelation rel:PTrelationList) {
				List<RelationMember> memb = rel.getMembers();
				Way prev = null;
				Edge edge = new Edge();
				ArrayList<Edge> listOfEdges = new ArrayList<>();
				for(RelationMember rm:memb) {
					if(rm.isWay()) {
						if(prev != null){
							if(NumberOfRoutesfromWay.get(rm.getWay()) == NumberOfRoutesfromWay.get(prev)){
								if(checkEqualityOfEdges(rm.getWay(),prev)){
									listOfEdges.get(listOfEdges.size()-1).addWayToEdge(rm.getWay());
								}
								else{
									edge = new Edge();
									edge.addWayToEdge(rm.getWay());
									listOfEdges.add(edge);
								}
							}
							else{
								edge = new Edge();
								edge.addWayToEdge(rm.getWay());
								listOfEdges.add(edge);
							}
						}
						else{
							edge.addWayToEdge(rm.getWay());
							listOfEdges.add(edge);
						}
						prev = rm.getWay();
					}
				}
				edgesToRelation.put(rel,listOfEdges);
			}
	}

	List<Edge> getEdgeListOfRelation(Relation rel){
		return edgesToRelation.get(rel);
	}

	public boolean checkEqualityOfEdges(Way w1,Way w2){
		List<IRelation> lis1= relationsToWay.get(w1);
		List<IRelation> lis2= relationsToWay.get(w2);
		for(IRelation rel:lis1){
			if(!lis2.contains(rel)){
				return false;
			}
		}
		return true;
	}
}
