package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;

/**
 * An OSM dataset may not be used for two renderers (global things like style cache)
 * <p>
 * So we need to do a read-only copy of it and sync it with the main data set.
 * <p>
 * Advantage for us: We have our own highlight flags, our own other stuff.
 * <p>
 * This data set allows for filtering the primitives (e.g. bounding box) and adding more primitives by subclassing it.
 */
public class DerivedDataSet {
    private final DataSet copyFrom;
    private final DataSet clone = new DataSet();
    private final Map<OsmPrimitive, OsmPrimitive> originalToCopy = new HashMap<>();

    // Currently, we assume that refresh is single threaded / synchronized
    // This is a list of all primitives that are derived to detect cycles.
    private final ArrayList<OsmPrimitive> primitivesCurrentlyDeriving = new ArrayList<>();

    private final Set<PrimitiveId> primitivesToHighlight = new HashSet<>();

    // TODO: Optimize some of these to make them faster
    private final DataSetListener dataSetListener = new DataSetListener() {
        @Override
        public void primitivesAdded(PrimitivesAddedEvent event) {
            if (!refreshNeeded) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    if (isIncluded(p)) {
                        addOrGetDerived(p);
                    }
                }
            }
        }

        @Override
        public void primitivesRemoved(PrimitivesRemovedEvent event) {
            markRefreshNeeded();
        }

        @Override
        public void tagsChanged(TagsChangedEvent event) {
            if (!refreshNeeded) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    OsmPrimitive inOurDataset = originalToCopy.get(p);
                    boolean shouldBeIncluded = isIncluded(p);
                    if (inOurDataset != null) {
                        if (shouldBeIncluded) {
                            // Everything fine => fix tags
                            inOurDataset.setKeys(p.getKeys());
                        } else {
                            // Remove this - currently, we can only do a refresh
                            markRefreshNeeded();
                        }
                    } else if (shouldBeIncluded) {
                        markRefreshNeeded();
                    }
                }
            }
        }

        @Override
        public void nodeMoved(NodeMovedEvent event) {
            if (!refreshNeeded) {
                Node myNode = (Node) originalToCopy.get(event.getNode());
                if (myNode != null) {
                    if (!isIncluded(myNode)) {
                        markRefreshNeeded();
                    } else {
                        myNode.setCoor(event.getNode().getCoor());
                    }
                } else if (isIncluded(event.getNode())) {
                    markRefreshNeeded();
                }
            }
        }

        @Override
        public void wayNodesChanged(WayNodesChangedEvent event) {
            markRefreshNeeded();
        }

        @Override
        public void relationMembersChanged(RelationMembersChangedEvent event) {
            markRefreshNeeded();
        }

        @Override
        public void otherDatasetChange(AbstractDatasetChangedEvent event) {
            markRefreshNeeded();
        }

        @Override
        public void dataChanged(DataChangedEvent event) {
            markRefreshNeeded();
        }
    };
    private boolean refreshNeeded = true;

    private void markRefreshNeeded() {
        refreshNeeded = true;
    }

    public DerivedDataSet(DataSet copyFrom) {
        this.copyFrom = copyFrom;
        copyFrom.addDataSetListener(dataSetListener);
        clone.setUploadPolicy(UploadPolicy.BLOCKED);
        clone.setDownloadPolicy(DownloadPolicy.BLOCKED);
    }

    /**
     * Test if a OSM primitive should be included in this data set
     *
     * @return The original primitive.
     */
    protected boolean isIncluded(OsmPrimitive primitive) {
        return true;
    }

    /**
     * Adds additional geometry to the new data set. Do not access getClone() in this method!
     * You can access geometry from the original data set by calling the addOrGetDerived* methods.
     *
     * @param addTo Accessor methods to add the new geometry.
     */
    protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
        // Nop
    }

    /**
     * Should be called before every access => way more performant than
     * keeping it up to date all the time, especially during downloads.
     */
    public final void refreshIfRequired() {
        if (refreshNeeded) {
            refreshAll();
        }
    }

    private synchronized void refreshAll() {
        clone.clear();
        originalToCopy.clear();

        copyFrom.getReadLock().lock();
        try {
            // Do this first => some allows the implementer to replace some geometry before we do.
            addAdditionalGeometry(new AdditionalGeometryAccess());

            for (Node n : copyFrom.getNodes()) {
                if (isIncluded(n)) {
                    addOrGetDerivedNode(n);
                }
            }

            for (Way w : copyFrom.getWays()) {
                if (isIncluded(w)) {
                    addOrGetDerivedWay(w);
                }
            }

            for (Relation r : copyFrom.getRelations()) {
                if (isIncluded(r)) {
                    addOrGetDerivedRelation(r);
                }
            }

            // For now, ignore the part of the selection that got deleted/changed in parent
            clone.setSelected(Collections.emptySet());
            // Ignore data source
            clone.setVersion(copyFrom.getVersion());
            refreshNeeded = false;
        } finally {
            copyFrom.getReadLock().unlock();
        }
    }

    /**
     * Find the representation of that original primitive in our data set. Adds it if not present.
     *
     * @param original The primitive
     * @return The derived primitve
     */
    protected synchronized OsmPrimitive addOrGetDerived(OsmPrimitive original) {
        Objects.requireNonNull(original, "original");
        if (original instanceof Node) {
            return addOrGetDerivedNode((Node) original);
        } else if (original instanceof Way) {
            return addOrGetDerivedWay((Way) original);
        } else if (original instanceof Relation) {
            return addOrGetDerivedRelation((Relation) original);
        } else {
            throw new IllegalArgumentException("Cannot handle " + original.getClass().getName());
        }
    }

    private Node addOrGetDerivedNode(Node original) {
        return addOrDerive(original, Node::new);
    }

    private Way addOrGetDerivedWay(Way original) {
        return addOrDerive(original, w -> {
            Way newWay = new Way(w, false, false);

            List<Node> newNodes = w.getNodes().stream()
                .map(this::addOrGetDerivedNode)
                .collect(Collectors.toList());
            newWay.setNodes(newNodes);
            return newWay;
        });
    }

    private Relation addOrGetDerivedRelation(Relation original) {
        return addOrDerive(original, o -> {
            Relation newRelation = new Relation(o, false, false);

            newRelation.setMembers(o.getMembers().stream()
                // This may recourse to an other relation => that's why we count.
                .map(this::addOrGetDerivedMember)
                .collect(Collectors.toList()));
            return newRelation;
        });
    }

    protected synchronized RelationMember addOrGetDerivedMember(RelationMember member) {
        return new RelationMember(member.getRole(), addOrGetDerived(member.getMember()));
    }

    @SuppressWarnings("unchecked")
    private <T extends OsmPrimitive> T addOrDerive(T original, Function<T, T> deriver) {
        // Cannot use compute if absent (recursion!)
        OsmPrimitive inClone = originalToCopy.get(original);
        if (inClone != null) {
            return (T) inClone;
        }

        if (primitivesCurrentlyDeriving.contains(original)) {
            throw new IllegalArgumentException("Cycle detected in dependencies of primitive " + original
                + "\nPrimitive IDs are " + primitivesCurrentlyDeriving.stream()
                .map(it -> it.getPrimitiveId().toString())
                .collect(Collectors.joining(", ")));
        }
        primitivesCurrentlyDeriving.add(original);
        try {
            T newPrimitive = deriver.apply(original);
            newPrimitive.setHighlighted(primitivesToHighlight.contains(newPrimitive.getPrimitiveId()));
            clone.addPrimitive(newPrimitive);
            registerCopy(original, newPrimitive);
            return newPrimitive;
        } finally {
            primitivesCurrentlyDeriving.remove(original);
        }
    }

    private <T extends OsmPrimitive> void registerCopy(T original, T newPrimitive) {
        if (originalToCopy.putIfAbsent(original, newPrimitive) != null) {
            throw new IllegalArgumentException("Attempted to register a copy twice for " + original);
        }
    }

    public DataSet getClone() {
        refreshIfRequired();
        return clone;
    }

    public void dispose() {
        copyFrom.removeDataSetListener(dataSetListener);
    }

    /**
     * Always use this method to set the highlighted primitives.
     * Otherwise, highlight data will be lost on sync.
     */
    public void highlight(Set<OsmPrimitive> toHighlight) {
        Set<PrimitiveId> toHighlightIds = toHighlight
            .stream()
            .map(IPrimitive::getPrimitiveId)
            .collect(Collectors.toSet());

        // Add missing
        setHighlightFlag(primitivesToHighlight, toHighlightIds, false);
        setHighlightFlag(toHighlightIds, primitivesToHighlight, true);

        primitivesToHighlight.clear();
        primitivesToHighlight.addAll(toHighlightIds);
    }

    public Set<PrimitiveId> getHighlightedPrimitives() {
        return Collections.unmodifiableSet(primitivesToHighlight);
    }

    private void setHighlightFlag(Set<PrimitiveId> including, Set<PrimitiveId> excluding, boolean flag) {
        including
            .stream()
            .filter(id -> !excluding.contains(id))
            .forEach(id -> {
                OsmPrimitive p = clone.getPrimitiveById(id);
                if (p != null) {
                    p.setHighlighted(flag);
                }
            });
    }

    @SuppressWarnings("unchecked")
    public <T extends OsmPrimitive> T findOriginal(T primitive) {
        return (T) originalToCopy
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == primitive)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    public class AdditionalGeometryAccess {
        public <T extends OsmPrimitive> void addAsCopy(T original, T newPrimitive) {
            Objects.requireNonNull(original, "original");
            Objects.requireNonNull(newPrimitive, "newPrimitive");
            registerCopy(original, newPrimitive);
            add(newPrimitive);
        }

        public void add(OsmPrimitive newPrimitive) {
            Objects.requireNonNull(newPrimitive, "newPrimitive");
            clone.addPrimitive(newPrimitive);
        }
    }
}
