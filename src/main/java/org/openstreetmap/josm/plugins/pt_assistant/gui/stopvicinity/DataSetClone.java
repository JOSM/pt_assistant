package org.openstreetmap.josm.plugins.pt_assistant.gui.stopvicinity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSourceAddedEvent;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;

/**
 * An OSM dataset may not be used for two renderers (global things like style cache)
 *
 * So we need to do a read-only copy of it and sync it with the main data set.
 *
 * Advantage for us: We have our own highlight flags, our own other stuff.
 */
public class DataSetClone {
    private final DataSet copyFrom;
    private final Collection<OsmPrimitive> toSelect;
    private final DataSet clone = new DataSet();
    private final Map<OsmPrimitive, OsmPrimitive> originalToCopy = new HashMap<>();

    // TODO: Optimize some of these to make them faster
    private final DataSetListener dataSetListener = new DataSetListener() {
        @Override
        public void primitivesAdded(PrimitivesAddedEvent event) {
            refreshAll();
        }

        @Override
        public void primitivesRemoved(PrimitivesRemovedEvent event) {
            refreshAll();
        }

        @Override
        public void tagsChanged(TagsChangedEvent event) {
            refreshAll();
        }

        @Override
        public void nodeMoved(NodeMovedEvent event) {
            Node myNode = (Node) originalToCopy.get(event.getNode());
            if (myNode != null) {
                myNode.setCoor(event.getNode().getCoor());
            }
            refreshAll();
        }

        @Override
        public void wayNodesChanged(WayNodesChangedEvent event) {
            refreshAll();
        }

        @Override
        public void relationMembersChanged(RelationMembersChangedEvent event) {
            refreshAll();
        }

        @Override
        public void otherDatasetChange(AbstractDatasetChangedEvent event) {
            refreshAll();
        }

        @Override
        public void dataChanged(DataChangedEvent event) {
            refreshAll();
        }
    };

    public DataSetClone(DataSet copyFrom, Collection<OsmPrimitive> toSelect) {
        this.copyFrom = copyFrom;
        this.toSelect = toSelect;
        copyFrom.addDataSetListener(dataSetListener);
        refreshAll();
        clone.setUploadPolicy(UploadPolicy.BLOCKED);
        clone.setDownloadPolicy(DownloadPolicy.BLOCKED);
    }

    private void refreshAll() {
        // Code copied from new DataSet(DataSet) constructor
        copyFrom.getReadLock().lock();
        try {
            clone.clear();
            originalToCopy.clear();

            for (Node n : copyFrom.getNodes()) {
                Node newNode = new Node(n);
                originalToCopy.put(n, newNode);
                clone.addPrimitive(newNode);
            }
            for (Way w : copyFrom.getWays()) {
                Way newWay = new Way(w, false, false);
                originalToCopy.put(w, newWay);
                List<Node> newNodes = w.getNodes().stream()
                    .map(n -> (Node) originalToCopy.get(n))
                    .collect(Collectors.toList());
                newWay.setNodes(newNodes);
                clone.addPrimitive(newWay);
            }
            // Because relations can have other relations as members we first clone all relations
            // and then get the cloned members
            Collection<Relation> relations = copyFrom.getRelations();
            for (Relation r : relations) {
                Relation newRelation = new Relation(r, false, false);
                originalToCopy.put(r, newRelation);
                clone.addPrimitive(newRelation);
            }
            for (Relation r : relations) {
                ((Relation) originalToCopy.get(r)).setMembers(r.getMembers().stream()
                    .map(rm -> new RelationMember(rm.getRole(), originalToCopy.get(rm.getMember())))
                    .collect(Collectors.toList()));
            }
            // For now, ignore the part of the selection that got deleted/changed in parent
            clone.setSelected(toSelect.stream()
                .map(originalToCopy::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
            // Ignore data source
            clone.setVersion(copyFrom.getVersion());
        } finally {
            copyFrom.getReadLock().unlock();
        }
    }

    public DataSet getClone() {
        return clone;
    }

    public void dispose() {
        copyFrom.removeDataSetListener(dataSetListener);
    }
}
