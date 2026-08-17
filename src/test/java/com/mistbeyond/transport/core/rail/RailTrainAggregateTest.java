package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.RailTrainSnapshot;
import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailTrainAggregateTest {
    @Test
    void manualTrainStartsWithoutSchedule() {
        RailTrainAggregate train = RailTrainAggregate.manual(new RailTrainId("train-1"), new GridPos(1, 2, 3));

        RailTrainSnapshot snapshot = train.snapshot();

        assertEquals(RailControlMode.MANUAL, snapshot.controlMode());
        assertEquals(new GridPos(1, 2, 3), snapshot.position());
        assertFalse(snapshot.derailed());
        assertTrue(snapshot.schedule().isEmpty());
    }

    @Test
    void derailAndResetToggleStateAndReleaseRoute() {
        RailTrainAggregate train = RailTrainAggregate.manual(new RailTrainId("train-1"), new GridPos(0, 0, 0));

        RailTrainAggregate derailed = train.asDerailed();
        RailTrainAggregate reset = derailed.reset();

        assertTrue(derailed.derailed());
        assertTrue(derailed.route().isEmpty());
        assertFalse(reset.derailed());
    }
}
