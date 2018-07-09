// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * This action allows the user to split a selected roundabout. The action will
 * look for ways going in and out of the roudabout which also are member of a
 * public transport route. Having found those, the roundabout will be split on
 * the common points between the ways and it. The routes will be fixed by
 * connecting the entry point to the exit point of the roundabout.
 *
 * @author giacomo, polyglot
 */
public class SplitRoundaboutAction extends JosmAction {

	private static final String ACTION_NAME = "Split Roundabout";
	private static final long serialVersionUID = 8912249304286025356L;
	private Map<Relation, Relation> changingRelations;

	/**
	 * Creates a new SplitRoundaboutAction
	 */
	public SplitRoundaboutAction() {
		super(ACTION_NAME, "icons/splitroundabout", ACTION_NAME, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		Way roundabout = (Way) getLayerManager().getEditDataSet().getSelected().iterator().next();

		// download the bbox around the roundabout
		DownloadOsmTask task = new DownloadOsmTask();
		task.setZoomAfterDownload(true);
		BBox rbbox = roundabout.getBBox();
		double latOffset = (rbbox.getTopLeftLat() - rbbox.getBottomRightLat()) / 10;
		double lonOffset = (rbbox.getBottomRightLon() - rbbox.getTopLeftLon()) / 10;

		Bounds area = new Bounds(rbbox.getBottomRightLat() - latOffset, rbbox.getTopLeftLon() - lonOffset,
				rbbox.getTopLeftLat() + latOffset, rbbox.getBottomRightLon() + lonOffset);
		Future<?> future = task.download(false, area, null);

		MainApplication.worker.submit(() -> {
			try {
				future.get();
				downloadIncompleteRelations(roundabout);
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	private void continueAfterDownload(Way roundabout) {
		// make the roundabout round, if requested
		if (Main.pref.getBoolean("pt_assistant.roundabout-splitter.alignalways")
				|| JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(Main.parent,
						tr("Do you want to make the roundabout round?"), tr("Roundabout round"),
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null)) {
			new AlignInCircleAction().actionPerformed(null);
		}

		// save the position of the roundabout inside each relation
		Map<Relation, List<Integer>> savedPositions = getSavedPositions(roundabout);

		// remove the roundabout from each relation
		MainApplication.undoRedo.add(getRemoveRoundaboutFromRelationsCommand(roundabout));

		// split the roundabout on the designated nodes
		List<Node> splitNodes = getSplitNodes(roundabout);

		SplitWayCommand result = SplitWayCommand.split(roundabout, splitNodes, Collections.emptyList());
		MainApplication.undoRedo.add(result);

		Collection<Way> splitWays = result.getNewWays();
		splitWays.add(result.getOriginalWay());

		changingRelations = new HashMap<>();

		// update the relations.
		MainApplication.undoRedo.add(getUpdateRelationsCommand(savedPositions, splitNodes, splitWays));

		// look at the editors which are already open and refresh them
		changingRelations.forEach((oldR, newR) -> {
			OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
			RelationEditor editor = RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer, oldR);
			if (editor != null) {
				editor.reloadDataFromRelation();
			}
		});
	}

	private void downloadIncompleteRelations(Way roundabout) {

		List<Relation> parents = getPTRouteParents(roundabout);
		parents.removeIf(r -> !r.hasIncompleteMembers());
		if (parents.isEmpty()) {
			continueAfterDownload(roundabout);
			return;
		}

		Future<?> future = MainApplication.worker
				.submit(new DownloadRelationMemberTask(parents,
						Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
								.buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
						MainApplication.getLayerManager().getEditLayer()));

		MainApplication.worker.submit(() -> {
			try {
				future.get();
				continueAfterDownload(roundabout);
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	public Command getUpdateRelationsCommand(Map<Relation, List<Integer>> savedPositions, List<Node> splitNodes,
			Collection<Way> splitWays) {

		changingRelations = updateRelations(savedPositions, splitNodes, splitWays);

		List<Command> commands = new ArrayList<>();
		changingRelations.forEach((oldR, newR) -> {
			commands.add(new ChangeCommand(oldR, newR));
		});

		return new SequenceCommand("Updating Relations for SplitRoundabout", commands);
	}

	private Map<Relation, Relation> updateRelations(Map<Relation, List<Integer>> savedPositions, List<Node> splitNodes,
			Collection<Way> splitWays) {
		Map<Relation, Relation> changingRelation = new HashMap<>();
		Map<Relation, Integer> memberOffset = new HashMap<>();

		savedPositions.forEach((r, positions) -> positions.forEach(i -> {
			if (!changingRelation.containsKey(r))
				changingRelation.put(r, new Relation(r));

			Relation c = changingRelation.get(r);

			if (!memberOffset.containsKey(r))
				memberOffset.put(r, 0);
			int offset = memberOffset.get(r);

			List<Pair<Way, Way>> entryExitWays = getEntryExitWays(c, i + offset, splitNodes);
			Way entryWay = entryExitWays.get(0).a;
			Way exitWay = entryExitWays.get(1).a;
			Way beforeEntryWay = entryExitWays.get(0).b;
			Way afterExitWay = entryExitWays.get(1).b;

			if (entryWay == null || exitWay == null) {
				openEditorsWithErrors(r,
						"The roundabout is not directly connected to either of the previous or next way in the relation", changingRelation);
				return;
			}

			// get the entry and exit nodes, exit if not found
			Node entryNode = getNodeInCommon(splitNodes, entryWay);
			Node exitNode = getNodeInCommon(splitNodes, exitWay);

			if (entryNode == null || exitNode == null) {
				openEditorsWithErrors(r,
						"The roundabout is not directly connected to either of the previous or next way in the relation", changingRelation);
				return;
			}

			boolean roundaboutIsIn2LegFormat = checkIfRoundaboutIsIn2LegFormat(entryWay, exitWay, beforeEntryWay,
					afterExitWay, splitNodes);

			if (needSwap(entryNode, entryWay, exitNode, exitWay) && !roundaboutIsIn2LegFormat) {
				Node temp = entryNode;
				entryNode = exitNode;
				exitNode = temp;

				Way tempWay = entryWay;
				entryWay = exitWay;
				exitWay = tempWay;

				tempWay = beforeEntryWay;
				beforeEntryWay = afterExitWay;
				afterExitWay = tempWay;
			}

			// swap in case of 2 leg format
			if (roundaboutIsIn2LegFormat) {
				if ((entryWay.isOneway() == 1 && entryWay.firstNode().equals(entryNode))
						|| (entryWay.isOneway() == -1 && entryWay.lastNode().equals(entryNode))) {
					Way tempWay = entryWay;
					entryWay = beforeEntryWay;
					beforeEntryWay = tempWay;

					entryNode = getNodeInCommon(splitNodes, entryWay);
				}

				if ((exitWay.isOneway() == 1 && exitWay.lastNode().equals(exitNode))
						|| (exitWay.isOneway() == -1 && exitWay.firstNode().equals(exitNode))) {
					Way tempWay = exitWay;
					exitWay = afterExitWay;
					afterExitWay = tempWay;

					exitNode = getNodeInCommon(splitNodes, exitWay);
				}
			}

			// starting from the entry node, add split ways until the
			// exit node is reached
			List<Way> parents = filterParents(entryNode.getParentWays(), entryWay, entryNode);

			if (parents.size() == 0) {
				openEditorsWithErrors(r, "Error in roundabout", changingRelation);
				return;
			}

			Way curr = parents.get(0);

			if (roundaboutIsIn2LegFormat) {
				if (r.hasTag("route", "bicycle")) {
					if (entryWay.hasTag("bicycle", "no", "use_sideway")
							|| exitWay.hasTag("bicycle", "no", "use_sideway")
							|| beforeEntryWay.hasTag("bicycle", "no", "use_sideway")
							|| afterExitWay.hasTag("bicycle", "no", "use_sideway")) {
						openEditorsWithErrors(r, "Error: The paths connecting the roundabout in the relation do not allow bicycles.", changingRelation);
						return;
					}
				}

				String role = curr.firstNode().equals(entryNode) ? "forward" : "backward";
				List<Integer> idx = findIndexOfWaysOfDifferentLegs(beforeEntryWay, entryWay, exitWay, afterExitWay, c);
				if (idx == null) {
					openEditorsWithErrors(r, "Error in roundabout", changingRelation);
					return;
				}
				Way middleWay1 = c.getMember(idx.get(2)).getWay();
				Way middleWay2 = c.getMember(idx.get(5)).getWay();

				Relation old = new Relation(c);
				offset -= removePreviousAfterMembers(c, idx);
				offset += addPreviousMember(c, entryWay, middleWay1, i + offset, role, old, entryNode);

				while (!curr.lastNode().equals(exitNode)) {
					c.addMember(i + offset++, new RelationMember(role, curr));
					parents = curr.lastNode().getParentWays();
					parents.remove(curr);
					parents.removeIf(w -> !splitWays.contains(w));
					curr = parents.get(0);
				}

				c.addMember(i + offset++, new RelationMember(role, curr));
				offset += addAfterMember(c, exitWay, middleWay2, i + offset, role, old, exitNode);
				// now go for the second leg
				entryWay = beforeEntryWay;
				exitWay = afterExitWay;
				entryNode = getNodeInCommon(splitNodes, entryWay);
				exitNode = getNodeInCommon(splitNodes, exitWay);

				parents = filterParentsInOppositeDirection(entryNode.getParentWays(), entryWay, entryNode);
				if (parents.size() != 0) {
					curr = parents.get(0);
					role = curr.firstNode().equals(entryNode) ? "backward" : "forward";
					offset += addPreviousMember(c, entryWay, middleWay1, i + offset, role, old, entryNode);
					while (!curr.firstNode().equals(exitNode)) {
						c.addMember(i + offset++, new RelationMember(role, curr));
						parents = curr.firstNode().getParentWays();
						parents.remove(curr);
						parents.removeIf(w -> !splitWays.contains(w));
						curr = parents.get(0);
					}
					c.addMember(i + offset++, new RelationMember(role, curr));
					offset += addAfterMember(c, exitWay, middleWay2, i + offset, role, old, exitNode);
				}
				memberOffset.put(r, offset);
			} else if (!c.hasTag("route", "bicycle") || (entryWay.isOneway() != 0 || exitWay.isOneway() != 0)) {
				while (!curr.lastNode().equals(exitNode)) {
					c.addMember(i + offset++, new RelationMember(null, curr));
					parents = curr.lastNode().getParentWays();
					parents.remove(curr);
					parents.removeIf(w -> !splitWays.contains(w));
					curr = parents.get(0);
				}
				c.addMember(i + offset++, new RelationMember(null, curr));
				memberOffset.put(r, offset);
			} else {
				String role = curr.firstNode().equals(entryNode) ? "forward" : "backward";
				while (!curr.lastNode().equals(exitNode)) {
					c.addMember(i + offset++, new RelationMember(role, curr));
					parents = curr.lastNode().getParentWays();
					parents.remove(curr);
					parents.removeIf(w -> !splitWays.contains(w));
					curr = parents.get(0);
				}
				c.addMember(i + offset++, new RelationMember(role, curr));

				// add the other side of the roundabout as well
				parents = filterParents(exitNode.getParentWays(), exitWay, exitNode);
				curr = parents.get(0);
				List<Way> lst = new ArrayList<>();
				if (parents.size() != 0) {
					role = curr.firstNode().equals(exitNode) ? "forward" : "backward";
					while (!curr.lastNode().equals(entryNode)) {
						lst.add(curr);
						parents = curr.lastNode().getParentWays();
						parents.remove(curr);
						parents.removeIf(w -> !splitWays.contains(w));
						curr = parents.get(0);
					}
					lst.add(curr);
					for (int k = lst.size() - 1; k >= 0; k--)
						c.addMember(i + offset++, new RelationMember(role, lst.get(k)));
				}
				memberOffset.put(r, offset);
			}
		}));
		return changingRelation;
	}

	private boolean checkIfRoundaboutIsIn2LegFormat(Way entryWay, Way exitWay, Way beforeEntryWay, Way afterExitWay,
			List<Node> splitNodes) {
		if (beforeEntryWay == null || afterExitWay == null)
			return false;

		if (!splitNodes.contains(beforeEntryWay.firstNode()) && !splitNodes.contains(beforeEntryWay.lastNode()))
			return false;

		if (!splitNodes.contains(afterExitWay.firstNode()) && !splitNodes.contains(afterExitWay.lastNode()))
			return false;

		if (beforeEntryWay.isOneway() == 0 || entryWay.isOneway() == 0)
			return false;

		if (afterExitWay.isOneway() == 0 || exitWay.isOneway() == 0)
			return false;

		return true;
	}

	private List<Way> filterParents(List<Way> parentWays, Way entryWay, Node entryNode) {
		List<Way> ret = new ArrayList<>();
		for (Way w : parentWays) {
			if (!w.equals(entryWay) && w.firstNode().equals(entryNode) && w.hasTag("junction", "roundabout")) {
				ret.add(w);
			}
		}
		return ret;
	}

	private List<Way> filterParentsInOppositeDirection(List<Way> parentWays, Way entryWay, Node entryNode) {
		List<Way> ret = new ArrayList<>();
		for (Way w : parentWays) {
			if (!w.equals(entryWay) && w.lastNode().equals(entryNode) && w.hasTag("junction", "roundabout"))
				ret.add(w);
		}
		return ret;
	}

	private List<Integer> findIndexOfWaysOfDifferentLegs(Way beforeEntryWay, Way entryWay, Way exitWay,
			Way afterExitWay, Relation relation) {
		List<Integer> idx = new ArrayList<>();
		int i1 = -1, i2 = -1, i3 = -1, i4 = -1;

		for (int i = 0; i < relation.getMembers().size(); i++) {
			if (relation.getMember(i).getUniqueId() == entryWay.getUniqueId() && i1 == -1) {
				i1 = i;
			}
			if (relation.getMember(i).getUniqueId() == beforeEntryWay.getUniqueId() && i2 == -1) {
				i2 = i;
			}
			if (relation.getMember(i).getUniqueId() == exitWay.getUniqueId() && i3 == -1) {
				i3 = i;
			}
			if (relation.getMember(i).getUniqueId() == afterExitWay.getUniqueId() && i4 == -1) {
				i4 = i;
			}
		}

		int middlewayInteger = -1;
		for (int i = i1; i >= 0; i--) {
			if (relation.getMember(i).isWay()) {
				Way w = relation.getMember(i).getWay();
				if (w.isOneway() == 0) {
					middlewayInteger = i;
					break;
				}
			}
		}

		idx.add(i1);
		idx.add(i2);
		idx.add(middlewayInteger);

		middlewayInteger = -1;
		for (int i = i4; i < relation.getMembers().size(); i++) {
			if (relation.getMember(i).isWay()) {
				Way w = relation.getMember(i).getWay();
				// way is not a one way
				if (w.isOneway() == 0) {
					middlewayInteger = i;
					break;
				}
			}
		}

		idx.add(i3);
		idx.add(i4);
		idx.add(middlewayInteger);

		for (int i = 0; i < 6; i++) {
			if (idx.get(i).intValue() == -1)
				return null;
		}

		return idx;
	}

	// check whether the ways were in reverse order and need to be split
	private boolean needSwap(Node entryNode, Way entryWay, Node exitNode, Way exitWay) {
		boolean entryReversed = false;
		boolean exitReversed = false;

		if (RouteUtils.isOnewayForPublicTransport(entryWay) == 0 && RouteUtils.isOnewayForPublicTransport(exitWay) == 0)
			return false;

		if (entryWay.firstNode().equals(entryNode) && RouteUtils.isOnewayForPublicTransport(entryWay) == 1)
			entryReversed = true;

		if (exitWay.lastNode().equals(exitNode) && RouteUtils.isOnewayForPublicTransport(exitWay) == 1)
			exitReversed = true;

		if (entryReversed && RouteUtils.isOnewayForPublicTransport(exitWay) == 0)
			return true;

		if (exitReversed && RouteUtils.isOnewayForPublicTransport(entryWay) == 0)
			return true;

		return entryReversed && exitReversed;
	}

	private Node getNodeInCommon(List<Node> nodes, Way way) {
		if (nodes.contains(way.lastNode()))
			return way.lastNode();
		else if (nodes.contains(way.firstNode()))
			return way.firstNode();

		return null;
	}

	// given a relation and the position where the roundabout was, it returns
	// the entry and exit ways of that occurrence of the roundabout
	private List<Pair<Way, Way>> getEntryExitWays(Relation r, Integer position, List<Node> splitNodes) {

		// the ways returned are the ones exactly before and after the roundabout
		Pair<Way, Way> waysBefore = new Pair<>(null, null);
		Pair<Way, Way> waysAfter = new Pair<>(null, null);

		if (position > 0) {
			RelationMember before = r.getMember(position - 1);
			if (before.isWay())
				waysBefore.a = before.getWay();
		}

		int count = 1;
		while (count < 10 && position - count > 0) {
			RelationMember before = r.getMember(position - count - 1);
			if (before.isWay()) {
				Way w = before.getWay();
				if (splitNodes.contains(w.firstNode()) || splitNodes.contains(w.lastNode())) {
					waysBefore.b = before.getWay();
					break;
				}
			}
			count++;
		}

		if (position < r.getMembers().size()) {
			RelationMember after = r.getMember(position);
			if (after.isWay())
				waysAfter.a = after.getWay();
		}

		count = 1;
		while (count < 10 && position + count < r.getMembers().size()) {
			RelationMember after = r.getMember(position + count);
			if (after.isWay()) {
				Way w = after.getWay();
				if (splitNodes.contains(w.firstNode()) || splitNodes.contains(w.lastNode())) {
					waysAfter.b = after.getWay();
					break;
				}
			}
			count++;
		}

		List<Pair<Way, Way>> ret = new ArrayList<>();
		ret.add(waysBefore);
		ret.add(waysAfter);

		return ret;
	}

	// split on all nodes which are the
	// entry or exit point for route relations
	public List<Node> getSplitNodes(Way roundabout) {
		Set<Node> noDuplicateSplitNodes = new HashSet<>(roundabout.getNodes());
		List<Node> splitNodes = new ArrayList<>(noDuplicateSplitNodes);

		splitNodes.removeIf(n -> {
			List<Way> parents = n.getParentWays();
			if (parents.size() == 1)
				return true; // return value for removeIf, not of the method
			parents.remove(roundabout);
			for (Way parent : parents) {
				if (!getRouteParents(parent).isEmpty()) {
					return false; // return value for removeIf
				}
			}
			return true; // return value for removeIf
		});
		return splitNodes;
	}

	public Command getRemoveRoundaboutFromRelationsCommand(Way roundabout) {
		List<Command> commands = new ArrayList<>();
		getPTRouteParents(roundabout).forEach(r -> {
			Relation c = new Relation(r);
			c.removeMembersFor(roundabout);
			commands.add(new ChangeCommand(r, c));
		});

		return new SequenceCommand("Remove roundabout from relations", commands);
	}

	// save the position of the roundabout inside each public transport route
	// it is contained in
	public Map<Relation, List<Integer>> getSavedPositions(Way roundabout) {

		Map<Relation, List<Integer>> savedPositions = new HashMap<>();

		for (Relation curr : getPTRouteParents(roundabout)) {
			// look at the editors which are already open and refresh,update them before making any changes
			OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
			GenericRelationEditor editor = (GenericRelationEditor) RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer, curr);
			if (editor != null) {
				editor.apply();
				curr = editor.getRelation();
			}

			for (int j = 0; j < curr.getMembersCount(); j++) {
				if (curr.getMember(j).getUniqueId() == roundabout.getUniqueId()) {
					if (!savedPositions.containsKey(curr))
						savedPositions.put(curr, new ArrayList<>());
					List<Integer> positions = savedPositions.get(curr);
					positions.add(j - positions.size());
				}
			}
		}

		return savedPositions;
	}

	private List<Relation> getPTRouteParents(Way roundabout) {
		List<Relation> referrers = OsmPrimitive.getFilteredList(roundabout.getReferrers(), Relation.class);
		referrers.removeIf(r -> (!RouteUtils.isPTRoute(r) && !RouteUtils.isBicycleRoute(r)));
		return referrers;
	}

	private List<Relation> getRouteParents(Way roundabout) {
		List<Relation> referrers = OsmPrimitive.getFilteredList(roundabout.getReferrers(), Relation.class);
		referrers.removeIf(r -> !RouteUtils.isRoute(r));
		return referrers;
	}

	private int removePreviousAfterMembers(Relation c, List<Integer> idx) {
		for (int i = idx.get(2) + 1; i < idx.get(5); i++) {
			c.removeMember(idx.get(2) + 1);
		}
		int maxIndex = idx.get(0) > idx.get(1) ? idx.get(0) : idx.get(1);
		int offset = maxIndex - idx.get(2);
		return offset;
	}

	private int addPreviousMember(Relation relation, Way entryWay, Way middleWay, int position, String role,
			Relation oldrelation, Node entryNode) {
		int offset = 0;
		List<Way> lst = new ArrayList<>();
		Way curr = entryWay;
		Node n = entryNode;

		while (!curr.equals(middleWay)) {
			lst.add(curr);
			if (curr.firstNode().equals(n))
				n = curr.lastNode();
			else
				n = curr.firstNode();
			List<Way> parent = n.getParentWays();
			parent.remove(curr);
			for (int i = 0; i < parent.size(); i++) {
				Collection<RelationMember> p = oldrelation.getMembersFor(Arrays.asList(parent.get(i)));
				if (p != null && p.size() != 0) {
					curr = parent.get(i);
					break;
				}
			}
		}
		for (int j = lst.size() - 1; j >= 0; j--) {
			relation.addMember(position + offset++, new RelationMember(role, lst.get(j)));
		}

		return offset;
	}

	private int addAfterMember(Relation relation, Way exitWay, Way middleWay, int position, String role,
			Relation oldrelation, Node exitNode) {
		int offset = 0;
		List<Way> lst = new ArrayList<>();
		Way curr = exitWay;

		Node n = exitNode;

		while (!curr.equals(middleWay) && curr != null) {
			lst.add(curr);
			if (curr.firstNode().equals(n))
				n = curr.lastNode();
			else
				n = curr.firstNode();
			List<Way> parent = n.getParentWays();
			parent.remove(curr);
			curr = null;
			List<Way> possibleways = new ArrayList<>();
			for (int i = 0; i < parent.size(); i++) {
				Collection<RelationMember> p = oldrelation.getMembersFor(Arrays.asList(parent.get(i)));
				if (p != null && p.size() != 0) {
					if (!parent.get(i).hasTag("junction", "roundabout"))
						possibleways.add(parent.get(i));

					if (parent.get(i).equals(middleWay)) {
						curr = middleWay;
						break;
					}
				}
			}

			if (possibleways.size() != 1)
				break;
			curr = parent.get(0);
		}

		for (int j = 0; j < lst.size(); j++) {
			relation.addMember(position + offset++, new RelationMember(role, lst.get(j)));
		}

		return offset;
	}

	void openEditorsWithErrors(Relation r, String s, Map<Relation, Relation> changingRelation) {
		// create editor:
		Relation old = new Relation(r);
		r.put("created_by", s);
		OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
		changingRelation.put(old, r);

		RelationEditor editor = RelationDialogManager.getRelationDialogManager().getEditorForRelation(layer, old);
		if (editor == null) {
			editor = RelationEditor.getEditor(layer, r, null);
			editor.setVisible(true);
			return;
		}
		editor.reloadDataFromRelation();
	}

	@Override
	protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
		setEnabled(false);
		if (selection == null || selection.size() != 1)
			return;
		OsmPrimitive selected = selection.iterator().next();
		if (selected.getType() != OsmPrimitiveType.WAY)
			return;
		if (((Way) selected).isClosed()
				&& (selected.hasTag("junction", "roundabout") || selected.hasTag("oneway", "yes"))) {
			setEnabled(true);
			return;
		}
	}
}
