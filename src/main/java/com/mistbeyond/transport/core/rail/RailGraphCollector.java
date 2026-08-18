package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackGraphSource;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RailGraphCollector {
    private RailGraphCollector() {
    }

    public static RailGraph collect(TrackGraphSource source, GridPos start) {
        Map<GridPos, RailNode> nodes = new HashMap<>();
        Set<RailEdge> edges = new HashSet<>();
        Set<String> edgeKeys = new HashSet<>();
        Set<Signal> signals = new HashSet<>();
        Set<GridPos> visited = new HashSet<>();
        ArrayDeque<GridPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            GridPos pos = queue.removeFirst();
            if (!visited.add(pos) || source.placementsAt(pos).isEmpty()) {
                continue;
            }
            source.signalAt(pos).ifPresent(placement -> signals.add(signalFor(pos, placement)));
            RailNode node = nodeFor(nodes, pos);
            for (TrackPlacement placement : source.placementsAt(pos)) {
                if (!pos.equals(placement.start())) {
                    continue;
                }
                GridPos neighbor = placement.end();
                if (!hasReversePlacement(source, neighbor, pos)) {
                    continue;
                }
                addEdge(nodes, edges, edgeKeys, queue, node, neighbor, placement);
            }
        }
        return new RailGraph(new HashSet<>(nodes.values()), edges, signals);
    }

    private static Signal signalFor(GridPos pos, SignalPlacement placement) {
        return new Signal(placement.id(), pos, placement.direction(), placement.type());
    }

    private static boolean hasReversePlacement(TrackGraphSource source, GridPos from, GridPos to) {
        return source.placementsAt(from).stream()
                .anyMatch(placement -> from.equals(placement.start()) && to.equals(placement.end()));
    }

    private static void addEdge(
            Map<GridPos, RailNode> nodes,
            Set<RailEdge> edges,
            Set<String> edgeKeys,
            ArrayDeque<GridPos> queue,
            RailNode startNode,
            GridPos endPos,
            TrackPlacement placement
    ) {
        RailNode endNode = nodeFor(nodes, endPos);
        String forwardKey = startNode.id().value() + "->" + endNode.id().value();
        String reverseKey = endNode.id().value() + "->" + startNode.id().value();
        if (edgeKeys.add(forwardKey) && !edgeKeys.contains(reverseKey)) {
            edges.add(new RailEdge(
                    new RailEdgeId("edge-" + forwardKey),
                    startNode.id(),
                    endNode.id(),
                    placement,
                    placement.lengthMeters()
            ));
        }
        queue.add(startNode.pos());
        queue.add(endPos);
    }

    private static RailNode nodeFor(Map<GridPos, RailNode> nodes, GridPos pos) {
        return nodes.computeIfAbsent(
                pos,
                ignored -> new RailNode(
                        new RailNodeId("node-" + pos.x() + "-" + pos.y() + "-" + pos.z()),
                        pos
                )
        );
    }
}
