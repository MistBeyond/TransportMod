package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.TrackGraphSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RailNetworkService {
    private static final ConcurrentMap<ResourceKey<Level>, RailNetwork> NETWORKS = new ConcurrentHashMap<>();

    private RailNetworkService() {
    }

    public static DispatchService dispatch(ResourceKey<Level> dimension) {
        return network(dimension).dispatch();
    }

    public static RailGraphView graph(ResourceKey<Level> dimension) {
        return network(dimension).graph();
    }

    public static RailGraphView collectGraph(TrackGraphSource source, GridPos start) {
        return RailGraphCollector.collect(source, start);
    }

    public static Optional<RailNodeId> farthestReachableNode(RailGraphView graph, RailNodeId start) {
        boolean startExists = graph.nodes().stream().anyMatch(node -> node.id().equals(start));
        if (!startExists) {
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

    public static void replaceGraph(ResourceKey<Level> dimension, RailGraphView graph) {
        NETWORKS.put(dimension, new RailNetwork(graph, new DispatchServiceImpl(graph, new ShortestPathRouter())));
    }

    private static RailNetwork network(ResourceKey<Level> dimension) {
        return NETWORKS.computeIfAbsent(dimension, ignored -> {
            RailGraph graph = RailGraph.empty();
            return new RailNetwork(graph, new DispatchServiceImpl(graph, new ShortestPathRouter()));
        });
    }

    private record RailNetwork(RailGraphView graph, DispatchService dispatch) {
    }
}
