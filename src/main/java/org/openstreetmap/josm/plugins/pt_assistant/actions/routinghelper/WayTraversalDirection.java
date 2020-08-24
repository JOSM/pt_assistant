package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

import java.util.function.Function;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Way;

public enum WayTraversalDirection {
    FORWARD(IWay::firstNode, IWay::lastNode),
    BACKWARD(IWay::lastNode, IWay::firstNode);

    private final Function<IWay<? extends INode>, INode> startNodeGetter;
    private final Function<IWay<? extends INode>, INode> endNodeGetter;

    WayTraversalDirection(
        @NotNull final Function<IWay<? extends INode>, INode> startNodeGetter,
        @NotNull final Function<IWay<? extends INode>, INode> endNodeGetter
    ) {
        this.startNodeGetter = startNodeGetter;
        this.endNodeGetter = endNodeGetter;
    }

    /**
     * Finds the first node that you come across when traversin the given way.
     *
     * @param way the way for which the first or last node is returned, depending on the direction
     * @return {@link #FORWARD} returns the {@link Way#firstNode()}, {@link #BACKWARD} returns the {@link Way#lastNode()}
     */
    @Nullable
    public INode getStartNodeFor(@NotNull final IWay<? extends INode> way) {
        return startNodeGetter.apply(way);
    }


    /**
     * Finds the node where traversal of the given way ends.
     *
     * @param way the way for which the first or last node is returned, depending on the direction
     * @return {@link #FORWARD} returns the {@link Way#lastNode()}, {@link #BACKWARD} returns the {@link Way#firstNode()}
     */
    @Nullable
    public INode getEndNodeFor(@NotNull final IWay<? extends INode> way) {
        return endNodeGetter.apply(way);
    }
}
