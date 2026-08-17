package com.mistbeyond.transport.api.rail.station;

import com.mistbeyond.transport.api.rail.graph.GridPos;

import java.util.Optional;

@FunctionalInterface
public interface RailStationLocator {
    Optional<GridPos> locate(RailStationId station);
}
