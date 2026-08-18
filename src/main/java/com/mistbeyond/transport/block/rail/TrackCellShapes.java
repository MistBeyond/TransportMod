package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.TrackAxis;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Collision shapes for rail track cells, generated from the same source of truth as the graph: {@link TrackPlacement}
 * entries (see ADR 0005 and {@code docs/roadmap/rail/tracks.md}).
 *
 * <p>A shape is a strip centered on the track axis covering both rails and the area between them. The strip is not
 * clipped to the owning block's 16x16 bounds, so it may overflow into neighboring cells. A simple cell (one axis, no
 * block entity) uses the axis strip; a complex cell (multiple placements) uses the union of its placement strips.
 * CURVE and CURVE_RAMP placements have no defined geometry yet, so they fall back to a full-cell box until curve
 * geometry is specified.
 */
public class TrackCellShapes {
    /**
     * Collision strip half-width follows the full rail profile: the 24 px gauge (rail center distance, 1.5 blocks)
     * plus both rail widths (2 px each), i.e. 26 px (1.625 blocks) total, so 13 px either side of the track axis
     * (ADR 0005, docs/roadmap/rail/tracks.md).
     */
    private static final double STRIP_HALF_WIDTH = 13.0;

    private static final VoxelShape EAST_WEST_STRIP =
            Block.box(0.0, 0.0, 8.0 - STRIP_HALF_WIDTH, 16.0, 2.0, 8.0 + STRIP_HALF_WIDTH);
    private static final VoxelShape NORTH_SOUTH_STRIP =
            Block.box(8.0 - STRIP_HALF_WIDTH, 0.0, 0.0, 8.0 + STRIP_HALF_WIDTH, 2.0, 16.0);
    /**
     * NW–SE diagonal strip: the 26 px gauge-wide band about the x = z line, approximated with axis-aligned column
     * boxes over the parallelogram that reaches from one cell corner to the opposite corner ({@code 0 <= x + z <= 32})
     * and overflows the strip width ({@code |x - z| <= 13*sqrt(2)}) into the two side neighbors. Like the straight
     * strip this is a corridor along the track axis, not a hexagon: the diagonal track collides continuously across
     * adjacent cells exactly like straight track (tracks.md "Collision and Occupancy").
     */
    private static final VoxelShape DIAGONAL_STRIP_NW_SE = diagonalStrip(true);
    /**
     * NE–SW diagonal strip: the same band about the x + z = 16 line, as a parallelogram from one cell corner to the
     * opposite ({@code |x - z| <= 16}) with the strip width overflow ({@code |x + z - 16| <= 13*sqrt(2)}).
     */
    private static final VoxelShape DIAGONAL_STRIP_NE_SW = diagonalStrip(false);
    /**
     * Placeholder for CURVE and CURVE_RAMP placements: exact curve geometry is defined later (ADR 0005), so the cell
     * falls back to a full 16x16x2 box.
     */
    private static final VoxelShape CURVE_PLACEHOLDER = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    private TrackCellShapes() {
    }

    /**
     * Strip shape for a simple cell storing a single track axis.
     */
    public static VoxelShape axisShape(TrackAxis axis) {
        return switch (axis) {
            case N_S -> NORTH_SOUTH_STRIP;
            case E_W -> EAST_WEST_STRIP;
            case NE_SW -> DIAGONAL_STRIP_NE_SW;
            case NW_SE -> DIAGONAL_STRIP_NW_SE;
        };
    }

