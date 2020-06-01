// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mendaction;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.naming.CannotProceedException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.command.SplitWayCommand.Strategy;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.pt_assistant.PTAssistantPluginPreferences;
import org.openstreetmap.josm.plugins.pt_assistant.actions.mendaction.AbstractMendRelationAction;
import org.openstreetmap.josm.plugins.pt_assistant.actions.mendaction.MendRelationInterface;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.NotificationUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mend the relations by going through each way, sorting them and proposing
 * fixes for the gaps that are found
 *
 * @author Biswesh
 */
public class PTMendRelationAction extends AbstractMendRelationAction {
    boolean firstCall = true;
    int nodeIdx = 0;

    /**
     *
     * @param editorAccess
     */
    public PTMendRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    public void initialise() {
        save();
        sortBelow(relation.getMembers());
        members = editor.getRelation().getMembers();

        // halt is true indicates the action was paused
        if (!halt) {
            downloadCounter = 0;
            firstCall = false;
            waysAlreadyPresent = new HashMap<>();
            extraWaysToBeDeleted = new ArrayList<>();
            setEnable = false;
            previousWay = null;
            currentWay = null;
            nextWay = null;
            noLinkToPreviousWay = true;
            nextIndex = true;
            shorterRoutes = false;
            showOption0 = false;
            currentIndex = 0;

            getListOfAllWays();

            final JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            final JCheckBox button1 = new JCheckBox("Around Stops");
            final JCheckBox button2 = new JCheckBox("Around Gaps");
            final JCheckBox button3 = new JCheckBox("On the fly");
            button3.setSelected(true);
            panel.add(new JLabel(tr("How would you want the download to take place?")), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button1, GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button2, GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(button3, GBC.eol().fill(GBC.HORIZONTAL));

            int i = JOptionPane.showConfirmDialog(null, panel);
            if (i == JOptionPane.OK_OPTION) {
                if (button1.isSelected()) {
                    aroundStops = true;
                } else if (button2.isSelected()) {
                    aroundGaps = true;
                } else if (button3.isSelected()) {
                    onFly = true;
                }
                downloadEntireArea();
            }
        } else {
            halt = false;
            callNextWay(currentIndex);
        }
    }

    public void callNextWay(int i) {
        Logging.debug("Index + " + i);
        downloadCounter++;
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
            callNextWay(++currentIndex);
        }
    }


