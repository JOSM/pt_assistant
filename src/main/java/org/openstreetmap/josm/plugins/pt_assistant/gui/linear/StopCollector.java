package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.LineRelation.StopPositionEvent;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Collects all stops that there are in several relations
 */
public class StopCollector {

    private final List<CollectedStopsForRelation> collectedStopsForRelation;
    private final List<FoundStop> allStops = new ArrayList<>();

    public StopCollector(List<LineRelation> forRelations) {
        this.collectedStopsForRelation = forRelations
            .stream()
            .map(CollectedStopsForRelation::new)
            .collect(Collectors.toList());

        CollectedStopsForRelation mostImportantCollection = collectedStopsForRelation
            .stream()
            .min(Comparator.comparing(it -> it.priority))
            .orElseThrow(() -> new IllegalArgumentException("Expected at least one relation"));

        collectStops(mostImportantCollection);
        collectedStopsForRelation
            .stream()
            .filter(it -> it != mostImportantCollection)
            .forEach(this::collectStops);

        collectedStopsForRelation.forEach(this::drawLines);
    }

    private void drawLines(CollectedStopsForRelation forRelation) {
        Color color = forRelation.getColor();
        for (int i = 0; i < allStops.size(); i++) {
            forRelation.cells.add(new LineGridCell(color));
        }

        int currentDrawingIndex = -1;
        int col = 0;
        boolean currentlyDrawingDown = false;
        for (StopWithNotes nextStop : forRelation.stops) {
            int nextIndex = index(nextStop);
            LineGridCell nextCell = forRelation.cells.get(nextIndex);
            if (currentDrawingIndex < 0) {
                // First stop.
                if (forRelation.stops.size() > 1) {
                    // We have a next stop => drawing direction is the one for the next stop
                    currentlyDrawingDown = index(forRelation.stops.get(1)) > nextIndex;
                }
            } else {
                // Connect the last and current stop with a line.
                LineGridCell currentCell = forRelation.cells.get(currentDrawingIndex);
                if (currentlyDrawingDown) {
                    currentCell.addDownwardsConnection(col);
                } else {
                    currentCell.addUpwardsConnection(col);
                }

                if (currentDrawingIndex < nextIndex && !currentlyDrawingDown) {
                    // We draw upwards, need to do a U turn down
                    forRelation.cells.get(currentDrawingIndex - 1).addDownwardsU(col);
                    col++;
                    currentlyDrawingDown = true;
                    currentCell.addThroughConnection(col);
                }
                if (currentDrawingIndex > nextIndex && currentlyDrawingDown) {
                    // We draw downwards, need to do a U turn up
                    forRelation.cells.get(currentDrawingIndex + 1).addUpwardsU(col);
                    col++;
                    currentlyDrawingDown = false;
                    currentCell.addThroughConnection(col);
                }

                for (int i = Math.min(currentDrawingIndex, nextIndex) + 1; i < Math.max(currentDrawingIndex, nextIndex); i++) {
                    forRelation.cells.get(i).addThroughConnection(col);
                }

                if (currentlyDrawingDown) {
                    nextCell.addUpwardsConnection(col);
                } else {
                    nextCell.addDownwardsConnection(col);
                }
            }
            if (currentlyDrawingDown) {
                nextCell.addDownwardsStop(col, nextStop.entryExit);
            } else {
                nextCell.addUpwardsStop(col, nextStop.entryExit);
            }

            if (currentlyDrawingDown ? nextStop.skippedBefore : nextStop.skippedAfter) {
                forRelation.cells.get(nextIndex - 1).addContinuityUp(col);
                nextCell.addUpwardsConnection(col);
            }
            if (currentlyDrawingDown ? nextStop.skippedAfter : nextStop.skippedBefore) {
                forRelation.cells.get(nextIndex + 1).addContinuityDown(col);
                nextCell.addDownwardsConnection(col);
            }

            currentDrawingIndex = nextStop.skippedAfter ? -1 : nextIndex;
        }
    }

    private int index(StopWithNotes stop) {
        return index(stop.stop);
    }

    private int index(FoundStop stop) {
        int i = allStops.indexOf(stop);
        if (i < 0) {
            throw new IllegalArgumentException("Stop not contained in this collector: " + stop);
        }
        return i;
    }

