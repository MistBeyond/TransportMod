package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;

import java.util.List;

public record RailRoute(
        RailNodeId start,
        RailNodeId end,
        List<RailRouteStep> steps
) {
    public RailRoute {
        steps = List.copyOf(steps);
    }

    public record RailRouteStep(
            RailEdgeId edgeId,
            RailNodeId entry,
            RailNodeId exit,
            TraversalDirection direction
    ) {
    }
}
