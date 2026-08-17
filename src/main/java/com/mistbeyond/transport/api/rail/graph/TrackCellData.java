package com.mistbeyond.transport.api.rail.graph;

import java.util.Optional;
import java.util.Set;

public interface TrackCellData {
    GridPos cell();

    Set<TrackPlacement> placements();

    Optional<SignalPlacement> signal();
}
