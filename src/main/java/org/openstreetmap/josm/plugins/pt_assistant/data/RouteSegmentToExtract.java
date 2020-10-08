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
     * Constructor
     *
     * @param relation The route or superroute relation for which this route segment is created
     *                 use addPTWay() to add ways one by one
     * @param ds       for the unit tests the dataset is the one of the file loaded
     *                 if it's determined automatically it weirdly uses another DataSet
     */
    public RouteSegmentToExtract(Relation relation, DataSet ds) {
        this(relation);
        activeDataSet = ds;
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
        WaySequence waysInCurrentRoute = new WaySequence(relation, index);
        if (waysInCurrentRoute.currentWay == null) {
            return null;
        }
        WaySequence nextWaysInCurrentRoute = new WaySequence(waysInCurrentRoute.currentWay, waysInCurrentRoute.nextWay, waysInCurrentRoute.wayAfterNextWay);

        if (this.itinerariesInSameDirection == null && nextWaysInCurrentRoute.currentWay != null) {
            TreeSet<Relation> itinerariesInSameDirection = getItinerariesInSameDirection(waysInCurrentRoute);
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
            List<Relation> parentRouteRelations =
                Utils.filteredCollection(waysInCurrentRoute.currentWay.getReferrers(), Relation.class).stream()
//                    .filter(r -> r.getId() != relation.getId())
                    .filter(RouteUtils::isVersionTwoPTRoute)
                    .collect(Collectors.toList());
            TreeSet<Relation> itinerariesInSameDirection = getItinerariesInSameDirection(waysInCurrentRoute, parentRouteRelations);
            boolean startNewSegmentInNewSegment = false;
            if (this.itinerariesInSameDirection != null
                    && itinerariesInSameDirection.size() != 0
                    && !itinerariesInSameDirection.equals(this.itinerariesInSameDirection)
                    && !(getMembershipCount(waysInCurrentRoute.currentWay, relation) > 1
                            && waysInCurrentRoute.currentWay.equals(getFirstWay(relation)))
            ) {
                startNewSegment = true;
            } else {
                this.itinerariesInSameDirection = itinerariesInSameDirection;
                for (Relation parentRoute : itinerariesInSameDirection) {
                    if (waysInCurrentRoute.currentWay == getLastWay(parentRoute)
                            || getMembershipCountOfWayInSameDirection(waysInCurrentRoute.currentWay, parentRoute) > 1
                                && !parentRoute.equals(relation))
                    {
                        startNewSegment = true;
                        if (getMembershipCountOfWayInSameDirection(waysInCurrentRoute.currentWay, parentRoute) > 1) {
                            startNewSegmentInNewSegment = true;
                        }
                    }
                }
            }
            if (startNewSegment) {
                RouteSegmentToExtract newSegment = new RouteSegmentToExtract(relation, activeDataSet);
                newSegment.addPTWayMember(index);
                newSegment.itinerariesInSameDirection = itinerariesInSameDirection;
                newSegment.populateLineIdentifierAndColourLists();
                newSegment.startNewSegment = startNewSegmentInNewSegment;

                return newSegment;
            } else {
                if (this.itinerariesInSameDirection == null) {
                    this.itinerariesInSameDirection = itinerariesInSameDirection;
                    populateLineIdentifierAndColourLists();
                }
                addWay(index);
            }
        }
        return null;
    }

    public Way getFirstWay(Relation ptRoute) {
        return getItineraryWays(ptRoute).get(0);
    }

    public Way getSecondWay(Relation ptRoute) {
        return getItineraryWays(ptRoute).get(1);
    }

    public Way getLastWay(Relation ptRoute) {
        final List<Way> highways = getItineraryWays(ptRoute);
        return highways.get(highways.size() - 1);
    }

    public TreeSet<Relation> getItinerariesInSameDirection(
        WaySequence waysInCurrentRoute) {
        final String wayInRelation = relation.getId() + "," + waysInCurrentRoute.currentWay.getId();
        if (parentRelationsForSameDirectionOfTravel.containsKey(wayInRelation)) {
            return parentRelationsForSameDirectionOfTravel.get(wayInRelation);
        }
        return null;
    }

    public TreeSet<Relation> getItinerariesInSameDirection(
        WaySequence waysInCurrentRoute,
        List<Relation> parentRouteRelations) {
        TreeSet<Relation> r = getItinerariesInSameDirection(waysInCurrentRoute);
        if (r != null) {
            return r;
        } else {
            TreeSet<Relation> itinerariesInSameDirection = new TreeSet<>();
            for (Relation parentRoute : parentRouteRelations) {
                final List<Way> parentRouteHighways = getItineraryWays(parentRoute);
                if (!itinerariesInSameDirection.contains(parentRoute)) {
                    if (getFirstWay(parentRoute) == waysInCurrentRoute.currentWay
                            && waysInCurrentRoute.previousWay != null
                            && !waysInCurrentRoute.previousWay.getNodes().contains(getSecondWay(parentRoute).getNode(0))) {
                        itinerariesInSameDirection.add(parentRoute);
                    }
                    findPreviousAndNextWayInRoute(parentRouteHighways, waysInCurrentRoute.currentWay).stream()
                        .filter(waysInParentRoute -> isItineraryInSameDirection(waysInCurrentRoute, waysInParentRoute))
                        .map(waysInParentRoute -> parentRoute)
                        .forEachOrdered(itinerariesInSameDirection::add);
                }
            }
            if (getMembershipCount(waysInCurrentRoute.currentWay, relation) < 2) {
                parentRelationsForSameDirectionOfTravel.put(
                    relation.getId() + "," + waysInCurrentRoute.currentWay.getId(),
                    itinerariesInSameDirection);
            }
            return itinerariesInSameDirection;
        }
    }

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
                        getItineraryWays(rm.getRelation()).forEach(itinerary::add);
                    }
                }
            );
            itineraryWays.put(ptRoute, itinerary);
            return itinerary;
        }
    }

    /**
     * @param way
     * @param routeRelation
     * @return number of times this way appears in the route relation, regardless of the sense it's traversed
     */
    public long getMembershipCount(Way way, Relation routeRelation) {
        return this.getItineraryWays(routeRelation).stream()
            .filter(w -> w.equals(way)).count();
    }
    /**
     * @param way
     * @param routeRelation
     * @return number of times this way appears in the route relation, being traversed in the same sense
     */
    public long getMembershipCountOfWayInSameDirection(Way way, Relation routeRelation) {
        List<Integer> indices = new ArrayList<>();
        List<RelationMember> members = routeRelation.getMembers();
        for (int i = 0; i < members.size(); i++) {
            RelationMember member = members.get(i);
            if (member.getMember().equals(way)) {
                indices.add(i);
            }
        }
        int counter = indices.size();
        assert !(counter > 2); // if this happens the method will have to be rewritten to accomodate 3 or more occurrences
                                      // of the same with in a single route relation
        if (counter == 2) {
            Way previousWay1 = null;
            Way previousWay2 = null;
            Way nextWay1 = null;
            Way nextWay2 = null;
            if (routeRelation.getMember(indices.get(0) - 1).isWay())
                previousWay1 = routeRelation.getMember(indices.get(0) - 1).getWay();
            if (routeRelation.getMember(indices.get(1) - 1).isWay())
                previousWay2 = routeRelation.getMember(indices.get(1) - 1).getWay();
            if (routeRelation.getMember(indices.get(0) + 1).isWay())
                nextWay1 = routeRelation.getMember(indices.get(0) + 1).getWay();
            if (routeRelation.getMember(indices.get(1) + 1).isWay())
                nextWay2 = routeRelation.getMember(indices.get(1) + 1).getWay();
            if (!isItineraryInSameDirection(new WaySequence(
                                                previousWay1,
                                                way,
                                                nextWay1),
                                            new WaySequence(
                                                previousWay2,
                                                way,
                                                nextWay2))) {
                counter--;
            }
        }
        return counter;
    }

    public boolean isItineraryInSameDirection(WaySequence waysInCurrentRoute,
                                              WaySequence waysInParentRoute) {
        assert waysInCurrentRoute.currentWay == waysInParentRoute.currentWay :
            "this only works when comparing two equivalent way sequences" ;

        // if all ways are present, try the simple solution first
        if (waysInCurrentRoute.previousWay != null
                && waysInCurrentRoute.nextWay != null
                && waysInParentRoute.previousWay != null
                && waysInParentRoute.nextWay != null
                && (waysInCurrentRoute.previousWay == waysInParentRoute.previousWay
                    ||  waysInCurrentRoute.nextWay == waysInParentRoute.nextWay)
//                && currentWayOccurrencesInParentRoute < 2
        ) {
            return (!waysInCurrentRoute.previousWay.equals(waysInParentRoute.nextWay) &&
                    !waysInCurrentRoute.nextWay.    equals(waysInParentRoute.previousWay));
        }

        // if not, compare on the nodes
        Node firstNodeCurrentWay = null;
        if (waysInCurrentRoute.previousWay != null) {
            firstNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                waysInCurrentRoute.previousWay, waysInCurrentRoute.currentWay).orElse(null);
        }
        Node lastNodeCurrentWay = null;
        if (waysInCurrentRoute.nextWay != null) {
            lastNodeCurrentWay = WayUtils.findCommonFirstLastNode(
                waysInCurrentRoute.currentWay, waysInCurrentRoute.nextWay).orElse(null);
        }
        Node firstNodeWayOfParent = null;
        if (waysInParentRoute.previousWay != null) {
            firstNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                waysInParentRoute.previousWay, waysInParentRoute.currentWay).orElse(null);
        }
        Node lastNodeWayOfParent = null;
        if (waysInParentRoute.nextWay != null) {
            lastNodeWayOfParent = WayUtils.findCommonFirstLastNode(
                waysInParentRoute.currentWay, waysInParentRoute.nextWay).orElse(null);
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
                if (wayAtIndexPosition == wayToLocate) {
                    wayAtIndexPosition = null;
                }
                waySequences.add(0, new WaySequence(
                    wayAtIndexPosition, wayToLocate, nextWay, wayAfterNextWay));
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
                    streetNames.add(streetName);
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
        extractedRelation.put("colour", getColoursSignature());
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
