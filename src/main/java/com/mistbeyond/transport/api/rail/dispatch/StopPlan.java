package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.graph.RailNodeId;

public record StopPlan(RailNodeId station, int dwellTicks) {
    public StopPlan {
        dwellTicks = Math.max(0, dwellTicks);
    }
}
