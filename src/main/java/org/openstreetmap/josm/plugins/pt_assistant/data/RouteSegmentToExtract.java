// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import static org.openstreetmap.josm.gui.MainApplication.getLayerManager;
import static org.openstreetmap.josm.plugins.pt_assistant.gui.PTAssistantPaintVisitor.RefTagComparator;
import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Represents a piece of a route that includes all the ways
 * belonging to the same bundle of itineraries of vehicles
 * traveling in the same direction in the case of public transport
 *
 * In case of 'personal' transport it contains all the ways for
 * both directions of travel
 *
 * It is meant to help with extracting segments of ways from
 * route relations so that these can be converted to superroute relations
 * in a convenient way
 *
 * @author Polyglot
 *
 */
public class RouteSegmentToExtract {
    private static final Map<String, Relation> ptSegments;
    private static final Map<String, TreeSet<Relation>> parentRelationsForSameDirectionOfTravel;
    private static final Map<Relation, List<Way>> itineraryWays;

    public void setActiveDataSet(DataSet activeDataSet) {
        this.activeDataSet = activeDataSet;
    }

    private DataSet activeDataSet;
    private Relation relation;
    private Relation extractedRelation;
    private boolean startNewSegment;

    private ArrayList<RelationMember> wayMembers;
    private List<Integer> indices;
    private TreeSet<Relation> itinerariesInSameDirection;
    private final TreeSet<String> lineIdentifiers;
    private final TreeSet<String> colours;
    private List<String> streetNames = null;
    private List<String> wayIds;
    private String wayIdsSignature;
    private final TagMap tags = new TagMap();

    static {
        ptSegments = new HashMap<>();
        parentRelationsForSameDirectionOfTravel = new HashMap<>();
        itineraryWays = new HashMap<>();
    }

    /**
     * Constructor
     * ptWays           The list of PTWay members to extract
     * indices          The indices corresponding to the ways
     * lineIdentifiers  The ref tag of the route parent route relations of the ways
     * colours          The colours of the public transport lines of this line bundle
     */
    private RouteSegmentToExtract() {
        wayMembers = new ArrayList<>();
        lineIdentifiers = new TreeSet<>(new RefTagComparator());
        colours = new TreeSet<>();
        wayIds = null;
        activeDataSet = getLayerManager().getActiveDataSet();
        startNewSegment = false;
    }

    /**
     * Constructor
     *
     * @param relation The route or superroute relation for which this route segment is created
     *                 use addPTWay() to add ways one by one
     */
    public RouteSegmentToExtract(Relation relation) {
        this();
        this.relation = relation;
        extractedRelation = null;

        indices = new ArrayList<>();
        addLineIdentifier(relation.get("ref"));
        addColour(relation.get("colour"));
    }

    /**
     * Constructor for use with non PT route relations
     *
     * @param relation        The route or superroute relation for which this route segment is created
     * @param selectedIndices ways will be added for these indices
     */
    public RouteSegmentToExtract(Relation relation, List<Integer> selectedIndices) {
        this(relation);

        indices = selectedIndices;

        for (Integer index : indices) {
            addWay(index, false);
        }
    }

    /**
     * Constructor
     *
     * @param existingRelation to be used when a potential sub route relation is encountered
     *                         while processing a PT route relation
     * @param updateTags       update the tags automatically or not?
     */
    public RouteSegmentToExtract(Relation existingRelation, Boolean updateTags) {
        this();
        this.relation = null;
        extractedRelation = existingRelation;

        indices = new ArrayList<>();

        this.wayMembers = (ArrayList<RelationMember>) existingRelation.getMembers().stream()
            .filter(RelationMember::isWay)
            .filter(RouteUtils::isPTWay)
            .collect(Collectors.toList());
        if (updateTags) {
            updateTags();
        }
        ptSegments.put(getWayIdsSignature(), extractedRelation);
    }

