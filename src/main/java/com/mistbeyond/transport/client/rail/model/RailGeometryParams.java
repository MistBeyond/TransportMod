package com.mistbeyond.transport.client.rail.model;

/**
 * Shared rail geometry parameters, in 16th-block pixel units, matched to the hand-made straight reference model
 * {@code block/track} (gauge 24 px = 1.5 blocks, rails 2 x 2.4 px with 3 px base/top caps, sleepers 33 x 1 x 3 px).
 *
 * <p>These live in {@code main} so that both the datagen simple-track generator
 * ({@code com.mistbeyond.transport.data.model.TrackModelGenerator}) and the runtime complex-cell baked-model route
 * consume exactly the same numbers; otherwise straight and diagonal/curve track drift apart (see
 * {@code docs/decisions/0006-complex-track-cell-rendering.md}).
 */
public class RailGeometryParams {
    /**
     * Rail center distance: 24 px = 1.5 blocks.
     */
    public static final float GAUGE = 24.0F;
    /**
     * Rail web cross-section: 2 px wide, 2.4 px tall (y 1.5..3.9).
     */
    public static final float RAIL_W = 2.0F;
    public static final float RAIL_BOTTOM = 1.5F;
    public static final float RAIL_TOP = 3.9F;
    /**
     * Rail base/top caps: 3 px wide, 0.5 px tall.
     */
    public static final float CAP_W = 3.0F;
    public static final float CAP_H = 0.5F;
    /**
     * Half-length of the rail elements along the local track axis, exactly the distance from the block center to a
     * corner (8*sqrt(2) px). Adjacent cells' rails then meet precisely at the seam plane: the end caps coincide
     * back-to-back and side faces abut, so the rail renders as one continuous piece with no seam and no z-fighting.
     */
    public static final float TRACK_HALF_LENGTH = 8.0F * (float) Math.sqrt(2.0);
    /**
     * Sleeper slab: 33 px long (overhangs into neighboring cells by design), 1 px tall, 3 px wide.
     */
    public static final float SLEEPER_LEN = 33.0F;
    public static final float SLEEPER_H = 1.0F;
    public static final float SLEEPER_W = 3.0F;
    /**
     * Along-track spacing of the sleepers. The diagonal cell pitch is 16*sqrt(2) px; three sleepers per cell at
     * spacing 16*sqrt(2)/3 keep the sleeper rhythm uniform across cells. The straight track's 8 px spacing cannot be
     * used here: 8 does not divide the diagonal pitch, so an 8 px rhythm would leave a visible gap at every cell
     * boundary.
     */
    public static final float SLEEPER_SPACING = 16.0F * (float) Math.sqrt(2.0) / 3.0F;
    /**
     * Rotation angle for the diagonal variant, in degrees.
     */
    public static final float DIAGONAL_ANGLE = 45.0F;
    /**
     * Rotation origin: the block center. The y coordinate is irrelevant for a Y-axis rotation.
     */
    public static final float ORIGIN_X = 8.0F;
    public static final float ORIGIN_Y = 8.0F;
    public static final float ORIGIN_Z = 8.0F;

    private RailGeometryParams() {
    }
}