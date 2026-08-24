package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

@FunctionalInterface
public interface TrackGraphSource {
    Set<TrackPlacement> placementsAt(GridPos cell);

    default Set<SignalPlacement> signalsAt(GridPos cell) {
        return Set.of();
    }

    default Optional<SignalPlacement> signalAt(GridPos cell) {
        return signalsAt(cell).stream().findFirst();
    }

    default TrackCellData cellDataAt(GridPos cell) {
        return new TrackCellDataRecord(cell, placementsAt(cell), signalsAt(cell));
    }
}
