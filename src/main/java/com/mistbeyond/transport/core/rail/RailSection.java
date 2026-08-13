package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;

import java.util.Set;

public record RailSection(
        RailSectionId id,
        Set<TrackSegmentId> segments,
        Set<SignalBoundary> boundaries
) implements RailSectionView {
    public RailSection {
        segments = Set.copyOf(segments);
        boundaries = Set.copyOf(boundaries);
    }

}