    /**
     * @return the WayMembers of this route segment
     */
    public List<RelationMember> getWayMembers() {
        return wayMembers;
    }

    /**
     * Sets the WayMembers of this route segment to the given list
     *
     * @param index its index in the relation specified in the constructor
     */
    public void addWay(Integer index) {
        this.addWay(index, true);
    }

    /**
     * Sets the WayMembers of this route segment to the given list
     *
     * @param index         its index in the relation specified in the constructor
     * @param updateIndices when the list of indices was set by the constructor
     *                      don't update it anymore
     */
    private void addWay(Integer index, boolean updateIndices) {
        assert relation != null;
        final RelationMember member = relation.getMember(index);
        if (member.isWay()) {
            if (updateIndices) {
                indices.add(index);
            }
            wayMembers.add(member);
            streetNames = null;
            wayIds = null;
        }
    }

    public RouteSegmentToExtract addPTWayMember(Integer index) {
        assert relation != null;
        WaySequence ws = new WaySequence(relation, index);
        if (ws.currentWay == null || ws.hasGap) {
            return null;
        }
        if (getLastWay(relation).equals(ws.currentWay)) {
            addWay(index);
            return this;
        }

        if (this.itinerariesInSameDirection == null) {
            TreeSet<Relation> itinerariesInSameDirection = getItinerariesInSameDirection(ws);
            if (itinerariesInSameDirection != null) {
                this.itinerariesInSameDirection = itinerariesInSameDirection;
                for (Relation relation : itinerariesInSameDirection) {
                    addLineIdentifier(relation.get("ref"));
                    addColour(relation.get("colour"));
                }
            }
        }

        if (wayMembers.size() == 0) {
            addWay(index, true);
        } else {
            boolean startNewSegmentInNewSegment = false;
            List<Relation> segmentRelations =
                Utils.filteredCollection(ws.currentWay.getReferrers(), Relation.class).stream()
                    .filter(r -> !r.hasKey("name")
                               && RouteUtils.isPTRoute(r)
                               && r.hasKey("note")
//                               && r.get("note").matches(".*\\([\\d;]+\\)")
                    )
                    .collect(Collectors.toList());
            List<Relation> parentRouteRelations =
                Utils.filteredCollection(ws.currentWay.getReferrers(), Relation.class).stream()
                    .filter(RouteUtils::isVersionTwoPTRoute)
                    .collect(Collectors.toList());
            for (Relation sr : segmentRelations) {
                RouteSegmentToExtract existingSegment = new RouteSegmentToExtract(sr, false);
                if (existingSegment.isLastWay(ws.currentWay)) {
                    final int existingSegmentSize = existingSegment.getWayMembers().size();
                    final int startIndexOfRange = index - existingSegmentSize + 1;
                    if (startIndexOfRange > 0) {
                        List<RelationMember> sl = relation.getMembers().subList(startIndexOfRange, index + 1);
                        if (sl.equals(existingSegment.getWayMembers()) && existingSegmentSize > 1) {
                            startNewSegmentInNewSegment = true;
                        }
                    }
                }
                parentRouteRelations.addAll(sr.getReferrers().stream()
                    .map(r -> (Relation) r)
                    .collect(Collectors.toList()));
            }

            TreeSet<Relation> itinerariesInSameDirection = getItinerariesInSameDirection(ws, parentRouteRelations);

            if (isDifferentBundleOfItinerariesAndNotFirstWayOrMultipleOccurrences(itinerariesInSameDirection)) {
                startNewSegment = true;
            } else {
                if (!this.itinerariesInSameDirection.equals(itinerariesInSameDirection)) {
                    this.itinerariesInSameDirection = itinerariesInSameDirection;
                }
                for (Relation parentRoute : itinerariesInSameDirection) {
                    final Optional<Node> commonNode1 = WayUtils.findFirstCommonNode(ws.previousWay, ws.currentWay);
                    final Optional<Node> commonNode2 = WayUtils.findFirstCommonNode(ws.currentWay, ws.nextWay);
                    if (!parentRoute.equals(relation) && (ws.currentWay == getLastWay(parentRoute)
                        || getMembershipCountOfWayInSameDirection(ws.currentWay, parentRoute) > 1
                            || getMembershipCountOfWayInSameDirection(ws.previousWay, parentRoute) > 1
                            && (commonNode1.map(it -> it.getParentWays().size() > 2).orElse(false)))) {
                        startNewSegment = true;
                        break;
                    }
                    // Some PT lines have variants that make a 'spoon like loop'
                    // In this case the ref will usually be the same, if not membership in a route_master will need to be checked
                    // The common node between the way that goes into the new sub route relation (currentWay)
                    // and the first one of the route relation currently processed (nextWay) will always have more than 2 parent ways

                    if (!startNewSegment && !relation.equals(parentRoute)
                        && Objects.equals(relation.get("ref"), parentRoute.get("ref"))) {
                        if (commonNode2
                            .map(it ->
                                it.getParentWays().stream()
                                    .filter(w -> (getItineraryWays(parentRoute).contains(w)))
                                    .count() > 2
                            )
                            .orElse(false)
                        ) {
                            startNewSegment = true;
                            break;
                        }
                    }
                }
            }
            if (startNewSegment) {
                RouteSegmentToExtract newSegment = new RouteSegmentToExtract(relation);
                newSegment.setActiveDataSet(activeDataSet);
                newSegment.addPTWayMember(index);
                newSegment.itinerariesInSameDirection = itinerariesInSameDirection;
                newSegment.populateLineIdentifierAndColourLists();
                newSegment.startNewSegment = startNewSegmentInNewSegment;

                return newSegment;
            } else {
                addWay(index);
            }
        }
        return null;
    }

