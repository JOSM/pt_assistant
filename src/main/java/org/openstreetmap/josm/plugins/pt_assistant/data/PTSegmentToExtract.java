// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.sort;
import static org.openstreetmap.josm.gui.MainApplication.getLayerManager;
import static org.openstreetmap.josm.plugins.pt_assistant.gui.PTAssistantPaintVisitor.RefTagComparator;

/**
 * Represents a piece of a route that includes all the ways
 * belonging to the same bundle of itineraries of vehicles
 * traveling in the same direction
 *
 * @author Polyglot
 *
 */
public class PTSegmentToExtract {
    /**
     *
     */
    private static Map<String, Relation> ptSegments;
    private final Relation relation;
    private Relation extractedRelation;

    private ArrayList<RelationMember> ptWays;
    private final List<Integer> indices;
    private final TreeSet<String> lineIdentifiers;
    private final TreeSet<String> colours;
    private List<String> streetNames;
    private List<Long> streetIds;

    static {
        ptSegments = new HashMap<>();
    }

    private Boolean updateTags;

    /**
     * Constructor
     * @param relation  The route or superroute relation for which this route segment is created
     * ptWays           The list of PTWay members to extract
     * indices          The indices corresponding to the ways
     * lineIdentifiers  The ref tag of the route parent route relations of the ways
     * colours          The colours of the public transport lines of this line bundle
     */
    public PTSegmentToExtract(Relation relation) {
        this.relation = relation;
        extractedRelation = null;

        ptWays = new ArrayList<>();
        indices = new ArrayList<>();
        lineIdentifiers = new TreeSet<>(new RefTagComparator());
        colours = new TreeSet<>();
        streetNames = null;
        streetIds = null;
    }

    public PTSegmentToExtract(Relation existingRelation, Boolean updateTags) {
        this.relation = null;
        extractedRelation = existingRelation;

        ptWays = new ArrayList<>();
        indices = new ArrayList<>();
        lineIdentifiers = new TreeSet<>();
        colours = new TreeSet<>();
        streetNames = null;
        streetIds = null;

        this.ptWays = (ArrayList<RelationMember>) existingRelation.getMembers().stream()
            .filter(RelationMember::isWay)
            .filter(RouteUtils::isPTWay)
            .collect(Collectors.toList());
        if (updateTags) {
            updateTags();
        }
        ptSegments.put(getWayIdsSignature(), extractedRelation);
    }

    /**
     * Returns the PTWays of this route segment
     * @return the PTWays of this route segment
     */
    public List<RelationMember> getPTWays() {
        return ptWays;
    }

    /**
     * Sets the PTWays of this route segment to the given list
     * @param index its index in the relation specified in the constructor
     */
    public void addPTWay(Integer index) {
        assert relation != null;
        final RelationMember member = relation.getMember(index);
        if(member.isWay()) {
            indices.add(0, index);
            ptWays.add(0, member);
            addLineIdentifier(relation.get("ref"));
            addColour(relation.get("colour"));
            streetNames = null;
            streetIds = null;
        }
    }

    public List<Long> getWayIds() {
        if (streetIds == null) {
            streetIds = new ArrayList<>();
            for (RelationMember rm : ptWays) {
                streetIds.add(rm.getWay().getId());
            }
        }
        return streetIds;
    }

    /**
     * @return All the Way member's ids as a ; delimited string
     */
    public String getWayIdsSignature() {
        return String.join(";", getWayIds().toString());
    }

    /**
     * Adds a line identifier to the list of line identifiers
     * @param lineIdentifier  The ref tag of the way's parent relation
     */
    public void addLineIdentifier(String lineIdentifier) {
        if (lineIdentifier != null && !lineIdentifiers.contains(lineIdentifier)) {
            lineIdentifiers.add(lineIdentifier);
        }
    }

    /**
     * Adds the colour to the list of colours
     * @param colour          The colour tag of the way's parent relation
     */
    public void addColour(String colour) {
        if (colour != null) {
            String colourUppercase = colour.toUpperCase();
            if (!colours.contains(colourUppercase)) {
                colours.add(colourUppercase);
            }
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
            for (RelationMember rm : ptWays) {
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

    public Relation extractToRelation(ArrayList<String> tagsToTransfer, Boolean substituteWaysWithRelation) {
        boolean extractedRelationAlreadyExists = false;
        if (ptSegments.containsKey(getWayIdsSignature())) {
            extractedRelation = ptSegments.get(getWayIdsSignature());
            extractedRelationAlreadyExists = true;
        } else {
            extractedRelation = new Relation();
            for (String tag : tagsToTransfer) {
                assert relation != null;
                extractedRelation.put(tag, relation.get(tag));
            }
            extractedRelation.put("type", "route");
        }
        int index = 0;
        boolean atLeast1MemberAddedToExtractedRelation = false;
        for (int i = indices.size() - 1; i >= 0; i--) {
            index = indices.get(i);
            assert relation != null;
            RelationMember relationMember = relation.removeMember(index);
            if (!extractedRelationAlreadyExists && RouteUtils.isPTWay(relationMember)) {
                extractedRelation.addMember(0, relationMember);
                atLeast1MemberAddedToExtractedRelation = true;
            }
        }

        if (atLeast1MemberAddedToExtractedRelation) {
            if (extractedRelation.getId() <= 0 && !ptSegments.containsKey(getWayIdsSignature())) {
                updateTags();
                UndoRedoHandler.getInstance().add(new AddCommand(getLayerManager().getActiveDataSet(),
                    extractedRelation));
                addPtSegment();
            }
            if (substituteWaysWithRelation) {
                // replace removed members with the extracted relation
                relation.addMember(limitIntegerTo(index, relation.getMembersCount()-1),
                                   new RelationMember("", extractedRelation));
            }
        } else {
            return null;
        }
        return extractedRelation;
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
}
