package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.station.RailStationId;

public record RailTrainStop(
        RailStationId station,
        int dwellTicks
) {
    public RailTrainStop {
        dwellTicks = Math.max(0, dwellTicks);
    }
}