    private boolean isDifferentBundleOfItinerariesAndNotFirstWayOrMultipleOccurrences(TreeSet<Relation> itinerariesInSameDirection) {
        if (this.itinerariesInSameDirection == null) {
            this.itinerariesInSameDirection = itinerariesInSameDirection;
        }
        return itinerariesInSameDirection != null
                && itinerariesInSameDirection.size() != 0
                && !itinerariesInSameDirection.equals(this.itinerariesInSameDirection);
    }

    /**
     * @param way to compare
     * @return is this the last way in our list of wayMembers?
     */
    public boolean isLastWay(Way way) {
        return wayMembers.get(wayMembers.size() - 1).getWay().equals(way);
    }

    /**
     * @param ptRoute relation to analyse
     * @return the first highway/railway of ptRoute
     */
    public Way getFirstWay(Relation ptRoute) {
        final ArrayList<Way> itineraryWays = getItineraryWays(ptRoute);
        if (itineraryWays.size() > 0) {
            return itineraryWays.get(0);
        }
        return null;
    }

    /**
     * @param ptRoute relation to analyse
     * @return the second highway/railway of ptRoute
     */
    public Way getSecondWay(Relation ptRoute) {
        final ArrayList<Way> itineraryWays = getItineraryWays(ptRoute);
        if (itineraryWays.size() > 1) {
            return itineraryWays.get(1);
        }
        return null;
    }

    /**
     * @param ptRoute relation to analyse
     * @return the last highway/railway of ptRoute
     */
    public Way getLastWay(Relation ptRoute) {
        final ArrayList<Way> itineraryWays = getItineraryWays(ptRoute);
        if (itineraryWays.size() > 0) {
            return itineraryWays.get(itineraryWays.size() - 1);
        }
        return null;
    }

