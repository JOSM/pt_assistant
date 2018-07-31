// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.validation.PaintVisitor;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mend the relations by going through each way, sorting them and proposing
 * fixes for the gaps that are found
 *
 * @since xxx
 */
public class MendRelationAction extends AbstractRelationEditorAction {
	Relation relation = null;
	MemberTableModel memberTableModel = null;
	GenericRelationEditor editor = null;
	HashMap<Way, Integer> waysAlreadyPresent = null;
	List<RelationMember> members = null;
	Way previousWay;
	Way currentWay;
	Way nextWay;
	List<Integer> extraWaysToBeDeleted = null;
	Node currentNode = null;
	boolean noLinkToPreviousWay = true;
	int currentIndex;
	int downloadCounter;
	boolean nextIndex = true;
	boolean setEnable = true;
	boolean firstCall = true;
	boolean halt = false;
	boolean abort = false;
	HashMap<Way, Character> wayColoring;
	HashMap<Character, List<Way>> wayListColoring;

	AbstractMapViewPaintable temporaryLayer = null;
	String notice = null;

	/**
	 * Constructs a new {@code RemoveSelectedAction}.
	 *
	 * @param memberTable
	 *            member table
	 * @param memberTableModel
	 *            member table model
	 * @param layer
	 *            OSM data layer
	 * @param relation
	 */
	public MendRelationAction(IRelationEditorActionAccess editorAccess) {
		super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
		putValue(SHORT_DESCRIPTION, tr("Select objects for selected relation members"));
		new ImageProvider("dialogs/relation", "routing_assistance").getResource().attachImageIcon(this, true);
		updateEnabledState();
		editor = (GenericRelationEditor) editorAccess.getEditor();
		memberTableModel = editorAccess.getMemberTableModel();
		OsmDataLayer layer = editor.getLayer();
		this.relation = editor.getRelation();
		editor.addWindowListener(new WindowEventHandler());

	}

