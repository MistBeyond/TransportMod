package com.mistbeyond.transport.api.rail.graph;

public record TrackPosition(
        RailEdgeId edgeId,
        double metersFromStart,
        TraversalDirection direction
) {
    public TrackPosition {
        metersFromStart = Math.max(0.0, metersFromStart);
    }
}