    /**
     * @param ptRoute relation to inventorise
     * @return All the highway/railway ways of the itinerary
     *         it drills down into sub relations
     */
    public ArrayList<Way> getItineraryWays(Relation ptRoute) {
        if (itineraryWays.containsKey(ptRoute)) {
            return (ArrayList<Way>) itineraryWays.get(ptRoute);
        } else {
            ArrayList<Way> itinerary = new ArrayList<>();
            ptRoute.getMembers().stream().filter(RouteUtils::isPTWay)
                .forEachOrdered(rm -> {
                        if (rm.isWay()) {
                            itinerary.add(rm.getWay());
                        } else if (rm.isRelation()) {
                            itinerary.addAll(getItineraryWays(rm.getRelation()));
                        }
                    }
                );
            itineraryWays.put(ptRoute, itinerary);
            return itinerary;
        }
    }

    /**
     * @param ws way sequence to analyse
     * @return all the route relations that describe itineraries in the same sense
     *         from a lookup table
     */
    public TreeSet<Relation> getItinerariesInSameDirection(WaySequence ws) {
        final String wayInRelation = relation.getId() + "," + ws.currentWay.getId();
        if (parentRelationsForSameDirectionOfTravel.containsKey(wayInRelation)) {
            return parentRelationsForSameDirectionOfTravel.get(wayInRelation);
        }
        return null;
    }

    /**
     * @param ws way sequence to analyse
     * @return all the route relations that describe itineraries in the same sense
     *         calculated, and stored in the lookup table if there is a match
     */
    public TreeSet<Relation> getItinerariesInSameDirection(WaySequence ws,
                                                           List<Relation> parentRouteRelations) {
        TreeSet<Relation> r = getItinerariesInSameDirection(ws);
        if (r != null) {
            return r;
        } else {
            TreeSet<Relation> itinerariesInSameDirection = new TreeSet<>();
            for (Relation parentRoute : parentRouteRelations) {
                final List<Way> parentRouteHighways = getItineraryWays(parentRoute);
                if (!itinerariesInSameDirection.contains(parentRoute)) {
                    if (ws.currentWay.equals(getFirstWay(parentRoute))
                            && ws.previousWay != null
                            && !ws.previousWay.getNodes().contains(getSecondWay(parentRoute).getNode(0))) {
                        itinerariesInSameDirection.add(parentRoute);
                    }
                    findPreviousAndNextWayInRoute(parentRouteHighways, ws.currentWay).stream()
                        .filter(ws::compareTraversal)
                        .map(waysInParentRoute -> parentRoute)
                        .forEachOrdered(itinerariesInSameDirection::add);
                }
            }
            if (getMembershipCount(ws.currentWay, relation) < 2) {
                parentRelationsForSameDirectionOfTravel.put(
                    relation.getId() + "," + ws.currentWay.getId(),
                    itinerariesInSameDirection);
            }
            return itinerariesInSameDirection;
        }
    }

    /**
     * @param way way to locate
     * @param routeRelation route relation to locate way in
     * @return number of times this way appears in the route relation, regardless of the sense it's traversed
     */
    public long getMembershipCount(Way way, Relation routeRelation) {
        return this.getItineraryWays(routeRelation).stream()
            .filter(w -> w.equals(way)).count();
    }
    /**
     * @param way way to locate
     * @param routeRelation route relation to locate way in
     * @return number of times this way appears in the route relation, being traversed in the same sense
     */
    public long getMembershipCountOfWayInSameDirection(Way way, Relation routeRelation) {
        List<Integer> indices = getIndicesFor(way, routeRelation);

        int counter = indices.size();
        for (int i = 0; i < indices.size() - 1 ; i++) {
            if (!new WaySequence(routeRelation, indices.get(i)).compareTraversal(
                new WaySequence(routeRelation, indices.get(i+1)))) {
                counter--;
            }
        }
        return counter;
    }

