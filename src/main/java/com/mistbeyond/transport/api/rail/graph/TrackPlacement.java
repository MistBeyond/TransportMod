package com.mistbeyond.transport.api.rail.graph;

public record TrackPlacement(GridPos originCell, GridDirection direction, TrackType trackType) {

    public TrackPlacement {
        if (trackType == TrackType.STRAIGHT && direction.diagonal()) {
            throw new IllegalArgumentException("Straight track cannot use a diagonal direction: " + direction);
        }
        if (trackType == TrackType.DIAGONAL_45 && !direction.diagonal()) {
            throw new IllegalArgumentException("Diagonal 45 track requires a diagonal direction: " + direction);
        }
    }

    public GridPos start() {
        return originCell;
    }

    public GridPos end() {
        return direction.step(originCell);
    }

    public double lengthMeters() {
        return trackType == TrackType.DIAGONAL_45 ? Math.sqrt(2.0) : 1.0;
    }
}
