package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

public record TrackCellDataRecord(
        GridPos cell,
        Set<TrackPlacement> placements,
        Optional<SignalPlacement> signal
) implements TrackCellData {
    public TrackCellDataRecord {
        placements = Set.copyOf(placements);
    }
}
