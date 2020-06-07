// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions.mend_relation;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.plugins.pt_assistant.actions.MendRelationAction;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author Biswesh and sudhanshu2
 */
public class PublicTransportMendRelation extends AbstractMendRelationAction {
    HashMap<Way, Integer> waysAlreadyPresent = null;

    int nodeIdx = 0;
    AbstractMapViewPaintable temporaryLayer = null;
    java.util.List<Node> backnodes = new ArrayList<>();

    public PublicTransportMendRelation(IRelationEditorActionAccess editorAccess) {
        super(editorAccess,
            I18n.marktr("Add oneway:bus=no to way"),
            "   [\"highway\"][\"highway\"!=\"footway\"][\"highway\"!=\"path\"][\"highway\"!=\"cycleway\"];\n");

        editor = (GenericRelationEditor) editorAccess.getEditor();
        memberTableModel = editorAccess.getMemberTableModel();
        relation = editor.getRelation();
        editor.addWindowListener(new AbstractMendRelationAction.WindowEventHandler());
    }

    /**
     * Initializes the Public Transport Mend Relation
     */
    public void initialise() {
        save();
        sortBelow(relation.getMembers(), 0);
        members = editor.getRelation().getMembers();
        if (!halt) {
            initializeStates();
            getListOfAllWays();
            makePanelToDownloadArea();
        } else {
            halt = false;
            callNextWay(currentIndex);
        }
    }

    /**
     *
     * @param way
     * @return
     */
    protected boolean isOneWayOrRoundabout(Way way) {
        return currentWay.isOneway() == 0
            && RouteUtils.isOnewayForPublicTransport(currentWay) == 0
            && !isSplitRoundAbout(currentWay);
    }























    /* Overriden in PersonalTransport */


    @Override
    public List<Way> getListOfAllWays() {
        return null;
    }

