package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.block.rail.TestTrackBlock;
import com.mistbeyond.transport.core.rail.RailGraphCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RailSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int SCAN_RADIUS = 24;
    private static final long REBUILD_INTERVAL_TICKS = 40;
    private static final float[][] COLORS = {
            {1.0F, 0.3F, 0.3F},
            {0.3F, 1.0F, 0.4F},
            {0.3F, 0.5F, 1.0F},
            {1.0F, 0.9F, 0.3F},
            {1.0F, 0.4F, 1.0F},
            {0.3F, 1.0F, 1.0F}
    };

    private static long lastScanTick = Long.MIN_VALUE;
    private static List<RenderedSection> sections = List.of();
    private final Minecraft minecraft;

    public RailSectionDebugRenderer(Minecraft minecraft) {
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
        long tick = level.getGameTime();
        if (tick - lastScanTick >= REBUILD_INTERVAL_TICKS) {
            lastScanTick = tick;
            sections = scan(level, BlockPos.containing(camX, camY, camZ));
        }
        renderSections();
    }

    private static void renderSections() {
        for (RenderedSection section : sections) {
            GizmoStyle style = GizmoStyle.strokeAndFill(section.strokeColor(), 2.0F, section.fillColor());
            for (AABB box : section.boxes()) {
                Gizmos.cuboid(box, style);
            }
            Gizmos.billboardTextOverBlock(
                    section.id(),
                    section.labelPos(),
                    0,
                    section.strokeColor(),
                    0.9F
            );
        }
    }

    private static List<RenderedSection> scan(Level level, BlockPos center) {
        List<RenderedSection> result = new ArrayList<>();
        Set<GridPos> visited = new HashSet<>();
        int minY = Math.max(level.getMinY(), center.getY() - SCAN_RADIUS);
        int maxY = Math.min(level.getMaxY(), center.getY() + SCAN_RADIUS);
        int sectionOffset = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - SCAN_RADIUS; x <= center.getX() + SCAN_RADIUS; x++) {
                for (int z = center.getZ() - SCAN_RADIUS; z <= center.getZ() + SCAN_RADIUS; z++) {
                    GridPos pos = new GridPos(x, y, z);
                    if (visited.contains(pos) || !TestTrackBlock.isTrackAt(level, pos)) {
                        continue;
                    }
                    RailGraphView graph = RailGraphCollector.collect(TestTrackBlock.source(level), pos);
                    if (graph.nodes().isEmpty()) {
                        continue;
                    }
                    graph.nodes().forEach(node -> visited.add(node.pos()));
                    result.addAll(sectionRecords(graph, sectionOffset));
                    sectionOffset += Math.max(1, graph.sections().size());
                }
            }
        }
        return result;
    }

    private static List<RenderedSection> sectionRecords(RailGraphView graph, int offset) {
        List<RailSectionView> sorted = graph.sections().stream()
                .sorted(Comparator.comparing(section -> section.id().value()))
                .toList();
        List<RenderedSection> result = new ArrayList<>();
        if (sorted.isEmpty()) {
            result.addAll(renderNodes(graph, "component-" + offset, offset));
            return result;
        }

        for (int i = 0; i < sorted.size(); i++) {
            RailSectionView section = sorted.get(i);
            Set<GridPos> cells = new HashSet<>();
            for (TrackSegmentId segmentId : section.segments()) {
                graph.segmentById(segmentId)
                        .flatMap(segment -> graph.edgeById(segment.edgeId()))
                        .ifPresent(edge -> {
                            cells.add(edge.placement().start());
                            cells.add(edge.placement().end());
                        });
            }
            if (cells.isEmpty()) {
                continue;
            }
            result.addAll(renderCells(section.id().value(), cells, offset + i));
        }
        return result;
    }

    private static List<RenderedSection> renderNodes(RailGraphView graph, String id, int colorIndex) {
        Set<GridPos> cells = new HashSet<>();
        for (RailNodeView node : graph.nodes()) {
            cells.add(node.pos());
        }
        return renderCells(id, cells, colorIndex);
    }

    private static List<RenderedSection> renderCells(String id, Set<GridPos> cells, int colorIndex) {
        List<AABB> boxes = new ArrayList<>();
        BlockPos labelPos = null;
        for (GridPos cell : cells) {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            boxes.add(new AABB(pos));
            if (labelPos == null) {
                labelPos = pos;
            }
        }
        if (labelPos == null) {
            return List.of();
        }
        float[] rgb = COLORS[Math.floorMod(colorIndex, COLORS.length)];
        int fill = ARGB.colorFromFloat(0.22F, rgb[0], rgb[1], rgb[2]);
        int stroke = ARGB.colorFromFloat(1.0F, rgb[0], rgb[1], rgb[2]);
        return List.of(new RenderedSection(id, labelPos, fill, stroke, List.copyOf(boxes)));
    }

    private record RenderedSection(
            String id,
            BlockPos labelPos,
            int fillColor,
            int strokeColor,
            List<AABB> boxes
    ) {
    }
}
