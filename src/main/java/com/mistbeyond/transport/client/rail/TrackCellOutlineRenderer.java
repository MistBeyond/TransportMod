package com.mistbeyond.transport.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.block.rail.TrackCellShapes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom block-outline renderer that draws the ideal, smooth outline of a track cell's collision shape instead of the
 * engine's per-box sawtooth. The vanilla block outline (the black wireframe shown when aiming at a block) is drawn by
 * tracing every edge of the reported collision shape; for a 45-degree strip that shape is an axis-aligned
 * box staircase, so the outline reads as a dense staircase. This renderer replaces that with one closed polygon per
 * placement — a rectangle for straight strips, a parallelogram for diagonal 45 strips — using the same geometry source
 * as the collision shape ({@link TrackCellShapes#placementOutlinePx}), so the displayed outline is the smooth ideal
 * collision area while the actual collision boxes stay unchanged.
 *
 * <p>The renderer is attached to every {@code mtm:rail_track_cell} outline through
 * {@link com.mistbeyond.transport.client.rail.RailTrackCellOutlineEvents}, and {@link #render} returns {@code true} to
 * suppress the vanilla outline for these cells. Curves are skipped until curve geometry is defined.
 */
public class TrackCellOutlineRenderer implements CustomBlockOutlineRenderer {
    /**
     * Outline color matching the vanilla block outline (black with moderate alpha).
     */
    private static final int OUTLINE_COLOR = 0x66000000;
    /**
     * Width requested via the vertex buffer's line-width element; vanilla passes its configured line width, so a small
     * default keeps the line visible on all renderers.
     */
    private static final float LINE_WIDTH = 1.0F;

    private final BlockPos pos;
    private final List<List<Vec3>> polygons;

    /**
     * @param pos      the targeted cell, in world coordinates
     * @param polygons one closed polygon (world coordinates) per placement; each list holds the outline corners
     */
    public TrackCellOutlineRenderer(BlockPos pos, List<List<Vec3>> polygons) {
        this.pos = pos;
        this.polygons = List.copyOf(polygons);
    }

    public BlockPos pos() {
        return pos;
    }

    /**
     * Builds the smooth outline polygons for a cell's placements, in world coordinates, from the collision outline
     * geometry. Empty placements (no track) yield an empty list; curve/ramp placements are skipped until curve
     * geometry is defined.
     */
    public static List<List<Vec3>> polygonsAt(BlockPos pos, Iterable<TrackPlacement> placements) {
        List<List<Vec3>> polygons = new ArrayList<>();
        double originX = pos.getX();
        double originZ = pos.getZ();
        for (TrackPlacement placement : placements) {
            List<double[]> outlinePx = TrackCellShapes.placementOutlinePx(placement);
            if (outlinePx.size() < 3) {
                continue;
            }
            List<Vec3> polygon = new ArrayList<>(outlinePx.size());
            for (double[] corner : outlinePx) {
                polygon.add(new Vec3(originX + corner[0] / 16.0, pos.getY(), originZ + corner[1] / 16.0));
            }
            polygons.add(List.copyOf(polygon));
        }
        return List.copyOf(polygons);
    }

    @Override
    public boolean render(
            BlockOutlineRenderState renderState,
            MultiBufferSource.BufferSource buffer,
            PoseStack poseStack,
            boolean translucentPass,
            LevelRenderState levelRenderState
    ) {
        Vec3 camera = levelRenderState.cameraRenderState.pos;
        VertexConsumer consumer = buffer.getBuffer(RenderTypes.lines());
        for (List<Vec3> polygon : polygons) {
            drawPolygon(consumer, poseStack, polygon, camera);
        }
        buffer.endLastBatch();
        return true;
    }

    /**
     * Draws one closed polygon as line segments in camera-relative world space, mirroring the way
     * {@code ShapeRenderer.renderShape} offsets the block shape by (pos - camera). The pose stack is the world-level
     * one used by the block-outline pass, so segments are emitted at world coordinates minus the camera position.
     */
    private static void drawPolygon(VertexConsumer consumer, PoseStack poseStack, List<Vec3> polygon, Vec3 camera) {
        PoseStack.Pose pose = poseStack.last();
        int n = polygon.size();
        for (int i = 0; i < n; i++) {
            Vec3 a = polygon.get(i);
            Vec3 b = polygon.get((i + 1) % n);
            Vector3f normal = new Vector3f((float) (b.x - a.x), (float) (b.y - a.y), (float) (b.z - a.z)).normalize();
            consumer.addVertex(pose, (float) (a.x - camera.x), (float) (a.y - camera.y + 0.125), (float) (a.z - camera.z))
                    .setColor(OUTLINE_COLOR)
                    .setNormal(pose, normal)
                    .setLineWidth(LINE_WIDTH);
            consumer.addVertex(pose, (float) (b.x - camera.x), (float) (b.y - camera.y + 0.125), (float) (b.z - camera.z))
                    .setColor(OUTLINE_COLOR)
                    .setNormal(pose, normal)
                    .setLineWidth(LINE_WIDTH);
        }
    }
}
