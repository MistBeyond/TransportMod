package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentView;

public record TrackSegment(TrackSegmentId id, RailEdgeId edgeId, double fromMeters, double toMeters)
        implements TrackSegmentView {
}
