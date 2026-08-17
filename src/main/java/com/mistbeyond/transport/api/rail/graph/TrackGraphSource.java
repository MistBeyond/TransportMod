package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

@FunctionalInterface
public interface TrackGraphSource {
    Set<TrackPlacement> placementsAt(GridPos cell);

    default Optional<SignalPlacement> signalAt(GridPos cell) {
        return Optional.empty();
    }

    default TrackCellData cellDataAt(GridPos cell) {
        return new TrackCellDataRecord(cell, placementsAt(cell), signalAt(cell));
    }
}
