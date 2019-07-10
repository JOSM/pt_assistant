package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;

public class Edge{

	private Color color;
	private List<Way> containWays = new ArrayList<>();

	Edge(List<Way> ways,Color color){
		containWays = ways;
		color = color;
	}
	Edge(){
		color = Color.BLUE;
	}

	public List<Way> getAllWays(){
		return containWays;
	}
//	public List<Way> getAllStops(){
//
//	}
	public Color getEdgeColor() {
		return color;
	}

	public void addWayToEdge(Way w) {
		containWays.add(w);
	}

}
