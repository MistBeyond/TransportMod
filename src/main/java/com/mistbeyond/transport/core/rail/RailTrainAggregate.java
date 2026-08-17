package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.RailTrainSnapshot;
import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailRoute;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.graph.GridPos;

import java.util.Optional;

public record RailTrainAggregate(
        RailTrainId id,
        RailControlMode controlMode,
        GridPos position,
        boolean derailed,
        Optional<RailTrainSchedule> schedule,
        Optional<RailRoute> route,
        int currentStopIndex
) {
    public RailTrainAggregate {
        currentStopIndex = Math.max(0, currentStopIndex);
    }

    public static RailTrainAggregate manual(RailTrainId id, GridPos position) {
        return new RailTrainAggregate(id, RailControlMode.MANUAL, position, false,
                Optional.empty(), Optional.empty(), 0);
    }

    public RailTrainAggregate withPosition(GridPos position) {
        return new RailTrainAggregate(id, controlMode, position, derailed, schedule, route, currentStopIndex);
    }

    public RailTrainAggregate withControlMode(RailControlMode controlMode) {
        return new RailTrainAggregate(id, controlMode, position, derailed, schedule, route, currentStopIndex);
    }

    public RailTrainAggregate withSchedule(
            RailTrainSchedule schedule,
            RailRoute route,
            int currentStopIndex
    ) {
        return new RailTrainAggregate(
                id,
                RailControlMode.AUTOMATIC,
                position,
                derailed,
                Optional.of(schedule),
                Optional.of(route),
                currentStopIndex
        );
    }

    public RailTrainAggregate asDerailed() {
        return new RailTrainAggregate(id, controlMode, position, true, schedule, Optional.empty(), currentStopIndex);
    }

    public RailTrainAggregate reset() {
        return new RailTrainAggregate(id, controlMode, position, false, schedule, Optional.empty(), currentStopIndex);
    }

    public RailTrainSnapshot snapshot() {
        return new RailTrainSnapshot(id, controlMode, position, derailed, schedule);
    }
}