    private void collectStops(CollectedStopsForRelation collectFor) {
        int nextInsertPosition = 0;
        boolean isCollectingDownward = true;
        int lastIndex = -1;
        List<StopPositionEvent> stopEvents = collectFor.findStopPositions();
        for (StopPositionEvent event : stopEvents) {
            FoundStopPosition stop = new FoundStopPosition(event.getStop());
            Optional<FoundStop> alreadyFoundStop = findStop(stop);
            FoundStop actualStop;
            if (alreadyFoundStop.isPresent()) {
                nextInsertPosition = allStops.indexOf(alreadyFoundStop.get());
                isCollectingDownward = nextInsertPosition > lastIndex;
                actualStop = alreadyFoundStop.get();
                if (isCollectingDownward) {
                    nextInsertPosition++;
                }
            } else {
                Optional<Relation> stopArea = stop.findStopArea();
                if (stopArea.isPresent()) {
                    actualStop = new FoundStopArea(stopArea.get());
                } else {
                    actualStop = stop;
                }
                if (lastIndex == -1) {
                    // This is the first node. Guess the best insert position for now.
                    // For this, we need to skip ahead and search the first stop that matches
                    // TODO: This code is not really efficcient
                    List<Integer> nextPositions = stopEvents.stream()
                        .map(StopPositionEvent::getStop)
                        .map(FoundStopPosition::new)
                        .map(this::findStop)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(this::index)
                        .limit(2)
                        .collect(Collectors.toList());
                    if (nextPositions.size() > 1) {
                        isCollectingDownward = nextPositions.get(0) < nextPositions.get(1);
                        nextInsertPosition = nextPositions.get(0) + (isCollectingDownward ? 0 : 1);
                    } else if (nextPositions.size() == 1) {
                        nextInsertPosition = nextPositions.get(0);
                    } else {
                        // No connection to any previous route. May also happen for first route
                        nextInsertPosition = allStops.size();
                    }
                }
                allStops.add(nextInsertPosition, actualStop);
                if (isCollectingDownward) {
                    nextInsertPosition++;
                }
            }
            StopWithNotes stopFound = new StopWithNotes(actualStop, stop.entryExit(),
                stop.member.getMember().get("ref"), event.isSkippedBefore(), event.isSkippedAfter());
            collectFor.stops.add(stopFound);
            lastIndex = index(stopFound);
        }

        // Now we add two NOP stops to top + button. We need them for drawing U turns.
        allStops.add(new NopStop());
        allStops.add(0, new NopStop());
    }

    private Optional<FoundStop> findStop(FoundStopPosition stop) {
        return allStops.stream()
            .filter(it -> it.matches(stop.member.getMember()))
            .findFirst();
    }

    public List<FoundStop> getAllStops() {
        return allStops;
    }

    private static class CollectedStopsForRelation {

        private final LineRelation line;
        private final double priority;
        // Stops in the order we visit them
        private final List<StopWithNotes> stops = new ArrayList<>();
        // Cells for all the stops collected, ordered by allStops
        private final List<LineGridCell> cells = new ArrayList<>();

        CollectedStopsForRelation(LineRelation line) {
            this.line = line;
            this.priority = line.getRelation().getMembers().stream().filter(it -> OSMTags.STOP_ROLES.contains(it.getRole())).count()
                + (line.isPrimary() ? 1_000_000 : 0);
        }

        public List<StopPositionEvent> findStopPositions() {
            return line.streamStops().collect(Collectors.toList());
        }

        public Color getColor() {
            return line.getColor();
        }
    }

    public List<List<LineGridCell>> getLineGrid() {
        return collectedStopsForRelation.stream()
            .map(it -> it.cells)
            .collect(Collectors.toList());
    }

    private static class StopWithNotes {
        private final FoundStop stop;
        private final EntryExit entryExit;
        private final String notes;
        private final boolean skippedBefore;
        private final boolean skippedAfter;

        public StopWithNotes(FoundStop stop, EntryExit entryExit, String notes, boolean skippedBefore, boolean skippedAfter) {
            this.stop = stop;
            this.entryExit = entryExit;
            this.notes = notes;
            this.skippedBefore = skippedBefore;
            this.skippedAfter = skippedAfter;
        }
    }

    public interface FoundStop {
        boolean isIncomplete();

        boolean matches(OsmPrimitive p);

        String getNameAndInfos();
    }

    private static class FoundStopPosition implements FoundStop {
        private final RelationMember member;

        FoundStopPosition(RelationMember member) {
            if (!OSMTags.STOP_ROLES.contains(member.getRole())) {
                throw new IllegalArgumentException("Not a stop position: " + member);
            }
            this.member = member;
        }

        Optional<Relation> findStopArea() {
            return member.getMember().getReferrers()
                .stream()
                .filter(it -> it.getType() == OsmPrimitiveType.RELATION
                    && it.hasTag(OSMTags.KEY_RELATION_TYPE, "public_transport")
                    && StopUtils.isStopArea((Relation) it)
                )
                .map(it -> (Relation) it)
                .findFirst();
        }

        EntryExit entryExit() {
            return EntryExit.ofRole(member.getRole());
        }

        @Override
        public boolean isIncomplete() {
            return member.getMember().isIncomplete();
        }

        @Override
        public boolean matches(OsmPrimitive p) {
            return this.member.getMember().equals(p);
        }

        @Override
        public String getNameAndInfos() {
            String name = member.getMember().get("name");
            return (name == null ? tr("- Stop without name -") : name);
        }
    }

    private static class FoundStopArea implements FoundStop {
        private final List<RelationMember> stops;
        private final Relation relation;

        FoundStopArea(Relation relation) {
            stops = relation.getMembers()
                .stream()
                .filter(m -> "stop".equals(m.getRole()))
                .collect(Collectors.toList());
            this.relation = relation;
        }

        @Override
        public boolean isIncomplete() {
            return relation.isIncomplete();
        }

        @Override
        public boolean matches(OsmPrimitive p) {
            return this.stops.stream().anyMatch(s -> s.getMember().equals(p));
        }

        @Override
        public String getNameAndInfos() {
            String name = relation.get("name");
            return (name == null ? tr("- No name -") : name) + " (" + tr("area relation " + relation.getId()) + ")";
        }
    }

    private static class NopStop implements FoundStop {
        @Override
        public boolean isIncomplete() {
            return false;
        }

        @Override
        public boolean matches(OsmPrimitive p) {
            return false;
        }

        @Override
        public String getNameAndInfos() {
            return "";
        }
    }
}