    public void addNewWays(List<Way> ways, int i) {
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
            Logging.error(e);
        }
    }

    public void deleteWayAfterIndex(Way way, int index) {
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
                    if (WayUtils.findNumberOfCommonFirstLastNodes(prev, way) != 0
                        && WayUtils.findNumberOfCommonFirstLastNodes(way, nextWay) != 0) {
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

    public void findNextWayAfterDownload(Way way, Node node1, Node node2) {
        currentWay = way;
        if (abort)
            return;

        List<Way> parentWays = findNextWay(way, node1);
        if (node2 != null)
            parentWays.addAll(findNextWay(way, node2));

        List<List<Way>> directRoute = getDirectRouteBetweenWays(currentWay, nextWay);
        if (directRoute == null || directRoute.size() == 0)
            showOption0 = false;
        else
            showOption0 = true;

        if (directRoute != null && directRoute.size() > 0 && !shorterRoutes && parentWays.size() > 0
            && notice == null) {
            displayFixVariantsWithOverlappingWays(directRoute);
            return;
        }

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
            }
        }
    }

    public List<List<Way>> getDirectRouteBetweenWays(Way current, Way next) {
        List<List<Way>> list = new ArrayList<>();
        List<Relation> r1;
        List<Relation> r2;
        try {
            r1 = new ArrayList<>(Utils.filteredCollection(current.getReferrers(), Relation.class));
            r2 = new ArrayList<>(Utils.filteredCollection(next.getReferrers(), Relation.class));
        } catch (Exception e) {
            return list;
        }

        List<Relation> rel = new ArrayList<>();
        String value = relation.get("route");

        for (Relation R1 : r1) {
            if (r2.contains(R1) && value.equals(R1.get("route")))
                rel.add(R1);
        }
        rel.remove(relation);

        for (Relation r : rel) {
            List<Way> lst = searchWayFromOtherRelations(r, current, next, false);
            boolean alreadyPresent = false;
            if (lst != null) {
                for (List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
            lst = searchWayFromOtherRelations(r, current, next, true);

            if (lst != null) {
                alreadyPresent = false;
                for (List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
        }

        return list;
    }

    public boolean isOneWayOrRoundabout(Way way) {
        return currentWay.isOneway() == 0
            && RouteUtils.isOnewayForPublicTransport(currentWay) == 0
            && !isSplitRoundabout(currentWay);
    }

    public List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
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

        // check if one of the way's intermediate node equals the first or last node of next way,
        // if so then break it(finally split in method getNextWayAfterSelection if the way is chosen)
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
        parentWays.stream().filter(it -> WayUtils.isOneWay(it) && !checkOneWaySatisfiability(it, node))
            .forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);
        waysToBeRemoved.clear();

        // check if both nodes of the ways are common, then remove
        for (Way w : parentWays) {
            if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) != 1 && !w.hasTag("junction", "roundabout")) {
                waysToBeRemoved.add(w);
            }
        }

        // check if any of them belong to roundabout, if yes then show ways accordingly
        parentWays.stream()
            .filter(it -> it.hasTag("junction", "roundabout")
                && WayUtils.findNumberOfCommonFirstLastNodes(way, it) == 1 && it.lastNode().equals(node))
            .forEach(waysToBeRemoved::add);

        // check mode of transport, also check if there is no loop
        if (relation.hasTag("route", "bus")) {
            parentWays.stream().filter(it -> !WayUtils.isSuitableForBuses(it)).forEach(waysToBeRemoved::add);
        } else if (RouteUtils.isPTRoute(relation)) {
            parentWays.stream().filter(it -> !isWaySuitableForOtherModes(it)).forEach(waysToBeRemoved::add);
        }

        parentWays.stream().filter(it -> it.equals(previousWay)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);
        waysToBeRemoved.clear();

        // check restrictions
        parentWays.stream().filter(it -> isRestricted(way, node)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);

        return parentWays;
    }

    public boolean checkOneWaySatisfiability(Way way, Node node) {
        String[] acceptedTags = new String[] { "yes", "designated" };

        if ((way.hasTag("oneway:bus", acceptedTags) || way.hasTag("oneway:psv", acceptedTags))
            && way.lastNode().equals(node) && relation.hasTag("route", "bus"))
            return false;

        if (!isNonSplitRoundabout(way) && way.hasTag("junction", "roundabout")) {
            if (way.lastNode().equals(node))
                return false;
        }

        if (RouteUtils.isOnewayForPublicTransport(way) == 0)
            return true;
        else if (RouteUtils.isOnewayForPublicTransport(way) == 1 && way.lastNode().equals(node))
            return false;
        else if (RouteUtils.isOnewayForPublicTransport(way) == -1 && way.firstNode().equals(node))
            return false;

        return true;
    }

    public void backtrackCurrentEdge() {
        Way way = currentWay;
        backNodes = way.getNodes();
        if (currentNode == null) {
            currentNode = currentWay.lastNode();
        }
        if (currentNode.equals(way.lastNode())) {
            Collections.reverse(backNodes);
        }
        int idx = 1;
        previousCurrentNode = currentNode;
        backTrack(currentWay, idx);
    }

    public void backTrack(Way way, int idx) {
        if (idx >= backNodes.size() - 1) {
            currentNode = previousCurrentNode;
            callNextWay(currentIndex);
            return;
        }
        Node nod = backNodes.get(idx);
        if (way.isInnerNode(nod)) {
            List<Way> fixVariants = new ArrayList<>();
            List<Way> allWays = nod.getParentWays();
            if (allWays != null) {
                for (Way w : allWays) {
                    if (!w.equals(currentWay)) {
                        if (!WayUtils.isOneWay(w)) {
                            if (relation.hasTag("route", "bus")) {
                                if (WayUtils.isSuitableForBuses(w)) {
                                    fixVariants.add(w);
                                }
                            } else if (relation.hasTag("route", "bicycle")) {
                                if (WayUtils.isSuitableForBicycle(w)) {
                                    fixVariants.add(w);
                                }
                            }
                        } else {
                            if (w.firstNode().equals(nod)) {
                                if (relation.hasTag("route", "bus")) {
                                    if (WayUtils.isSuitableForBuses(w)) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag("route", "bicycle")) {
                                    if (WayUtils.isSuitableForBicycle(w)) {
                                        fixVariants.add(w);
                                    }
                                }
                            } else {
                                if (relation.hasTag("route", "bus")) {
                                    if (RouteUtils.isOnewayForPublicTransport(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag("route", "bicycle")) {
                                    if (RouteUtils.isOnewayForBicycles(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            currentNode = nod;
            if (fixVariants.size() > 0) {
                displayBacktrackFixVariant(fixVariants, idx);
            } else {
                backTrack(way, idx + 1);
            }
        }
    }

    public Way findWayAfterChunk(Way way) {
        Way w1 = null;
        Way wayToKeep = null;
        List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        Strategy strategy = new TempStrategy();
        List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(currentWay, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
            w1 = result.getNewWays().get(0);
            wayToKeep = w1;
        }
        return wayToKeep;
    }

    public void getNextWayAfterBackTrackSelection(Way way) {
        save();
        List<Integer> lst = new ArrayList<>();
        lst.add(currentIndex + 1);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ind);
        Way temp = members.get(ind[0]).getWay();
        for (int i = 0; i < ind.length; i++) {
            members.remove(ind[i] - i);
        }
        save();
        List<RelationMember> c = new ArrayList<>();
        List<Way> ways = new ArrayList<>();
        ways.add(temp);
        int p = currentIndex;
        c.add(new RelationMember("", ways.get(0)));
        members.addAll(p + 1, c);
        save();
        int indx = currentIndex;
        addNewWays(Collections.singletonList(way), indx);
        currentNode = getOtherNode(way, currentNode);
        if (currentIndex < members.size() - 1) {
            callNextWay(++currentIndex);
        } else {
            deleteExtraWays();
        }
    }

    public void getNextWayAfterSelection(List<Way> ways) {
        if (ways != null) {
            /*
             * check if the selected way is not a complete way but rather a part of a parent
             * way, then split the actual way (the partial way was created in method
             * removeViolatingWaysFromParentWays but here we are finally splitting the
             * actual way and adding to the relation) here there can be 3 cases - 1) if the
             * current node is the node splitting a certain way 2) if next way's first node
             * is splitting the way 3) if next way's last node is splitting the way
             */
            Logging.debug("Number of ways " + ways.size());
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
                                    addNewWays(Collections.singletonList(v), ind);
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
                                breakNode = Collections.singletonList(nextWay.firstNode());
                                break;
                            } else if (w.equals(v)) {
                                addNewWays(Collections.singletonList(v), ind);
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
                                breakNode = Collections.singletonList(nextWay.lastNode());
                                break;
                            } else if (w.equals(v)) {
                                addNewWays(Collections.singletonList(v), ind);
                                ind++;
                                prev = v;
                                brk = true;
                                break;
                            }
                        }
                    }

                    if (w1 != null && !brk) {
                        SplitWayCommand result = SplitWayCommand.split(w1, breakNode, Collections.emptyList());
                        if (result != null) {
                            UndoRedoHandler.getInstance().add(result);
                            if (result.getOriginalWay().getNodes().contains(w.firstNode())
                                && result.getOriginalWay().getNodes().contains(w.lastNode()))
                                w = result.getOriginalWay();
                            else
                                w = result.getNewWays().get(0);

                            addNewWays(Collections.singletonList(w), ind);
                            prev = w;
                            ind++;
                        }

                    } else if (!brk) {
                        Logging.debug("none");
                    }
                } else {
                    if (w.isInnerNode(currentNode) && !w.firstNode().equals(w.lastNode())) {
                        findWayafterchunkRoundabout(w);
                    }
                    addNewWays(Collections.singletonList(w), ind);
                    prev = w;
                    ind++;
                }
            }
            Way way = members.get(currentIndex).getWay();
            Way nexWay = members.get(currentIndex + 1).getWay();
            Node n = WayUtils.findCommonFirstLastNode(nexWay, way, currentNode).orElse(null);
            currentNode = getOtherNode(nexWay, n);
            save();
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Logging.error(e);
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

    public void removeCurrentEdge() {
        List<Integer> lst = new ArrayList<>();
        lst.add(currentIndex);
        int j = currentIndex;
        Way curr = currentWay;
        Node n = getOtherNode(curr, currentNode);

        while (true) {
            int i = getPreviousWayIndex(j);
            if (i == -1)
                break;

            Way prevWay = members.get(i).getWay();

            if (prevWay == null)
                break;

            if (!WayUtils.findCommonFirstLastNode(curr, prevWay).filter(node -> node.getParentWays().size() <= 2).isPresent()) {
                break;
            }

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

        /*
        TODO : NOT IN INTERFACE
         */

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

    List<Way> findCurrentEdge() {
        List<Way> lst = new ArrayList<>();
        lst.add(currentWay);
        int j = currentIndex;
        Way curr = currentWay;
        while (true) {
            int i = getPreviousWayIndex(j);
            if (i == -1)
                break;

            Way prevWay = members.get(i).getWay();

            if (prevWay == null)
                break;

            if (!WayUtils.findCommonFirstLastNode(curr, prevWay).filter(node -> node.getParentWays().size() <= 2)
                .isPresent()) {
                break;
            }

            lst.add(prevWay);
            curr = prevWay;
            j = i;
        }
        return lst;
    }

    protected void removeWayAfterSelection(List<Integer> wayIndices, Character chr) {
        if (chr == 'A' || chr == '1') {
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
        } else if (chr == 'B' || chr == '2') {
            if (currentIndex < members.size() - 1) {
                notice = null;
                currentIndex = wayIndices.get(wayIndices.size() - 1);
                callNextWay(currentIndex);
            } else {
                notice = null;
                deleteExtraWays();
            }
        } else if (chr == 'C' || chr == '4') {
            List<Command> cmdlst = new ArrayList<>();
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            for (int i = 0; i < lst.length; i++) {
                Way w = members.get(lst[i]).getWay();
                TagMap newKeys = w.getKeys();
                newKeys.put("oneway", "bus=no");
                cmdlst.add(new ChangePropertyCommand(Collections.singleton(w), newKeys));
            }
            UndoRedoHandler.getInstance().add(new SequenceCommand("Add tags", cmdlst));
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
        if (chr == 'R' || chr == '3') {
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


    private void findWayafterchunkRoundabout(Way way) {
        List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        splitNode = way.lastNode();
        Strategy strategy = new TempStrategyRoundabout();
        List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(way, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
        }
    }

    private void removeKeyListenerAndTemporaryLayer(KeyListener keyListener) {
        MainApplication.getMap().mapView.removeKeyListener(keyListener);
        MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
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
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayListColoring.put(alphabet, fixVariants.get(i));
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporaryLayers();

        if (abort)
            return;

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        // display the fix variants:
        temporaryLayer = new MendRelationAddMultipleLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    setEnable = true;
                    shorterRoutes = false;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

    public void downloadAreaAroundWay(Way way) {
        if (abort) {
            return;
        }

        if (downloadCounter > 160 && onFly) {
            downloadCounter = 0;
            DownloadOsmTask task = new DownloadOsmTask();
            BoundsUtils.createBoundsWithPadding(way.getBBox(), .1).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);
                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (2)");
                        if (currentIndex >= members.size() - 1) {
                            deleteExtraWays();
                        } else {
                            callNextWay(++currentIndex);
                        }
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            if (currentIndex >= members.size() - 1) {
                deleteExtraWays();
            } else {
                callNextWay(++currentIndex);
            }
        }
    }

    protected void downloadAreaAroundWay(Way way, Way prevWay, List<Way> ways) {
        if (abort) {
            return;
        }

        if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
            downloadCounter = 0;
            DownloadOsmTask task = new DownloadOsmTask();
            BoundsUtils.createBoundsWithPadding(way.getBBox(), .2).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);

                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (3)");
                        goToNextWays(way, prevWay, ways);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            goToNextWays(way, prevWay, ways);
        }
    }

    void downloadAreaAroundWay(Way way, Node node1, Node node2) {
        if (abort) {
            return;
        }

        if ((downloadCounter > 160 || way.isOutsideDownloadArea() || way.isNew()) && onFly) {
            downloadCounter = 0;
            DownloadOsmTask task = new DownloadOsmTask();
            BoundsUtils.createBoundsWithPadding(way.getBBox(), .4).ifPresent(area -> {
                Future<?> future = task.download(DEFAULT_DOWNLOAD_PARAMS, area, null);
                MainApplication.worker.submit(() -> {
                    try {
                        NotificationUtils.downloadWithNotifications(future, tr("Area around way") + " (1)");
                        findNextWayAfterDownload(way, node1, node2);
                    } catch (InterruptedException | ExecutionException e1) {
                        Logging.error(e1);
                    }
                });
            });
        } else {
            findNextWayAfterDownload(way, node1, node2);
        }
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

        // check if the way equals the next way, if so then don't add any new ways to the list
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

    void displayFixVariants(List<Way> fixVariants) {
        final List<Character> allowedCharacters;
        try {
            allowedCharacters = findLettersVariants(fixVariants);
        } catch (CannotProceedException e) {
            return;
        }

        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();

        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numerics and the plain numerics
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backtrackCurrentEdge();
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(Collections.singletonList(fixVariants.get(idx)));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
    }

    void displayBacktrackFixVariant(List<Way> fixVariants, int idx1) {
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();
        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporaryLayers();

        if (abort)
            return;

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        MainApplication.getMap().mapView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                downloadCounter = 0;
                if (abort) {
                    removeKeyListenerAndTemporaryLayer(this);
                    return;
                }
                char typedKeyUpperCase = Character.toString(e.getKeyChar()).toUpperCase().toCharArray()[0];
                if (allowedCharacters.contains(typedKeyUpperCase)) {
                    int idx = typedKeyUpperCase - 65;
                    if (numeric) {
                        // for numpad numbers and the plain numbers
                        if (typedKeyUpperCase <= 57)
                            idx = typedKeyUpperCase - 49;
                        else
                            idx = typedKeyUpperCase - 97;
                    }
                    nextIndex = true;
                    if (typedKeyUpperCase == 'S' || typedKeyUpperCase == '7') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        getNextWayAfterSelection(null);
                    } else if (typedKeyUpperCase == 'Q' || typedKeyUpperCase == '9') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        removeCurrentEdge();
                    } else if (typedKeyUpperCase == 'W' || typedKeyUpperCase == '0') {
                        shorterRoutes = !shorterRoutes;
                        removeKeyListenerAndTemporaryLayer(this);
                        callNextWay(currentIndex);
                    } else if (typedKeyUpperCase == 'V' || typedKeyUpperCase == '8') {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        backTrack(currentWay, idx1 + 1);
                    } else {
                        removeKeyListenerAndTemporaryLayer(this);
                        shorterRoutes = false;
                        findWayAfterChunk(currentWay);
                        getNextWayAfterBackTrackSelection(fixVariants.get(idx));
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    MainApplication.getMap().mapView.removeKeyListener(this);
                    nextIndex = false;
                    shorterRoutes = false;
                    setEnable = true;
                    halt = true;
                    setEnabled(true);
                    MainApplication.getMap().mapView.removeTemporaryLayer(temporaryLayer);
                }
            }
        });
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

    private List<Character> findLettersVariants(List<Way> fixVariants) throws CannotProceedException {
        char alphabet = 'A';
        boolean numeric = PTAssistantPluginPreferences.NUMERICAL_OPTIONS.get();
        wayColoring = new HashMap<>();
        final List<Character> allowedCharacters = new ArrayList<>();
        if (numeric) {
            alphabet = '1';
            allowedCharacters.add('7');
            if (showOption0)
                allowedCharacters.add('0');
            allowedCharacters.add('8');
            allowedCharacters.add('9');
        } else {
            allowedCharacters.add('S');
            if (showOption0)
                allowedCharacters.add('W');
            allowedCharacters.add('V');
            allowedCharacters.add('Q');
        }

        for (int i = 0; i < 5 && i < fixVariants.size(); i++) {
            allowedCharacters.add(alphabet);
            wayColoring.put(fixVariants.get(i), alphabet);
            alphabet++;
        }

        // remove any existing temporary layer
        removeTemporaryLayers();

        if (abort) {
            throw new CannotProceedException("Aborted");
        }

        // zoom to problem:
        AutoScaleAction.zoomTo(fixVariants);

        // display the fix variants:
        temporaryLayer = new MendRelationAddLayer();
        MainApplication.getMap().mapView.addTemporaryLayer(temporaryLayer);

        // // add the key listener:
        MainApplication.getMap().mapView.requestFocus();
        return allowedCharacters;
    }






    Node checkValidityOfWays(Way way, int nextWayIndex) {
        boolean nextWayDelete = false;
        Node node = null;
        nextIndex = false;
        notice = null;

        final NodePair commonEndNodes = WayUtils.findCommonFirstLastNodes(nextWay, way);
        if (commonEndNodes.getA() != null && commonEndNodes.getB() != null) {
            nextWayDelete = true;
            notice = "Multiple common nodes found between current and next way";
        } else if (commonEndNodes.getA() != null) {
            node = commonEndNodes.getA();
        } else if (commonEndNodes.getB() != null) {
            node = commonEndNodes.getB();
        } else {
            // the nodes can be one of the intermediate nodes
            for (Node n : nextWay.getNodes()) {
                if (way.getNodes().contains(n)) {
                    node = n;
                    currentNode = n;
                }
            }
        }

        // check if there is a restricted relation that doesn't allow both the ways together
        if (node != null && (nextWay, way, node)) {
            nextWayDelete = true;
        }

        if (isNonSplitRoundabout(way)) {
            nextWayDelete = false;
            for (Node n : way.getNodes()) {
                if (nextWay.firstNode().equals(n) || nextWay.lastNode().equals(n)) {
                    node = n;
                    currentNode = n;
                }
            }
        }

        if (isNonSplitRoundabout(nextWay)) {
            nextWayDelete = false;
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
}
