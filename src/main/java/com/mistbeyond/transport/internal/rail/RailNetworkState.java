package com.mistbeyond.transport.internal.rail;

import com.mistbeyond.transport.api.rail.RailTrainSnapshot;

import java.util.List;

public record RailNetworkState(
        List<RailTrainSnapshot> trains
) {
    public static final RailNetworkState EMPTY = new RailNetworkState(List.of());

    public RailNetworkState {
        trains = List.copyOf(trains);
    }
}