    void callNextWay(int i) {
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
                    downloadArea.downloadAreaAroundWay(way);
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
                    downloadArea.downloadAreaAroundWay(way);
                } else {
                    currentNode = nextWay.lastNode();
                    previousWay = way;
                    nextIndex = false;
                    downloadArea.downloadAreaAroundWay(way);
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

    boolean checkOneWaySatisfiability(Way way, Node node) {
        String[] acceptedTags = new String[] { "yes", "designated" };

        if ((way.hasTag("oneway:bus", acceptedTags) || way.hasTag("oneway:psv", acceptedTags))
            && way.lastNode().equals(node) && relation.hasTag("route", "bus"))
            return false;

        if (!isNonSplitRoundAbout(way) && way.hasTag("junction", "roundabout")) {
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

    Way findNextWayAfterDownload(Way way, Node node1, Node node2) {
        currentWay = way;
        if (abort)
            return null;

        java.util.List<Way> parentWays = findNextWay(way, node1);
        if (node2 != null)
            parentWays.addAll(findNextWay(way, node2));

        java.util.List<java.util.List<Way>> directRoute = getDirectRouteBetweenWays(currentWay, nextWay);
        if (directRoute == null || directRoute.size() == 0)
            showOption0 = false;
        else
            showOption0 = true;

        if (directRoute != null && directRoute.size() > 0 && !shorterRoutes && parentWays.size() > 0
            && notice == null) {
            displayFixVariantsWithOverlappingWays(directRoute);
            return null;
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
                return null;
            }
        }
        return null;
    }

    @Override
    public int getMemberSize() {
        return 0;
    }

    @Override
    public void callDisplayWaysToRemove(List<Integer> wayIndices) {

    }

    List<java.util.List<Way>> getDirectRouteBetweenWays(Way current, Way next) {
        java.util.List<java.util.List<Way>> list = new ArrayList<>();
        java.util.List<Relation> r1;
        java.util.List<Relation> r2;
        try {
            r1 = new ArrayList<>(Utils.filteredCollection(current.getReferrers(), Relation.class));
            r2 = new ArrayList<>(Utils.filteredCollection(next.getReferrers(), Relation.class));
        } catch (Exception e) {
            return list;
        }

        java.util.List<Relation> rel = new ArrayList<>();
        String value = relation.get("route");

        for (Relation R1 : r1) {
            if (r2.contains(R1) && value.equals(R1.get("route")))
                rel.add(R1);
        }
        rel.remove(relation);

        for (Relation r : rel) {
            java.util.List<Way> lst = searchWayFromOtherRelations(r, current, next);
            boolean alreadyPresent = false;
            if (lst != null) {
                for (java.util.List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
            lst = searchWayFromOtherRelationsReversed(r, current, next);

            if (lst != null) {
                alreadyPresent = false;
                for (java.util.List<Way> l : list) {
                    if (l.containsAll(lst))
                        alreadyPresent = true;
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
        }

        return list;
    }

    List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
        parentWays.remove(way);
        if (abort)
            return null;
        java.util.List<Way> waysToBeRemoved = new ArrayList<>();
        // check if any of the way is joining with its intermediate nodes
        java.util.List<Way> waysToBeAdded = new ArrayList<>();
        for (Way w : parentWays) {
            if (node != null && !w.isFirstLastNode(node)) {
                Way w1 = new Way();
                Way w2 = new Way();

                java.util.List<Node> lst1 = new ArrayList<>();
                java.util.List<Node> lst2 = new ArrayList<>();
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

                java.util.List<Node> lst1 = new ArrayList<>();
                java.util.List<Node> lst2 = new ArrayList<>();
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
        parentWays.stream().filter(it -> isRestricted(it, way, node)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);

        return parentWays;
    }

    public void backTrack(Way way, int idx) {
        if (idx >= backnodes.size() - 1) {
            currentNode = prevCurrenNode;
            callNextWay(currentIndex);
            return;
        }
        Node nod = backnodes.get(idx);
        if (way.isInnerNode(nod)) {
            java.util.List<Way> fixVariants = new ArrayList<>();
            java.util.List<Way> allWays = nod.getParentWays();
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

    @Override
    public Way getCurrentWay() {
        return null;
    }

    @Override
    public Way getNextWay() {
        return null;
    }

    public Way findWayAfterChunk(Way way) {
        Way w1 = null;
        Way wayToKeep = null;
        java.util.List<Node> breakNode = new ArrayList<>();
        breakNode.add(currentNode);
        SplitWayCommand.Strategy strategy = new MendRelationAction.TempStrategy();
        java.util.List<java.util.List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(currentWay, breakNode);
        SplitWayCommand result = SplitWayCommand.splitWay(way, wayChunks, Collections.emptyList(), strategy);
        if (result != null) {
            UndoRedoHandler.getInstance().add(result);
            w1 = result.getNewWays().get(0);
            wayToKeep = w1;
        }
        return wayToKeep;
    }

    void backtrackCurrentEdge() {
        Way backTrackWay = currentWay;
        Way way = backTrackWay;
        backnodes = way.getNodes();
        if (currentNode == null) {
            currentNode = currentWay.lastNode();
        }
        if (currentNode.equals(way.lastNode())) {
            Collections.reverse(backnodes);
        }
        int idx = 1;
        prevCurrenNode = currentNode;
        backTrack(currentWay, idx);
    }

    void getNextWayAfterBackTrackSelection(Way way) {
        save();
        java.util.List<Integer> lst = new ArrayList<>();
        lst.add(currentIndex + 1);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        memberTableModel.remove(ind);
        Way temp = members.get(ind[0]).getWay();
        for (int i = 0; i < ind.length; i++) {
            members.remove(ind[i] - i);
        }
        save();
        java.util.List<RelationMember> c = new ArrayList<>();
        java.util.List<Way> ways = new ArrayList<>();
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

    @Override
    public List<RelationMember> getMembers() {
        return null;
    }

    @Override
    public String getNotice() {
        return null;
    }

    @Override
    public void removeWayAfterSelection(List<Integer> wayIndices, char ch) {

    }

    void getNextWayAfterSelection(java.util.List<Way> ways) {
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
                java.util.List<Node> breakNode = null;
                boolean brk = false;

                if (w.isNew()) {
                    if (prev != null) {
                        java.util.List<Way> par = new ArrayList<>(prev.firstNode().getParentWays());
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
                                    java.util.List<Node> temp = new ArrayList<>();
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

    @Override
    public void removeTemporaryLayers() {

    }

    void addNewWays(java.util.List<Way> ways, int i) {
        try {
            java.util.List<RelationMember> c = new ArrayList<>();
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



    void removeCurrentEdge() {
        java.util.List<Integer> lst = new ArrayList<>();
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

            if (!WayUtils.findCommonFirstLastNode(curr, prevWay).filter(node -> node.getParentWays().size() <= 2)
                .isPresent()) {
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

    void RemoveWayAfterSelection(java.util.List<Integer> wayIndices, Character chr) {
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
            java.util.List<Command> cmdlst = new ArrayList<>();
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

}
