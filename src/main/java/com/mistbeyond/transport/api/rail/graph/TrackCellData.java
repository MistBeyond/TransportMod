package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

public interface TrackCellData {
    GridPos cell();

    Set<TrackPlacement> placements();

    Set<SignalPlacement> signals();

    /**
     * Legacy single-signal accessor for backwards compatibility: returns an arbitrary signal when the cell
     * contains one, otherwise empty. New code should use {@link #signals()}.
     */
    default Optional<SignalPlacement> signal() {
        return signals().stream().findFirst();
    }
}
