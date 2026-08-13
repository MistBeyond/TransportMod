package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.block.rail.TestTrackBlock.TrackAxis;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTrackAxisTest {
    @Test
    void northSouthAxisEmitsBothCardinalDirections() {
        assertEquals(
                Set.of(GridDirection.NORTH, GridDirection.SOUTH),
                TestTrackBlock.placementDirectionsForAxis(TrackAxis.NORTH_SOUTH)
        );
    }

    @Test
    void eastWestAxisEmitsBothCardinalDirections() {
        assertEquals(
                Set.of(GridDirection.EAST, GridDirection.WEST),
                TestTrackBlock.placementDirectionsForAxis(TrackAxis.EAST_WEST)
        );
    }

}
