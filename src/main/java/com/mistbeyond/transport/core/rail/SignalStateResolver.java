package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchSnapshot;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentView;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalBoundaryView;
import com.mistbeyond.transport.api.rail.section.SignalState;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.api.rail.section.SignalView;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SignalStateResolver {
    private static final double EPSILON = 1.0E-6;

    private SignalStateResolver() {
    }

    /**
     * Resolves RED/GREEN plus the ERROR health indicator for every signal. A block signal is directional and protects
     * the next section in its facing direction (docs/roadmap/rail/sections.md), so it turns red only when that
     * specific section is reserved or manually claimed; a blocked section behind the signal keeps it green. A
     * misconfigured signal (no track on its facing side, or a path signal whose facing side never reaches a routing
     * node) reports {@code error} and behaves as RED. On bidirectional track signals are needed in both directions.
     */
    public static Set<SignalState> resolve(RailGraphView graph, DispatchSnapshot dispatch) {
        Set<RailSectionId> blocked = new HashSet<>();
        dispatch.reservations().forEach(reservation -> blocked.add(reservation.sectionId()));
        blocked.addAll(dispatch.manualClaims().keySet());

        Map<RailNodeId, RailNodeView> nodesById = indexNodes(graph);
        Map<RailNodeId, Set<RailEdgeView>> incidence = incidence(graph);

        Set<SignalState> result = new HashSet<>();
        for (SignalView signal : graph.signals()) {
            RailNodeView cellNode = graph.nodeAt(signal.cell()).orElse(null);
            boolean error = misconfigured(nodesById, incidence, cellNode, signal);
            boolean red = error || graph.sections().stream()
                    .anyMatch(section -> protects(graph, signal, section) && blocked.contains(section.id()));
            result.add(new SignalState(signal.id(), red ? SignalAspect.RED : SignalAspect.GREEN, error));
        }
        return result;
    }

    /**
     * Whether the signal is misconfigured (ADR 0008, docs/roadmap/rail/sections.md): a signal whose facing side has no
     * track, or a path signal whose facing side never reaches a routing node. A misconfigured signal shows the ERROR
     * indicator and behaves as RED. The signal cell itself is absent when no track exists there at all.
     */
    private static boolean misconfigured(
            Map<RailNodeId, RailNodeView> nodesById,
            Map<RailNodeId, Set<RailEdgeView>> incidence,
            @Nullable RailNodeView cellNode,
            SignalView signal
    ) {
        if (cellNode == null || !hasTrackOnFacingSide(nodesById, incidence, cellNode, signal.direction())) {
            return true;
        }
        return signal.type() == SignalType.PATH
                && !reachesRoutingNode(nodesById, incidence, cellNode, signal.direction());
    }

    private static boolean hasTrackOnFacingSide(
            Map<RailNodeId, RailNodeView> nodesById,
            Map<RailNodeId, Set<RailEdgeView>> incidence,
            RailNodeView node,
            GridDirection facing
    ) {
        return facingEdge(nodesById, incidence, node, facing).isPresent();
    }

    /**
     * Walks the unique approach chain from the signal's cell along its facing side and reports whether the chain
     * eventually reaches a routing node (a same-height junction or crossing: a graph node with degree &gt;= 3). At
     * each degree-2 pass-through cell the single non-backtracking edge is followed, so bends in the chain are
     * traversed; a dead end (degree &lt;= 1) or a loop before any routing node means the path signal's facing side
     * never reaches a routing node and it is misconfigured. A start cell that is itself a junction reaches one
     * immediately.
     */
    private static boolean reachesRoutingNode(
            Map<RailNodeId, RailNodeView> nodesById,
            Map<RailNodeId, Set<RailEdgeView>> incidence,
            RailNodeView start,
            GridDirection facing
    ) {
        if (degree(incidence, start.id()) >= 3) {
            return true;
        }
        Optional<RailEdgeView> first = facingEdge(nodesById, incidence, start, facing);
        if (first.isEmpty()) {
            return false;
        }
        Set<RailNodeId> visited = new HashSet<>();
        visited.add(start.id());
        RailNodeView current = start;
        RailEdgeView edge = first.get();
        while (true) {
            RailNodeView other = otherEndpoint(nodesById, edge, current.id());
            if (other == null || !visited.add(other.id())) {
                return false; // broken link or loop before any routing node.
            }
            int nextDegree = degree(incidence, other.id());
            if (nextDegree >= 3) {
                return true; // reached the guarded junction.
            }
            if (nextDegree <= 1) {
                return false; // dead end before any routing node.
            }
            RailEdgeView incoming = edge;
            RailEdgeView next = null;
            for (RailEdgeView candidate : incidence.getOrDefault(other.id(), Set.of())) {
                if (!candidate.id().equals(incoming.id())) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                return false;
            }
            current = other;
            edge = next;
        }
    }

    /**
     * The edge that connects the signal's cell to its neighbor in the facing direction, if any.
     */
    private static Optional<RailEdgeView> facingEdge(
            Map<RailNodeId, RailNodeView> nodesById,
            Map<RailNodeId, Set<RailEdgeView>> incidence,
            RailNodeView node,
            GridDirection facing
    ) {
        GridPos neighbor = facing.step(node.pos());
        for (RailEdgeView edge : incidence.getOrDefault(node.id(), Set.of())) {
            RailNodeView other = otherEndpoint(nodesById, edge, node.id());
            if (other != null && other.pos().equals(neighbor)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    private static @Nullable RailNodeView otherEndpoint(
            Map<RailNodeId, RailNodeView> nodesById,
            RailEdgeView edge,
            RailNodeId self
    ) {
        if (edge.start().equals(self)) {
            return nodesById.get(edge.end());
        }
        if (edge.end().equals(self)) {
            return nodesById.get(edge.start());
        }
        return null;
    }

    private static int degree(Map<RailNodeId, Set<RailEdgeView>> incidence, RailNodeId id) {
        return incidence.getOrDefault(id, Set.of()).size();
    }

    private static Map<RailNodeId, RailNodeView> indexNodes(RailGraphView graph) {
        Map<RailNodeId, RailNodeView> result = new HashMap<>();
        for (RailNodeView node : graph.nodes()) {
            result.put(node.id(), node);
        }
        return Map.copyOf(result);
    }

    private static Map<RailNodeId, Set<RailEdgeView>> incidence(RailGraphView graph) {
        Map<RailNodeId, Set<RailEdgeView>> result = new HashMap<>();
        for (RailEdgeView edge : graph.edges()) {
            result.computeIfAbsent(edge.start(), ignored -> new HashSet<>()).add(edge);
            result.computeIfAbsent(edge.end(), ignored -> new HashSet<>()).add(edge);
        }
        Map<RailNodeId, Set<RailEdgeView>> frozen = new HashMap<>();
        result.forEach((id, edges) -> frozen.put(id, Set.copyOf(edges)));
        return Map.copyOf(frozen);
    }

    /**
     * Whether the signal's facing direction points into this section, i.e. whether this section is the one the signal
     * protects. For a boundary on edge e at meters m: the section on the side the signal faces (from m towards the
     * edge end when facing along e, from m towards the edge start when facing against e) is protected. A boundary on
     * an edge the signal does not face (for example the crossing edge at a junction) never makes a section protected.
     */
    private static boolean protects(RailGraphView graph, SignalView signal, RailSectionView section) {
        for (SignalBoundaryView boundary : section.boundaries()) {
            if (!boundary.id().equals(signal.id())) {
                continue;
            }
            Optional<RailEdgeView> edge = graph.edgeById(boundary.edgeId());
            if (edge.isEmpty()) {
                continue;
            }
            GridDirection edgeDirection = edge.get().placement().direction();
            boolean facesAlong = edgeDirection == signal.direction();
            boolean facesAgainst = edgeDirection.opposite() == signal.direction();
            if (!facesAlong && !facesAgainst) {
                continue;
            }
            double meters = boundary.metersFromStart();
            for (TrackSegmentId segmentId : section.segments()) {
                Optional<TrackSegmentView> segment = graph.segmentById(segmentId);
                if (segment.isEmpty() || !segment.get().edgeId().equals(boundary.edgeId())) {
                    continue;
                }
                TrackSegmentView view = segment.get();
                if (facesAlong && Math.abs(view.fromMeters() - meters) <= EPSILON) {
                    return true;
                }
                if (facesAgainst && Math.abs(view.toMeters() - meters) <= EPSILON) {
                    return true;
                }
            }
        }
        return false;
    }
}
