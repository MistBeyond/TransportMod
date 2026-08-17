package com.mistbeyond.transport.api.rail.graph;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Set;

/**
 * The four track axes a simple track cell can represent (N–S, E–W, NE–SW, NW–SE). A simple cell is a bidirectional
 * segment, so each pair of opposite {@link GridDirection}s is equivalent and the BlockState {@code direction}
 * property stores one axis value instead of eight one-way directions; the graph adapter expands the axis back into
 * both directional placements (see ADR 0005 and docs/roadmap/rail/tracks.md). Signal direction still uses the full
 * 8-direction {@link GridDirection}.
 */
public enum TrackAxis implements StringRepresentable {
    N_S(GridDirection.NORTH, GridDirection.SOUTH),
    E_W(GridDirection.EAST, GridDirection.WEST),
    NE_SW(GridDirection.NORTH_EAST, GridDirection.SOUTH_WEST),
    NW_SE(GridDirection.NORTH_WEST, GridDirection.SOUTH_EAST);

    private final GridDirection first;
    private final GridDirection second;

    TrackAxis(GridDirection first, GridDirection second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Collapses a full 8-direction value to the axis of the track segment it describes.
     */
    public static TrackAxis from(GridDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> N_S;
            case EAST, WEST -> E_W;
            case NORTH_EAST, SOUTH_WEST -> NE_SW;
            case NORTH_WEST, SOUTH_EAST -> NW_SE;
        };
    }

    public boolean diagonal() {
        return this == NE_SW || this == NW_SE;
    }

    /**
     * Both directional placements of this axis. A simple cell emits both, so a line of same-facing simple cells forms
     * a connected bidirectional track graph.
     */
    public Set<GridDirection> directions() {
        return Set.of(first, second);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
