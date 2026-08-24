package com.mistbeyond.transport.client.rail.model;

import com.mistbeyond.transport.api.rail.graph.TrackAxis;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.TrackCellData;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.api.rail.section.SignalType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure geometry generation for a track cell, shared by the datagen simple models and the runtime baked-model route
 * (ADR 0006). Elements are axis-aligned boxes in 16th-block pixel coordinates with a Y rotation about the block
 * center (8, 8, 8); the rotation follows the same convention as the vanilla model system, so the runtime geometry is
 * identical to the datagen models (straight {@code block/track}, diagonal {@code block/track_diagonal}) and adjacent
 * cells' rails meet exactly at the seam plane.
 *
 * <p>Storage model: a simple cell stores one axis, a complex cell stores placements; {@link #elementsFor(TrackCellData)}
 * unions the per-axis geometry (opposite directional placements collapse to the same axis, so a bidirectional
 * straight pair generates one set of rails, not two overlapping copies).
 */
public class TrackCellGeometry {
    public record Element(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            float rotationDegrees,
            String texture
    ) {
    }

    private static final String RAIL = "rail";
    private static final String SLEEPER = "sleeper";
    private static final String SIGNAL_BLOCK = "signal_block";
    private static final String SIGNAL_PATH = "signal_path";
    private static final float BLOCK_CENTER = 8.0F;
    /**
     * Sleeper slab overhang: 33 px long so it spans the full 16 px cell and 8.5 px into each neighbor (matches
     * {@code block/track} and the datagen diagonal model).
     */
    private static final float SLEEPER_X0 = -8.5F;
    private static final float SLEEPER_X1 = 24.5F;
    private static final float SLEEPER_W_HALF = RailGeometryParams.SLEEPER_W / 2.0F;
    /**
     * Half-length of the straight rails: end faces at the cell borders (stored axis-aligned, no rotation).
     */
    private static final float STRAIGHT_RAIL_HALF_LENGTH = 8.0F;
    /**
     * Half-distance of the straight track's two sleepers from the cell center: 8 px spacing, so they sit at
     * z = 4 and z = 12, matching {@code block/track}.
     */
    private static final float STRAIGHT_SLEEPER_HALF_SPACING = 4.0F;

    private static final List<Element> STRAIGHT = List.copyOf(straightElements());
    private static final List<Element> DIAGONAL = List.copyOf(diagonalElements());

    private TrackCellGeometry() {
    }

    /**
     * Geometry for a simple cell storing a single axis (the BlockState fallback of the dynamic model).
     */
    public static List<Element> elementsForAxis(TrackAxis axis) {
        return rotated(axis.diagonal() ? DIAGONAL : STRAIGHT, angleFor(axis));
    }

    /**
     * Geometry for a complex cell from its placement set: the union of the per-axis geometry. Curve and curve-ramp
     * placements have no defined geometry yet and are skipped.
     */
    public static List<Element> elementsFor(TrackCellData data) {
        List<Element> result = new ArrayList<>();
        Set<TrackAxis> seenAxes = new HashSet<>();
        for (TrackPlacement placement : data.placements()) {
            TrackType type = placement.trackType();
            if (type != TrackType.STRAIGHT && type != TrackType.DIAGONAL_45) {
                continue;
            }
            TrackAxis axis = TrackAxis.from(placement.direction());
            if (seenAxes.add(axis)) {
                result.addAll(rotated(axis.diagonal() ? DIAGONAL : STRAIGHT, angleFor(axis)));
            }
        }
        if (!data.signals().isEmpty()) {
            for (var signal : data.signals()) {
                result.addAll(signalElements(signal.direction(), signal.type()));
            }
        }
        return List.copyOf(result);
    }

    static float angleFor(TrackAxis axis) {
        return switch (axis) {
            case N_S -> 0.0F;
            case E_W -> 90.0F;
            case NW_SE -> 45.0F;
            case NE_SW -> 135.0F;
        };
    }

    private static List<Element> straightElements() {
        List<Element> elements = new ArrayList<>();
        rails(elements, STRAIGHT_RAIL_HALF_LENGTH);
        sleeper(elements, BLOCK_CENTER - STRAIGHT_SLEEPER_HALF_SPACING);
        sleeper(elements, BLOCK_CENTER + STRAIGHT_SLEEPER_HALF_SPACING);
        return List.copyOf(elements);
    }

    private static List<Element> diagonalElements() {
        List<Element> elements = new ArrayList<>();
        rails(elements, RailGeometryParams.TRACK_HALF_LENGTH);
        sleeper(elements, BLOCK_CENTER - RailGeometryParams.SLEEPER_SPACING);
        sleeper(elements, BLOCK_CENTER);
        sleeper(elements, BLOCK_CENTER + RailGeometryParams.SLEEPER_SPACING);
        return List.copyOf(elements);
    }

    /**
     * One track per rail center: web plus base and top caps, running along the local Z axis for {@code halfLength}
     * either side of the cell center.
     */
    private static void rails(List<Element> out, float halfLength) {
        for (float center : new float[]{
                BLOCK_CENTER - RailGeometryParams.GAUGE / 2.0F,
                BLOCK_CENTER + RailGeometryParams.GAUGE / 2.0F
        }) {
            float fromZ = BLOCK_CENTER - halfLength;
            float toZ = BLOCK_CENTER + halfLength;
            out.add(new Element(
                    center - RailGeometryParams.RAIL_W / 2.0F, RailGeometryParams.RAIL_BOTTOM, fromZ,
                    center + RailGeometryParams.RAIL_W / 2.0F, RailGeometryParams.RAIL_TOP, toZ,
                    0.0F, RAIL
            ));
            out.add(new Element(
                    center - RailGeometryParams.CAP_W / 2.0F, RailGeometryParams.RAIL_BOTTOM - RailGeometryParams.CAP_H, fromZ,
                    center + RailGeometryParams.CAP_W / 2.0F, RailGeometryParams.RAIL_BOTTOM, toZ,
                    0.0F, RAIL
            ));
            out.add(new Element(
                    center - RailGeometryParams.CAP_W / 2.0F, RailGeometryParams.RAIL_TOP, fromZ,
                    center + RailGeometryParams.CAP_W / 2.0F, RailGeometryParams.RAIL_TOP + RailGeometryParams.CAP_H, toZ,
                    0.0F, RAIL
            ));
        }
    }

    private static void sleeper(List<Element> out, float zCenter) {
        out.add(new Element(
                SLEEPER_X0, 0.0F, zCenter - SLEEPER_W_HALF,
                SLEEPER_X1, RailGeometryParams.SLEEPER_H, zCenter + SLEEPER_W_HALF,
                0.0F, SLEEPER
        ));
    }

    private static List<Element> signalElements(GridDirection direction, SignalType type) {
        // Placeholder signal post: 2x2 footprint, 8 px tall, placed at the cell edge the signal faces.
        String tex = type == SignalType.PATH ? SIGNAL_PATH : SIGNAL_BLOCK;
        float h = 8.0F;
        float y0 = 2.0F;
        float y1 = y0 + h;
        // 2 px thick post centered on the edge.
        return switch (direction) {
            case NORTH -> List.of(new Element(7.0F, y0, 0.0F, 9.0F, y1, 2.0F, 0.0F, tex));
            case SOUTH -> List.of(new Element(7.0F, y0, 14.0F, 9.0F, y1, 16.0F, 0.0F, tex));
            case EAST -> List.of(new Element(14.0F, y0, 7.0F, 16.0F, y1, 9.0F, 0.0F, tex));
            case WEST -> List.of(new Element(0.0F, y0, 7.0F, 2.0F, y1, 9.0F, 0.0F, tex));
            case NORTH_EAST -> List.of(new Element(14.0F, y0, 0.0F, 16.0F, y1, 2.0F, 0.0F, tex));
            case SOUTH_EAST -> List.of(new Element(14.0F, y0, 14.0F, 16.0F, y1, 16.0F, 0.0F, tex));
            case SOUTH_WEST -> List.of(new Element(0.0F, y0, 14.0F, 2.0F, y1, 16.0F, 0.0F, tex));
            case NORTH_WEST -> List.of(new Element(0.0F, y0, 0.0F, 2.0F, y1, 2.0F, 0.0F, tex));
        };
    }

    private static List<Element> rotated(List<Element> elements, float angleDegrees) {
        if (angleDegrees == 0.0F) {
            return elements;
        }
        List<Element> out = new ArrayList<>(elements.size());
        for (Element element : elements) {
            out.add(new Element(
                    element.fromX(), element.fromY(), element.fromZ(),
                    element.toX(), element.toY(), element.toZ(),
                    angleDegrees, element.texture()
            ));
        }
        return List.copyOf(out);
    }
}