    public List<Integer> getIndicesFor(OsmPrimitive primitive, Relation routeRelation) {
        List<Integer> indices = new ArrayList<>();
        List<RelationMember> members = routeRelation.getMembers();
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getMember().equals(primitive)) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * for all occurrences of wayToLocate this method returns the way before it and the way after it
     * @param highwayMembers          The members list of the relation
     * @param wayToLocate      The way to locate in the list
     * @return a list of way sequences
     */
    private static List<WaySequence> findPreviousAndNextWayInRoute(List<Way> highwayMembers, Way wayToLocate) {
        Way wayAtIndexPosition;
        Way nextWay = null;
        Way wayAfterNextWay = null;
        boolean foundWay = false;
        List<WaySequence> waySequences = new ArrayList<>();
        for (int j = highwayMembers.size() - 1; j>=0 ; j--) {
            wayAtIndexPosition = highwayMembers.get(j);
            if (foundWay) {
                final WaySequence waySequence = new WaySequence(
                    wayAtIndexPosition, wayToLocate, nextWay, wayAfterNextWay);
                if (!waySequence.hasGap) {
                    waySequences.add(0, waySequence);
                }
                wayAfterNextWay = null;
                nextWay = null;
                foundWay = false;
                continue;
            }
            if (wayAtIndexPosition.equals(wayToLocate)) {
                foundWay = true;
            } else {
                wayAfterNextWay = nextWay;
                nextWay = wayAtIndexPosition;
            }
        }
        return waySequences;
    }

    @SuppressWarnings("unused")
    public List<String> getWayIds() {
        if (wayIds == null) {
            wayIds = new ArrayList<>();
            for (RelationMember rm : wayMembers) {
                wayIds.add(String.valueOf(rm.getWay().getId()));
            }
        }
        wayIdsSignature = String.join(";", wayIds);
        return wayIds;
    }

    /**
     * @return All the Way member's ids as a ; delimited string
     */
    public String getWayIdsSignature() {
        getWayIds();
        return wayIdsSignature;
    }

    /**
     * Adds a line identifier to the list of line identifiers
     * @param lineIdentifier  The ref tag of the way's parent relation
     */
    public void addLineIdentifier(String lineIdentifier) {
        if (lineIdentifier != null) {
            lineIdentifiers.add(lineIdentifier);
        }
    }

    /**
     * Adds the colour to the list of colours
     * @param colour          The colour tag of the way's parent relation
     */
    public void addColour(String colour) {
        if (colour != null) {
            colours.add(colour.toUpperCase());
        }
    }

    public TreeSet<String> getLineIdentifiers() {
        populateLineIdentifierAndColourLists();
        return lineIdentifiers;
    }

    public void populateLineIdentifierAndColourLists() {
        if (itinerariesInSameDirection == null) return;
        for (Relation relation : itinerariesInSameDirection) {
            addLineIdentifier(relation.get("ref"));
            addColour(relation.get("colour"));
        }
    }

    public String getLineIdentifiersSignature() {
        return String.join(";", getLineIdentifiers());
    }

    public TreeSet<String> getColours() {
        if (colours.size() < 2) {
            populateLineIdentifierAndColourLists();
        }
        return colours;
    }

    public String getColoursSignature() {
        String signature = String.join(";", getColours());
        if (signature.isEmpty()) {
            return null;
        } else {
            return signature;
        }
    }

    /**
     * determines the distinct street names or refs of the Way members
     * @return All the distinct street names or refs of the Way members
     */
    public List<String> getStreetNames() {
        if (streetNames == null) {
            streetNames = new ArrayList<>();
            String streetName;
            for (RelationMember rm : wayMembers) {
                streetName = rm.getWay().get("name");
                if (streetName == null) {
                    streetName = rm.getWay().get("ref");
                }
                if (streetName != null) {
                    if (!streetNames.contains(streetName)) {
                        streetNames.add(streetName);
                    }
                }
            }
        }
        return streetNames;
    }

    /**
     * @return All the distinct street names or refs of the Way members as a ; delimited string
     */
    @SuppressWarnings("unused")
    public String getStreetNamesSignature() {
        return String.join(";", getStreetNames());
    }