	@Override
	protected void updateEnabledState() {
		if (relation != null && !((relation.hasTag("route", "bus") && relation.hasTag("public_transport:version", "2"))
				|| ((RouteUtils.isPTRoute(relation) && !relation.hasTag("route", "bus"))))) {
			setEnabled(false);
			return;
		}
		if (!setEnable) {
			setEnabled(false);
			return;
		}
		// only enable the action if we have members referring to the selected
		// primitives
		setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (relation.hasIncompleteMembers()) {
			downloadIncompleteRelations();
			new Notification(tr("Downloading incomplete relation members. Kindly wait till download gets over."))
					.setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3600).show();
		} else {
			initialise();
		}
	}

	void initialise() {
		save();
		sortBelow(relation.getMembers(), 0);
		members = editor.getRelation().getMembers();
		if (halt == false) {
			downloadCounter = 0;
			firstCall = false;
			waysAlreadyPresent = new HashMap<>();
			getListOfAllWays();
			extraWaysToBeDeleted = new ArrayList<>();
			setEnable = false;
			previousWay = null;
			currentWay = null;
			nextWay = null;
			noLinkToPreviousWay = true;
			nextIndex = true;
			currentIndex = 0;
		} else {
			halt = false;
		}

		callNextWay(currentIndex);
	}

	void callNextWay(int i) {
		System.out.println("Index + " + i);
		if (i < members.size() && members.get(i).isWay()) {
			if (currentNode == null)
				noLinkToPreviousWay = true;

			// find the next index in members which is a way
			int j = getNextWayIndex(i);
			if (j >= members.size()) {
				deleteExtraWays();
				return;
			}

			Way way = members.get(i).getWay();
			this.nextWay = members.get(j).getWay();

			// if some ways are invalid then remove them
			Node node = checkValidityOfWays(way, j);

			// if nextIndex was set as true in checkValidity method then don't move forward
			if (abort || nextIndex) {
				nextIndex = false;
				return;
			} else {
				nextIndex = true;
			}

			if (noLinkToPreviousWay) {
				if (node == null) {
					// not connected in both ways
					currentWay = way;
					nextIndex = false;
					findNextWayBeforeDownload(way, way.firstNode(), way.lastNode());
				} else {
					noLinkToPreviousWay = false;
					currentNode = getOtherNode(nextWay, node);
					previousWay = way;
					nextIndex = false;
					downloadAreaAroundWay(way);
				}
			} else {
				if (node == null) {
					currentWay = way;
					nextIndex = false;
					findNextWayBeforeDownload(way, currentNode);
				} else if (nextWay.lastNode().equals(node)) {
					currentNode = nextWay.firstNode();
					previousWay = way;
					nextIndex = false;
					downloadAreaAroundWay(way);
				} else {
					currentNode = nextWay.lastNode();
					previousWay = way;
					nextIndex = false;
					downloadAreaAroundWay(way);
				}
			}
		}

		if (abort)
			return;

		// for members which are not way
		if (i >= members.size() - 1) {
			deleteExtraWays();
		} else if (nextIndex) {
			downloadCounter++;
			callNextWay(++currentIndex);
		}
	}

	boolean isNonSplitRoundAbout(Way way) {
		if (way.hasTag("junction", "roundabout") && way.firstNode().equals(way.lastNode()))
			return true;
		return false;
	}

	Node checkValidityOfWays(Way way, int nextWayIndex) {
		boolean nextWayDelete = false;
		Node node = null;
		nextIndex = false;

		int numberOfNodes = findNumberOfCommonFirstLastNode(nextWay, way);
		if (numberOfNodes > 1) {
			nextWayDelete = true;
			notice = "2 or more Common nodes found between current and next way";
		} else if (numberOfNodes == 1) {
			// the method only checks at the first last nodes
			node = findCommonFirstLastNode(nextWay, way, currentNode);
		} else {
			// the nodes can be one of the intermediate nodes
			for (Node n : nextWay.getNodes()) {
				if (way.getNodes().contains(n)) {
					node = n;
					currentNode = n;
				}
			}
		}

		if (way.hasTag("junction", "roundabout")) {
			if (isNonSplitRoundAbout(way)) {
				nextWayDelete = false;
				for (Node n : way.getNodes()) {
					if (nextWay.firstNode().equals(n) || nextWay.lastNode().equals(n)) {
						node = n;
						currentNode = n;
					}
				}
			}
		}

		if (nextWay.hasTag("junction", "roundabout")) {
			if (isNonSplitRoundAbout(nextWay)) {
				nextWayDelete = false;
			}
		}

		if (node != null && !checkOneWaySatisfiability(nextWay, node)) {
			nextWayDelete = true;
			notice = "vehicle travels against oneway restriction";
		}

		if (nextWayDelete) {
			currentWay = way;
			nextIndex = true;
			removeWay(nextWayIndex);
			return null;
		}

		nextIndex = false;
		return node;
	}

	void deleteExtraWays() {
		int[] ints = extraWaysToBeDeleted.stream().mapToInt(Integer::intValue).toArray();
		memberTableModel.remove(ints);
		setEnable = true;
		setEnabled(true);
		halt = false;
	}

	boolean isOneWay(Way w) {
		if (w.isOneway() == 0)
			return false;
		return true;
	}

	void removeWay(int j) {
		List<Integer> Int = new ArrayList<>();
		List<Way> lst = new ArrayList<>();
		Int.add(j);
		lst.add(members.get(j).getWay());

		// if the way at members.get(j) is one way then check if the next ways are on
		// way, if so then remove them as well
		if (isOneWay(members.get(j).getWay())) {
			while (true) {
				int k = getNextWayIndex(j);
				if (k == -1 || k >= members.size())
					break;

				if (!isOneWay(members.get(k).getWay()))
					break;
				if (findNumberOfCommonFirstLastNode(members.get(k).getWay(), members.get(j).getWay()) == 0)
					break;

				j = k;
				Int.add(j);
				lst.add(members.get(j).getWay());
			}
		}

		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		ds.setSelected(lst);

		downloadAreaBeforeRemovalOption(lst, Int);
	}

	void getListOfAllWays() {
		for (int i = 0; i < members.size() - 1; i++) {
			if (members.get(i).isWay()) {
				waysAlreadyPresent.put(members.get(i).getWay(), 1);
			}
		}
	}

	int getNextWayIndex(int idx) {
		int j = members.size();

		for (j = idx + 1; j < members.size(); j++) {
			if (members.get(j).isWay())
				break;
		}
		return j;
	}

	int getPreviousWayIndex(int idx) {
		int j;

		for (j = idx - 1; j >= 0; j--) {
			if (members.get(j).isWay())
				return j;
		}
		return -1;
	}

	void sortBelow(List<RelationMember> members, int index) {
		RelationSorter relationSorter = new RelationSorter();
		final List<RelationMember> subList = members.subList(Math.max(0, index), members.size());
		final List<RelationMember> sorted = relationSorter.sortMembers(subList);
		subList.clear();
		subList.addAll(sorted);
		memberTableModel.fireTableDataChanged();
	}

	private void downloadIncompleteRelations() {

		List<Relation> parents = Arrays.asList(relation);

		Future<?> future = MainApplication.worker
				.submit(new DownloadRelationMemberTask(parents,
						Utils.filteredCollection(DownloadSelectedIncompleteMembersAction
								.buildSetOfIncompleteMembers(new ArrayList<>(parents)), OsmPrimitive.class),
						MainApplication.getLayerManager().getEditLayer()));

		MainApplication.worker.submit(() -> {
			try {
				future.get();
				new Notification(tr("Download over")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(3000).show();
				initialise();
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	int findNumberOfCommonFirstLastNode(Way way, Way previousWay) {
		Node node1 = previousWay.firstNode();
		Node node2 = previousWay.lastNode();
		int count = 0;
		if (way.firstNode().equals(node1) || way.firstNode().equals(node2)) {
			count++;
		}
		if (way.lastNode().equals(node1) || way.lastNode().equals(node2)) {
			count++;
		}

		return count++;
	}

	Node findCommonFirstLastNode(Way way, Way previousWay) {
		Node node1 = previousWay.firstNode();
		Node node2 = previousWay.lastNode();

		if (way.firstNode().equals(node1) || way.firstNode().equals(node2)) {
			return way.firstNode();
		}
		if (way.lastNode().equals(node1) || way.lastNode().equals(node2)) {
			return way.lastNode();
		}

		return null;
	}

	Node findCommonFirstLastNode(Way way, Way prevWay, Node currentNode) {
		if (currentNode == null) {
			if (way.firstNode().equals(prevWay.firstNode()) || way.firstNode().equals(prevWay.lastNode()))
				return way.firstNode();
			else
				return way.lastNode();
		}
		if (way.lastNode().equals(prevWay.lastNode()) || way.lastNode().equals(prevWay.firstNode()))
			return way.lastNode();
		else
			return way.firstNode();
	}

	void addNewWays(List<Way> ways, int i) {
		try {
			List<RelationMember> c = new ArrayList<>();
			for (int k = 0; k < ways.size(); k++) {
				c.add(new RelationMember("", ways.get(k)));
				// check if the way that is getting added is already present or not
				if (!waysAlreadyPresent.containsKey(ways.get(k)))
					waysAlreadyPresent.put(ways.get(k), 1);
				else {
					deleteWayAfterIndex(ways.get(k), i);
				}
			}

			memberTableModel.addMembersAfterIdx(ways, i);
			members.addAll(i + 1, c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void deleteWayAfterIndex(Way way, int index) {
		for (int i = index + 1; i < members.size(); i++) {
			if (members.get(i).isWay() && members.get(i).getWay().equals(way)) {
				Way prev = null;
				Way next = null;
				boolean del = true;
				if (i > 0 && members.get(i - 1).isWay())
					prev = members.get(i - 1).getWay();
				if (i < members.size() - 1 && members.get(i + 1).isWay())
					next = members.get(i + 1).getWay();
				// if the next index where the same way comes is well connected with its prev
				// and next way then don't delete it in that index
				if (prev != null && next != null) {
					if (findNumberOfCommonFirstLastNode(prev, way) != 0
							&& findNumberOfCommonFirstLastNode(way, nextWay) != 0) {
						del = false;
					}
				}
				if (del) {
					int[] x = { i };
					memberTableModel.remove(x);
					members.remove(i);
					break;
				}
			}
		}
	}

	Way findNextWayBeforeDownload(Way way, Node node) {
		nextIndex = false;
		DataSet ds = MainApplication.getLayerManager().getEditDataSet();
		ds.setSelected(way);
		AutoScaleAction.zoomTo(Arrays.asList(way));
		downloadAreaAroundWay(way, node, null);
		return null;
	}

	Way findNextWayBeforeDownload(Way way, Node node1, Node node2) {
		nextIndex = false;
		AutoScaleAction.zoomTo(Arrays.asList(way));
		downloadAreaAroundWay(way, node1, node2);
		return null;
	}

	Way findNextWayAfterDownload(Way way, Node node1, Node node2) {
		currentWay = way;
		if (abort)
			return null;

		List<Way> parentWays = findNextWay(way, node1);
		if (node2 != null)
			parentWays.addAll(findNextWay(way, node2));

		if (parentWays.size() == 1) {
			goToNextWays(parentWays.get(0), way, new ArrayList<>());
		} else if (parentWays.size() > 1) {
			nextIndex = false;
			displayFixVariants(parentWays);
		} else {
			nextIndex = true;
			if (currentIndex >= members.size() - 1) {
				deleteExtraWays();
			} else {
				callNextWay(++currentIndex);
				return null;
			}
		}
		return null;
	}

	void goToNextWays(Way way, Way prevWay, List<Way> wayList) {
		List<List<Way>> lst = new ArrayList<>();
		previousWay = prevWay;
		Node node1 = null;
		for (Node n : way.getNodes()) {
			if (prevWay.getNodes().contains(n)) {
				node1 = n;
				break;
			}
		}

		if (node1 == null) {
			lst.add(wayList);
			displayFixVariantsWithOverlappingWays(lst);
			return;
		}

		// check if the way equals the next way, if so then don't add any new ways to
		// the list
		if (way == nextWay) {
			lst.add(wayList);
			displayFixVariantsWithOverlappingWays(lst);
			return;
		}

		Node node = getOtherNode(way, node1);
		wayList.add(way);
		List<Way> parents = node.getParentWays();
		parents.remove(way);

		// if the ways directly touch the next way
		if (way.isFirstLastNode(nextWay.firstNode()) || way.isFirstLastNode(nextWay.lastNode())) {
			lst.add(wayList);
			displayFixVariantsWithOverlappingWays(lst);
			return;
		}

		// if next way turns out to be a roundabout
		if (nextWay.containsNode(node) && nextWay.hasTag("junction", "roundabout")) {
			lst.add(wayList);
			displayFixVariantsWithOverlappingWays(lst);
			return;
		}

		// remove all the invalid ways from the parent ways
		parents = removeInvalidWaysFromParentWays(parents, node, way);

		if (parents.size() == 1) {
			// if (already the way exists in the ways to be added
			if (wayList.contains(parents.get(0))) {
				lst.add(wayList);
				displayFixVariantsWithOverlappingWays(lst);
				return;
			}
			downloadAreaAroundWay(parents.get(0), way, wayList);
			return;
		} else if (parents.size() > 1) {
			// keep the most probable option s option A
			Way minWay = parents.get(0);
			double minLength = findDistance(minWay, nextWay, node);
			for (int k = 1; k < parents.size(); k++) {
				double length = findDistance(parents.get(k), nextWay, node);
				if (minLength > length) {
					minLength = length;
					minWay = parents.get(k);
				}
			}
			parents.remove(minWay);
			parents.add(0, minWay);

			// add all the list of ways to list of list of ways
			for (int i = 0; i < parents.size(); i++) {
				List<Way> wl = new ArrayList<>(wayList);
				wl.add(parents.get(i));
				lst.add(wl);
			}

			displayFixVariantsWithOverlappingWays(lst);
			return;
		} else {
			lst.add(wayList);
			displayFixVariantsWithOverlappingWays(lst);
			return;
		}
	}

	List<Way> findNextWay(Way way, Node node) {
		List<Way> parentWays = node.getParentWays();
		parentWays = removeInvalidWaysFromParentWays(parentWays, node, way);

		// if the way is a roundabout but it has not find any suitable option for next
		// way, look at parents of all nodes
		if (way.hasTag("junction", "roundabout") && parentWays.size() == 0) {
			for (Node n : way.getNodes())
				parentWays.addAll(removeInvalidWaysFromParentWaysOfRoundabouts(n.getParentWays(), n, way));
		}

		// put the most possible answer in front
		Way frontWay = null;
		for (int i = 0; i < parentWays.size(); i++) {
			if (checkIfWayConnectsToNextWay(parentWays.get(i), 0, node)) {
				frontWay = parentWays.get(i);
				break;
			}
		}

		if (frontWay == null && parentWays.size() > 0) {
			Way minWay = parentWays.get(0);
			double minLength = findDistance(minWay, nextWay, node);
			for (int i = 1; i < parentWays.size(); i++) {
				double length = findDistance(parentWays.get(i), nextWay, node);
				if (minLength > length) {
					minLength = length;
					minWay = parentWays.get(i);
				}
			}
			frontWay = minWay;
		}

		if (frontWay != null) {
			parentWays.remove(frontWay);
			parentWays.add(0, frontWay);
		}

		return parentWays;
	}

	boolean checkIfWayConnectsToNextWay(Way way, int count, Node node) {

		if (count < 50) {
			if (way.equals(nextWay))
				return true;

			// check if way;s intermediate node is next way's first or last node
			if (way.getNodes().contains(nextWay.firstNode()) || way.getNodes().contains(nextWay.lastNode()))
				return true;

			node = getOtherNode(way, node);
			List<Way> parents = node.getParentWays();
			if (parents.size() != 1)
				return false;
			else
				way = parents.get(0);

			count += 1;
			if (checkIfWayConnectsToNextWay(way, count, node))
				return true;
		}
		return false;
	}

	List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
		parentWays.remove(way);
		if (abort)
			return null;
		List<Way> waysToBeRemoved = new ArrayList<>();
		// check if any of the way is joining with its intermediate nodes
		List<Way> waysToBeAdded = new ArrayList<>();
		for (Way w : parentWays) {
			if (node != null && !w.isFirstLastNode(node)) {
				Way w1 = new Way();
				Way w2 = new Way();

				List<Node> lst1 = new ArrayList<>();
				List<Node> lst2 = new ArrayList<>();
				boolean firsthalf = true;

				for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
					if (firsthalf) {
						lst1.add(nodePair.a);
						lst1.add(nodePair.b);
						if (nodePair.b.equals(node))
							firsthalf = false;
					} else {
						lst2.add(nodePair.a);
						lst2.add(nodePair.b);
					}
				}

				w1.setNodes(lst1);
				w2.setNodes(lst2);

				w1.setKeys(w.getKeys());
				w2.setKeys(w.getKeys());

				if (!w.hasTag("junction", "roundabout")) {
					waysToBeRemoved.add(w);
					waysToBeAdded.add(w1);
					waysToBeAdded.add(w2);
				}
			}
		}

		// check if one of the way's intermediate node equals the first or last node of
		// next way,
		// if so then break it(finally split in method getNextWayAfterSelection if the
		// way is chosen)
		for (Way w : parentWays) {
			Node nextWayNode = null;
			if (w.getNodes().contains(nextWay.firstNode()) && !w.isFirstLastNode(nextWay.firstNode())) {
				nextWayNode = nextWay.firstNode();
			} else if (w.getNodes().contains(nextWay.lastNode()) && !w.isFirstLastNode(nextWay.lastNode())) {
				nextWayNode = nextWay.lastNode();
			}

			if (nextWayNode != null) {
				Way w1 = new Way();
				Way w2 = new Way();

				List<Node> lst1 = new ArrayList<>();
				List<Node> lst2 = new ArrayList<>();
				boolean firsthalf = true;

				for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
					if (firsthalf) {
						lst1.add(nodePair.a);
						lst1.add(nodePair.b);
						if (nodePair.b.equals(nextWayNode))
							firsthalf = false;
					} else {
						lst2.add(nodePair.a);
						lst2.add(nodePair.b);
					}
				}

				w1.setNodes(lst1);
				w2.setNodes(lst2);

				w1.setKeys(w.getKeys());
				w2.setKeys(w.getKeys());

				if (!w.hasTag("junction", "roundabout")) {
					waysToBeRemoved.add(w);
					if (w1.containsNode(node))
						waysToBeAdded.add(w1);
					if (w2.containsNode(node))
						waysToBeAdded.add(w2);
				}
			}
		}
		parentWays.addAll(waysToBeAdded);
		// one way direction doesn't match
		for (Way w : parentWays) {
			if (w.isOneway() != 0) {
				if (w.isOneway() == 1 && w.lastNode().equals(node)) {
					waysToBeRemoved.add(w);
				} else if (w.isOneway() == -1 && w.firstNode().equals(node)) {
					waysToBeRemoved.add(w);
				}
			}
		}
		parentWays.removeAll(waysToBeRemoved);
		waysToBeRemoved.clear();

		// check if both nodes of the ways are common, then remove
		for (Way w : parentWays) {
			if (findNumberOfCommonFirstLastNode(way, w) != 1 && !w.hasTag("junction", "roundabout")) {
				waysToBeRemoved.add(w);
			}
		}

		// check if any of them belong to roundabout, if yes then show ways accordingly
		for (Way w : parentWays) {
			if (w.hasTag("junction", "roundabout")) {
				if (findNumberOfCommonFirstLastNode(way, w) == 1) {
					if (w.lastNode().equals(node)) {
						waysToBeRemoved.add(w);
					}
				}
			}
		}

		// check mode of transport, also check if there is no loop
		for (Way w : parentWays) {
			if (relation.hasTag("route", "bus")) {
				if (!isWaySuitableForBuses(w)) {
					waysToBeRemoved.add(w);
				}
			} else if (RouteUtils.isPTRoute(relation)) {
				if (!isWaySuitableForOtherModes(w)) {
					waysToBeRemoved.add(w);
				}
			}
		}

		for (Way w : parentWays) {
			if (w.equals(previousWay)) {
				waysToBeRemoved.add(w);
			}
		}
		// check restrictions
		int count = 1;
		for (Way w : parentWays) {
			if (isRestricted(w, way)) {
				waysToBeRemoved.add(w);
			}
		}

		parentWays.removeAll(waysToBeRemoved);
		return parentWays;
	}

	List<Way> removeInvalidWaysFromParentWaysOfRoundabouts(List<Way> parents, Node node, Way way) {
		List<Way> parentWays = parents;
		parentWays.remove(way);
		if (abort)
			return null;
		List<Way> waysToBeRemoved = new ArrayList<>();

		// one way direction doesn't match
		for (Way w : parentWays) {
			if (w.isOneway() != 0) {
				if (w.isOneway() == 1 && w.lastNode().equals(node)) {
					waysToBeRemoved.add(w);
				} else if (w.isOneway() == -1 && w.firstNode().equals(node)) {
					waysToBeRemoved.add(w);
				}
			}
		}

		// check if any of them belong to roundabout, if yes then show ways accordingly
		for (Way w : parentWays) {
			if (w.hasTag("junction", "roundabout")) {
				if (findNumberOfCommonFirstLastNode(way, w) == 1) {
					if (w.lastNode().equals(node)) {
						waysToBeRemoved.add(w);
					}
				}
			}
		}

		// check mode of transport, also check if there is no loop
		for (Way w : parentWays) {
			if (!isWaySuitableForBuses(w)) {
				waysToBeRemoved.add(w);
			}

			if (w.equals(previousWay)) {
				waysToBeRemoved.add(w);
			}
		}

		parentWays.removeAll(waysToBeRemoved);
		return parentWays;
	}

	double findDistance(Way way, Way nextWay, Node node) {
		Node otherNode = getOtherNode(way, node);
		double Lat = (nextWay.firstNode().lat() + nextWay.lastNode().lat()) / 2;
		double Lon = (nextWay.firstNode().lon() + nextWay.lastNode().lon()) / 2;
		return (otherNode.lat() - Lat) * (otherNode.lat() - Lat) + (otherNode.lon() - Lon) * (otherNode.lon() - Lon);
	}

	void downloadAreaAroundWay(Way way, Node node1, Node node2) {
		if (abort)
			return;

		downloadCounter = 0;

		DownloadOsmTask task = new DownloadOsmTask();
		new Notification(tr("Downloading Data")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();

		BBox rbbox = way.getBBox();
		double latOffset = (rbbox.getTopLeftLat() - rbbox.getBottomRightLat()) / 10;
		double lonOffset = (rbbox.getBottomRightLon() - rbbox.getTopLeftLon()) / 10;
		Bounds area = new Bounds(rbbox.getBottomRightLat() - 10 * latOffset, rbbox.getTopLeftLon() - 10 * lonOffset,
				rbbox.getTopLeftLat() + 10 * latOffset, rbbox.getBottomRightLon() + 10 * lonOffset);
		Future<?> future = task.download(false, area, null);
		MainApplication.worker.submit(() -> {
			try {
				future.get();
				new Notification(tr("Download over")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();
				findNextWayAfterDownload(way, node1, node2);
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	void downloadAreaAroundWay(Way way) {
		if (abort)
			return;

		if (downloadCounter > 70) {
			downloadCounter = 0;

			DownloadOsmTask task = new DownloadOsmTask();
			BBox rbbox = way.getBBox();
			double latOffset = (rbbox.getTopLeftLat() - rbbox.getBottomRightLat()) / 10;
			double lonOffset = (rbbox.getBottomRightLon() - rbbox.getTopLeftLon()) / 10;
			Bounds area = new Bounds(rbbox.getBottomRightLat() - 3 * latOffset, rbbox.getTopLeftLon() - 3 * lonOffset,
					rbbox.getTopLeftLat() + 3 * latOffset, rbbox.getBottomRightLon() + 3 * lonOffset);
			Future<?> future = task.download(false, area, null);

			MainApplication.worker.submit(() -> {
				try {
					future.get();
					if (currentIndex >= members.size() - 1) {
						deleteExtraWays();
					} else {
						callNextWay(++currentIndex);
					}
				} catch (InterruptedException | ExecutionException e1) {
					Logging.error(e1);
					return;
				}
			});
		} else {
			if (currentIndex >= members.size() - 1) {
				deleteExtraWays();
				downloadCounter++;
			} else {
				callNextWay(++currentIndex);
				downloadCounter++;
			}
		}
	}

	void downloadAreaAroundWay(Way way, Way prevWay, List<Way> ways) {
		if (abort)
			return;

		downloadCounter = 0;

		DownloadOsmTask task = new DownloadOsmTask();
		new Notification(tr("Downloading Data")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();

		BBox rbbox = way.getBBox();
		double latOffset = (rbbox.getTopLeftLat() - rbbox.getBottomRightLat()) / 10;
		double lonOffset = (rbbox.getBottomRightLon() - rbbox.getTopLeftLon()) / 10;
		Bounds area = new Bounds(rbbox.getBottomRightLat() - 3 * latOffset, rbbox.getTopLeftLon() - 3 * lonOffset,
				rbbox.getTopLeftLat() + 3 * latOffset, rbbox.getBottomRightLon() + 3 * lonOffset);
		Future<?> future = task.download(false, area, null);
		MainApplication.worker.submit(() -> {
			try {
				future.get();
				new Notification(tr("Download over")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();
				goToNextWays(way, prevWay, ways);
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	void downloadAreaBeforeRemovalOption(List<Way> wayList, List<Integer> Int) {
		if (abort)
			return;

		downloadCounter = 0;

		DownloadOsmTask task = new DownloadOsmTask();
		new Notification(tr("Downloading Data")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();

		double maxLat = wayList.get(0).getBBox().getTopLeftLat(), minLat = wayList.get(0).getBBox().getBottomRightLat();
		double maxLon = wayList.get(0).getBBox().getBottomRightLon(), minLon = wayList.get(0).getBBox().getTopLeftLon();

		for (Way way : wayList) {
			BBox rbbox = way.getBBox();
			maxLat = maxLat > rbbox.getTopLeftLat() ? maxLat : rbbox.getTopLeftLat();
			minLat = minLat < rbbox.getBottomRightLat() ? minLat : rbbox.getBottomRightLat();
			maxLon = maxLon > rbbox.getBottomRightLon() ? maxLon : rbbox.getBottomRightLon();
			minLon = minLon < rbbox.getTopLeftLon() ? minLon : rbbox.getTopLeftLon();
		}

		double latOffset = (maxLat - minLat) / 10;
		double lonOffset = (maxLon - minLon) / 10;

		Bounds area = new Bounds(minLat - 7 * latOffset, minLon - 7 * lonOffset, maxLat + 7 * latOffset,
				maxLon + 7 * lonOffset);
		Future<?> future = task.download(false, area, null);

		MainApplication.worker.submit(() -> {
			try {
				future.get();
				new Notification(tr("Download over")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(2200).show();
				displayWaysToRemove(Int);
			} catch (InterruptedException | ExecutionException e1) {
				Logging.error(e1);
				return;
			}
		});
	}

	boolean isRestricted(Way currentWay, Way previousWay) {
		List<Relation> parentRelation = OsmPrimitive.getFilteredList(previousWay.getReferrers(), Relation.class);
		String[] restrictions = new String[] { "restriction", "restriction:bus", "restriction:trolleybus",
				"restriction:tram", "restriction:subway", "restriction:light_rail", "restriction:rail",
				"restriction:train", "restriction:trolleybus" };

		parentRelation.removeIf(rel -> {
			if (rel.hasKey("except")) {
				String[] val = rel.get("except").split(";");
				for (String s : val) {
					if (relation.hasTag("route", s)) return true;
				}
			}

			if (!rel.hasTag("type", restrictions))
				return true;
			else if (rel.hasTag("type", "restriction") && rel.hasKey("restriction"))
				return false;
			else {
				boolean remove = true;
				String routeValue = relation.get("route");
				for (String s : restrictions) {
					String sub = s.substring(12);
					if (routeValue.equals(sub) && rel.hasTag("type", s)) remove = false;
					else if (routeValue.equals(sub) && rel.hasKey("restriction:" + sub)) remove = false;
				}
				return remove;
			}
		});

		for (Relation r : parentRelation) {
			Collection<RelationMember> curMemberList = r.getMembersFor(Arrays.asList(currentWay));
			Collection<RelationMember> prevMemberList = r.getMembersFor(Arrays.asList(previousWay));

			if (curMemberList.isEmpty() || prevMemberList.isEmpty()) continue;

			RelationMember curMember = curMemberList.stream().collect(Collectors.toList()).get(0);
			RelationMember prevMember = prevMemberList.stream().collect(Collectors.toList()).get(0);

			final String curRole = curMember.getRole();
			final String prevRole = prevMember.getRole();

			if (curRole.equals("to") && prevRole.equals("from")) {
				String[] acceptedTags = new String[] { "no_right_turn", "no_left_turn", "no_u_turn", "no_straight_on",
						"no_entry", "no_exit" };
				for (String s : restrictions)
					if (r.hasTag(s, acceptedTags))
						return true;
			}
		}

		return false;
	}

	boolean isWaySuitableForBuses(Way way) {

		String[] acceptedHighwayTags = new String[] { "motorway", "trunk", "primary", "secondary", "tertiary",
				"unclassified", "road", "residential", "service", "motorway_link", "trunk_link", "primary_link",
				"secondary_link", "tertiary_link", "living_street", "bus_guideway", "road" };

		if (way.hasTag("highway", acceptedHighwayTags) || way.hasTag("cycleway", "share_busway")
				|| way.hasTag("cycleway", "shared_lane")) {
			return true;
		}

		if ((way.hasTag("highway", "pedestrian")
				&& (way.hasTag("bus", "yes", "designated") || way.hasTag("psv", "yes", "designated")))) {
			return true;
		}

		return false;
	}

	boolean isWaySuitableForOtherModes(Way way) {

		if (relation.hasTag("route", "tram"))
			return way.hasTag("railway", "tram");

		if (relation.hasTag("route", "subway"))
			return way.hasTag("railway", "subway");

		if (relation.hasTag("route", "light_rail"))
			return way.hasTag("railway", "light_rail");

		if (relation.hasTag("route", "rail"))
			return way.hasTag("railway", "rail");

		if (relation.hasTag("route", "train"))
			return way.hasTag("railway", "train");

		if (relation.hasTag("route", "trolleybus"))
			return way.hasTag("trolley_wire", "yes");

		return false;
	}

	void addAlreadyExistingWay(Way w) {
		// delete the way if only if we haven't crossed it yet
		if (waysAlreadyPresent.get(w) == 1) {
			deleteWay(w);
		}
	}

	void deleteWay(Way w) {
		for (int i = 0; i < members.size(); i++) {
			if (members.get(i).isWay() && members.get(i).getWay().equals(w)) {
				int[] x = { i };
				memberTableModel.remove(x);
				members.remove(i);
				break;
			}
		}
	}

	boolean checkOneWaySatisfiability(Way way, Node node) {
		String[] acceptedTags = new String[] { "yes", "designated" };

		if ((way.hasTag("oneway:bus", acceptedTags) || way.hasTag("oneway:psv", acceptedTags))
				&& way.lastNode().equals(node))
			return false;

		if (!isNonSplitRoundAbout(way) && way.hasTag("junction", "roundabout")) {
			if (way.lastNode().equals(node))
				return false;
		}

		if (isOnewayForPublicTransport(way) == 0)
			return true;
		else if (isOnewayForPublicTransport(way) == 1 && way.lastNode().equals(node))
			return false;
		else if (isOnewayForPublicTransport(way) == -1 && way.firstNode().equals(node))
			return false;

		return true;
	}

	// to be removed and taken from routeUtils later
	int isOnewayForPublicTransport(Way way) {

		if (OsmUtils.isTrue(way.get("oneway")) || OsmUtils.isReversed(way.get("oneway"))
				|| way.hasTag("junction", "roundabout") || way.hasTag("highway", "motorway")) {

			if (!way.hasTag("busway", "lane") && !way.hasTag("busway", "opposite_lane")
					&& !way.hasTag("busway:left", "lane") && !way.hasTag("busway:right", "lane")
					&& !way.hasTag("oneway:bus", "no") && !way.hasTag("oneway:psv", "no")
					&& !way.hasTag("trolley_wire", "backward")) {

				if (OsmUtils.isReversed(way.get("oneway"))) {
					return -1;
				}
				return 1;
			}
		}
		return 0;
	}

	Node getOtherNode(Way way, Node currentNode) {
		if (way.firstNode().equals(currentNode))
			return way.lastNode();
		else
			return way.firstNode();
	}

	void displayFixVariants(List<Way> fixVariants) {
		// find the letters of the fix variants:
		char alphabet = 'A';
		boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
		wayColoring = new HashMap<>();
		final List<Character> allowedCharacters = new ArrayList<>();

		if (numeric) {
			alphabet = '1';
			allowedCharacters.add('7');
			allowedCharacters.add('9');
		} else {
			allowedCharacters.add('S');
			allowedCharacters.add('Q');
		}

		for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
			allowedCharacters.add(alphabet);
			wayColoring.put(fixVariants.get(i), alphabet);
			alphabet++;
		}

		if (abort)
			return;

		// zoom to problem:
		final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

		for (Way variant : fixVariants)
			waysToZoom.add(variant);

		if (SwingUtilities.isEventDispatchThread()) {
			AutoScaleAction.zoomTo(waysToZoom);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					AutoScaleAction.zoomTo(waysToZoom);
				}
			});
		}

		// display the fix variants:
		temporaryLayer = new MendRelationAddLayer();
		MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

		// // add the key listener:
		MainApplication.getMap().mapView.requestFocus();
		MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (abort) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
					return;
				}
				Character typedKey = e.getKeyChar();
				Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
				if (allowedCharacters.contains(typedKeyUpperCase)) {
					System.out.println(typedKeyUpperCase);
					int idx = typedKeyUpperCase.charValue() - 65;
					if (numeric) {
						// for numpad numerics and the plain numerics
						if (typedKeyUpperCase.charValue() <= 57)
							idx = typedKeyUpperCase.charValue() - 49;
						else
							idx = typedKeyUpperCase.charValue() - 97;
					}
					nextIndex = true;
					if (typedKeyUpperCase.charValue() == 'S' || typedKeyUpperCase.charValue() == '7') {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						getNextWayAfterSelection(null);
					} else if (typedKeyUpperCase.charValue() == 'Q' || typedKeyUpperCase.charValue() == '9') {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						removeCurrentEdge();
					} else {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						getNextWayAfterSelection(Arrays.asList(fixVariants.get(idx)));
					}

				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					System.out.println("ESC");
					nextIndex = false;
					setEnable = true;
					halt = true;
					setEnabled(true);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});
	}

	void displayFixVariantsWithOverlappingWays(List<List<Way>> fixVariants) {
		// find the letters of the fix variants:
		char alphabet = 'A';
		boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
		wayListColoring = new HashMap<>();
		final List<Character> allowedCharacters = new ArrayList<>();

		if (numeric) {
			alphabet = '1';
			allowedCharacters.add('7');
			allowedCharacters.add('9');
		} else {
			allowedCharacters.add('S');
			allowedCharacters.add('Q');
		}

		for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
			allowedCharacters.add(alphabet);
			wayListColoring.put(alphabet, fixVariants.get(i));
			alphabet++;
		}

		if (abort)
			return;

		// zoom to problem:
		final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

		for (List<Way> variants : fixVariants) {
			for (Way variant : variants) {
				waysToZoom.add(variant);
			}
		}

		if (SwingUtilities.isEventDispatchThread()) {
			AutoScaleAction.zoomTo(waysToZoom);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					AutoScaleAction.zoomTo(waysToZoom);
				}
			});
		}

		// display the fix variants:
		temporaryLayer = new MendRelationAddMultipleLayer();
		MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

		// // add the key listener:
		MainApplication.getMap().mapView.requestFocus();
		MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (abort) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
					return;
				}
				Character typedKey = e.getKeyChar();
				Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
				if (allowedCharacters.contains(typedKeyUpperCase)) {
					int idx = typedKeyUpperCase.charValue() - 65;
					System.out.println(typedKey);
					if (numeric) {
						// for numpad numerics and the plain numerics
						if (typedKeyUpperCase.charValue() <= 57)
							idx = typedKeyUpperCase.charValue() - 49;
						else
							idx = typedKeyUpperCase.charValue() - 97;
					}
					nextIndex = true;
					if (typedKeyUpperCase.charValue() == 'S' || typedKeyUpperCase.charValue() == '7') {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						getNextWayAfterSelection(null);
					} else if (typedKeyUpperCase.charValue() == 'Q' || typedKeyUpperCase.charValue() == '9') {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						removeCurrentEdge();
					}else {
						MainApplication.getMap().mapView.removeKeyListener(this);
						MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
						getNextWayAfterSelection(fixVariants.get(idx));
					}

				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					System.out.println("ESC");
					nextIndex = false;
					setEnable = true;
					halt = true;
					setEnabled(true);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});
	}

	void displayWaysToRemove(List<Integer> wayIndices) {

		// find the letters of the fix variants:
		char alphabet = 'A';
		boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
		if (numeric)
			alphabet = '1';
		wayColoring = new HashMap<>();
		final List<Character> allowedCharacters = new ArrayList<>();

		if (numeric) {
			allowedCharacters.add('1');
			allowedCharacters.add('2');
			allowedCharacters.add('3');
		} else {
			allowedCharacters.add('A');
			allowedCharacters.add('B');
			allowedCharacters.add('R');
		}

		for (int i = 0; i < 5 && i < wayIndices.size(); i++) {
			wayColoring.put(members.get(wayIndices.get(i).intValue()).getWay(), alphabet);
		}

		if (notice.equals("vehicle travels against oneway restriction")) {
			if (numeric) {
				allowedCharacters.add('4');
			} else {
				allowedCharacters.add('C');
			}
		}

		if (abort)
			return;

		// zoom to problem:
		final Collection<OsmPrimitive> waysToZoom = new ArrayList<>();

		for (Integer i : wayIndices)
			waysToZoom.add(members.get(i.intValue()).getWay());

		if (SwingUtilities.isEventDispatchThread()) {
			AutoScaleAction.zoomTo(waysToZoom);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					AutoScaleAction.zoomTo(waysToZoom);
				}
			});
		}

		// display the fix variants:
		temporaryLayer = new MendRelationRemoveLayer();
		MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

		// // add the key listener:
		MainApplication.getMap().mapView.requestFocus();
		MainApplication.getMap().mapView.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (abort) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
					return;
				}
				Character typedKey = e.getKeyChar();
				Character typedKeyUpperCase = typedKey.toString().toUpperCase().toCharArray()[0];
				if (allowedCharacters.contains(typedKeyUpperCase)) {
					nextIndex = true;
					MainApplication.getMap().mapView.removeKeyListener(this);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
					System.out.println(typedKeyUpperCase);
					if (typedKeyUpperCase.charValue() == 'R' || typedKeyUpperCase.charValue() == '3') {
						wayIndices.add(0, currentIndex);
					}
					RemoveWayAfterSelection(wayIndices, typedKeyUpperCase);
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					MainApplication.getMap().mapView.removeKeyListener(this);
					System.out.println("ESC");
					nextIndex = false;
					setEnable = true;
					halt = true;
					setEnabled(true);
					MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});
	}

	void RemoveWayAfterSelection(List<Integer> wayIndices, Character chr) {
		if (chr.charValue() == 'A' || chr.charValue() == '1') {
			// remove all the ways
			int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
			memberTableModel.remove(lst);
			for (int i = 0; i < lst.length; i++) {
				members.remove(lst[i] - i);
			}
			// OK.actionPerformed(null);
			save();
			if (currentIndex < members.size() - 1) {
				notice = null;
				callNextWay(currentIndex);
			} else {
				notice = null;
				deleteExtraWays();
			}
		} else if (chr.charValue() == 'B' || chr.charValue() == '2') {
			if (currentIndex < members.size() - 1) {
				notice = null;
				currentIndex = wayIndices.get(wayIndices.size() - 1);
				callNextWay(currentIndex);
			} else {
				notice = null;
				deleteExtraWays();
			}
		} else if (chr.charValue() == 'C' || chr.charValue() == '4') {
			List<Command> cmdlst = new ArrayList<>();
			int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
			for (int i = 0; i < lst.length; i++) {
				Way w = members.get(lst[i]).getWay();
				TagMap newKeys = w.getKeys();
				newKeys.put("oneway", "bus=no");
				cmdlst.add(new ChangePropertyCommand(Collections.singleton(w), newKeys));
			}
			MainApplication.undoRedo.add(new SequenceCommand("Add tags", cmdlst));
			// OK.actionPerformed(null);
			save();
			if (currentIndex < members.size() - 1) {
				notice = null;
				callNextWay(currentIndex);
			} else {
				notice = null;
				deleteExtraWays();
			}
		}
		if (chr.charValue() == 'R' || chr.charValue() == '3') {
			// calculate the previous index
			int prevIndex = -1;
			for (int i = currentIndex - 1; i >= 0; i--) {
				if (members.get(i).isWay()) {
					prevIndex = i;
					break;
				}
			}
			// remove all the ways
			int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
			memberTableModel.remove(lst);
			for (int i = 0; i < lst.length; i++) {
				members.remove(lst[i] - i);
			}
			// OK.actionPerformed(null);
			save();
			if (prevIndex != -1) {
				notice = null;
				callNextWay(prevIndex);
			} else {
				notice = null;
				deleteExtraWays();
			}
		}
	}

	void getNextWayAfterSelection(List<Way> ways) {
		if (ways != null) {
			/*
			 * check if the selected way is not a complete way but rather a part of a parent
			 * way, then split the actual way (the partial way was created in method
			 * removeViolatingWaysFromParentWays but here we are finally splitting the
			 * actual way and adding to the relation) here there can be 3 cases - 1) if the
			 * current node is the node splitting a certain way 2) if next way's first node
			 * is splitting the way 3) if next way's last node is splitting the way
			 */
			System.out.println("Number of ways " + ways.size());
			int ind = currentIndex;
			Way prev = currentWay;
			for (int i = 0; i < ways.size(); i++) {
				Way w = ways.get(i);
				Way w1 = null;
				List<Node> breakNode = null;
				boolean brk = false;

				if (w.isNew()) {

					if (prev != null) {
						List<Way> par = new ArrayList<>(prev.firstNode().getParentWays());
						par.addAll(prev.lastNode().getParentWays());
						for (Way v : par) {
							if (v.getNodes().containsAll(w.getNodes())) {
								if (w.equals(v)) {
									addNewWays(Arrays.asList(v), ind);
									prev = v;
									ind++;
									brk = true;
									break;
								} else {
									List<Node> temp = new ArrayList<>();
									if (!v.isFirstLastNode(w.firstNode()))
										temp.add(w.firstNode());
									if (!v.isFirstLastNode(w.lastNode()))
										temp.add(w.lastNode());
									if (temp.size() != 0) {
										w1 = v;
										breakNode = new ArrayList<>(temp);
										break;
									}
								}
							}
						}
					}

					// check if the new way is part of one of the parentWay of the nextWay's first
					// node
					for (Way v : nextWay.firstNode().getParentWays()) {
						if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
							if (!w.equals(v) && !v.isFirstLastNode(nextWay.firstNode())) {
								w1 = v;
								breakNode = Arrays.asList(nextWay.firstNode());
								break;
							} else if (w.equals(v)) {
								addNewWays(Arrays.asList(v), ind);
								prev = v;
								ind++;
								brk = true;
								break;
							}
						}
					}

					// check if the new way is part of one of the parentWay of the nextWay's first
					for (Way v : nextWay.lastNode().getParentWays()) {
						if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
							if (!w.equals(v) && !v.isFirstLastNode(nextWay.lastNode())) {
								w1 = v;
								breakNode = Arrays.asList(nextWay.lastNode());
								break;
							} else if (w.equals(v)) {
								addNewWays(Arrays.asList(v), ind);
								ind++;
								prev = v;
								brk = true;
								break;
							}
						}
					}

					if (w1 != null && brk == false) {
						SplitWayCommand result = SplitWayCommand.split(w1, breakNode, Collections.emptyList());
						if (result != null) {
							MainApplication.undoRedo.add(result);
							if (result.getOriginalWay().getNodes().contains(w.firstNode())
									&& result.getOriginalWay().getNodes().contains(w.lastNode()))
								w = result.getOriginalWay();
							else
								w = result.getNewWays().get(0);

							addNewWays(Arrays.asList(w), ind);
							prev = w;
							ind++;
						}

					} else if (brk == false) {
						System.out.println("none");
					}
				} else {
					addNewWays(Arrays.asList(w), ind);
					prev = w;
					ind++;
				}
			}
			Way way = members.get(currentIndex).getWay();
			Way nextWay = members.get(currentIndex + 1).getWay();
			Node n = findCommonFirstLastNode(nextWay, way, currentNode);
			currentNode = getOtherNode(nextWay, n);
			save();
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			currentNode = null;
		}
		previousWay = currentWay;
		if (currentIndex < members.size() - 1) {
			callNextWay(++currentIndex);
		} else
			deleteExtraWays();
	}

	List<Way> findCurrentEdge (){
		List<Way> lst = new ArrayList<>();
		lst.add(currentWay);
		int j = currentIndex;
		Way curr = currentWay;
		while (true) {
			int i = getPreviousWayIndex(j);
			if (i == -1) break;

			Way prevWay = members.get(i).getWay();

			if (prevWay == null) break;

			Node n = findCommonFirstLastNode(curr, prevWay);

			if (n == null) break;
			if (n.getParentWays().size() > 2) break;

			lst.add(prevWay);
			curr = prevWay;
			j = i;
		}
		return lst;
	}

	void removeCurrentEdge() {
		List<Integer> lst = new ArrayList<>();
		lst.add(currentIndex);
		int j = currentIndex;
		Way curr = currentWay;
		Node n = null;

		while (true) {
			int i = getPreviousWayIndex(j);
			if (i == -1) break;

			Way prevWay = members.get(i).getWay();

			if (prevWay == null) break;

			n = findCommonFirstLastNode(curr, prevWay);

			if (n == null) break;
			if (n.getParentWays().size() > 2) break;

			lst.add(i);
			curr = prevWay;
			j = i;
		}

		int prevInd = getPreviousWayIndex(j);

		Collections.reverse(lst);
		int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
		memberTableModel.remove(ind);
		for (int i = 0; i < ind.length; i++) {
			members.remove(ind[i] - i);
		}

		save();

		if (prevInd >= 0) {
			currentNode = n;
			currentIndex = prevInd;
			callNextWay(currentIndex);
		} else {
			notice = null;
			deleteExtraWays();
		}
	}

	void removeTemporarylayers() {
		List<MapViewPaintable> tempLayers = MainApplication.getMap().mapView.getTemporaryLayers();
		for (int i = 0; i < tempLayers.size(); i++)
			MainApplication.getMap().mapView.removeTemporaryLayer(tempLayers.get(i));
	}

	void defaultStates() {
		currentIndex = 0;
		currentNode = null;
		previousWay = null;
		noLinkToPreviousWay = true;
		nextIndex = true;
		extraWaysToBeDeleted = new ArrayList<>();
		halt = false;
	}

	@SuppressWarnings("javadoc")
	public void stop() {
		defaultStates();
		nextIndex = false;
		abort = true;
		removeTemporarylayers();
	}

	public void save() {
		editor.apply();
		// editor.reloadDataFromRelation();
	}

	private class MendRelationAddLayer extends AbstractMapViewPaintable {

		@Override
		public void paint(Graphics2D g, MapView mv, Bounds bbox) {
			MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
			paintVisitor.drawVariants();
		}
	}

	private class MendRelationRemoveLayer extends AbstractMapViewPaintable {

		@Override
		public void paint(Graphics2D g, MapView mv, Bounds bbox) {
			MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
			paintVisitor.drawOptionsToRemoveWays();
		}
	}

	private class MendRelationAddMultipleLayer extends AbstractMapViewPaintable {

		@Override
		public void paint(Graphics2D g, MapView mv, Bounds bbox) {
			MendRelationPaintVisitor paintVisitor = new MendRelationPaintVisitor(g, mv);
			paintVisitor.drawMultipleVariants(wayListColoring);
		}
	}

	class MendRelationPaintVisitor extends PaintVisitor {
		/** The graphics */
		private final Graphics g;
		/** The MapView */
		private final MapView mv;
		private HashMap<Way, List<Character>> waysColoring;

		public MendRelationPaintVisitor(Graphics2D g, MapView mv) {
			super(g, mv);
			this.g = g;
			this.mv = mv;
		}

		/*
		 * Functions in this class are directly taken from PTAssistantPaintVisitor with
		 * some slight modification
		 */

		void drawVariants() {
			drawFixVariantsWithParallelLines();

			Color[] colors = { new Color(0, 255, 150), new Color(255, 0, 0, 150), new Color(0, 0, 255, 150),
					new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

			double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
			double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

			boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
			Character chr = 'A';
			if (numeric)
				chr = '1';

			for (int i = 0; i < 5; i++) {
				if (wayColoring.containsValue(chr)) {
					drawFixVariantLetter(chr.toString(), colors[i], letterX, letterY, 50);
					letterY = letterY + 60;
				}
				chr++;
			}

			// display the "Esc", "Skip" label:
			drawFixVariantLetter("Esc", Color.WHITE, letterX, letterY, 40);
			letterY = letterY + 60;
			if (numeric) {
				drawFixVariantLetter("7 : Skip", Color.WHITE, letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("9 : Remove current edge(white)", Color.WHITE, letterX, letterY, 25);
			}
			else {
				drawFixVariantLetter("S : Skip", Color.WHITE, letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("Q : Remove current edge(white)", Color.WHITE, letterX, letterY, 25);
			}
		}

		void drawOptionsToRemoveWays() {
			drawFixVariantsWithParallelLinesWhileRemove();
			boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

			Color[] colors = { new Color(255, 0, 0, 150), new Color(0, 255, 0, 150), new Color(255, 255, 0, 150),
					new Color(0, 255, 255, 200) };

			double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
			double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

			if (notice != null) {
				drawFixVariantLetter("Error:  " + notice, Color.WHITE, letterX, letterY, 25);
				letterY = letterY + 60;
			}
			if (numeric) {
				drawFixVariantLetter("1 : Remove Ways", colors[1], letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("2 : Do Not Remove Ways", colors[0], letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("3 : Remove ways along with previous way", colors[3], letterX, letterY, 25);
				letterY = letterY + 60;
				if (notice.equals("vehicle travels against oneway restriction")) {
					drawFixVariantLetter("4 : Add oneway:bus=no to way", colors[2], letterX, letterY, 25);
				}
			} else {
				drawFixVariantLetter("A : Remove Ways", colors[1], letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("B : Do Not Remove Ways", colors[0], letterX, letterY, 25);
				letterY = letterY + 60;
				if (notice.equals("vehicle travels against oneway restriction")) {
					drawFixVariantLetter("C : Add oneway:bus=no to way", colors[2], letterX, letterY, 25);
					letterY = letterY + 60;
				}
				drawFixVariantLetter("R : Remove ways along with previous way", colors[3], letterX, letterY, 25);
			}

			letterY = letterY + 60;
			drawFixVariantLetter("Esc", Color.WHITE, letterX, letterY, 30);
		}

		void drawMultipleVariants(HashMap<Character, List<Way>> fixVariants) {
			waysColoring = new HashMap<>();
			addFixVariants(fixVariants);
			drawFixVariantsWithParallelLines(waysColoring);

			Color[] colors = { new Color(0, 255, 0, 150), new Color(255, 0, 0, 150), new Color(0, 0, 255, 150),
					new Color(255, 255, 0, 150), new Color(0, 255, 255, 150) };

			int colorIndex = 0;

			double letterX = MainApplication.getMap().mapView.getBounds().getMinX() + 20;
			double letterY = MainApplication.getMap().mapView.getBounds().getMinY() + 100;

			for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
				Character c = entry.getKey();
				if (fixVariants.get(c) != null) {
					drawFixVariantLetter(c.toString(), colors[colorIndex % 5], letterX, letterY, 50);
					colorIndex++;
					letterY = letterY + 60;
				}
			}

			boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

			// display the "Esc", "Skip" label:
			drawFixVariantLetter("Esc", Color.WHITE, letterX, letterY, 40);
			letterY = letterY + 60;
			if (numeric) {
				drawFixVariantLetter("7 : Skip", Color.WHITE, letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("9 : Remove current edge(white)", Color.WHITE, letterX, letterY, 25);
			}
			else {
				drawFixVariantLetter("S : Skip", Color.WHITE, letterX, letterY, 25);
				letterY = letterY + 60;
				drawFixVariantLetter("Q : Remove current edge(white)", Color.WHITE, letterX, letterY, 25);
			}

		}

		protected void drawFixVariantsWithParallelLines() {

			HashMap<Character, Color> colors = new HashMap<>();
			colors.put('A', new Color(0, 255, 0, 200));
			colors.put('B', new Color(255, 0, 0, 200));
			colors.put('C', new Color(0, 0, 255, 200));
			colors.put('D', new Color(255, 255, 0, 200));
			colors.put('E', new Color(0, 255, 255, 200));
			colors.put('1', new Color(0, 255, 0, 200));
			colors.put('2', new Color(255, 0, 0, 200));
			colors.put('3', new Color(0, 0, 255, 200));
			colors.put('4', new Color(255, 255, 0, 200));
			colors.put('5', new Color(0, 255, 255, 200));

			for (Entry<Way, Character> entry : wayColoring.entrySet()) {
				Way way = entry.getKey();
				for (Pair<Node, Node> nodePair : way.getNodePairs(false)) {
					Character letter = entry.getValue();
					drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(colors.get(letter)));
				}
			}
			for (Pair<Node, Node> nodePair : currentWay.getNodePairs(false)) {
				drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(new Color(255, 255, 255, 190)));
			}
			for (Pair<Node, Node> nodePair : nextWay.getNodePairs(false)) {
				drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(new Color(169, 169, 169, 210)));
			}
		}

		protected void drawFixVariantsWithParallelLines(Map<Way, List<Character>> waysColoring) {

			HashMap<Character, Color> colors = new HashMap<>();
			colors.put('A', new Color(0, 255, 0, 200));
			colors.put('B', new Color(255, 0, 0, 200));
			colors.put('C', new Color(0, 0, 255, 200));
			colors.put('D', new Color(255, 255, 0, 200));
			colors.put('E', new Color(0, 255, 255, 200));
			colors.put('1', new Color(0, 255, 0, 200));
			colors.put('2', new Color(255, 0, 0, 200));
			colors.put('3', new Color(0, 0, 255, 200));
			colors.put('4', new Color(255, 255, 0, 200));
			colors.put('5', new Color(0, 255, 255, 200));

			for (Entry<Way, List<Character>> entry : waysColoring.entrySet()) {
				Way way = entry.getKey();
				List<Character> letterList = waysColoring.get(way);
				List<Color> wayColors = new ArrayList<>();
				for (Character letter : letterList) {
					wayColors.add(colors.get(letter));
				}
				for (Pair<Node, Node> nodePair : way.getNodePairs(false)) {
					drawSegmentWithParallelLines(nodePair.a, nodePair.b, wayColors);
				}
			}

			List<Way> currentEdge = findCurrentEdge();

			for (Way w : currentEdge) {
				for (Pair<Node, Node> nodePair : w.getNodePairs(false)) {
					drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(new Color(255, 255, 255, 190)));
				}
			}

			for (Pair<Node, Node> nodePair : nextWay.getNodePairs(false)) {
				drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(new Color(169, 169, 169, 210)));
			}

		}

		void drawFixVariantsWithParallelLinesWhileRemove() {

			HashMap<Character, Color> colors = new HashMap<>();
			colors.put('A', new Color(0, 255, 0, 200));
			colors.put('B', new Color(255, 0, 0, 200));
			colors.put('C', new Color(0, 0, 255, 200));
			colors.put('D', new Color(255, 255, 0, 200));
			colors.put('E', new Color(0, 255, 255, 200));
			colors.put('1', new Color(0, 255, 0, 200));
			colors.put('2', new Color(255, 0, 0, 200));
			colors.put('3', new Color(0, 0, 255, 200));
			colors.put('4', new Color(255, 255, 0, 200));
			colors.put('5', new Color(0, 255, 255, 200));

			for (Entry<Way, Character> entry : wayColoring.entrySet()) {
				Way way = entry.getKey();
				for (Pair<Node, Node> nodePair : way.getNodePairs(false)) {
					Character letter = entry.getValue();
					drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(colors.get(letter)));
				}
			}
			for (Pair<Node, Node> nodePair : currentWay.getNodePairs(false)) {
				drawSegmentWithParallelLines(nodePair.a, nodePair.b, Arrays.asList(new Color(255, 255, 255, 190)));
			}
		}

		void drawSegmentWithParallelLines(Node n1, Node n2, List<Color> colors) {
			if (!n1.isDrawable() || !n2.isDrawable() || !isSegmentVisible(n1, n2)) {
				return;
			}

			Point p1 = mv.getPoint(n1);
			Point p2 = mv.getPoint(n2);
			double t = Math.atan2((double) p2.x - p1.x, (double) p2.y - p1.y);
			double cosT = 9 * Math.cos(t);
			double sinT = 9 * Math.sin(t);
			double heightCosT = 9 * Math.cos(t);
			double heightSinT = 9 * Math.sin(t);

			double prevPointX = p1.x;
			double prevPointY = p1.y;
			double nextPointX = p1.x + heightSinT;
			double nextPointY = p1.y + heightCosT;

			Color currentColor = colors.get(0);
			int i = 0;
			g.setColor(currentColor);
			g.fillOval(p1.x - 9, p1.y - 9, 18, 18);

			if (colors.size() == 1) {
				int[] xPoints = { (int) (p1.x + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT), (int) (p1.x - cosT) };
				int[] yPoints = { (int) (p1.y - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT), (int) (p1.y + sinT) };
				g.setColor(currentColor);
				g.fillPolygon(xPoints, yPoints, 4);
			} else {
				boolean iterate = true;
				while (iterate) {
					currentColor = colors.get(i % colors.size());

					int[] xPoints = { (int) (prevPointX + cosT), (int) (nextPointX + cosT), (int) (nextPointX - cosT),
							(int) (prevPointX - cosT) };
					int[] yPoints = { (int) (prevPointY - sinT), (int) (nextPointY - sinT), (int) (nextPointY + sinT),
							(int) (prevPointY + sinT) };
					g.setColor(currentColor);
					g.fillPolygon(xPoints, yPoints, 4);

					prevPointX = prevPointX + heightSinT;
					prevPointY = prevPointY + heightCosT;
					nextPointX = nextPointX + heightSinT;
					nextPointY = nextPointY + heightCosT;
					i++;
					if ((p1.x < p2.x && nextPointX >= p2.x) || (p1.x >= p2.x && nextPointX <= p2.x)) {
						iterate = false;
					}
				}

				int[] lastXPoints = { (int) (prevPointX + cosT), (int) (p2.x + cosT), (int) (p2.x - cosT),
						(int) (prevPointX - cosT) };
				int[] lastYPoints = { (int) (prevPointY - sinT), (int) (p2.y - sinT), (int) (p2.y + sinT),
						(int) (prevPointY + sinT) };
				g.setColor(currentColor);
				g.fillPolygon(lastXPoints, lastYPoints, 4);
			}

			g.setColor(currentColor);
			g.fillOval(p2.x - 9, p2.y - 9, 18, 18);
		}

		void addFixVariants(HashMap<Character, List<Way>> fixVariants) {
			for (Entry<Character, List<Way>> entry : fixVariants.entrySet()) {
				Character currentFixVariantLetter = entry.getKey();
				List<Way> fixVariant = entry.getValue();
				for (Way way : fixVariant) {
					if (waysColoring.containsKey(way)) {
						if (!waysColoring.get(way).contains(currentFixVariantLetter)) {
							waysColoring.get(way).add(currentFixVariantLetter);
						}
					} else {
						List<Character> letterList = new ArrayList<>();
						letterList.add(currentFixVariantLetter);
						waysColoring.put(way, letterList);
					}
				}
			}
		}

		/**
		 * Visuallizes the letters for each fix variant
		 */
		private void drawFixVariantLetter(String letter, Color color, double letterX, double letterY, int size) {
			g.setColor(color);
			Font stringFont = new Font("SansSerif", Font.PLAIN, size);
			g.setFont(stringFont);
			try {
				g.drawString(letter, (int) letterX, (int) letterY);
				g.drawString(letter, (int) letterX, (int) letterY);
			} catch (NullPointerException ex) {
				// do nothing
				Logging.trace(ex);
			}

		}
	}

	class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
           editor.cancel();
           System.out.println("close");
           stop();
        }
    }
}
