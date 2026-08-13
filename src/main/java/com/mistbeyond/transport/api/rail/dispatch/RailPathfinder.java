package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.graph.RailGraphView;

import java.util.Optional;

public interface RailPathfinder {
    Optional<RailRoute> findRoute(RailGraphView graph, RouteRequest request);
}
