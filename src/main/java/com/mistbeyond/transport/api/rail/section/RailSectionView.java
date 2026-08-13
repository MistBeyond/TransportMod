package com.mistbeyond.transport.api.rail.section;

import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;

import java.util.Set;

public interface RailSectionView {
    RailSectionId id();

    Set<TrackSegmentId> segments();

    Set<? extends SignalBoundaryView> boundaries();
}
