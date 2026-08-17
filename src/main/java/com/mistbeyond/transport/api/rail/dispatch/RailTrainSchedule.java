package com.mistbeyond.transport.api.rail.dispatch;

import java.util.List;

public record RailTrainSchedule(
        RailTrainScheduleType type,
        List<RailTrainStop> stops
) {
    public RailTrainSchedule {
        stops = List.copyOf(stops);
        if (stops.size() < 2) {
            throw new IllegalArgumentException("A train schedule requires at least two stops");
        }
    }

    public RailTrainStop nextStop(RailTrainStop current) {
        int index = stops.indexOf(current);
        if (index < 0) {
            return stops.getFirst();
        }
        int next = index + 1;
        if (next >= stops.size()) {
            return type == RailTrainScheduleType.LOOP ? stops.getFirst() : stops.getLast();
        }
        return stops.get(next);
    }
}
