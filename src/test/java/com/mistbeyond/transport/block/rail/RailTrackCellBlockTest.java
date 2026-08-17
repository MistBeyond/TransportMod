package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.TrackAxis;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailTrackCellBlockTest {
    @Test
    void straightCellEmitsBidirectionalPlacements() {
        GridPos cell = new GridPos(3, 0, 5);
        Set<TrackPlacement> placements = RailTrackCellBlock.placementsFor(cell, TrackAxis.E_W);

        assertEquals(2, placements.size());
        assertTrue(placements.contains(new TrackPlacement(cell, GridDirection.EAST, TrackType.STRAIGHT)));
        assertTrue(placements.contains(new TrackPlacement(cell, GridDirection.WEST, TrackType.STRAIGHT)));
    }

    @Test
    void diagonalCellEmitsBidirectionalPlacements() {
        GridPos cell = new GridPos(3, 0, 5);
        Set<TrackPlacement> placements = RailTrackCellBlock.placementsFor(cell, TrackAxis.NE_SW);

        assertEquals(2, placements.size());
        assertTrue(placements.contains(new TrackPlacement(cell, GridDirection.NORTH_EAST, TrackType.DIAGONAL_45)));
        assertTrue(placements.contains(new TrackPlacement(cell, GridDirection.SOUTH_WEST, TrackType.DIAGONAL_45)));
    }

    @Test
    void axisCollapsesOppositeDirectionsAndExpandsBack() {
        for (GridDirection direction : GridDirection.values()) {
            TrackAxis axis = TrackAxis.from(direction);

            assertEquals(2, axis.directions().size());
            assertTrue(axis.directions().contains(direction));
            assertTrue(axis.directions().contains(direction.opposite()));
        }
    }

    @Test
    void yawMapsToAxisIn45DegreeSteps() {
        assertEquals(TrackAxis.N_S, RailTrackCellBlock.axisFromYaw(0.0F));
        assertEquals(TrackAxis.N_S, RailTrackCellBlock.axisFromYaw(180.0F));
        assertEquals(TrackAxis.E_W, RailTrackCellBlock.axisFromYaw(90.0F));
        assertEquals(TrackAxis.E_W, RailTrackCellBlock.axisFromYaw(-90.0F));
        assertEquals(TrackAxis.NE_SW, RailTrackCellBlock.axisFromYaw(45.0F));
        assertEquals(TrackAxis.NE_SW, RailTrackCellBlock.axisFromYaw(-135.0F));
        assertEquals(TrackAxis.NW_SE, RailTrackCellBlock.axisFromYaw(135.0F));
        assertEquals(TrackAxis.NW_SE, RailTrackCellBlock.axisFromYaw(-45.0F));
    }
}
