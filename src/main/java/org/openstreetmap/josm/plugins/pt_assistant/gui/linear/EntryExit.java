package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;

enum EntryExit {
    ENTRY,
    EXIT,
    BOTH;

    public static EntryExit ofRole(String role) {
        if (role.equals(OSMTags.STOP_ENTRY_ROLE)) {
            return ENTRY;
        } else if (role.equals(OSMTags.PLATFORM_EXIT_ROLE)) {
            return EXIT;
        } else {
            return BOTH;
        }
    }
}
