package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

public record TrackCellDataRecord(
        GridPos cell,
        Set<TrackPlacement> placements,
        Set<SignalPlacement> signals
) implements TrackCellData {
    public TrackCellDataRecord {
        placements = Set.copyOf(placements);
        signals = Set.copyOf(signals);
    }

    /**
     * Backwards-compatible constructor: single optional signal.
     */
    public TrackCellDataRecord(GridPos cell, Set<TrackPlacement> placements, Optional<SignalPlacement> signal) {
        this(cell, placements, signal.map(Set::of).orElse(Set.of()));
    }
}
