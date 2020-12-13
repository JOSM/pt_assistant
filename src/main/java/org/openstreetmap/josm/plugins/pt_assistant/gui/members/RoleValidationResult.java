package org.openstreetmap.josm.plugins.pt_assistant.gui.members;

import java.util.Objects;

public class RoleValidationResult {

    private final boolean valid;
    private final String correctedRole;

    private RoleValidationResult(boolean valid, String correctedRole) {
        this.valid = valid;
        this.correctedRole = correctedRole;
    }

    public boolean isValid() {
        return valid;
    }

    public String getCorrectedRole() {
        return correctedRole;
    }

    @Override
    public String toString() {
        return "RoleValidationResult{" +
            "valid=" + valid +
            ", correctedRole='" + correctedRole + '\'' +
            '}';
    }

    public static RoleValidationResult valid() {
        return new RoleValidationResult(true, null);
    }

    public static RoleValidationResult wrongRole(String correctedRole) {
        return new RoleValidationResult(false, Objects.requireNonNull(correctedRole, "correctedRole"));
    }
}
