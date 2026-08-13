package com.mistbeyond.transport.api.rail.graph;

public interface TrackSegmentView {
    TrackSegmentId id();

    RailEdgeId edgeId();

    double fromMeters();

    double toMeters();
}
