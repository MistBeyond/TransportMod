package com.mistbeyond.transport.api.rail.graph;

public enum TraversalDirection {
    FORWARD,
    REVERSE;

    public TraversalDirection opposite() {
        return this == FORWARD ? REVERSE : FORWARD;
    }
}
