// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.plugins.pt_assistant.TestFiles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class DirecionTestTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void testOnewayTrue() {

        DataSet ds = TestFiles.importOsmFile(TestFiles.ONEWAY_WRONG_DIRECTION(), "testLayer");

        PTAssistantValidatorTest test = new PTAssistantValidatorTest();

        List<TestError> errors = new ArrayList<>();

        for (Relation r : ds.getRelations()) {
            WayChecker wayChecker = new WayChecker(r, test);
            wayChecker.performDirectionTest();
            errors.addAll(wayChecker.getErrors());
        }

        assertEquals(errors.size(), 2);
        int onewayErrorCaught = 0;
        for (TestError e : errors) {
            if (e.getCode() == PTAssistantValidatorTest.ERROR_CODE_DIRECTION) {
                onewayErrorCaught++;
            }
        }

        assertEquals(onewayErrorCaught, 2);

        boolean detectedErrorsAreCorrect = true;
        for (TestError e : errors) {
            if (e.getCode() == PTAssistantValidatorTest.ERROR_CODE_DIRECTION) {
                @SuppressWarnings("unchecked")
                Collection<OsmPrimitive> highlighted = (Collection<OsmPrimitive>) e.getHighlighted();
                for (OsmPrimitive highlightedPrimitive: highlighted) {
                    if (highlightedPrimitive.getId() != 225732678 && highlightedPrimitive.getId() != 24215210) {
                        detectedErrorsAreCorrect = false;
                    }
                }
            }
        }

        assertTrue(detectedErrorsAreCorrect);
    }
}
