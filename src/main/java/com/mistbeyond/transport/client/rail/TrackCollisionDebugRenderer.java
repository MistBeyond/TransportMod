package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
import com.mistbeyond.transport.block.rail.TrackCellShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * F3 debug overlay for track cell collision shapes. The actual collision shapes are axis-aligned box staircases (the
 * engine cannot express 45-degree strips as smooth surfaces), so this overlay draws each placement's ideal outline
 * from the same geometry source ({@link TrackCellShapes#placementOutlinePx}): straight strips as one rectangle,
 * diagonal 45 strips as one parallelogram. The result is the smooth outline of the collision, without the engine's
 * per-box sawtooth. Curve outlines are sampled points once curve geometry is defined; until then curve cells are
 * skipped.
 *
 * <p>The scan is cached and re-run at most once per second or when the camera moves, and each emitted polygon is
 * frustum-culled, so the overlay costs almost nothing per frame.
 */
public class TrackCollisionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    /**
     * Scan half-extent around the camera, in blocks.
     */
    private static final int SCAN_RADIUS = 24;
    /**
     * Re-scan when the camera moves this far from the last scan center, in blocks.
     */
    private static final int RESCAN_DISTANCE = 8;
    /**
     * Periodic re-scan interval so track edits show up without camera movement, in ticks (1 second).
     */
    private static final long RESCAN_INTERVAL_TICKS = 20;
    /**
     * Outline color: a warm yellow that stands apart from the vanilla F3+B green collision boxes and the colored
     * section bars.
     */
    private static final int OUTLINE_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.85F, 0.2F);
    /**
     * Outline line width in pixels.
     */
    private static final float OUTLINE_WIDTH = 2.0F;
    /**
     * Collision strip top: 2 px = 0.125 blocks. Outlines are drawn just above it so they never z-fight the model.
     */
    private static final double OUTLINE_Y_OFFSET = 0.02;

    private final Minecraft minecraft;
    @Nullable
    private BlockPos scanCenter;
    private long lastScanTick = Long.MIN_VALUE;
    private List<RenderedOutline> outlines = List.of();

    public TrackCollisionDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(
            double camX,
            double camY,
            double camZ,
            DebugValueAccess debugValues,
            Frustum frustum,
            float partialTicks
    ) {
        Level level = minecraft.level;
        if (level == null || !minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        BlockPos camera = BlockPos.containing(camX, camY, camZ);
        if (shouldRescan(camera, level.getGameTime())) {
            scanCenter = camera;
            lastScanTick = level.getGameTime();
            outlines = scan(level, camera);
        }
        render(outlines, frustum);
    }

    private boolean shouldRescan(BlockPos camera, long tick) {
        if (scanCenter == null) {
            return true;
        }
        if (tick - lastScanTick >= RESCAN_INTERVAL_TICKS) {
            return true;
        }
        int dx = camera.getX() - scanCenter.getX();
        int dz = camera.getZ() - scanCenter.getZ();
        return dx * dx + dz * dz >= RESCAN_DISTANCE * RESCAN_DISTANCE;
    }

    private static List<RenderedOutline> scan(Level level, BlockPos center) {
        List<RenderedOutline> result = new ArrayList<>();
        int minY = Math.max(level.getMinY(), center.getY() - SCAN_RADIUS);
        int maxY = Math.min(level.getMaxY(), center.getY() + SCAN_RADIUS);
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - SCAN_RADIUS; x <= center.getX() + SCAN_RADIUS; x++) {
                for (int z = center.getZ() - SCAN_RADIUS; z <= center.getZ() + SCAN_RADIUS; z++) {
                    GridPos cell = new GridPos(x, y, z);
                    if (!RailTrackCellBlock.isTrackAt(level, cell)) {
                        continue;
                    }
                    for (TrackPlacement placement : RailTrackCellBlock.source(level).placementsAt(cell)) {
                        List<double[]> polygon = TrackCellShapes.placementOutlinePx(placement);
                        if (polygon.size() < 3) {
                            continue;
                        }
                        result.add(new RenderedOutline(cell, List.copyOf(polygon)));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static void render(List<RenderedOutline> outlines, Frustum frustum) {
        for (RenderedOutline outline : outlines) {
            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            List<Vec3> points = new ArrayList<>(outline.polygon().size());
            for (double[] corner : outline.polygon()) {
                double x = outline.cell().x() + corner[0] / 16.0;
                double z = outline.cell().z() + corner[1] / 16.0;
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxZ = Math.max(maxZ, z);
                points.add(new Vec3(x, outline.cell().y() + 0.125 + OUTLINE_Y_OFFSET, z));
            }
            if (!frustum.isVisible(new AABB(minX, outline.cell().y(), minZ, maxX, outline.cell().y() + 1.0, maxZ))) {
                continue;
            }
            int n = points.size();
            for (int i = 0; i < n; i++) {
                Gizmos.line(points.get(i), points.get((i + 1) % n), OUTLINE_COLOR, OUTLINE_WIDTH);
            }
        }
    }

    record RenderedOutline(
            GridPos cell,
            List<double[]> polygon
    ) {
    }
}
