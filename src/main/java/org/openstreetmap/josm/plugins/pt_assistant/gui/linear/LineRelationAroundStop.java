package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * Same as a {@link LineRelation}, but only the part around a given stop position
 */
public class LineRelationAroundStop extends LineRelation {
    private final Predicate<OsmPrimitive> isStop;

    public LineRelationAroundStop(Relation relation, boolean primary, Predicate<OsmPrimitive> isStop) {
        super(relation, primary);
        this.isStop = isStop;
    }

    @Override
    public Stream<StopPositionEvent> streamStops() {
        List<RelationMember> stops = streamRawStops().collect(Collectors.toList());
        List<Integer> indexesAtWhichOurStopIs = IntStream.range(0, stops.size())
            .filter(i -> isStop.test(stops.get(i).getMember()))
            .boxed()
            .collect(Collectors.toList());

        IntPredicate contains = index -> indexesAtWhichOurStopIs.stream().anyMatch(test -> Math.abs(index - test) <= 3);

        return IntStream.range(0, stops.size())
            .filter(contains)
            .mapToObj(i -> new StopPositionEvent(stops.get(i),
                i > 0 && !contains.test(i - 1),
                i < stops.size() - 1 && !contains.test(i + 1)));
    }
}
