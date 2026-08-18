package com.mistbeyond.transport.client.rail.model;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.TrackAxis;
import com.mistbeyond.transport.api.rail.graph.TrackCellDataRecord;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackCellGeometryTest {
    private static final GridPos CELL = new GridPos(3, 0, 5);
    private static final float EPSILON = 1.0E-3F;

    @Test
    void straightAxisUsesEightElementsWithExpectedAngles() {
        List<TrackCellGeometry.Element> elements = TrackCellGeometry.elementsForAxis(TrackAxis.N_S);

        assertEquals(8, elements.size());
        assertTrue(elements.stream().allMatch(e -> e.rotationDegrees() == 0.0F));
    }

    @Test
    void eastWestStraightIsRotatedNinetyDegrees() {
        List<TrackCellGeometry.Element> elements = TrackCellGeometry.elementsForAxis(TrackAxis.E_W);

        assertTrue(elements.stream().allMatch(e -> e.rotationDegrees() == 90.0F));
        // Rails sit at rail center distance 24 px either side of the model center, symmetric about x = 8.
        long leftRails = elements.stream()
                .filter(e -> e.texture().equals("rail") && e.toX() <= 8.0F)
                .count();
        long rightRails = elements.stream()
                .filter(e -> e.texture().equals("rail") && e.fromX() >= 8.0F)
                .count();
        assertEquals(3, leftRails);
        assertEquals(3, rightRails);
    }

    @Test
    void straightSleepersAreSpacedEightPixels() {
        List<TrackCellGeometry.Element> sleepers = TrackCellGeometry.elementsForAxis(TrackAxis.N_S).stream()
                .filter(e -> e.texture().equals("sleeper"))
                .toList();

        assertEquals(2, sleepers.size());
        for (TrackCellGeometry.Element sleeper : sleepers) {
            assertEquals(RailGeometryParams.SLEEPER_W, sleeperWidth(sleeper), EPSILON);
            float center = zCenter(sleeper);
            assertTrue(center == 4.0F || center == 12.0F, "unexpected sleeper center " + center);
        }
    }

    @Test
    void diagonalSleeperRhythmMatchesGlobals() {
        List<Float> centers = TrackCellGeometry.elementsForAxis(TrackAxis.NW_SE).stream()
                .filter(e -> e.texture().equals("sleeper"))
                .map(TrackCellGeometryTest::zCenter)
                .toList();

        assertEquals(3, centers.size());
        // Centers 8 - 16*sqrt(2)/3, 8 and 8 + 16*sqrt(2)/3 keep the rhythm uniform across cells.
        assertTrue(centers.contains(8.0F));
        assertTrue(centers.contains(8.0F - RailGeometryParams.SLEEPER_SPACING));
        assertTrue(centers.contains(8.0F + RailGeometryParams.SLEEPER_SPACING));
    }

    @Test
    void diagonalRailsSeamAtTheCellCorners() {
        // NW_SE rotates the local Z-aligned rail strip by +45 degrees about the cell center; the rail end faces then
        // land exactly on the cell corners, so adjacent cells' rails meet back-to-back (no seam, no z-fighting).
        TrackCellGeometry.Element web = TrackCellGeometry.elementsForAxis(TrackAxis.NW_SE).stream()
                .filter(e -> e.texture().equals("rail") && e.fromX() <= -4.0F)
                .findFirst()
                .orElseThrow();
        assertEquals(-5.0F, web.fromX(), EPSILON);
        assertEquals(-3.0F, web.toX(), EPSILON);
        // Half-length 8*sqrt(2): 8 - 11.3137 = -3.3137, 8 + 11.3137 = 19.3137.
        assertEquals(8.0F - RailGeometryParams.TRACK_HALF_LENGTH, web.fromZ(), EPSILON);
        assertEquals(8.0F + RailGeometryParams.TRACK_HALF_LENGTH, web.toZ(), EPSILON);
    }

    @Test
    void oppositePlacementsCollapseToOneGeometrySet() {
        TrackCellDataRecord data = new TrackCellDataRecord(
                CELL,
                Set.of(
                        new TrackPlacement(CELL, GridDirection.EAST, TrackType.STRAIGHT),
                        new TrackPlacement(CELL, GridDirection.WEST, TrackType.STRAIGHT)
                ),
                Optional.empty()
        );

        List<TrackCellGeometry.Element> elements = TrackCellGeometry.elementsFor(data);

        assertEquals(8, elements.size());
    }

    @Test
    void crossingUnionsBothAxes() {
        TrackCellDataRecord data = new TrackCellDataRecord(
                CELL,
                Set.of(
                        new TrackPlacement(CELL, GridDirection.EAST, TrackType.STRAIGHT),
                        new TrackPlacement(CELL, GridDirection.NORTH, TrackType.STRAIGHT)
                ),
                Optional.empty()
        );

        List<TrackCellGeometry.Element> elements = TrackCellGeometry.elementsFor(data);

        assertEquals(16, elements.size());
        assertTrue(elements.stream().anyMatch(e -> e.rotationDegrees() == 0.0F));
        assertTrue(elements.stream().anyMatch(e -> e.rotationDegrees() == 90.0F));
    }

    @Test
    void diagonalAnglesMatchTheDatagenBlockstate() {
        assertEquals(45.0F, TrackCellGeometry.angleFor(TrackAxis.NW_SE));
        assertEquals(135.0F, TrackCellGeometry.angleFor(TrackAxis.NE_SW));
        assertEquals(0.0F, TrackCellGeometry.angleFor(TrackAxis.N_S));
        assertEquals(90.0F, TrackCellGeometry.angleFor(TrackAxis.E_W));
    }

    @Test
    void sleeperUvsMatchTheStraightModelForEveryOrientation() {
        // Sleepers must use the single 16px-period convention of the hand-made straight model on every face for
        // every rotation: the diagonal sleepers render with the same plank density as the straight ones instead of
        // sampling two texture periods (the box-extent UVs would).
        for (TrackAxis axis : TrackAxis.values()) {
            List<TrackCellGeometry.Element> elements = TrackCellGeometry.elementsForAxis(axis);
            for (TrackCellGeometry.Element sleeper : elements.stream()
                    .filter(e -> e.texture().equals("sleeper"))
                    .toList()) {
                assertSleeperUvs(sleeper);
            }
        }
    }

    private static void assertSleeperUvs(TrackCellGeometry.Element sleeper) {
        for (Direction face : Direction.values()) {
            CuboidFace.UVs uvs = TrackCellDynamicModel.faceUvs(sleeper, face);
            switch (face.getAxis()) {
                case X -> assertEquals(
                        new CuboidFace.UVs(0.0F, 1.5F, 3.0F, 2.0F), uvs,
                        "end face of sleeper must use straight-model UVs"
                );
                case Y -> assertEquals(
                        new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 3.0F), uvs,
                        "top/bottom of sleeper must use a single 16px period"
                );
                case Z -> assertEquals(
                        new CuboidFace.UVs(0.0F, 1.5F, 16.0F, 2.0F), uvs,
                        "side face of sleeper must use straight-model UVs"
                );
            }
        }
    }

    private static float zCenter(TrackCellGeometry.Element element) {
        return (element.fromZ() + element.toZ()) / 2.0F;
    }

    private static float sleeperWidth(TrackCellGeometry.Element element) {
        return element.toZ() - element.fromZ();
    }
}