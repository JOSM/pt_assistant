package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Same as a {@link LineRelation}, but only the part around a given stop position
 */
public class LineRelationAroundStop extends LineRelation {
    private final Predicate<OsmPrimitive> isStop;
    private final int aroundStop;

    public LineRelationAroundStop(RelationAccess relation, boolean primary, Predicate<OsmPrimitive> isStop, int aroundStop) {
        super(relation, primary);
        this.isStop = isStop;
        this.aroundStop = aroundStop;
        if (aroundStop < 0) {
            throw new IllegalArgumentException("Must be at leas 0, but got " + aroundStop);
        }
    }

    @Override
    public LineRefKey getLineRefKey() {
        List<OsmPrimitive> platforms = getRelation().getMembers()
            .stream()
            .filter(it -> it.getRole().equals("platform"))
            .map(RelationMember::getMember)
            .filter(isStop)
            .distinct()
            .collect(Collectors.toList());
        if (platforms.size() == 1) {
            return new LineRefKeyPlatform(platforms.get(0));
        } else if (platforms.isEmpty()) {
            return new LineRefKeyEmpty();
        } else {
            return new LineRefKeyPlatforms(platforms);
        }
    }

    @Override
    public Stream<StopPositionEvent> streamStops() {
        List<RelationMember> stops = streamRawStops().collect(Collectors.toList());

        List<List<RelationMember>> stopsByStopArea = new ArrayList<>();
        Relation lastArea = null;
        for (RelationMember stop : stops) {
            Relation area = StopUtils.findContainingStopArea(stop.getMember());
            if (area != null && area == lastArea) {
                stopsByStopArea.get(stopsByStopArea.size() - 1).add(stop);
            } else {
                stopsByStopArea.add(new ArrayList<>(Arrays.asList(stop)));
            }
            lastArea = area;
        }

        List<Integer> indexesAtWhichOurStopIs = IntStream.range(0, stopsByStopArea.size())
            .filter(i -> stopsByStopArea.get(i).stream().anyMatch(s -> isStop.test(s.getMember())))
            .boxed()
            .collect(Collectors.toList());

        IntPredicate contains = index -> indexesAtWhichOurStopIs.stream().anyMatch(test -> Math.abs(index - test) <= aroundStop);

        return IntStream.range(0, stopsByStopArea.size())
            .filter(contains)
            .boxed()
            .flatMap(i -> {
                List<RelationMember> subStops = stopsByStopArea.get(i);
                boolean skippedBefore = i > 0 && !contains.test(i - 1);
                boolean skippedAfter = i < stopsByStopArea.size() - 1 && !contains.test(i + 1);
                ArrayList<StopPositionEvent> events = new ArrayList<>();
                for (int j = 0; j < subStops.size(); j++) {
                    events.add(new StopPositionEvent(subStops.get(j),
                        j == 0 && skippedBefore,
                        j == subStops.size() - 1 && skippedAfter));
                }
                return events.stream();
            });
    }
}
