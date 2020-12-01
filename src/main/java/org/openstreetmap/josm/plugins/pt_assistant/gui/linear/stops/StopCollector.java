package org.openstreetmap.josm.plugins.pt_assistant.gui.linear.stops;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JButton;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.LineGridCell;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRelation;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.lines.LineRelation.StopPositionEvent;

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

        collectedStopsForRelation
            .stream()
            .sorted(Comparator.<CollectedStopsForRelation, Double>comparing(it -> it.priority).reversed())
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
        int nextIndex = 0;
        boolean isCollectingDownward = true;
        StopWithNotes lastStopFound = null;
        List<StopPositionEvent> stopEvents = collectFor.findStopPositions();
        for (StopPositionEvent event : stopEvents) {
            FoundStopPosition stop = new FoundStopPosition(event.getStop());
            Optional<FoundStop> alreadyFoundStop = findStop(stop);
            FoundStop actualStop;
            if (alreadyFoundStop.isPresent()) {
                if (lastStopFound != null && lastStopFound.stop.equals(alreadyFoundStop.get())) {
                    // We found a stop twice
                    // This may happen e.g. if the train stops there twice or if platform + stop is added for a stop area.
                    lastStopFound.skippedAfter = event.isSkippedAfter();
                    lastStopFound.addMeta(stop);
                    continue;
                }
                nextIndex = allStops.indexOf(alreadyFoundStop.get());
                int lastIndex = lastStopFound == null ? -1 : index(lastStopFound);
                isCollectingDownward = nextIndex > lastIndex;
                actualStop = alreadyFoundStop.get();
                if (isCollectingDownward) {
                    nextIndex++;
                }
            } else {
                Optional<Relation> stopArea = stop.findStopArea();
                if (stopArea.isPresent()) {
                    actualStop = new FoundStopArea(stopArea.get());
                } else {
                    actualStop = stop;
                }
                if (lastStopFound == null) {
                    // This is the first node. Guess the best insert position for now.
                    // For this, we need to skip ahead and search the first stop that matches
                    // TODO: This code is not really efficcient
                    List<Integer> nextPositions = stopEvents.stream()
                        .map(StopPositionEvent::getStop)
                        .map(FoundStopPosition::new)
                        .map(this::findStop)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .distinct()
                        .map(this::index)
                        .limit(2)
                        .collect(Collectors.toList());
                    if (nextPositions.size() > 1) {
                        isCollectingDownward = nextPositions.get(0) < nextPositions.get(1);
                        nextIndex = nextPositions.get(0) + (isCollectingDownward ? 0 : 1);
                    } else if (nextPositions.size() == 1) {
                        nextIndex = nextPositions.get(0);
                    } else {
                        // No connection to any previous route. May also happen for first route
                        nextIndex = allStops.size();
                    }
                }
                allStops.add(nextIndex, actualStop);
                if (isCollectingDownward) {
                    nextIndex++;
                }
            }
            StopWithNotes stopFound = new StopWithNotes(actualStop, event.isSkippedBefore(), event.isSkippedAfter());
            stopFound.addMeta(stop);
            collectFor.stops.add(stopFound);
            lastStopFound = stopFound;
        }

        // Now we add two NOP stops to top + button. We need them for drawing U turns.
        allStops.add(new NopStop());
        allStops.add(0, new NopStop());
    }

    private Optional<FoundStop> findStop(FoundStopPosition stop) {
        return allStops.stream()
            .filter(it -> it.matches(stop.getMember().getMember()))
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
            this.priority = line.getRelation().getMembers().stream().filter(it -> OSMTags.STOPS_AND_PLATFORMS_ROLES.contains(it.getRole())).count()
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
        private EntryExit entryExit;
        private String notes;
        private final boolean skippedBefore;
        private boolean skippedAfter;

        public StopWithNotes(FoundStop stop, boolean skippedBefore, boolean skippedAfter) {
            this.stop = stop;
            this.notes = "";
            this.skippedBefore = skippedBefore;
            this.skippedAfter = skippedAfter;
        }

        public void addMeta(FoundStopPosition stop) {
            EntryExit entryExit = stop.entryExit();
            Objects.requireNonNull(entryExit, "entryExit");
            if (this.entryExit == null) {
                this.entryExit = entryExit;
            }

            // TODO: Add to notes: stop.member.getMember().get("ref")
        }
    }

    private static JButton createActionButton(final String iconName, final String tool, final Runnable action) {
        return new JButton(new JosmAction("", iconName, tool, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
}
