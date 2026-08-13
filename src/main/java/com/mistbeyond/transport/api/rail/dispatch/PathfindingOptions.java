package com.mistbeyond.transport.api.rail.dispatch;

public record PathfindingOptions(double stationPenalty) {
    public static final PathfindingOptions DEFAULT = new PathfindingOptions(50.0);

    public PathfindingOptions {
        stationPenalty = Math.max(0.0, stationPenalty);
    }
}
