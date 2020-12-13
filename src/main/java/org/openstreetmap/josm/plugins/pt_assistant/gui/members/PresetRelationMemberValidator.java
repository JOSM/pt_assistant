package org.openstreetmap.josm.plugins.pt_assistant.gui.members;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;

public class PresetRelationMemberValidator implements RelationMemberValidator {
    private final Collection<TaggingPreset> presets;

    public PresetRelationMemberValidator(RelationAccess relation) {
        presets = TaggingPresets.getMatchingPresets(
            EnumSet.of(relation.isMultipolygon() ? TaggingPresetType.MULTIPOLYGON : TaggingPresetType.RELATION),
            relation.getTags(), false);
    }

    @Override
    public RoleValidationResult validateAndSuggest(int memberIndex, RelationMember member) {
        Set<String> roles = presets.stream()
            .map(preset -> preset.suggestRoleForOsmPrimitive(member.getMember()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (roles.size() == 1) {
            String role = roles.iterator().next();
            if (role.equals(member.getRole())) {
                return RoleValidationResult.valid();
            } else {
                return RoleValidationResult.wrongRole(role);
            }
        } else {
            return RoleValidationResult.valid();
        }
    }

    @Override
    public String getPrimitiveText(int memberIndex, RelationMember member) {
        return null;
    }
}
