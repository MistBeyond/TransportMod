package com.mistbeyond.transport.api.rail.graph;

import java.util.Set;

@FunctionalInterface
public interface TrackGraphSource {
    Set<TrackPlacement> placementsAt(GridPos cell);
}
