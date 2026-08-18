package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.PathfindingOptions;
import com.mistbeyond.transport.api.rail.dispatch.RailPathfinder;
import com.mistbeyond.transport.api.rail.dispatch.RailRoute;
import com.mistbeyond.transport.api.rail.dispatch.RouteRequest;
import com.mistbeyond.transport.api.rail.dispatch.StopPlan;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class ShortestPathRouter implements RailPathfinder {
    private static final double EPSILON = 1.0E-9;

    @Override
    public Optional<RailRoute> findRoute(RailGraphView graph, RouteRequest request) {
        List<RailNodeId> targets = new ArrayList<>();
        for (StopPlan stop : request.stops()) {
            targets.add(stop.station());
        }
        targets.add(request.destination());

        Set<RailNodeId> stopNodes = new HashSet<>();
        for (StopPlan stop : request.stops()) {
            stopNodes.add(stop.station());
        }

        RailNodeId current = request.start();
        List<RailRoute.RailRouteStep> steps = new ArrayList<>();
        for (RailNodeId target : targets) {
            Optional<List<RailRoute.RailRouteStep>> leg =
                    findLeg(graph, current, target, stopNodes, request.options());
            if (leg.isEmpty()) {
                return Optional.empty();
            }
            steps.addAll(leg.get());
            current = target;
        }
        return Optional.of(new RailRoute(request.start(), request.destination(), steps));
    }

    private Optional<List<RailRoute.RailRouteStep>> findLeg(
            RailGraphView graph,
            RailNodeId start,
            RailNodeId target,
            Set<RailNodeId> stopNodes,
            PathfindingOptions options
    ) {
        if (start.equals(target)) {
            return Optional.of(List.of());
        }

        Map<RailNodeId, Double> bestCost = new HashMap<>();
        Map<RailNodeId, RailRoute.RailRouteStep> previous = new HashMap<>();
        PriorityQueue<NodeState> queue = new PriorityQueue<>(
                Comparator.comparingDouble(NodeState::cost).thenComparing(state -> state.node().value())
        );
        bestCost.put(start, 0.0);
        queue.add(new NodeState(start, 0.0));

        while (!queue.isEmpty()) {
            NodeState current = queue.poll();
            if (!current.node().equals(target)
                    && current.cost() > bestCost.getOrDefault(current.node(), Double.POSITIVE_INFINITY) + EPSILON) {
                continue;
            }
            if (current.node().equals(target)) {
                return reconstruct(target, previous);
            }

            List<RailEdgeView> sortedEdges = graph.edges().stream()
                    .sorted(Comparator.comparing(RailEdgeView::id, Comparator.comparing(RailEdgeId::value)))
                    .toList();
            for (RailEdgeView edge : sortedEdges) {
                addTransition(current, edge, edge.start(), edge.end(), TraversalDirection.FORWARD,
                        target, stopNodes, options, bestCost, previous, queue);
                addTransition(current, edge, edge.end(), edge.start(), TraversalDirection.REVERSE,
                        target, stopNodes, options, bestCost, previous, queue);
            }
        }
        return Optional.empty();
    }

    private void addTransition(
            NodeState current,
            RailEdgeView edge,
            RailNodeId from,
            RailNodeId to,
            TraversalDirection direction,
            RailNodeId target,
            Set<RailNodeId> stopNodes,
            PathfindingOptions options,
            Map<RailNodeId, Double> bestCost,
            Map<RailNodeId, RailRoute.RailRouteStep> previous,
            PriorityQueue<NodeState> queue
    ) {
        if (!from.equals(current.node())) {
            return;
        }
        double stationPenalty = stopNodes.contains(to) && !to.equals(target) ? options.stationPenalty() : 0.0;
        double nextCost = current.cost() + edge.lengthMeters() + stationPenalty;
        if (nextCost < bestCost.getOrDefault(to, Double.POSITIVE_INFINITY) - EPSILON) {
            bestCost.put(to, nextCost);
            previous.put(to, new RailRoute.RailRouteStep(edge.id(), from, to, direction));
            queue.add(new NodeState(to, nextCost));
        }
    }

    private Optional<List<RailRoute.RailRouteStep>> reconstruct(
            RailNodeId target,
            Map<RailNodeId, RailRoute.RailRouteStep> previous
    ) {
        List<RailRoute.RailRouteStep> reversed = new ArrayList<>();
        RailNodeId current = target;
        while (previous.containsKey(current)) {
            RailRoute.RailRouteStep step = previous.get(current);
            reversed.add(step);
            current = step.entry();
        }
        List<RailRoute.RailRouteStep> result = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }
        return Optional.of(result);
    }

    private record NodeState(RailNodeId node, double cost) {
    }
}
