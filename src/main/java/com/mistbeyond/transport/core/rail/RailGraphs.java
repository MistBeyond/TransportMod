package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RailGraphs {
    private RailGraphs() {
    }

    public static Optional<RailNodeId> farthestReachableNode(RailGraphView graph, RailNodeId start) {
        if (graph.nodes().stream().noneMatch(node -> node.id().equals(start))) {
            return Optional.empty();
        }

        Map<RailNodeId, Set<RailNodeId>> adjacency = new HashMap<>();
        for (RailEdgeView edge : graph.edges()) {
            adjacency.computeIfAbsent(edge.start(), ignored -> new HashSet<>()).add(edge.end());
            adjacency.computeIfAbsent(edge.end(), ignored -> new HashSet<>()).add(edge.start());
        }

        Map<RailNodeId, Integer> hops = new HashMap<>();
        ArrayDeque<RailNodeId> queue = new ArrayDeque<>();
        hops.put(start, 0);
        queue.add(start);
        RailNodeId farthest = start;
        int farthestHops = 0;
        while (!queue.isEmpty()) {
            RailNodeId current = queue.removeFirst();
            int currentHops = hops.get(current);
            for (RailNodeId neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (hops.containsKey(neighbor)) {
                    continue;
                }
                int nextHops = currentHops + 1;
                hops.put(neighbor, nextHops);
                queue.add(neighbor);
                if (nextHops > farthestHops) {
                    farthestHops = nextHops;
                    farthest = neighbor;
                }
            }
        }
        return Optional.of(farthest);
    }

    public static Optional<RailNodeId> nearestReachableNode(RailGraphView graph, GridPos position) {
        return graph.nodes().stream()
                .min(Comparator.comparingInt(node -> manhattan(node.pos(), position)))
                .map(com.mistbeyond.transport.api.rail.graph.RailNodeView::id);
    }

    private static int manhattan(GridPos first, GridPos second) {
        return Math.abs(first.x() - second.x())
                + Math.abs(first.y() - second.y())
                + Math.abs(first.z() - second.z());
    }
}
