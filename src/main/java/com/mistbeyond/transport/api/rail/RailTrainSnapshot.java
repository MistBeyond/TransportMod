package com.mistbeyond.transport.api.rail;

import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.graph.GridPos;

import java.util.Optional;

public record RailTrainSnapshot(
        RailTrainId id,
        RailControlMode controlMode,
        GridPos position,
        boolean derailed,
        Optional<RailTrainSchedule> schedule
) {
}
