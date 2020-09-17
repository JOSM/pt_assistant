// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

/**
 * Represents a piece of a route that includes all the ways
 * belonging to the same bundle of itineraries of vehicles
 * traveling in the same direction
 *
 * @author Polyglot
 *
 */
public class PTSegmentToExtract {
    private final Relation relation;

    private final ArrayList<RelationMember> ptWays;
    private List<Integer> indices;
    private List<String> lineIdentifiers;
    private List<String> colours;
    private List<String> streetNames;

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

        this.ptWays = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.lineIdentifiers = new ArrayList<>();
        this.colours = new ArrayList<>();
        this.streetNames = null;
    }

    /**
     * Returns the PTWays of this route segment
     * @return the PTWays of this route segment
     */
    public List<RelationMember> getPTWays() {
        return this.ptWays;
    }

    /**
     * Sets the PTWays of this route segment to the given list
     * @param ptWay           the Way Member to add
     * @param index           its index in the relation specified in the constructor
     * @param lineIdentifier  The ref tag of the relation
     * @param colour          The colour tag of the relation
     */
    public void addPTWay(RelationMember ptWay, Integer index, String lineIdentifier, String colour) {
        this.ptWays.add(0, ptWay);
        this.indices.add(0, index);
        addLineIdentifier(lineIdentifier);
        addColour(colour);
        this.streetNames = null;
    }

    /**
     * Adds a line identifier to the list of line identifiers
     * @param lineIdentifier  The ref tag of the way's parent relation
     */
    public void addLineIdentifier(String lineIdentifier) {
        if (lineIdentifier != null && !lineIdentifiers.contains(lineIdentifier)) {
            this.lineIdentifiers.add(lineIdentifier);
        }
    }

    /**
     * Adds the colour to the list of colours
     * @param colour          The colour tag of the way's parent relation
     */
    public void addColour(String colour) {
        if (colour != null && !colours.contains(colour)) {
            this.colours.add(colour);
        }
    }

    public List<String> getLineIdentifiers() {
        sort(this.lineIdentifiers);
        return this.lineIdentifiers;
    }

    public String getLineIdentifiersSignature() {
        return String.join(";", getLineIdentifiers());
    }

    public List<String> getColours() {
        sort(this.colours);
        return this.colours;
    }

    public String getColoursSignature() {
        return String.join(";", getColours());
    }

    /**
     * determines the distinct street names or refs of the Way members
     * @return All the distinct street names or refs of the Way members
     */
    public List<String> getStreetNames() {
        if (this.streetNames == null) {
            this.streetNames = new ArrayList<>();
            String streetName;
            for (RelationMember rm : this.ptWays) {
                streetName = rm.getWay().get("name");
                if (streetName == null) {
                    streetName = rm.getWay().get("ref");
                }
                if (streetName != null) {
                    this.streetNames.add(streetName);
                }
            }
        }
        return this.streetNames;
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
        return this.indices;
    }
}
