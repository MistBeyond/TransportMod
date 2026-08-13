package com.mistbeyond.transport.api.rail.graph;

public interface RailEdgeView {
    RailEdgeId id();

    RailNodeId start();

    RailNodeId end();

    TrackPlacement placement();

    double lengthMeters();
}
