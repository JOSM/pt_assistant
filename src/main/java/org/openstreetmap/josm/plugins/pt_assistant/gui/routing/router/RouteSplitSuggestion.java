package org.openstreetmap.josm.plugins.pt_assistant.gui.routing.router;

import java.util.List;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A way that should be split at startAtIndex and endAtIndex
 * <br/>
 * Both may be at the end of the way, indicating that that index should be ignored
 * <br/>
 * startAtIndex and endAtIndex may be in any order on the way.
 * They define the direciton the new route segment should go: it starts at startAtIndex and goes towards endAtIndex
 */
public class RouteSplitSuggestion {
    private final Way way;
    private final int startAtIndex;
    private final int endAtIndex;
    private final Node startAtNode;
    private final Node endAtNode;

    public RouteSplitSuggestion(Way way, int startAtIndex, int endAtIndex) {
        this.way = way;
        this.startAtIndex = startAtIndex;
        this.endAtIndex = endAtIndex;
        if (startAtIndex == endAtIndex) {
            throw new IllegalArgumentException("Invalid split segment: Start and end are the same.");
        }
        this.startAtNode = way.getNode(startAtIndex);
        this.endAtNode = way.getNode(endAtIndex);
    }

    public Way getWay() {
        return way;
    }

    public Node getStartAtNode() {
        return startAtNode;
    }

    public Node getEndAtNode() {
        return endAtNode;
    }

    /**
     * Get the segment that is before the new split segment (in the direction of the way, not of the split)
     * @return The nodes of that segment. A list with one node if it is empty.
     */
    public List<Node> getSegmentBefore() {
        return way.getNodes().subList(0, Math.min(startAtIndex, endAtIndex) + 1);
    }

    public List<Node> getSegment() {
        return way.getNodes().subList(Math.min(startAtIndex, endAtIndex), Math.max(startAtIndex, endAtIndex) + 1);
    }

    public List<Node> getSegmentAfter() {
        return way.getNodes().subList(Math.max(startAtIndex, endAtIndex), way.getNodesCount());
    }

    public Stream<Node> streamSplitNodes() {
        return Stream.of(getStartAtNode(), getEndAtNode())
            .filter(it -> !way.isFirstLastNode(it));
    }

    @Override
    public String toString() {
        return "RouteSplitSuggestion{" +
            "way=" + way +
            ", startAtNode=" + startAtNode +
            ", endAtNode=" + endAtNode +
            '}';
    }

}
