// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.actions;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractMendRelationAction extends AbstractRelationEditorAction {

    /**
     * Override method with initializing the values for the required mend relation function
     */
    public abstract void initialise();

    /**
     *
     *
     * @param members
     * @param typeRoute
     * @param aroundGaps
     * @param aroundStops
     * @return
     */
    protected String getQuery(List<RelationMember> members, String typeRoute, boolean aroundGaps, boolean aroundStops) {
        StringBuilder timeoutString = new StringBuilder("[timeout:100];\n(\n");
        String wayFormatterString = "   way(%.6f,%.6f,%.6f,%.6f)\n";
        List<Node> nodeList = aroundGaps ? getBrokenNodes(members) : new ArrayList<>();

        if (aroundStops) {
            nodeList.addAll(members.stream().filter(RelationMember::isNode).map(RelationMember::getNode).collect(Collectors.toList()));
        }

        for (int i = 0; i < nodeList.size(); i++) {
            Node n = nodeList.get(i);
            double maxLatitude = n.getBBox().getTopLeftLat() + 0.001;
            double minLatitude = n.getBBox().getBottomRightLat() - 0.001;

            double maxLongitude = n.getBBox().getBottomRightLon() + 0.001;
            double minLongitude = n.getBBox().getTopLeftLon() - 0.001;

            timeoutString.append(String.format(wayFormatterString, minLatitude, minLongitude, maxLatitude, maxLongitude)).append(typeRoute);
        }

        return timeoutString.append(");\n(._;<;);\n(._;>;);\nout meta;").toString();
    }

    /**
     *
     * @param members
     * @return
     */
    private List<Node> getBrokenNodes(List<RelationMember> members) {
        List<Node> brokenNodes = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isWay()) {
                int j = getNextWayIndex(members, i);
                if (j < members.size()) {
                    Way w = members.get(i).getWay();
                    Way v = members.get(j).getWay();
                    if (findNumberOfCommonNodes(w, v) != 1) {
                        brokenNodes.add(w.firstNode());
                        brokenNodes.add(w.lastNode());
                    }
                }
            }
        }
        return brokenNodes;
    }

    /**
     *
     * @param members
     * @param index
     * @return
     */
    private int getNextWayIndex(List<RelationMember> members, int index) {
        int j = index + 1;
        for (; j < members.size(); j++) {
            if (members.get(j).isWay())
                break;
        }
        return j;
    }

    /**
     *
     * @param currentWay
     * @param previousWay
     * @return
     */
    int findNumberOfCommonNodes(Way currentWay, Way previousWay) {
        int count = 0;
        List<Node> nodes = currentWay.getNodes();
        List<Node> previousWayNodes = previousWay.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            for (int j = 0; j < previousWayNodes.size(); j++) {
                Node previousNode = previousWayNodes.get(j);
                if (currentNode.equals(previousNode))
                    count = count + 1;
            }
        }
        return count;
    }
}
