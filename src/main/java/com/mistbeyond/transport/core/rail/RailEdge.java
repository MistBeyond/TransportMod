package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;

public record RailEdge(
        RailEdgeId id,
        RailNodeId start,
        RailNodeId end,
        TrackPlacement placement,
        double lengthMeters
) implements RailEdgeView {
}
