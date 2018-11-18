// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.List;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.plugins.pt_assistant.actions.CreatePlatformNodeAction;

/**
 * PT assistant plugin properties.
 */
public final class PTProperties {
    private static final String EMPTY_STRING = "";

    public static final BooleanProperty DOWNLOAD_INCOMPLETE =
        new BooleanProperty("pt_assistant.download-incomplete", false);
    public static final BooleanProperty STOP_AREA_TESTS =
        new BooleanProperty("pt_assistant.stop-area-tests", false);
    public static final BooleanProperty PROCEED_WITHOUT_FIX =
        new BooleanProperty("pt_assistant.proceed-without-fix", true);

    /**
     * Options for the roundabout splitter.
     */
    public static final BooleanProperty ROUNDABOUT_SPLITTER_ALIGN_ALWAYS =
        new BooleanProperty("pt_assistant.roundabout-splitter.alignalways", false);

    /**
     * Options used in {@link CreatePlatformNodeAction}.
     */
    public static final BooleanProperty SUBSTITUTE_PLATFORMWAY_RELATION =
        new BooleanProperty("pt_assistant.substitute-platformway-relation", true);
    public static final BooleanProperty TRANSFER_STOPPOSITION_TAG =
        new BooleanProperty("pt_assistant.transfer-stopposition-tag", true);
    public static final BooleanProperty TRANSFER_PLATFORMWAY_TAG =
        new BooleanProperty("pt_assistant.transfer-platformway-tag", true);

    /**
     * Options for the wizard
     */
    public static final StringProperty WIZARD_PAGES =
        new StringProperty("pt_assistant.wizard.pages", EMPTY_STRING);
    public static final StringProperty WIZARD_INFORMATION =
        new StringProperty("pt_assistant.wizard.0.information", EMPTY_STRING);
    public static final IntegerProperty WIZARD_1_SUGGESTION =
        new IntegerProperty("pt_assistant.wizard.1.suggestion", 12);
    public static List<List<String>> getWizardSuggestions(final int i) {
        if (i < 2) {
            throw new IllegalArgumentException("Only wizard suggestions > 1 are lists of lists!");
        }
        return Preferences.main().getListOfLists(String.format("pt_assistant.wizard.%d.suggestion", i));
    }

    private PTProperties() {
        // Private constructor to avoid instantiation
    }
}
