package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;
import com.mistbeyond.transport.api.rail.graph.TrackPosition;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentView;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RailGraph implements RailGraphView {
    private static final double EPSILON = 1.0E-6;

    private final Set<RailNode> nodes;
    private final Set<RailEdge> edges;
    private final Set<Signal> signals;
    private final Set<RailSection> sections;
    private final Map<GridPos, RailNode> nodesByPos;
    private final Map<RailEdgeId, RailEdge> edgesById;
    private final Map<TrackSegmentId, TrackSegment> segmentsById;

    public RailGraph(Set<RailNode> nodes, Set<RailEdge> edges, Set<Signal> signals) {
        this.nodes = Set.copyOf(nodes);
        this.edges = Set.copyOf(edges);
        this.signals = Set.copyOf(signals);
        RailSectionBuilder.SectionBuildResult built =
                RailSectionBuilder.buildResult(this.edges, this.signals);
        Map<RailNodeId, RailNode> nodesById = new HashMap<>();
        this.nodesByPos = new HashMap<>();
        for (RailNode node : this.nodes) {
            nodesById.put(node.id(), node);
            nodesByPos.put(node.pos(), node);
        }
        this.edgesById = new HashMap<>();
        for (RailEdge edge : this.edges) {
            if (!nodesById.containsKey(edge.start()) || !nodesById.containsKey(edge.end())) {
                throw new IllegalArgumentException("Rail edge references unknown node: " + edge.id());
            }
            edgesById.put(edge.id(), edge);
        }
        this.segmentsById = built.segmentsById();
        this.sections = built.sections();
    }

    public static RailGraph empty() {
        return new RailGraph(Set.of(), Set.of(), Set.of());
    }

    @Override
    public Set<RailNodeView> nodes() {
        return Set.copyOf(nodes);
    }

    @Override
    public Set<RailEdgeView> edges() {
        return Set.copyOf(edges);
    }

    @Override
    public Set<SignalView> signals() {
        return Set.copyOf(signals);
    }

    @Override
    public Set<RailSectionView> sections() {
        return Set.copyOf(sections);
    }

    @Override
    public Optional<RailNodeView> nodeAt(GridPos pos) {
        return Optional.<RailNodeView>ofNullable(nodesByPos.get(pos));
    }

    @Override
    public Optional<RailEdgeView> edgeById(RailEdgeId id) {
        return Optional.<RailEdgeView>ofNullable(edgesById.get(id));
    }

    @Override
    public Optional<RailSectionView> sectionAt(TrackPosition position) {
        return sections.stream()
                .filter(section -> section.segments().stream().anyMatch(segmentId -> {
                    TrackSegment segment = segmentsById.get(segmentId);
                    return segment != null
                            && segment.edgeId().equals(position.edgeId())
                            && position.metersFromStart() >= segment.fromMeters() - EPSILON
                            && position.metersFromStart() <= segment.toMeters() + EPSILON;
                }))
                .map(RailSectionView.class::cast)
                .findFirst();
    }

    @Override
    public Optional<TrackSegmentView> segmentById(TrackSegmentId id) {
        return Optional.<TrackSegmentView>ofNullable(segmentsById.get(id));
    }

}
