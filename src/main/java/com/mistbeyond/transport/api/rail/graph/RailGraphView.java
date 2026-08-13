package com.mistbeyond.transport.api.rail.graph;

import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalView;

import java.util.Optional;
import java.util.Set;

public interface RailGraphView {
    Set<RailNodeView> nodes();

    Set<RailEdgeView> edges();

    Set<SignalView> signals();

    Set<RailSectionView> sections();

    Optional<RailNodeView> nodeAt(GridPos pos);

    Optional<RailEdgeView> edgeById(RailEdgeId id);

    Optional<RailSectionView> sectionAt(TrackPosition position);

    default Optional<TrackSegmentView> segmentById(TrackSegmentId id) {
        return Optional.empty();
    }
}
