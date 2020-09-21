package org.openstreetmap.josm.plugins.pt_assistant.data;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PTSegmentToExtractTest {

    private DataSet ds;

    @Before
    public void init() throws FileNotFoundException, IllegalDataException {
        ds = OsmReader.parseDataSet(new FileInputStream(AbstractTest.PATH_TO_PT_BEFORE_SPLITTING_TEST), null);
    }

    @BeforeEach
    void setUp() {

    }

    @Test
    void someTest() {
        Collection<Relation> allRelations = ds.getRelations();
        Relation bus601RouteRelation = allRelations.stream().filter(relation -> relation.hasTag("ref", "601")).findFirst().get();
        PTSegmentToExtract segment = new PTSegmentToExtract(bus601RouteRelation);
        assertEquals("601", segment.getLineIdentifiersSignature());
        segment.addLineIdentifier("600");
        assertEquals("600;601", segment.getLineIdentifiersSignature());
        assertEquals("", segment.getWayIdsSignature());
        assertEquals(Collections.emptyList(), segment.getPTWays());
    }
}