    /**
     * @return The first and the last street name, separated by " - "
     *         or only one of them, followed by a space
     *
     *         or an empty string
     */
    public String getFirstAndLastStreetNameOrRef() {
        List<String> streetNames = getStreetNames();
        String first = "";
        String last = "";
        if (streetNames.size() > 0) {
            first = streetNames.get(0);
            if (streetNames.size() > 1) {
                last = streetNames.get(streetNames.size() - 1);
            }
        }
        if (first.equals(last)) {
            last = "";
        }
        String names;
        if (!"".equals(first) && !"".equals(last)) {
            names = String.join(" - ", first, last);
        } else if (!"".equals(first)) {
            names = first;
        } else if (!"".equals(last)) {
            names = last;
        } else {
            return "";
        }
        return names + " ";
    }

    public List<Integer> getIndices() {
        return indices;
    }

    /**
     * @param tagsToTransfer list of tags to transfer from relation this segment will be extracted from
     * @param substituteWaysWithRelation add the extracted relation where the ways were removed?
     * @return the relation that contains the extracted ways, or null if an empty relation would have been created
     */
    public Relation extractToRelation(List<String> tagsToTransfer, Boolean removeWaysFromRelation , Boolean substituteWaysWithRelation) {
        assert relation != null;
        boolean extractedRelationAlreadyExists = false;
        if (ptSegments.containsKey(getWayIdsSignature())) {
            extractedRelation = ptSegments.get(wayIdsSignature);
            extractedRelationAlreadyExists = true;
        } else {
            extractedRelation = new Relation();
            extractedRelation.setKeys(tags);
            for (String tag : tagsToTransfer) {
                extractedRelation.put(tag, relation.get(tag));
            }
            extractedRelation.put("type", "route");
        }
        boolean atLeast1MemberAddedToExtractedRelation = false;
        int index = 0;
        for (int i = indices.size() - 1; i >= 0; i--) {
            index = indices.get(i);
            RelationMember relationMember;
            if (removeWaysFromRelation) {
                relationMember = relation.removeMember(index);
            } else {
                relationMember = relation.getMember(index);
            }
            if (!extractedRelationAlreadyExists && isPTWay(relationMember)) {
                extractedRelation.addMember(0,
                                            new RelationMember("", relationMember.getMember()));
                atLeast1MemberAddedToExtractedRelation = true;
            }
        }

        if (atLeast1MemberAddedToExtractedRelation || extractedRelationAlreadyExists) {
            updateTags();
            if (extractedRelation.getId() <= 0 && !extractedRelationAlreadyExists) {
                getExtractRelationCommand().executeCommand();
                addPtSegment();
            }
            if (substituteWaysWithRelation) {
                // replace removed members with the extracted relation
                relation.addMember(limitIntegerTo(index, relation.getMembersCount()),
                                   new RelationMember("", extractedRelation));
            }
        } else {
            return null;
        }
        return extractedRelation;
    }

    public Command getExtractRelationCommand() {
        List<Command> commands = new ArrayList<>();
        commands.add(new AddCommand(activeDataSet, extractedRelation));

        return new SequenceCommand(tr("Extract sub relation"), commands);
    }

    public String getNote() {
        return String.format("%s(%s)", getFirstAndLastStreetNameOrRef(), getLineIdentifiersSignature());
    }

    public void updateTags() {
        extractedRelation.put("note", getNote());
//        extractedRelation.put("route_ref", getLineIdentifiersSignature());
    }

    private void addPtSegment() {
        if (extractedRelation != null) {
            ptSegments.put(getWayIdsSignature(), extractedRelation);
        }
    }

    public static int limitIntegerTo(int index, int limit) {
        if (index > limit) {
            index = limit;
        }
        return index;
    }

    public void put(String key, String value) {
        tags.put(key, value);
    }
}
