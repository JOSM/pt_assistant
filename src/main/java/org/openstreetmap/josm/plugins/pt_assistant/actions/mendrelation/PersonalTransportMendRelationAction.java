package org.openstreetmap.josm.plugins.pt_assistant.actions.mendrelation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mend the relations for buses, walking and horses by going through each way,
 * sorting them and proposing fixes for the gaps that are found
 *
 * @author Ashish Singh and sudhanshu2
 */
public class PersonalTransportMendRelationAction extends PublicTransportMendRelationAction {
    private static final String FORWARD = "forward";
    private static final String BACKWARD = "backward";
    private static final String ROUTE = "route";
    private static final String ROUNDABOUT = "roundabout";
    private static final String JUNCTION = "junction";
    private static final String BICYCLE = "bicycle";

    ////////////////////////Assigning Variables///////////////

    Way lastForWay;
    Way lastBackWay;
    int brokenidx;
    HashMap<Node, Integer> isThere = new HashMap<>();
    static HashMap<Way, Integer> isWaythere = new HashMap<>();
    static List<WayConnectionType> links;
    static WayConnectionType link;
    static WayConnectionType prelink;
    List<List<Way>> directroutes;
    WayConnectionTypeCalculator connectionTypeCalculator = new WayConnectionTypeCalculator();

    /////////////Editor Access To Bicycle Routing Helper//////////////