    /**
     * Column approximation of a 45-degree strip as a parallelogram (the diagonal analogue of the straight strip's
     * rectangle): the band is bounded by the track-axis extent (one cell corner to the opposite corner) and the strip
     * width (26 px, half-width D = 13*sqrt(2) measured perpendicular to the axis). In local cell pixels:
     *
     * <pre>
     * NW–SE:  0 <= x + z <= 32          (track-axis extent),  |x - z| <= D    (strip width)
     * NE–SW:  |x - z| <= 16             (track-axis extent),  |x + z - 16| <= D (strip width)
     * </pre>
     * <p>
     * Each 1 px wide x-column becomes one axis-aligned box whose z range is the column's intersection with the band,
     * so the union is a staircase approximation of the diagonal corridor that overflows into neighboring cells.
     */
    private static VoxelShape diagonalStrip(boolean nwSe) {
        double d = STRIP_HALF_WIDTH * Math.sqrt(2.0);
        List<VoxelShape> columns = new ArrayList<>();
        // 1 px columns: the finest step Minecraft's box-based collision supports. Coarser columns (e.g. 2 px) would
        // read as a heavy staircase on the overflow edges; 1 px keeps the same parallelogram but reduces the sawtooth
        // to the engine's minimum. The whole cell is covered either way (the 26 px band is wider than the cell), so
        // the extra boxes only refine the edges that overflow into neighbors.
        for (double x0 = -20.0; x0 <= 36.0; x0 += 1.0) {
            double x1 = x0 + 1.0;
            if (nwSe) {
                addColumn(columns, x0, x1,
                        t -> Math.max(-t, t - d),
                        t -> Math.min(32.0 - t, t + d));
            } else {
                addColumn(columns, x0, x1,
                        t -> Math.max(t - 16.0, 16.0 - d - t),
                        t -> Math.min(t + 16.0, 16.0 + d - t));
            }
        }
        VoxelShape first = columns.removeFirst();
        return Shapes.or(first, columns.toArray(new VoxelShape[0]));
    }

    /**
     * Adds the column box for x in [x0, x1] whose z range spans the widest band of the strip over the column: each x
     * gives z in [lo(x), hi(x)], and the box takes the smallest lower bound and largest upper bound over the two
     * column endpoints so the strip is covered without gaps.
     */
    private static void addColumn(List<VoxelShape> columns, double x0, double x1,
                                  java.util.function.DoubleUnaryOperator lo,
                                  java.util.function.DoubleUnaryOperator hi) {
        double zLo = Math.min(lo.applyAsDouble(x0), lo.applyAsDouble(x1));
        double zHi = Math.max(hi.applyAsDouble(x0), hi.applyAsDouble(x1));
        if (zLo < zHi) {
            columns.add(Block.box(x0, 0.0, zLo, x1, 2.0, zHi));
        }
    }

    /**
     * Strip shape for one placement. A single axis-aligned {@link VoxelShape} box cannot express a 45-degree strip, so
     * diagonal strips are approximated with multiple axis-aligned boxes.
     */
    public static VoxelShape placementShape(TrackPlacement placement) {
        GridDirection direction = placement.direction();
        return switch (placement.trackType()) {
            case STRAIGHT -> switch (direction) {
                case NORTH, SOUTH -> NORTH_SOUTH_STRIP;
                case EAST, WEST -> EAST_WEST_STRIP;
                default -> throw new IllegalArgumentException("Straight track cannot use direction " + direction);
            };
            case DIAGONAL_45 -> switch (direction) {
                case NORTH_EAST, SOUTH_WEST -> DIAGONAL_STRIP_NE_SW;
                case NORTH_WEST, SOUTH_EAST -> DIAGONAL_STRIP_NW_SE;
                default -> throw new IllegalArgumentException("Diagonal 45 track cannot use direction " + direction);
            };
            case CURVE, CURVE_RAMP -> CURVE_PLACEHOLDER;
        };
    }

    /**
     * Union of the placement strips of a complex cell. The collision of a complex cell is the union of all rail
     * collision shapes in that cell (runtime contract: complex collision MUST be the union of the cell's rail
     * collision shapes).
     */
    public static VoxelShape cellShape(Set<TrackPlacement> placements) {
        if (placements.isEmpty()) {
            return Shapes.empty();
        }
        VoxelShape[] shapes = new VoxelShape[placements.size()];
        int i = 0;
        for (TrackPlacement placement : placements) {
            shapes[i++] = placementShape(placement);
        }
        return Shapes.or(shapes[0], Arrays.copyOfRange(shapes, 1, shapes.length));
    }

