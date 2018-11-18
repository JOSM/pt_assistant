package org.openstreetmap.josm.plugins.pt_assistant.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;

public class PrimitiveUtils {
    /**
     * @param primitive the primitive for which the occurences should be found
     * @param relations a {@link List} of {@link Relation}s in which this method searches for occurences
     * @return a map where the keys are the relations given as argument {@code relations}.
     *     The relations are mapped to a list of indices of all members of that relation that are equal
     *     to the primitive given as method parameter {@code primitive}.
     */
    public static Map<Relation, List<Integer>> findIndicesOfPrimitiveInRelations(final OsmPrimitive primitive, final List<Relation> relations) {
        return relations.stream().collect(Collectors.toMap(Function.identity(), it -> findIndicesOfPrimitiveInRelation(primitive, it)));
    }

    public static List<Integer> findIndicesOfPrimitiveInRelation(final OsmPrimitive primitive, final Relation relation) {
        return IntStream.range(0, relation.getMembers().size())
            .filter(i -> relation.getMember(i).refersTo(primitive))
            .boxed()
            .collect(Collectors.toList());
    }
}
