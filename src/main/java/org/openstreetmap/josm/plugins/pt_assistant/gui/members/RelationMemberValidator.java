package org.openstreetmap.josm.plugins.pt_assistant.gui.members;

import org.openstreetmap.josm.data.osm.RelationMember;

public interface RelationMemberValidator {

    RoleValidationResult validateAndSuggest(int memberIndex, RelationMember member);

    String getPrimitiveText(int memberIndex, RelationMember member);
}
