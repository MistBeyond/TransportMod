package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainScheduleType;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainStop;
import com.mistbeyond.transport.api.rail.station.RailStationId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RailTrainScheduleTest {
    @Test
    void loopScheduleContinuesFromFirstStop() {
        RailTrainStop first = new RailTrainStop(new RailStationId("a"), 0);
        RailTrainStop second = new RailTrainStop(new RailStationId("b"), 0);
        RailTrainSchedule schedule = new RailTrainSchedule(
                RailTrainScheduleType.LOOP,
                List.of(first, second)
        );

        assertEquals(second, schedule.nextStop(first));
        assertEquals(first, schedule.nextStop(second));
    }

    @Test
    void oneWayScheduleStopsAtLastStop() {
        RailTrainStop first = new RailTrainStop(new RailStationId("a"), 0);
        RailTrainStop second = new RailTrainStop(new RailStationId("b"), 0);
        RailTrainSchedule schedule = new RailTrainSchedule(
                RailTrainScheduleType.ONE_WAY,
                List.of(first, second)
        );

        assertEquals(second, schedule.nextStop(first));
        assertEquals(second, schedule.nextStop(second));
    }

    @Test
    void scheduleRejectsSingleStop() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RailTrainSchedule(
                        RailTrainScheduleType.ONE_WAY,
                        List.of(new RailTrainStop(new RailStationId("a"), 0))
                )
        );
    }
}
