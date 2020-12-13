package org.openstreetmap.josm.plugins.pt_assistant.gui.members;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteStop;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteStopPositionOnWay;
import org.openstreetmap.josm.plugins.pt_assistant.gui.routing.RouteTraverser;

public class RouteRelationMemberValidator implements RelationMemberValidator {
    private final RouteTraverser route;
    private final List<RouteStop> stops;
    private final Map<OsmPrimitive, List<RouteStopPositionOnWay>> stopsReverse;

    public RouteRelationMemberValidator(RelationAccess relation) {
        route = new RouteTraverser(relation);
        stops = route.findStopPositions();

        stopsReverse = stops
            .stream()
            .filter(stop -> stop instanceof RouteStopPositionOnWay)
            .map(stop -> (RouteStopPositionOnWay) stop)
            .collect(Collectors.groupingBy(stop -> stop.getPosition().getWay().getWay()));
    }

    @Override
    public RoleValidationResult validateAndSuggest(int memberIndex, RelationMember member) {
        return RoleValidationResult.valid();
    }

    @Override
    public String getPrimitiveText(int memberIndex, RelationMember member) {
        if (OSMTags.STOPS_AND_PLATFORMS_ROLES.contains(member.getRole())) {
            return stops.stream()
                // TODO: Handle duplicate stops
                .filter(stop -> stop.getRelationMembers().contains(member.getMember()))
                .map(stop -> "<html>" + getStopText(stop) + "</html>")
                .findFirst()
                .orElse(null);
        } else {
            // Normal route part
            List<RouteStopPositionOnWay> stops = stopsReverse.get(member.getMember());
            if (stops != null) {
                return "<html>" + stops.stream()
                    .sorted(Comparator.comparing(stop -> stop.getPosition().getOffsetInRoute()))
                    .map(this::getStopText)
                    .collect(Collectors.joining(" ")) + "</html>";
            } else {
                return null;
            }
        }
    }

    private String getStopText(RouteStop stop) {
        return "<font color=\"" + (stop instanceof RouteStopPositionOnWay ? "#4d9bff" : "#ff3636") + "\">"
            + stop.getStopIndex() + "</font>";
    }
}
