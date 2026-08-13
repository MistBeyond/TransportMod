package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class RailSectionBuilder {
    private static final double EPSILON = 1.0E-6;

    private RailSectionBuilder() {
    }

    public static SectionBuildResult buildResult(
            Set<RailEdge> edges,
            Set<Signal> signals
    ) {
        Map<RailEdgeId, RailEdge> edgesById = indexEdges(edges);
        Map<RailEdgeId, List<SignalBoundary>> boundariesByEdge = boundariesByEdge(signals, edges);
        Map<TrackSegmentId, TrackSegment> segmentsById = new HashMap<>();
        Map<EndpointKey, Set<TrackSegmentId>> adjacency = new HashMap<>();

        List<RailEdgeId> sortedEdgeIds = edges.stream()
                .map(RailEdge::id)
                .sorted(Comparator.comparing(RailEdgeId::value))
                .toList();
        for (RailEdgeId edgeId : sortedEdgeIds) {
            RailEdge edge = edgesById.get(edgeId);
            List<SignalBoundary> boundaries = boundariesByEdge.getOrDefault(edgeId, List.of());
            List<Double> splits = boundaries.stream()
                    .map(SignalBoundary::metersFromStart)
                    .distinct()
                    .sorted()
                    .toList();

            double from = 0.0;
            for (int i = 0; i <= splits.size(); i++) {
                double to = i < splits.size() ? splits.get(i) : edge.lengthMeters();
                if (to - from <= EPSILON) {
                    from = to;
                    continue;
                }
                TrackSegmentId segmentId = new TrackSegmentId(edgeId.value() + "#" + i);
                TrackSegment segment = new TrackSegment(segmentId, edgeId, from, to);
                segmentsById.put(segmentId, segment);

                List<SignalBoundary> edgeBoundaries = boundariesByEdge.getOrDefault(edgeId, List.of());
                addEndpoint(adjacency, startKey(segment, edge, edgeBoundaries), segmentId);
                addEndpoint(adjacency, endKey(segment, edge, edgeBoundaries), segmentId);
                from = to;
            }
        }

        List<Set<TrackSegmentId>> components = connectedComponents(segmentsById, adjacency);
        Set<RailSection> sections = new HashSet<>();
        int index = 0;
        for (Set<TrackSegmentId> component : components) {
            Set<SignalBoundary> sectionBoundaries = new HashSet<>();
            for (TrackSegmentId segmentId : component) {
                TrackSegment segment = segmentsById.get(segmentId);
                for (SignalBoundary boundary : boundariesByEdge.getOrDefault(segment.edgeId(), List.of())) {
                    if (Math.abs(boundary.metersFromStart() - segment.fromMeters()) <= EPSILON
                            || Math.abs(boundary.metersFromStart() - segment.toMeters()) <= EPSILON) {
                        sectionBoundaries.add(boundary);
                    }
                }
            }
            sections.add(new RailSection(
                    new RailSectionId("section-" + index),
                    component,
                    sectionBoundaries
            ));
            index++;
        }
        return new SectionBuildResult(sections, Map.copyOf(segmentsById));
    }

    private static Map<RailEdgeId, RailEdge> indexEdges(Set<RailEdge> edges) {
        Map<RailEdgeId, RailEdge> result = new HashMap<>();
        for (RailEdge edge : edges) {
            if (result.put(edge.id(), edge) != null) {
                throw new IllegalArgumentException("Duplicate rail edge id: " + edge.id());
            }
        }
        return result;
    }

    private static Map<RailEdgeId, List<SignalBoundary>> boundariesByEdge(
            Set<Signal> signals,
            Set<RailEdge> edges
    ) {
        Map<RailEdgeId, List<SignalBoundary>> result = new HashMap<>();
        for (Signal signal : signals) {
            for (RailEdge edge : edges) {
                double meters = positionOnEdge(signal, edge);
                if (meters >= 0.0) {
                    TraversalDirection direction = meters <= EPSILON
                            ? TraversalDirection.FORWARD
                            : TraversalDirection.REVERSE;
                    result.computeIfAbsent(edge.id(), ignored -> new ArrayList<>())
                            .add(new SignalBoundary(signal.id(), edge.id(), meters, direction));
                }
            }
        }
        result.values().forEach(list -> list.sort(Comparator.comparingDouble(SignalBoundary::metersFromStart)));
        return result;
    }

    private static double positionOnEdge(Signal signal, RailEdge edge) {
        GridPos start = edge.placement().start();
        GridPos end = edge.placement().end();
        int dx = end.x() - start.x();
        int dz = end.z() - start.z();
        if (dx == 0 && dz == 0) {
            return -1.0;
        }
        int sx = signal.cell().x() - start.x();
        int sz = signal.cell().z() - start.z();
        double t;
        if (dx != 0) {
            t = (double) sx / dx;
        } else {
            t = (double) sz / dz;
        }
        if (t < 0.0 || t > 1.0) {
            return -1.0;
        }
        double expectedX = t * dx;
        double expectedZ = t * dz;
        if (Math.abs(sx - expectedX) > 1.0E-6 || Math.abs(sz - expectedZ) > 1.0E-6) {
            return -1.0;
        }
        return t * edge.lengthMeters();
    }

    private static void addEndpoint(
            Map<EndpointKey, Set<TrackSegmentId>> adjacency,
            @Nullable
            EndpointKey key,
            TrackSegmentId segmentId
    ) {
        if (key != null) {
            adjacency.computeIfAbsent(key, ignored -> new HashSet<>()).add(segmentId);
        }
    }

    private static @Nullable EndpointKey startKey(
            TrackSegment segment,
            RailEdge edge,
            List<SignalBoundary> boundaries
    ) {
        if (segment.fromMeters() <= EPSILON && isOpenAt(boundaries, 0.0)) {
            return EndpointKey.node(edge.start());
        }
        return null;
    }

    private static @Nullable EndpointKey endKey(
            TrackSegment segment,
            RailEdge edge,
            List<SignalBoundary> boundaries
    ) {
        if (segment.toMeters() >= edge.lengthMeters() - EPSILON
                && isOpenAt(boundaries, edge.lengthMeters())) {
            return EndpointKey.node(edge.end());
        }
        return null;
    }

    private static boolean isOpenAt(List<SignalBoundary> boundaries, double meters) {
        return boundaries.stream()
                .noneMatch(boundary -> Math.abs(boundary.metersFromStart() - meters) <= EPSILON);
    }

    private static List<Set<TrackSegmentId>> connectedComponents(
            Map<TrackSegmentId, TrackSegment> segmentsById,
            Map<EndpointKey, Set<TrackSegmentId>> adjacency
    ) {
        Set<TrackSegmentId> remaining = new TreeSet<>(Comparator.comparing(TrackSegmentId::value));
        remaining.addAll(segmentsById.keySet());

        List<Set<TrackSegmentId>> components = new ArrayList<>();
        while (!remaining.isEmpty()) {
            TrackSegmentId first = remaining.iterator().next();
            Set<TrackSegmentId> component = new HashSet<>();
            ArrayDeque<TrackSegmentId> queue = new ArrayDeque<>();
            queue.add(first);
            remaining.remove(first);
            while (!queue.isEmpty()) {
                TrackSegmentId current = queue.removeFirst();
                if (!component.add(current)) {
                    continue;
                }
                for (EndpointKey key : keysOf(current, adjacency)) {
                    for (TrackSegmentId neighbor : adjacency.getOrDefault(key, Set.of())) {
                        if (remaining.remove(neighbor)) {
                            queue.addLast(neighbor);
                        }
                    }
                }
            }
            components.add(component);
        }
        components.sort(Comparator.comparing(component -> component.stream()
                .map(TrackSegmentId::value)
                .min(String::compareTo)
                .orElse("")));
        return components;
    }

    private static List<EndpointKey> keysOf(TrackSegmentId id, Map<EndpointKey, Set<TrackSegmentId>> adjacency) {
        List<EndpointKey> keys = new ArrayList<>();
        adjacency.forEach((key, segmentIds) -> {
            if (segmentIds.contains(id)) {
                keys.add(key);
            }
        });
        return keys;
    }

    private record EndpointKey(String value) {
        static EndpointKey node(com.mistbeyond.transport.api.rail.graph.RailNodeId nodeId) {
            return new EndpointKey("node:" + nodeId.value());
        }
    }

    public record SectionBuildResult(
            Set<RailSection> sections,
            Map<TrackSegmentId, TrackSegment> segmentsById
    ) {
    }
}
