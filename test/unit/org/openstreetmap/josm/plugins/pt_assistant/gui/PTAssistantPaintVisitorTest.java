package org.openstreetmap.josm.plugins.pt_assistant.gui;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;


class PTAssistantPaintVisitorTest {

    @Test
    void testRouteRefTagComparator() {
        final Comparator<String> c = new PTAssistantPaintVisitor.RefTagComparator();
        assertEquals(0, c.compare("", ""));
        assertEquals(0, c.compare(null, ""));
        assertEquals(0, c.compare("", null));
        assertEquals(0, c.compare(null, null));

        assertTrue(c.compare(null, "1") < 0);
        assertTrue(c.compare("1", null) > 0);

        assertTrue(c.compare("1", "a") < 0);
        assertTrue(c.compare("a", "1") > 0);

        assertEquals(0, c.compare("ABC", "ABC"));
        assertEquals(0, c.compare("1", "1"));
        assertEquals(0, c.compare("42", "42"));

        final List<String> listToSort = Arrays.asList("ABC", "1", "123", "42", "123a", "5Ä", "Ä5", "19999999999", "54321", "");
        listToSort.sort(c);

        assertEquals(Arrays.asList("", "1", "5Ä", "42", "123", "123a", "54321", "19999999999", "ABC", "Ä5"), listToSort);
    }
}