    public PersonalTransportMendRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        super.editor = (GenericRelationEditor) editorAccess.getEditor();
        super.memberTableModel = editorAccess.getMemberTableModel();
        super.relation = editor.getRelation();
        super.editor.addWindowListener(new PersonalTransportMendRelationAction.WindowEventHandler());
    }

    /////////////on action call initialise()/////////////

    @Override
    public void initialise() {
        save();
        sortBelow(super.relation.getMembers());
        super.members = super.editor.getRelation().getMembers();
        super.members.removeIf(m -> !m.isWay());
        links = connectionTypeCalculator.updateLinks(super.members);
        if (!super.halt) {
            updateStates();
            getListOfAllWays();
            makepanelanddownloadArea();
        } else {
            super.halt = false;
            callNextWay(super.currentIndex);
        }
    }

    @Override
    protected String getOverpassApiWaySelectorString() {
        return "[\"highway\"][\"highway\"!=\"motorway\"]";
    }

    ////////calling the nextway /////////
    //this function has to iterate over all super.members of relation table doesn't matter they are broken or not
    //so after every filtering this function has to be called with super.currentIndex+1
    @Override
    public void callNextWay(int idx) {
        save();
        Logging.debug("Index + " + idx);
        super.downloadCounter++;
        if (idx < super.members.size() && super.members.get(idx).isWay()) {
            if (super.currentNode == null)
                super.noLinkToPreviousWay = true;

            int nexidx = getNextWayIndex(idx);
            if (nexidx >= super.members.size()) {
                deleteExtraWays();
                makeNodesZeros();
                return;
            }

            link = links.get(nexidx);
            prelink = links.get(idx);

            if (prelink.isOnewayLoopBackwardPart) {
                lastBackWay = super.members.get(idx).getWay();
            }
            if (prelink.isOnewayLoopForwardPart) {
                lastForWay = super.members.get(idx).getWay();
            }

            Way way = super.members.get(idx).getWay();
            if (isWaythere.get(way) == null) {
                for (Node nod : way.getNodes()) {
                    if (isThere.get(nod) == null || isThere.get(nod) == 0) {
                        isThere.put(nod, 1);
                    } else {
                        isThere.put(nod, isThere.get(nod) + 1);
                    }
                }
                isWaythere.put(way, 1);
            }
            super.wayToReachAfterGap = super.members.get(nexidx).getWay();
            Node node = checkVaildityOfWays(way, nexidx);
            if (super.abort || super.nextIndex) {
                super.nextIndex = false;
                return;
            } else {
                super.nextIndex = true;
            }
            if (super.noLinkToPreviousWay) {
                if (node == null) {
                    super.currentWay = way;
                    super.nextIndex = false;
                    findNextWayBeforeDownload(way, way.firstNode(), way.lastNode());
                } else {
                    super.noLinkToPreviousWay = false;
                    super.previousWay = way;
                    super.currentNode = getOtherNode(super.wayToReachAfterGap, node);
                    super.nextIndex = false;
                    downloadAreaAroundWay(way);
                }
            } else {
                if (node == null) {
                    if (super.members.get(super.currentIndex).getRole().equals(FORWARD)
                            && prelink.isOnewayLoopBackwardPart) {
                        node = way.lastNode();
                    } else if (super.members.get(super.currentIndex).getRole().equals(BACKWARD)
                            && prelink.isOnewayLoopForwardPart) {
                        node = way.lastNode();
                    } else {
                        if (super.currentIndex - 1 >= 0) {
                            Way prevw = super.members.get(super.currentIndex - 1).getWay();
                            if (Objects.equals(prevw.firstNode(), way.lastNode()) || Objects.equals(prevw.lastNode(), way.lastNode())) {
                                node = way.lastNode();
                            } else {
                                node = way.firstNode();
                            }
                        }
                    }
                    if (link.isOnewayLoopBackwardPart) {
                        super.previousWay = way;
                        super.nextIndex = false;
                        if (isThere.get(way.firstNode()) != null && isThere.get(way.firstNode()) != 0) {
                            node = way.firstNode();
                        } else {
                            node = way.lastNode();
                        }
                        downloadAreaAroundWay(way);
                    } else {
                        super.currentNode = getOtherNode(way, node);
                        super.currentWay = way;
                        super.nextIndex = false;
                        findNextWayBeforeDownload(way, super.currentNode);
                    }
                } else {
                    super.previousWay = way;
                    super.currentNode = getOtherNode(super.wayToReachAfterGap, node);
                    super.nextIndex = false;
                    downloadAreaAroundWay(way);
                }
            }

        }
        if (super.abort)
            return;

        if (idx >= super.members.size() - 1) {
            deleteExtraWays();
            makeNodesZeros();
        } else if (super.nextIndex) {
            callNextWay(++super.currentIndex);
        }
    }

    @Override
    boolean checkOneWaySatisfiability(Way way, Node node) {
        String[] acceptedTags = new String[] { "yes", "designated" };

        if ((link.isOnewayLoopBackwardPart && super.relation.hasTag(ROUTE, BICYCLE))
                || prelink.isOnewayLoopBackwardPart) {
            return true;
        }

        if (way.hasTag("oneway:bicycle", acceptedTags) && Objects.equals(way.lastNode(), node)
                && relation.hasTag(ROUTE, BICYCLE))
            return false;

        if (!WayUtils.isNonSplitRoundAbout(way) && way.hasTag(JUNCTION, ROUNDABOUT) && Objects.equals(node, way.lastNode())) {
            return false;
        }

        if (RouteUtils.isOnewayForBicycles(way) == 0)
            return true;
        else if (RouteUtils.isOnewayForBicycles(way) == 1 && Objects.equals(way.lastNode(), node))
            return false;
        else if (RouteUtils.isOnewayForBicycles(way) == -1 && Objects.equals(way.firstNode(), node))
            return false;

        return true;
    }

    private Node checkVaildityOfWays(Way way, int nexidx) { // TODO: Fix typo: vaildity → validity
        boolean nexWayDelete = false;
        Node node = null;
        super.nextIndex = false;
        super.notice = null;
        final NodePair commonEndNodes = WayUtils.findCommonFirstLastNodes(super.wayToReachAfterGap, way);
        if (commonEndNodes.getA() != null && commonEndNodes.getB() != null) {
            nexWayDelete = true;
            super.notice = "Multiple common nodes found between current and next way";
        } else if (commonEndNodes.getA() != null) {
            node = commonEndNodes.getA();
        } else if (commonEndNodes.getB() != null) {
            node = commonEndNodes.getB();
        } else {
            // the nodes can be one of the intermediate nodes
            for (Node n : super.wayToReachAfterGap.getNodes()) {
                if (way.getNodes().contains(n)) {
                    node = n;
                    super.currentNode = n;
                }
            }
        }
        if (node != null && isRestricted(super.wayToReachAfterGap, way, node)) {
            nexWayDelete = true;
        }
        if (WayUtils.isNonSplitRoundAbout(way)) {
            nexWayDelete = false;
            for (Node n : way.getNodes()) {
                if (Objects.equals(super.wayToReachAfterGap.firstNode(), n) || Objects.equals(super.wayToReachAfterGap.lastNode(), n)) {
                    node = n;
                    super.currentNode = n;
                }
            }
        }

        if (WayUtils.isNonSplitRoundAbout(super.wayToReachAfterGap)) {
            nexWayDelete = false;
        }

        if (node != null && !checkOneWaySatisfiability(super.wayToReachAfterGap, node)) {
            nexWayDelete = true;
            super.notice = "vehicle travels against oneway restriction";
        }
        if (nexWayDelete) {
            super.currentWay = way;
            super.nextIndex = true;
            removeWay(nexidx);
            return null;
        }

        super.nextIndex = false;
        return node;

    }

    @Override
    void findNextWayAfterDownload(Way way, Node node1, Node node2) {
        // TODO Auto-generated method stub
        super.currentWay = way;
        if (super.abort)
            return;
        List<Way> parentWays = findNextWay(way, node1);

        if (node2 != null) {
            parentWays.addAll(findNextWay(way, node2));
        }
        directroutes = getDirectRouteBetweenWaysByLookingAtOtherRouteRelations(super.currentWay, super.wayToReachAfterGap);
        if (directroutes == null || directroutes.isEmpty()) {
            super.showOption0 = false;
        } else {
            super.showOption0 = true;
        }
        if (directroutes != null && !directroutes.isEmpty() && !super.shorterRoutes && !parentWays.isEmpty()
                && super.notice == null) {
            displayFixVariantsWithOverlappingWays(directroutes);
            return;
        }
        if (parentWays.size() == 1) {
            goToNextWays(parentWays.get(0), way, new ArrayList<>());
        } else if (parentWays.size() > 1) {
            super.nextIndex = false;
            displayFixVariants(parentWays);
        } else {
            super.nextIndex = true;
            if (super.currentIndex >= super.members.size() - 1) {
                deleteExtraWays();
                makeNodesZeros();
            } else {
                callNextWay(++super.currentIndex);
            }
        }
    }

    @Override
    List<List<Way>> getDirectRouteBetweenWaysByLookingAtOtherRouteRelations(Way current, Way next) {
        //trying to find the other route relations which can connect these ways
        List<List<Way>> list = new ArrayList<>();
        List<Relation> r1;
        List<Relation> r2;
        try {
            r1 = new ArrayList<>(Utils.filteredCollection(current.getReferrers(), Relation.class));
            r2 = new ArrayList<>(Utils.filteredCollection(next.getReferrers(), Relation.class));
        } catch (Exception e) {
            Logging.error(e);
            return list;
        }

        List<Relation> rel = new ArrayList<>();
        //checking whether relations you are getting are bicycle routes or not
        String value = super.relation.get(ROUTE);

        for (Relation R1 : r1) {
            if (r2.contains(R1) && value.equals(R1.get(ROUTE)))
                rel.add(R1);
        }
        rel.remove(super.relation);

        for (Relation r : rel) {
            List<Way> lst = searchWayFromOtherRelations(r, current, next, false);
            boolean alreadyPresent = false;
            if (lst != null) {
                for (List<Way> l : list) {
                    if (l.containsAll(lst)) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
            lst = searchWayFromOtherRelations(r, current, next, true);

            if (lst != null) {
                alreadyPresent = false;
                for (List<Way> l : list) {
                    if (l.containsAll(lst)) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if (!alreadyPresent)
                    list.add(lst);
            }
        }

        return list;
    }

    List<Way> searchWayFromOtherRelations(Relation r, Way current, Way next, boolean reverse) {
        List<RelationMember> member = r.getMembers();
        List<Way> lst = new ArrayList<>();
        boolean canAdd = false;
        Way prev = null;
        for (RelationMember relationMember : member) {
            if (relationMember.isWay()) {
                Way w = relationMember.getWay();
                if (!reverse) {
                    if (w.equals(current)) {
                        lst.clear();
                        canAdd = true;
                        prev = w;
                    } else if (w.equals(next) && !lst.isEmpty()) {
                        return lst;
                    } else if (canAdd) {
                        if (findNumberOfCommonNodes(w, prev) != 0) {
                            lst.add(w);
                            prev = w;
                        } else {
                            break;
                        }
                    }
                } else {
                    if (w.equals(next)) {
                        lst.clear();
                        canAdd = true;
                        prev = w;
                    } else if (w.equals(current) && !lst.isEmpty()) {
                        Collections.reverse(lst);
                        return lst;
                    } else if (canAdd) {
                        // not valid in reverse if it is oneway or part of roundabout
                        if (findNumberOfCommonNodes(w, prev) != 0 && RouteUtils.isOnewayForBicycles(w) == 0
                                && !WayUtils.isPartOfSplitRoundAbout(w)) {
                            lst.add(w);
                            prev = w;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    List<Way> removeInvalidWaysFromParentWays(List<Way> parentWays, Node node, Way way) {
        parentWays.remove(way);
        if (super.abort)
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

                if (!w.hasTag(JUNCTION, ROUNDABOUT)) {
                    waysToBeRemoved.add(w);
                    waysToBeAdded.add(w1);
                    waysToBeAdded.add(w2);
                }
                // if(IsWaythere.get(w))
            }
        }

        // check if one of the way's intermediate node equals the first or last node of next way,
        // if so then break it(finally split in method getNextWayAfterSelection if the way is chosen)
        for (Way w : parentWays) {
            Node nextWayNode = null;
            if (w.getNodes().contains(super.wayToReachAfterGap.firstNode()) && !w.isFirstLastNode(super.wayToReachAfterGap.firstNode())) {
                nextWayNode = super.wayToReachAfterGap.firstNode();
            } else if (w.getNodes().contains(super.wayToReachAfterGap.lastNode())
                    && !w.isFirstLastNode(super.wayToReachAfterGap.lastNode())) {
                nextWayNode = super.wayToReachAfterGap.lastNode();
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

                if (!w.hasTag(JUNCTION, ROUNDABOUT)) {
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

        for (Way w : parentWays) {
            if (WayUtils.findNumberOfCommonFirstLastNodes(way, w) != 1 && !w.hasTag(JUNCTION, ROUNDABOUT)) {
                waysToBeRemoved.add(w);
            }
        }

        // check if any of them belong to roundabout, if yes then show ways accordingly
        parentWays.stream()
                .filter(it -> it.hasTag(JUNCTION, ROUNDABOUT)
                        && WayUtils.findNumberOfCommonFirstLastNodes(way, it) == 1 && it.lastNode().equals(node))
                .forEach(waysToBeRemoved::add);

        // check mode of transport, also check if there is no loop
        if (super.relation.hasTag(ROUTE, BICYCLE)) {
            parentWays.stream().filter(it -> !WayUtils.isSuitableForBicycle(it)).forEach(waysToBeRemoved::add);
        }

        parentWays.stream().filter(it -> it.equals(super.previousWay)).forEach(waysToBeRemoved::add);

        parentWays.removeAll(waysToBeRemoved);
        waysToBeRemoved.clear();

        // check restrictions
        parentWays.stream().filter(it -> isRestricted(it, way, node)).forEach(waysToBeRemoved::add);
        for (Way w : parentWays) {
            if (isWaythere.get(w) != null) {
                waysToBeRemoved.add(w);
            }
        }
        parentWays.removeAll(waysToBeRemoved);
        return parentWays;
    }

    @Override
    public void backTrack(Way way, int idx) {
        if (idx >= super.backNodes.size() - 1) {
            super.currentNode = previousCurrentNode;
            callNextWay(super.currentIndex);
            return;
        }
        Node nod = super.backNodes.get(idx);
        if (way.isInnerNode(nod)) {
            List<Way> fixVariants = new ArrayList<>();
            List<Way> allWays = nod.getParentWays();
            if (allWays != null) {
                for (Way w : allWays) {
                    if (!w.equals(super.currentWay)) {
                        if (!WayUtils.isOneWay(w)) {
                            if (relation.hasTag(ROUTE, "bus")) {
                                if (WayUtils.isSuitableForBuses(w)) {
                                    fixVariants.add(w);
                                }
                            } else if (relation.hasTag(ROUTE, BICYCLE)) {
                                if (WayUtils.isSuitableForBicycle(w)) {
                                    fixVariants.add(w);
                                }
                            }
                        } else {
                            if (w.firstNode().equals(nod)) {
                                if (relation.hasTag(ROUTE, "bus")) {
                                    if (WayUtils.isSuitableForBuses(w)) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag(ROUTE, BICYCLE)) {
                                    if (WayUtils.isSuitableForBicycle(w)) {
                                        fixVariants.add(w);
                                    }
                                }
                            } else {
                                if (relation.hasTag(ROUTE, "bus")) {
                                    if (RouteUtils.isOnewayForPublicTransport(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                } else if (relation.hasTag(ROUTE, BICYCLE)) {
                                    if (RouteUtils.isOnewayForBicycles(w) == 0) {
                                        fixVariants.add(w);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            super.currentNode = nod;
            if (!fixVariants.isEmpty()) {
                displayBacktrackFixVariant(fixVariants, idx);
            } else {
                backTrack(way, idx + 1);
            }
        }
    }

    @Override
    void getNextWayAfterBackTrackSelection(Way way) {
        save();
        List<Integer> lst = new ArrayList<>();
        lst.add(super.currentIndex + 1);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        Way temp = super.members.get(ind[0]).getWay();
        super.memberTableModel.remove(ind);
        for (int i = 0; i < ind.length; i++) {
            super.members.remove(ind[i] - i);
        }
        List<RelationMember> c = new ArrayList<>();
        List<Way> ways = new ArrayList<>();
        ways.add(temp);
        int p = super.currentIndex;
        c.add(new RelationMember("", ways.get(0)));
        super.members.addAll(p + 1, c);
        save();
        int indx = super.currentIndex;
        addNewWays(Collections.singletonList(way), indx);
        save();
        super.memberTableModel.fireTableDataChanged();
        super.currentNode = getOtherNode(way, super.currentNode);
        if (super.currentIndex < super.members.size() - 1) {
            callNextWay(++super.currentIndex);
            return;
        } else {
            deleteExtraWays();
        }
    }

    @Override
    void getNextWayAfterSelection(List<Way> ways) {
        int flag = 0;
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
            int ind = super.currentIndex;
            Way prev = super.currentWay;
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
                                    if (!temp.isEmpty()) {
                                        w1 = v;
                                        breakNode = new ArrayList<>(temp);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // check if the new way is part of one of the parentWay of the super.nextWay's first
                    // node
                    for (Way v : super.wayToReachAfterGap.firstNode().getParentWays()) {
                        if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                            if (!w.equals(v) && !v.isFirstLastNode(super.wayToReachAfterGap.firstNode())) {
                                w1 = v;
                                breakNode = Collections.singletonList(super.wayToReachAfterGap.firstNode());
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

                    // check if the new way is part of one of the parentWay of the super.nextWay's first
                    for (Way v : super.wayToReachAfterGap.lastNode().getParentWays()) {
                        if (v.getNodes().containsAll(w.getNodes()) && w1 == null) {
                            if (!w.equals(v) && !v.isFirstLastNode(super.wayToReachAfterGap.lastNode())) {
                                w1 = v;
                                breakNode = Collections.singletonList(super.wayToReachAfterGap.lastNode());
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
                    addNewWays(Collections.singletonList(w), ind);
                    prev = w;
                    ind++;
                    if (findNextWay(w, super.currentNode).size() >= 2) {
                        break;
                    }
                }
            }
            Way way = super.members.get(super.currentIndex).getWay();
            Way nextw = super.members.get(super.currentIndex + 1).getWay();
            Node n = WayUtils.findCommonFirstLastNode(nextw, way, super.currentNode).orElse(null);
            if (n != null) {
                super.currentNode = getOtherNode(nextw, n);
            } else {
                Node node1 = super.currentWay.firstNode();
                Node node2 = super.currentWay.lastNode();
                if (nextw.firstNode().equals(node1) || nextw.lastNode().equals(node1)) {
                    super.currentNode = getOtherNode(nextw, node1);
                } else {
                    super.currentNode = getOtherNode(nextw, node2);
                }
            }
            if (isThere.get(super.currentNode) != null && isThere.get(super.currentNode) >= 3) {
                super.currentIndex++;
                if (super.currentIndex <= super.members.size() - 1) {
                    assignRolesafterloop(super.currentNode);
                    flag = 1;
                } else {
                    deleteExtraWays();
                }
            }
            save();
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                Logging.error(e);
            }
        } else {
            super.currentNode = null;
        }
        super.previousWay = super.currentWay;
        if (flag == 1) {
            fixgapAfterlooping(brokenidx);
            flag = 0;
        } else if (super.currentIndex < super.members.size() - 1) {
            callNextWay(++super.currentIndex);
        } else {
            deleteExtraWays();
            makeNodesZeros();
        }
    }

    @Override
    void addNewWays(List<Way> ways, int i) {
        try {
            List<RelationMember> c = new ArrayList<>();
            String s = "";
            int[] idx = new int[1];
            idx[0] = i + 1;
            Way w = ways.get(0);
            for (int k = 0; k < ways.size(); k++) {
                c.add(new RelationMember(s, ways.get(k)));
                // check if the way that is getting added is already present or not
                if (!super.waysAlreadyPresent.containsKey(ways.get(k)) && isWaythere.get(ways.get(k)) == null) {
                    super.waysAlreadyPresent.put(ways.get(k), 1);
                    for (Node node : ways.get(k).getNodes()) {
                        if (isThere.get(node) == null || isThere.get(node) == 0) {
                            isThere.put(node, 1);
                        } else {
                            isThere.put(node, isThere.get(node) + 1);
                        }
                    }
                    isWaythere.put(ways.get(k), 1);
                } else {
                    deleteWayAfterIndex(ways.get(k), i);
                }
            }
            super.memberTableModel.addMembersAfterIdx(ways, i);
            super.memberTableModel.updateRole(idx, s);
            super.members.addAll(i + 1, c);
            save();
            super.currentNode = getOtherNode(w, super.currentNode);
            links = connectionTypeCalculator.updateLinks(super.members);
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    void assignRolesafterloop(Node jointNode) {
        int idx = super.currentIndex;
        int[] idxlst = new int[1];
        Node node1;
        Way w = super.members.get(idx).getWay();
        Node node = null;
        String s = "";
        if (w.firstNode().equals(jointNode)) {
            node = w.lastNode();
            s = BACKWARD;
        } else {
            s = FORWARD;
            node = w.firstNode();
        }
        idxlst[0] = idx;
        idx--;
        double minLength = WayUtils.calcDistanceSqToFirstOrLastWayNode(getOtherNode(w, jointNode), super.wayToReachAfterGap);
        super.memberTableModel.updateRole(idxlst, s);
        while (true) {
            w = super.members.get(idx).getWay();
            if (w.firstNode().equals(node)) {
                node = w.lastNode();
                node1 = w.firstNode();
                s = BACKWARD;
                // roles[idx]=s;
            } else {
                node = w.firstNode();
                node1 = w.lastNode();
                s = FORWARD;
                // roles[idx]=s;
            }
            idxlst[0] = idx;
            double length = WayUtils.calcDistanceSqToFirstOrLastWayNode(getOtherNode(w, node1), super.wayToReachAfterGap);
            if (minLength > length) {
                minLength = length;
            }
            super.memberTableModel.updateRole(idxlst, s);
            if (w.firstNode().equals(jointNode) || w.lastNode().equals(jointNode)) {
                break;
            }
            idx--;
        }
        super.currentNode = jointNode;
        brokenidx = idx;
        sortBelow(super.relation.getMembers());
    }

    void fixgapAfterlooping(int idx) {
        super.currentWay = super.members.get(idx).getWay();

        IntStream.range(idx, super.currentIndex + 1)
            .mapToObj(super.members::get)
            .map(RelationMember::getWay)
            .min(Comparator.comparingDouble(current ->
                Stream.of(current.lastNode(), current.firstNode())
                    .filter(Objects::nonNull)
                    .map(it -> findNextWay(current, it))
                    .flatMap(Collection::stream)
                    .filter(it -> isWaythere.get(it) == null)
                    .mapToDouble(it -> WayUtils.calcDistanceSqToFirstOrLastWayNode(it.lastNode(), super.wayToReachAfterGap))
                    .min()
                    .orElse(-Double.MAX_VALUE)
            ))
            .ifPresent(way -> downloadAreaAroundWay(way, way.lastNode(), way.firstNode()));
    }

    @Override
    void deleteWayAfterIndex(Way way, int index) {
        for (int i = index + 1; i < super.members.size(); i++) {
            if (super.members.get(i).isWay() && super.members.get(i).getWay().equals(way)) {
                Way prev = null;
                Way next = null;
                boolean del = true;
                if (i > 0 && super.members.get(i - 1).isWay())
                    prev = super.members.get(i - 1).getWay();
                if (i < super.members.size() - 1 && super.members.get(i + 1).isWay())
                    next = super.members.get(i + 1).getWay();
                // if the next index where the same way comes is well connected with its prev
                // and next way then don't delete it in that index
                if (prev != null && next != null) {
                    if (WayUtils.findNumberOfCommonFirstLastNodes(prev, way) != 0
                            && WayUtils.findNumberOfCommonFirstLastNodes(way, super.wayToReachAfterGap) != 0) {
                        del = false;
                    }
                }
                if (del) {
                    int[] x = { i };
                    super.memberTableModel.remove(x);
                    super.members.remove(i);
                    break;
                }
            }
        }
        links = connectionTypeCalculator.updateLinks(super.members);
    }

    @Override
    void removeCurrentEdge() {
        List<Integer> lst = new ArrayList<>();
        lst.add(super.currentIndex);
        int j = super.currentIndex;
        Way curr = super.currentWay;
        Node n = null;
        if (isWaythere.get(curr) != null) {
            isWaythere.put(curr, null);
        }
        for (Node node : curr.getNodes()) {
            isThere.put(node, isThere.get(node) - 1);
        }

        int prevInd = getPreviousWayIndex(j);

        Collections.reverse(lst);
        int[] ind = lst.stream().mapToInt(Integer::intValue).toArray();
        super.memberTableModel.remove(ind);
        for (int i = 0; i < ind.length; i++) {
            super.members.remove(ind[i] - i);
        }

        save();

        if (prevInd >= 0) {
            super.currentIndex = prevInd;
            Way w = super.members.get(super.currentIndex).getWay();
            if (super.currentIndex - 1 >= 0) {
                Way prevw = super.members.get(super.currentIndex - 1).getWay();
                if (prevw.firstNode().equals(w.lastNode()) || prevw.lastNode().equals(w.lastNode())) {
                    n = w.lastNode();
                } else {
                    n = w.firstNode();
                }
            }
            super.currentNode = n;

            isWaythere.put(w, null);
            for (Node node : w.getNodes()) {
                isThere.put(node, isThere.get(node) - 1);
            }
            callNextWay(super.currentIndex);
        } else {
            super.notice = null;
            deleteExtraWays();
        }
    }

    @Override
    void RemoveWayAfterSelection(List<Integer> wayIndices, Character chr) {
        if (chr == 'A' || chr == '1') {
            // remove all the ways
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            for (int i = 0; i < lst.length; i++) {
                Way way = super.members.get(i).getWay();
                if (isWaythere.get(way) != null) {
                    isWaythere.put(way, null);
                }
                for (Node node : way.getNodes()) {
                    if (isThere.get(node) != null) {
                        isThere.put(node, isThere.get(node) - 1);
                    }
                }
            }
            super.memberTableModel.remove(lst);
            for (int i = 0; i < lst.length; i++) {
                super.members.remove(lst[i] - i);
            }
            // OK.actionPerformed(null);
            save();
            if (super.currentIndex < super.members.size() - 1) {
                super.notice = null;
                callNextWay(super.currentIndex);
            } else {
                super.notice = null;
                deleteExtraWays();
            }
        } else if (chr == 'B' || chr == '2') {
            if (super.currentIndex < super.members.size() - 1) {
                super.notice = null;
                super.currentIndex = wayIndices.get(wayIndices.size() - 1);
                callNextWay(super.currentIndex);
            } else {
                super.notice = null;
                deleteExtraWays();
            }
        } else if (chr == 'C' || chr == '4') {
            List<Command> cmdlst = new ArrayList<>();
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            for (int i = 0; i < lst.length; i++) {
                Way w = super.members.get(lst[i]).getWay();
                TagMap newKeys = w.getKeys();
                newKeys.put("oneway", "bicycle=no");
                cmdlst.add(new ChangePropertyCommand(Collections.singleton(w), newKeys));
            }
            UndoRedoHandler.getInstance().add(new SequenceCommand("Add tags", cmdlst));
            // OK.actionPerformed(null);
            save();
            if (super.currentIndex < super.members.size() - 1) {
                super.notice = null;
                callNextWay(super.currentIndex);
            } else {
                super.notice = null;
                deleteExtraWays();
            }
        }
        if (chr == 'R' || chr == '3') {
            // calculate the previous index
            int prevIndex = -1;
            for (int i = super.currentIndex - 1; i >= 0; i--) {
                if (super.members.get(i).isWay()) {
                    prevIndex = i;
                    break;
                }
            }
            // remove all the ways
            int[] lst = wayIndices.stream().mapToInt(Integer::intValue).toArray();
            super.memberTableModel.remove(lst);
            for (int i = 0; i < lst.length; i++) {
                super.members.remove(lst[i] - i);
            }
            // OK.actionPerformed(null);
            save();
            if (prevIndex != -1) {
                super.notice = null;
                callNextWay(prevIndex);
            } else {
                super.notice = null;
                deleteExtraWays();
            }
        }
    }

    public void updateStates() {
        super.downloadCounter = 0;
        super.waysAlreadyPresent = new HashMap<>();
        super.extraWaysToBeDeleted = new ArrayList<>();
        setEnable = false;
        super.previousWay = null;
        super.currentWay = null;
        super.wayToReachAfterGap = null;
        super.noLinkToPreviousWay = true;
        super.nextIndex = true;
        super.shorterRoutes = false;
        super.showOption0 = false;
        super.currentIndex = 0;
    }

    /////////make panel////////////
    public void makepanelanddownloadArea() {
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
                super.aroundStops = true;
            } else if (button2.isSelected()) {
                super.aroundGaps = true;
            } else if (button3.isSelected()) {
                super.onFly = true;
            }
            downloadEntireArea();
        }
    }

    void makeNodesZeros() {
        for (int i = 0; i < super.members.size(); i++) {
            Way n = super.members.get(i).getWay();
            Node f = n.firstNode();
            Node l = n.lastNode();
            isThere.put(f, 0);
            isThere.put(l, 0);
            isWaythere.put(n, null);
        }
    }
}
