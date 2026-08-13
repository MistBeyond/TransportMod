package com.mistbeyond.transport.api.rail.section;

import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;

public interface SignalBoundaryView {
    SignalId id();

    RailEdgeId edgeId();

    double metersFromStart();

    TraversalDirection direction();
}