    /**
     * The single box enclosing the union of a cell's placement strips, used as the aiming/terrain ({@code getShape})
     * shape. One box instead of the 1 px collision staircase keeps vanilla break particles at normal quantity; the
     * exact staircase is still used for collision.
     */
    public static VoxelShape cellBoundingShape(Set<TrackPlacement> placements) {
        if (placements.isEmpty()) {
            return Shapes.empty();
        }
        AABB bounds = null;
        for (TrackPlacement placement : placements) {
            AABB placementBounds = placementShape(placement).bounds();
            bounds = bounds == null ? placementBounds : bounds.minmax(placementBounds);
        }
        return Block.box(
                bounds.minX * 16.0, bounds.minY * 16.0, bounds.minZ * 16.0,
                bounds.maxX * 16.0, bounds.maxY * 16.0, bounds.maxZ * 16.0
        );
    }

    /**
     * The ideal, smooth outline of one placement's collision strip, in cell-local pixels (x, z), as a closed polygon.
     * The collision shape itself is a staircase approximation of this outline; rendering the outline directly shows
     * the same collision area without the engine's per-box sawtooth (straight strip = one rectangle, diagonal 45 strip
     * = one parallelogram). Curve types have no defined geometry yet and return an empty list; curve outlines will be
     * sampled arc points once curve geometry exists.
     */
    public static List<double[]> placementOutlinePx(TrackPlacement placement) {
        GridDirection direction = placement.direction();
        return switch (placement.trackType()) {
            case STRAIGHT -> switch (direction) {
                case NORTH, SOUTH -> List.of(
                        new double[]{8.0 - STRIP_HALF_WIDTH, 0.0},
                        new double[]{8.0 + STRIP_HALF_WIDTH, 0.0},
                        new double[]{8.0 + STRIP_HALF_WIDTH, 16.0},
                        new double[]{8.0 - STRIP_HALF_WIDTH, 16.0});
                case EAST, WEST -> List.of(
                        new double[]{0.0, 8.0 - STRIP_HALF_WIDTH},
                        new double[]{16.0, 8.0 - STRIP_HALF_WIDTH},
                        new double[]{16.0, 8.0 + STRIP_HALF_WIDTH},
                        new double[]{0.0, 8.0 + STRIP_HALF_WIDTH});
                default -> throw new IllegalArgumentException("Straight track cannot use direction " + direction);
            };
            case DIAGONAL_45 -> {
                double d = STRIP_HALF_WIDTH * Math.sqrt(2.0);
                yield switch (direction) {
                    // 0 <= x + z <= 32 (track axis), |x - z| <= d (strip width): the four corners of the parallelogram.
                    case NORTH_WEST, SOUTH_EAST -> List.of(
                            new double[]{d / 2.0, -d / 2.0},
                            new double[]{(32.0 + d) / 2.0, (32.0 - d) / 2.0},
                            new double[]{(32.0 - d) / 2.0, (32.0 + d) / 2.0},
                            new double[]{-d / 2.0, d / 2.0});
                    // |x - z| <= 16 (track axis), |x + z - 16| <= d (strip width).
                    case NORTH_EAST, SOUTH_WEST -> List.of(
                            new double[]{16.0 - d / 2.0, -d / 2.0},
                            new double[]{16.0 + d / 2.0, d / 2.0},
                            new double[]{d / 2.0, 16.0 + d / 2.0},
                            new double[]{-d / 2.0, 16.0 - d / 2.0});
                    default ->
                            throw new IllegalArgumentException("Diagonal 45 track cannot use direction " + direction);
                };
            }
            case CURVE, CURVE_RAMP -> List.of();
        };
    }
}