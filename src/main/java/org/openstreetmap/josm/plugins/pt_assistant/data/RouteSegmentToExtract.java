// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
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
    private DataSet activeDataSet;
    private Relation relation;
    private Relation extractedRelation;

    private ArrayList<RelationMember> wayMembers;
    private List<Integer> indices;
    private final TreeSet<String> lineIdentifiers;
    private final TreeSet<String> colours;
    private List<String> streetNames = null;
    private List<String> wayIds;
    private String wayIdsSignature;
    private final TagMap tags = new TagMap();

    static {
        ptSegments = new HashMap<>();
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
    }
    /**
     * Constructor
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
     * @param relation The route or superroute relation for which this route segment is created
     *                 use addPTWay() to add ways one by one
     * @param ds for the unit tests the dataset is the one of the file loaded
     *           if it's determined automatically it weirdly uses another DataSet
     */
    public RouteSegmentToExtract(Relation relation, DataSet ds) {
        this(relation);
        activeDataSet = ds;
    }

    /**
     * Constructor for use with non PT route relations
     * @param relation The route or superroute relation for which this route segment is created
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
     * @param existingRelation to be used when a potential sub route relation is encountered
     *                         while processing a PT route relation
     * @param updateTags update the tags automatically or not?
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
     * @param index its index in the relation specified in the constructor
     */
    public void addWay(Integer index) {
        this.addWay(index, true);
    }

    /**
     * Sets the WayMembers of this route segment to the given list
     * @param index its index in the relation specified in the constructor
     * @param updateIndices when the list of indices was set by the constructor
     *                      don't update it anymore
     */
    private void addWay(Integer index, boolean updateIndices) {
        assert relation != null;
        final RelationMember member = relation.getMember(index);
        if(member.isWay()) {
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
        final RelationMember member = relation.getMember(index);
        RelationMember previousMember;
        Way previousWay = null;
        if (member.isWay() && isPTWay(member)) {
            boolean startNewSegment = false;
            if (index > 1) {
                previousMember = relation.getMember(index - 1);
                if (isPTWay(previousMember)) {
                    previousWay = previousMember.getWay();
                }
            }
            Way currentWay = member.getWay();
            Way nextWay = null;
            if (index < relation.getMembersCount()-1) {
                final RelationMember nextMember = relation.getMember(index + 1);
                if (nextMember.isWay()) {
                    nextWay = nextMember.getWay();
                }
            }
            Way wayAfterNextWay = null;
            if (index < relation.getMembersCount()-2) {
                final RelationMember nextNextMember = relation.getMember(index + 2);
                if (nextNextMember.isWay()) {
                    wayAfterNextWay = nextNextMember.getWay();
                } else if (nextNextMember.isRelation()) {
                    wayAfterNextWay = nextNextMember.getRelation().getMember(0).getWay();
                }
            }
            if (wayMembers.size() == 0) {
                addWay(index, true);
            } else {
                List<Relation> parentRouteRelations =
                    Utils.filteredCollection(currentWay.getReferrers(), Relation.class).stream()
                        .filter(r -> r.getId() != relation.getId())
                        .filter(RouteUtils::isVersionTwoPTRoute)
                        .collect(Collectors.toList());
                ArrayList<Relation> itinerariesInSameDirection = new ArrayList<>();
                for (Relation parentRoute : parentRouteRelations) {
                    final List<RelationMember> parentRouteMembers = parentRoute.getMembers();
                    List<Way> ways = new ArrayList<>();
                    for (RelationMember rm : parentRouteMembers) {
                        if (rm.isWay()) {
                            ways.add(rm.getWay());
                        }
                    }
                    if (ways.contains(currentWay)) {
                        for (WayTriplet<Way, Way, Way> waysInParentRoute : findPreviousAndNextWayInRoute(parentRouteMembers, currentWay)) {
                            if (isItineraryInSameDirection(previousWay, nextWay, waysInParentRoute)) {
                                itinerariesInSameDirection.add(parentRoute);
                            }
                        }
                    }
                }
                for (Relation parentRoute : itinerariesInSameDirection) {
                    boolean addToLineIdentifier = true;
                    Way lastWayInParentRoute = null;
                    List<RelationMember> members = parentRoute.getMembers();
                    for (int j = members.size() - 1; j>=0 ; j--) {
                        RelationMember rm = members.get(j);
                        if (rm.isWay() && RouteUtils.isPTWay(rm)) {
                            lastWayInParentRoute = rm.getWay();
                            break;
                        }
                    }
                    if (currentWay == lastWayInParentRoute) {
                        startNewSegment = true;
                    }

                    for (WayTriplet<Way, Way, Way> waysInParentRoute : findPreviousAndNextWayInRoute(parentRoute.getMembers(), currentWay)) {
                        if (waysInParentRoute.nextWay != null) {
                            if (!nextWay.equals(waysInParentRoute.nextWay)){
                                // if one of the parent relations has a different next way
                                // it's time to start a new segment
                                startNewSegment = true;
                                addToLineIdentifier = false;
                            } else {
                                addLineIdentifier(parentRoute.get("ref"));
                                addColour(parentRoute.get("colour"));
                            }
                        }
                    }
                    /*
                     If the previous way before the next way's parent
                     routes isn't the same as currentWay a split is also needed
                    */
                    if (isPreviousWayDifferentFromCurrentWayInAtLeastOneOfTheParentsOfNextWay(
                            currentWay, nextWay, wayAfterNextWay)) {
                        startNewSegment = true;
                    } else if (addToLineIdentifier) {
                        addLineIdentifier(parentRoute.get("ref"));
                        addColour(parentRoute.get("colour"));
                    }
                }

                boolean currentWayIsFirstWayInOneOfTheParentWays = false;
                Way firstWayInParent = null;
                for (Relation prRel : parentRouteRelations) {
                    for (RelationMember rm : prRel.getMembers()) {
                        if (rm.isWay() && isPTWay(rm)) {
                            if (firstWayInParent == null) {
                                firstWayInParent = rm.getWay();
                            } else {
                                if (rm.getWay() == nextWay && firstWayInParent == currentWay) {
                                    currentWayIsFirstWayInOneOfTheParentWays = true;
                                    addLineIdentifier(prRel.get("ref"));
                                    addColour(prRel.get("colour"));
                                }
                                break;
                            }
                        }
                    }
                }
                if (itinerariesInSameDirection.isEmpty() && index < relation.getMembersCount() - 2) {
                    final RelationMember nextMember = relation.getMember(index + 2);
                    if (nextMember.isRelation()) {
                        Relation previousRelation = nextMember.getRelation();
                        if (!lineIdentifiers.first().equals(previousRelation.get("route_ref"))) {
                            startNewSegment = true;
                        }
                    }
                }
                if (startNewSegment
                        || currentWayIsFirstWayInOneOfTheParentWays
                        || itinerariesInSameDirection.isEmpty() && lineIdentifiers.size() > 1
                    ) {
                    RouteSegmentToExtract newSegment = new RouteSegmentToExtract(relation, activeDataSet);
                    newSegment.addPTWayMember(index);
                    return newSegment;
                } else {
                    addWay(index);
                }
            }
        }
        return null;
    }

    private boolean isPreviousWayDifferentFromCurrentWayInAtLeastOneOfTheParentsOfNextWay(
                           Way currentWay, Way nextWay, Way wayAfterNextWay) {
        List<Relation> parentRoutesOfNextWay =
            Utils.filteredCollection(nextWay.getReferrers(), Relation.class).stream()
            .filter(RouteUtils::isVersionTwoPTRoute)
            .collect(Collectors.toList());

        for (Relation parentRouteOfNextWay : parentRoutesOfNextWay) {
            for (WayTriplet<Way, Way, Way> waysInParentRouteOfNextWay : findPreviousAndNextWayInRoute(parentRouteOfNextWay.getMembers(), nextWay)) {
                if (waysInParentRouteOfNextWay.previousWay != null
                        && !waysInParentRouteOfNextWay.previousWay.equals(currentWay)
                        && isItineraryInSameDirection(currentWay, wayAfterNextWay, waysInParentRouteOfNextWay)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isItineraryInSameDirection(Way previousWay, Way nextWay,
                                               WayTriplet<Way, Way, Way> waysInParentRoute) {
        if (previousWay != null && nextWay != null) {
            return !(previousWay.equals(waysInParentRoute.nextWay) ||
                     nextWay.equals(waysInParentRoute.previousWay));
        }
        return false;
    }

    /**
     * for all occurrences of wayToLocate this method returns the way before it and the way after it
     * @param members          The members list of the relation
     * @param wayToLocate      The way to locate in the list
     * @return a list of way triplets
     */
    private static List<WayTriplet<Way,Way,Way>> findPreviousAndNextWayInRoute(List<RelationMember> members, Way wayToLocate) {
        final long wayToLocateId = wayToLocate.getId();
        Way previousWay;
        Way nextWay = null;
        boolean foundWay = false;
        List<WayTriplet<Way,Way,Way>> wayTriplets = new ArrayList<>();
        for (int j = members.size() - 1; j>=0 ; j--) {
            RelationMember rm = members.get(j);
            if (rm.isWay() && RouteUtils.isPTWay(rm)) {
                previousWay = rm.getWay();
                if (foundWay) {
                    if (previousWay == wayToLocate) {
                        previousWay = null;
                    }
                    wayTriplets.add(0, new WayTriplet<>(previousWay,wayToLocate,nextWay));
                    nextWay = null;
                    foundWay = false;
                    continue;
                }
                if (previousWay.getId() == wayToLocateId) {
                    foundWay = true;
                } else {
                    nextWay = previousWay;
                }
            }
        }
        return wayTriplets;
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
        return lineIdentifiers;
    }

    public String getLineIdentifiersSignature() {
        return String.join(";", getLineIdentifiers());
    }

    public TreeSet<String> getColours() {
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
