package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.graph.RailNodeId;

import java.util.List;

public record RouteRequest(
        RailTrainId trainId,
        RailNodeId start,
        RailNodeId destination,
        List<StopPlan> stops,
        PathfindingOptions options
) {
    public RouteRequest {
        stops = List.copyOf(stops);
    }
}
