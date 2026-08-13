package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;
import com.mistbeyond.transport.api.rail.section.SignalBoundaryView;
import com.mistbeyond.transport.api.rail.section.SignalId;

public record SignalBoundary(
        SignalId id,
        RailEdgeId edgeId,
        double metersFromStart,
        TraversalDirection direction
) implements SignalBoundaryView {
    public SignalBoundary {
        metersFromStart = Math.max(0.0, metersFromStart);
    }
}
