package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.plugins.pt_assistant.actions.CreatePlatformNodeAction;

public final class PTProperties {


    public static final BooleanProperty DOWNLOAD_INCOMPLETE =
        new BooleanProperty("pt_assistant.download-incomplete", false);
    public static final BooleanProperty STOP_AREA_TESTS =
        new BooleanProperty("pt_assistant.stop-area-tests", false);
    public static final BooleanProperty PROCEED_WITHOUT_FIX =
        new BooleanProperty("pt_assistant.proceed-without-fix", true);

    /**
     * Options used in {@link CreatePlatformNodeAction}.
     */
    public static final BooleanProperty SUBSTITUTE_PLATFORMWAY_RELATION =
        new BooleanProperty("pt_assistant.substitute-platformway-relation", true);
    public static final BooleanProperty TRANSFER_STOPPOSITION_TAG =
        new BooleanProperty("pt_assistant.transfer-stopposition-tag", true);
    public static final BooleanProperty TRANSFER_PLATFORMWAY_TAG =
        new BooleanProperty("pt_assistant.transfer-platformway-tag", true);
}
