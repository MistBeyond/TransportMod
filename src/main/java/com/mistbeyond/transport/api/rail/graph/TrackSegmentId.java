package com.mistbeyond.transport.api.rail.graph;

public record TrackSegmentId(String value) {
    public RailEdgeId edgeId() {
        int hashIndex = value.lastIndexOf('#');
        return new RailEdgeId(hashIndex >= 0 ? value.substring(0, hashIndex) : value);
    }
}
