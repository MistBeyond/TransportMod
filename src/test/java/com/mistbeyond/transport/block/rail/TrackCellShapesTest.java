package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackCellShapesTest {
    private static final GridPos CELL = new GridPos(3, 0, 5);

    @Test
    void cardinalStripSpansGaugeBandAndOverflowsNeighbors() {
        VoxelShape shape = TrackCellShapes.placementShape(placement(GridDirection.EAST, TrackType.STRAIGHT));

        // 16 px = 1.0 block; VoxelShape bounds are in block units.
        assertEquals(0.0, shape.min(Direction.Axis.X), 1.0E-6);
        assertEquals(1.0, shape.max(Direction.Axis.X), 1.0E-6);
        assertEquals(0.0, shape.min(Direction.Axis.Y), 1.0E-6);
        assertEquals(0.125, shape.max(Direction.Axis.Y), 1.0E-6);
        // Not clipped to the owning block: half strip width 13 px either side of z = 8 (8 px = 0.5 block).
        assertEquals(-0.3125, shape.min(Direction.Axis.Z), 1.0E-6);
        assertEquals(1.3125, shape.max(Direction.Axis.Z), 1.0E-6);
    }

    @Test
    void diagonalStripsFollowTheStripRuleAndOverflowNeighbors() {
        // The diagonal 45 collision is the same 26px gauge-wide band as the straight strip, rotated to the diagonal
        // axis and approximated with axis-aligned boxes (tracks.md): a corridor along the track axis that covers the
        // full cell and overflows into the neighbors on the strip-width sides. It must not collapse to a cell-clipped
        // 16x16 box, and it must not read as a hexagon cut from a square window.
        for (GridDirection direction : new GridDirection[]{
                GridDirection.NORTH_WEST, GridDirection.SOUTH_EAST,
                GridDirection.NORTH_EAST, GridDirection.SOUTH_WEST
        }) {
            VoxelShape shape = TrackCellShapes.placementShape(placement(direction, TrackType.DIAGONAL_45));

            // The corridor covers the whole cell in both horizontal axes (the diagonal band is wider than the cell).
            assertTrue(shape.min(Direction.Axis.X) <= 0.0, "cell must be covered in x");
            assertTrue(shape.max(Direction.Axis.X) >= 1.0, "cell must be covered in x");
            assertTrue(shape.min(Direction.Axis.Z) <= 0.0, "cell must be covered in z");
            assertTrue(shape.max(Direction.Axis.Z) >= 1.0, "cell must be covered in z");
            // The strip-width overflow reaches into the neighbors (not clipped to the cell bounds).
            assertTrue(shape.min(Direction.Axis.X) < 0.0 || shape.min(Direction.Axis.Z) < 0.0,
                    "must overflow into a neighbor");
            assertTrue(shape.max(Direction.Axis.X) > 1.0 || shape.max(Direction.Axis.Z) > 1.0,
                    "must overflow into a neighbor");
            assertEquals(0.0, shape.min(Direction.Axis.Y), 1.0E-6);
            assertEquals(0.125, shape.max(Direction.Axis.Y), 1.0E-6);
        }
    }

    @Test
    void crossingCellUnionCoversBothAxesAndOverflows() {
        VoxelShape shape = TrackCellShapes.cellShape(Set.of(
                placement(GridDirection.EAST, TrackType.STRAIGHT),
                placement(GridDirection.NORTH, TrackType.STRAIGHT)
        ));

        // Union spans the gauge band of both strips, overflowing the owning block on both axes.
        assertEquals(-0.3125, shape.min(Direction.Axis.X), 1.0E-6);
        assertEquals(1.3125, shape.max(Direction.Axis.X), 1.0E-6);
        assertEquals(-0.3125, shape.min(Direction.Axis.Z), 1.0E-6);
        assertEquals(1.3125, shape.max(Direction.Axis.Z), 1.0E-6);
        assertEquals(0.0, shape.min(Direction.Axis.Y), 1.0E-6);
        assertEquals(0.125, shape.max(Direction.Axis.Y), 1.0E-6);
    }

    @Test
    void curvePlacementFallsBackToFullCellBox() {
        VoxelShape shape = TrackCellShapes.placementShape(placement(GridDirection.EAST, TrackType.CURVE));

        assertEquals(0.0, shape.min(Direction.Axis.X), 1.0E-6);
        assertEquals(1.0, shape.max(Direction.Axis.X), 1.0E-6);
        assertEquals(0.0, shape.min(Direction.Axis.Z), 1.0E-6);
        assertEquals(1.0, shape.max(Direction.Axis.Z), 1.0E-6);
        assertEquals(0.125, shape.max(Direction.Axis.Y), 1.0E-6);
    }

    @Test
    void emptyCellHasEmptyShape() {
        assertTrue(TrackCellShapes.cellShape(Set.of()).isEmpty());
    }

    @Test
    void boundingCellShapeIsASingleBoxEnclosingTheStrips() {
        // The aiming/terrain (getShape) shape must be one box (so vanilla break particles stay at normal quantity)
        // that still encloses the collision strips; the exact staircase remains for collision.
        Set<TrackPlacement> placements = Set.of(placement(GridDirection.NORTH_WEST, TrackType.DIAGONAL_45));
        VoxelShape bounding = TrackCellShapes.cellBoundingShape(placements);
        VoxelShape collision = TrackCellShapes.cellShape(placements);

        assertEquals(bounding.min(Direction.Axis.X), collision.min(Direction.Axis.X), 1.0E-6);
        assertEquals(bounding.max(Direction.Axis.X), collision.max(Direction.Axis.X), 1.0E-6);
        assertEquals(bounding.min(Direction.Axis.Z), collision.min(Direction.Axis.Z), 1.0E-6);
        assertEquals(bounding.max(Direction.Axis.Z), collision.max(Direction.Axis.Z), 1.0E-6);
        assertEquals(0.125, bounding.max(Direction.Axis.Y), 1.0E-6);
    }

    @Test
    void emptyBoundingCellIsEmpty() {
        assertTrue(TrackCellShapes.cellBoundingShape(Set.of()).isEmpty());
    }

    private static TrackPlacement placement(GridDirection direction, TrackType type) {
        return new TrackPlacement(CELL, direction, type);
    }
}