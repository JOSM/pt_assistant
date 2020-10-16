// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static org.openstreetmap.josm.gui.MainApplication.getLayerManager;
import static org.openstreetmap.josm.plugins.pt_assistant.gui.PTAssistantPaintVisitor.RefTagComparator;
import static org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils.isPTWay;
import static org.openstreetmap.josm.tools.I18n.tr;

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
                indices.add(0, index);
            }
            wayMembers.add(0, member);
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
                    .filter(r -> "route".equals(r.get("type")) && "bus".equals(r.get("route")) && r.hasKey("route_ref"))
                    .collect(Collectors.toList());
            List<Relation> parentRouteRelations =
                Utils.filteredCollection(ws.currentWay.getReferrers(), Relation.class).stream()
                    .filter(RouteUtils::isVersionTwoPTRoute)
                    .collect(Collectors.toList());
            for (Relation sr : segmentRelations) {
                RouteSegmentToExtract existingSegment = new RouteSegmentToExtract(sr, true);
                if (existingSegment.getLastWay().equals(ws.currentWay)) {
                    final int existingSegmentSize = existingSegment.getWayMembers().size();
                    if (existingSegmentSize < 2) break;
                    final int startIndexOfRange = index - existingSegmentSize + 1;
                    if (startIndexOfRange > 0) {
                        List<RelationMember> sl = relation.getMembers().subList(startIndexOfRange, index + 1);
                        if (sl.equals(existingSegment.getWayMembers())) {
                            startNewSegment = true;
                        }
                    }
                }
                parentRouteRelations.addAll(sr.getReferrers().stream()
                    .filter(r -> Objects.equals(r.getType(), Relation.class) && "superroute".equals(r.get("type")))
                    .map(r -> (Relation) r)
                    .collect(Collectors.toList()));
            }

            TreeSet<Relation> itinerariesInSameDirection = getItinerariesInSameDirection(ws, parentRouteRelations);

            if (this.itinerariesInSameDirection != null
                && itinerariesInSameDirection.size() != 0
                && !itinerariesInSameDirection.equals(this.itinerariesInSameDirection)
                && !(getMembershipCount(ws.currentWay, relation) > 1
                && ws.currentWay.equals(getFirstWay(relation)))
            ) {
                startNewSegment = true;
            } else {
                this.itinerariesInSameDirection = itinerariesInSameDirection;
                for (Relation parentRoute : itinerariesInSameDirection) {
                    final Node commonNode2 = WayUtils.findFirstCommonNode(ws.currentWay, ws.nextWay).orElseGet(null);
                    final long membershipCountOfWayInSameDirectionInParentRoute =
                        getMembershipCountOfWayInSameDirection(ws.currentWay, parentRoute);
                    if (ws.currentWay == getLastWay(parentRoute)
                        || membershipCountOfWayInSameDirectionInParentRoute > 1
                        && !parentRoute.equals(relation)
                        && (commonNode2.getParentWays().size() > 2)
                    ) {
                        startNewSegment = true;
                        if (membershipCountOfWayInSameDirectionInParentRoute > 1) {
                            startNewSegmentInNewSegment = true;
                        }
                        break;
                    }
                    // Some PT lines have variants that make a 'spoon like loop'
                    // In this case the ref will usually be the same, if not membership in a route_master will need to be checked
                    // The common node between the way that goes into the new sub route relation (currentWay)
                    // and the first one of the route relation currently processed (nextWay) will always have more than 2 parent ways

                    if (!startNewSegment && !relation.equals(parentRoute)
                        && Objects.equals(relation.get("ref"), parentRoute.get("ref"))) {
                        if (commonNode2.getParentWays().stream()
                            .filter(w -> (getItineraryWays(parentRoute).contains(w)))
                            .count() > 2) {
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

    /**
     * @return The last way of the relation that is being processed
     */
    public Way getLastWay() {
        return wayMembers.get(wayMembers.size() - 1).getWay();
    }

    /**
     * @param ptRoute relation to analyse
     * @return the first highway/railway of ptRoute
     */
    public Way getFirstWay(Relation ptRoute) {
        return getItineraryWays(ptRoute).get(0);
    }

    /**
     * @param ptRoute relation to analyse
     * @return the second highway/railway of ptRoute
     */
    public Way getSecondWay(Relation ptRoute) {
        return getItineraryWays(ptRoute).get(1);
    }

    /**
     * @param ptRoute relation to analyse
     * @return the last highway/railway of ptRoute
     */
    public Way getLastWay(Relation ptRoute) {
        final List<Way> highways = getItineraryWays(ptRoute);
        return highways.get(highways.size() - 1);
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
                    if (getFirstWay(parentRoute) == ws.currentWay
                            && ws.previousWay != null
                            && !ws.previousWay.getNodes().contains(getSecondWay(parentRoute).getNode(0))) {
                        itinerariesInSameDirection.add(parentRoute);
                    }
                    findPreviousAndNextWayInRoute(parentRouteHighways, ws.currentWay).stream()
                        .filter(waysInParentRoute -> isItineraryInSameDirection(ws, waysInParentRoute))
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
            if (!isItineraryInSameDirection(new WaySequence(routeRelation, indices.get(i)),
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

    public boolean isItineraryInSameDirection(WaySequence ws,
                                              WaySequence parent_ws) {
        assert ws.currentWay == parent_ws.currentWay :
            "this only works when comparing two equivalent way sequences" ;

        // if all ways are present, try the simple solution first
        if (ws.previousWay != null
                && ws.nextWay != null
                && parent_ws.previousWay != null
                && parent_ws.nextWay != null
                && (ws.previousWay == parent_ws.previousWay
                    ||  ws.nextWay == parent_ws.nextWay)
        ) {
            return (!ws.previousWay.equals(parent_ws.nextWay) &&
                    !ws.nextWay.    equals(parent_ws.previousWay));
        }

        // if not, compare on the nodes
        Node firstNodeCurrentWay = null;
        if (ws.previousWay != null) {
            firstNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                ws.previousWay, ws.currentWay).orElse(null);
        }
        Node lastNodeCurrentWay = null;
        if (ws.nextWay != null) {
            lastNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                ws.currentWay, ws.nextWay).orElse(null);
        }
        Node firstNodeWayOfParent = null;
        if (parent_ws.previousWay != null) {
            firstNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                parent_ws.previousWay, parent_ws.currentWay).orElse(null);
        }
        Node lastNodeWayOfParent = null;
        if (parent_ws.nextWay != null) {
            lastNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                parent_ws.currentWay, parent_ws.nextWay).orElse(null);
        }

        return (firstNodeCurrentWay != null && firstNodeCurrentWay.equals(firstNodeWayOfParent)
                ||
                lastNodeCurrentWay != null && lastNodeCurrentWay.equals(lastNodeWayOfParent));
    }

    /**
     * for all occurrences of wayToLocate this method returns the way before it and the way after it
     * @param highwayMembers          The members list of the relation
     * @param wayToLocate      The way to locate in the list
     * @return a list of way triplets
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
        if (lineIdentifiers.size() < 2) {
            populateLineIdentifierAndColourLists();
        }
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
    public Relation extractToRelation(List<String> tagsToTransfer, Boolean substituteWaysWithRelation) {
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
            RelationMember relationMember = relation.removeMember(index);
            if (!extractedRelationAlreadyExists && isPTWay(relationMember)) {
                extractedRelation.addMember(0,
                                            new RelationMember("", relationMember.getMember()));
                atLeast1MemberAddedToExtractedRelation = true;
            }
        }

        if (atLeast1MemberAddedToExtractedRelation || extractedRelationAlreadyExists) {
            if (extractedRelation.getId() <= 0 && !extractedRelationAlreadyExists) {
                if (relation.hasTag("public_transport:version", "2")) {
                    updateTags();
                }
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

    public void updateTags() {
        final String lineIdentifiersSignature = getLineIdentifiersSignature();
        extractedRelation.put("note",
            String.format("%s(%s)", getFirstAndLastStreetNameOrRef(), lineIdentifiersSignature));
        extractedRelation.put("route_ref", lineIdentifiersSignature);
        // extractedRelation.put("street_names", getStreetNamesSignature());
        // extractedRelation.put("colour", getColoursSignature());
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
