package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * F3 debug overlay for rail sections and signals. Connected track components are scanned around the camera and each
 * rail section is drawn as a thick line along its track edges with the section id floating above; signals are drawn
 * as markers with a direction arrow, since signals define where sections split. The scan is cached and re-run at most
 * once per second or when the camera moves, and emitted boxes are frustum-culled, so the overlay costs almost nothing
 * per frame.
 */
public class RailSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    /**
     * Scan half-extent around the camera, in blocks.
     */
    private static final int SCAN_RADIUS = 24;
    /**
     * Re-scan when the camera moves this far from the last scan center, in blocks.
     */
    private static final int RESCAN_DISTANCE = 8;
    /**
     * Periodic re-scan interval so track and signal edits show up without camera movement, in ticks (1 second).
     */
    private static final long RESCAN_INTERVAL_TICKS = 20;

    private static final float[][] SECTION_COLORS = {
            {1.0F, 0.3F, 0.3F},
            {0.3F, 1.0F, 0.4F},
            {0.3F, 0.5F, 1.0F},
            {1.0F, 0.9F, 0.3F},
            {1.0F, 0.4F, 1.0F},
            {0.3F, 1.0F, 1.0F}
    };
    private static final int SIGNAL_FILL = ARGB.colorFromFloat(0.25F, 1.0F, 1.0F, 1.0F);
    private static final int SIGNAL_STROKE = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
    private static final int SIGNAL_ARROW = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
    /**
     * Signal marker palette: white when the state has not been synced yet, red/green from the resolved aspect
     * (player-visible signal state is limited to RED and GREEN, see docs/roadmap/rail/sections.md).
     */
    private static final int[] SIGNAL_RED_PALETTE = {
            ARGB.colorFromFloat(0.25F, 1.0F, 0.25F, 0.25F),
            ARGB.colorFromFloat(1.0F, 1.0F, 0.25F, 0.25F),
            ARGB.colorFromFloat(1.0F, 1.0F, 0.25F, 0.25F)
    };
    private static final int[] SIGNAL_GREEN_PALETTE = {
            ARGB.colorFromFloat(0.25F, 0.3F, 1.0F, 0.3F),
            ARGB.colorFromFloat(1.0F, 0.3F, 1.0F, 0.3F),
            ARGB.colorFromFloat(1.0F, 0.3F, 1.0F, 0.3F)
    };
    /**
     * Signal marker palette for the ERROR health state (a misconfigured signal): amber, so it is clearly neither red
     * nor green. The ERROR indicator is carried out of band, not as a third aspect (ADR 0008).
     */
    private static final int[] SIGNAL_ERROR_PALETTE = {
            ARGB.colorFromFloat(0.25F, 1.0F, 0.5F, 0.1F),
            ARGB.colorFromFloat(1.0F, 1.0F, 0.6F, 0.1F),
            ARGB.colorFromFloat(1.0F, 0.95F, 0.9F, 0.2F)
    };
    /**
     * Half cross-section of a track bar: 0.25 blocks across and 0.1 blocks tall, matching the cardinal bars.
     */
    private static final double BAR_HALF_WIDTH = 0.25;
    private static final double BAR_HALF_HEIGHT = 0.1;

    private final Minecraft minecraft;
    @Nullable
    private BlockPos scanCenter;
    private long lastScanTick = Long.MIN_VALUE;
    private List<RenderedSection> sections = List.of();
    private List<RenderedSignal> signals = List.of();

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
        BlockPos camera = BlockPos.containing(camX, camY, camZ);
        if (shouldRescan(camera, level.getGameTime())) {
            scanCenter = camera;
            lastScanTick = level.getGameTime();
            ScanResult result = scan(level, camera);
            sections = result.sections();
            signals = result.signals();
        }
        render(sections, signals, frustum);
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

    private static ScanResult scan(Level level, BlockPos center) {
        List<RenderedSection> result = new ArrayList<>();
        List<RenderedSignal> signals = new ArrayList<>();
        Set<GridPos> visited = new HashSet<>();
        int minY = Math.max(level.getMinY(), center.getY() - SCAN_RADIUS);
        int maxY = Math.min(level.getMaxY(), center.getY() + SCAN_RADIUS);
        int colorOffset = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - SCAN_RADIUS; x <= center.getX() + SCAN_RADIUS; x++) {
                for (int z = center.getZ() - SCAN_RADIUS; z <= center.getZ() + SCAN_RADIUS; z++) {
                    GridPos pos = new GridPos(x, y, z);
                    if (visited.contains(pos) || !RailTrackCellBlock.isTrackAt(level, pos)) {
                        continue;
                    }
                    RailGraphView graph = RailGraphCollector.collect(RailTrackCellBlock.source(level), pos);
                    if (graph.nodes().isEmpty()) {
                        continue;
                    }
                    graph.nodes().forEach(node -> visited.add(node.pos()));
                    result.addAll(sectionsOf(graph, colorOffset));
                    signals.addAll(signalsOf(graph));
                    colorOffset += Math.max(1, graph.sections().size());
                }
            }
        }
        return new ScanResult(List.copyOf(result), List.copyOf(signals));
    }

    /**
     * Maps a graph to its section visuals: one thick line per track edge plus an id label per section. Sections that
     * carry signal boundaries (i.e. were split by signals) show the boundary count in their label. A component without
     * sections (isolated nodes) falls back to a single "component-N" group drawn as node dots.
     */
    static List<RenderedSection> sectionsOf(RailGraphView graph, int colorOffset) {
        List<RailSectionView> sorted = graph.sections().stream()
                .sorted(Comparator.comparing(section -> section.id().value()))
                .toList();
        List<RenderedSection> result = new ArrayList<>();
        if (sorted.isEmpty()) {
            List<TrackVisual> dots = new ArrayList<>();
            graph.nodes().stream()
                    .sorted(Comparator.comparing(node -> node.pos().toString()))
                    .forEach(node -> dots.add(new TrackVisual(node.pos(), node.pos())));
            result.add(renderedSection("component-" + colorOffset, dots, colorOffset));
            return result;
        }

        for (int i = 0; i < sorted.size(); i++) {
            RailSectionView section = sorted.get(i);
            List<TrackVisual> tracks = new ArrayList<>();
            section.segments().stream()
                    .sorted(Comparator.comparing(TrackSegmentId::value))
                    .forEach(segmentId -> graph.segmentById(segmentId)
                            .flatMap(segment -> graph.edgeById(segment.edgeId()))
                            .ifPresent(edge -> tracks.add(new TrackVisual(
                                    edge.placement().start(),
                                    edge.placement().end()
                            ))));
            if (tracks.isEmpty()) {
                continue;
            }
            String label = section.boundaries().isEmpty()
                    ? section.id().value()
                    : section.id().value() + " (" + section.boundaries().size() + ")";
            result.add(renderedSection(label, tracks, colorOffset + i));
        }
        return result;
    }

    /**
     * Maps a graph to its signal visuals: one marker per signal with id, type, and facing direction.
     */
    static List<RenderedSignal> signalsOf(RailGraphView graph) {
        return graph.signals().stream()
                .sorted(Comparator.comparing(signal -> signal.id().value()))
                .map(signal -> new RenderedSignal(
                        signal.id().value(),
                        new BlockPos(signal.cell().x(), signal.cell().y(), signal.cell().z()),
                        signal.direction(),
                        signal.type().name()
                ))
                .toList();
    }

    private static RenderedSection renderedSection(String id, List<TrackVisual> tracks, int colorIndex) {
        if (tracks.isEmpty()) {
            throw new IllegalArgumentException("Cannot render an empty section: " + id);
        }
        TrackVisual first = tracks.getFirst();
        BlockPos labelPos = new BlockPos(first.start().x(), first.start().y(), first.start().z());
        float[] rgb = SECTION_COLORS[Math.floorMod(colorIndex, SECTION_COLORS.length)];
        int fill = ARGB.colorFromFloat(0.22F, rgb[0], rgb[1], rgb[2]);
        int stroke = ARGB.colorFromFloat(1.0F, rgb[0], rgb[1], rgb[2]);
        return new RenderedSection(id, labelPos, fill, stroke, List.copyOf(tracks));
    }

    private static void render(List<RenderedSection> sections, List<RenderedSignal> signals, Frustum frustum) {
        for (RenderedSection section : sections) {
            GizmoStyle style = GizmoStyle.strokeAndFill(section.strokeColor(), 2.0F, section.fillColor());
            for (TrackVisual track : section.tracks()) {
                GridPos start = track.start();
                GridPos end = track.end();
                List<AABB> boxes = trackBoxes(start, end);
                if (boxes.stream().noneMatch(frustum::isVisible)) {
                    continue;
                }
                if (start.x() != end.x() && start.z() != end.z()) {
                    emitDiagonalBar(start, end, style);
                } else {
                    for (AABB box : boxes) {
                        Gizmos.cuboid(box, style);
                    }
                }
            }
            if (frustum.isVisible(new AABB(section.labelPos()))) {
                Gizmos.billboardTextOverBlock(
                        section.id(),
                        section.labelPos(),
                        0,
                        section.strokeColor(),
                        0.9F
                );
            }
        }
        for (RenderedSignal signal : signals) {
            BlockPos cell = signal.cell();
            SignalAspect aspect = RailSignalsCache.aspectOf(new SignalId(signal.id()));
            boolean error = RailSignalsCache.errorOf(new SignalId(signal.id()));
            int fill = SIGNAL_FILL;
            int stroke = SIGNAL_STROKE;
            int arrow = SIGNAL_ARROW;
            if (error) {
                fill = SIGNAL_ERROR_PALETTE[0];
                stroke = SIGNAL_ERROR_PALETTE[1];
                arrow = SIGNAL_ERROR_PALETTE[2];
            } else if (aspect == SignalAspect.RED) {
                fill = SIGNAL_RED_PALETTE[0];
                stroke = SIGNAL_RED_PALETTE[1];
                arrow = SIGNAL_RED_PALETTE[2];
            } else if (aspect == SignalAspect.GREEN) {
                fill = SIGNAL_GREEN_PALETTE[0];
                stroke = SIGNAL_GREEN_PALETTE[1];
                arrow = SIGNAL_GREEN_PALETTE[2];
            }
            GizmoStyle style = GizmoStyle.strokeAndFill(stroke, 2.0F, fill);
            if ("PATH".equals(signal.type())) {
                emitPathDiamond(cell, style);
            } else {
                Gizmos.cuboid(new AABB(cell).deflate(0.25), style);
            }
            GridDirection direction = signal.direction();
            Vec3 start = Vec3.atCenterOf(cell).add(direction.dx() * 0.3, 0.0, direction.dz() * 0.3);
            Vec3 end = Vec3.atCenterOf(cell).add(direction.dx() * 0.75, 0.0, direction.dz() * 0.75);
            Gizmos.arrow(start, end, arrow);
            Gizmos.billboardTextOverBlock(signal.id(), cell, 0, stroke, 0.8F);
            String detail = signal.type() + " " + signal.direction();
            if (aspect != null) {
                detail += " " + aspect;
            }
            if (error) {
                detail += " ERROR";
            }
            Gizmos.billboardTextOverBlock(detail, cell, 1, stroke, 0.7F);
        }
    }

    /**
     * Distinct F3 marker for path signals: a horizontal diamond (rhombus) on the cell plane, so path vs block signals
     * are distinguishable at a glance (ADR 0008 C1). Block signals keep the translucent cuboid marker.
     */
    private static void emitPathDiamond(BlockPos cell, GizmoStyle style) {
        Vec3 center = Vec3.atCenterOf(cell).add(0.0, 0.08, 0.0);
        Vec3 north = center.add(0.0, 0.0, -0.28);
        Vec3 east = center.add(0.28, 0.0, 0.0);
        Vec3 south = center.add(0.0, 0.0, 0.28);
        Vec3 west = center.add(-0.28, 0.0, 0.0);
        Gizmos.rect(north, east, south, west, style);
    }

    /**
     * Builds the culling boxes for one track edge: a single bar along a cardinal edge, the bounding box of a rotated
     * bar along a diagonal edge, and a small dot for an isolated node (start == end). The line floats just above the
     * track model and spans from the start cell center to the end cell center.
     */
    static List<AABB> trackBoxes(GridPos start, GridPos end) {
        int dx = end.x() - start.x();
        int dz = end.z() - start.z();
        double y0 = start.y() + 0.35;
        double y1 = start.y() + 0.55;
        if (dx == 0 && dz == 0) {
            double cx = start.x() + 0.5;
            double cz = start.z() + 0.5;
            return List.of(new AABB(cx - 0.175, y0, cz - 0.175, cx + 0.175, y1, cz + 0.175));
        }
        if (dz == 0) {
            double minX = Math.min(start.x(), end.x()) + 0.5;
            double cz = start.z() + 0.5;
            return List.of(new AABB(minX, y0, cz - 0.25, minX + 1.0, y1, cz + 0.25));
        }
        if (dx == 0) {
            double minZ = Math.min(start.z(), end.z()) + 0.5;
            double cx = start.x() + 0.5;
            return List.of(new AABB(cx - 0.25, y0, minZ, cx + 0.25, y1, minZ + 1.0));
        }
        return List.of(diagonalBarBounds(start, end));
    }

    /**
     * Emits a rotated thick bar for a diagonal track edge, matching the look of the cardinal bars. The gizmo API has no
     * rotated cuboid, so the bar is assembled from six quads: top, bottom, the two long side faces, and the two end
     * caps, all with the section's fill and stroke style.
     */
    private static void emitDiagonalBar(GridPos start, GridPos end, GizmoStyle style) {
        Vec3[] c = diagonalBarCorners(start, end);
        Gizmos.rect(c[2], c[0], c[4], c[6], style); // top
        Gizmos.rect(c[3], c[7], c[5], c[1], style); // bottom
        Gizmos.rect(c[1], c[5], c[4], c[0], style); // side towards +p
        Gizmos.rect(c[3], c[2], c[6], c[7], style); // side towards -p
        Gizmos.rect(c[3], c[1], c[0], c[2], style); // cap at start
        Gizmos.rect(c[7], c[6], c[4], c[5], style); // cap at end
    }

    /**
     * Bounding box of the rotated bar, used for frustum culling.
     */
    private static AABB diagonalBarBounds(GridPos start, GridPos end) {
        Vec3[] c = diagonalBarCorners(start, end);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : c) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * The eight corners of the rotated bar for one 45-degree track edge, ordered {@code [end(0/1)][side(0/1)][up(0/1)]}
     * where side 0 is towards the positive horizontal perpendicular and up 0 is upwards. The bar has the same
     * cross-section as the cardinal bars and spans from the start cell center to the end cell center.
     */
    private static Vec3[] diagonalBarCorners(GridPos start, GridPos end) {
        double midY = start.y() + 0.45;
        Vec3 a = new Vec3(start.x() + 0.5, midY, start.z() + 0.5);
        Vec3 b = new Vec3(end.x() + 0.5, midY, end.z() + 0.5);
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        Vec3 perpendicular = new Vec3(-dz / length * BAR_HALF_WIDTH, 0.0, dx / length * BAR_HALF_WIDTH);
        Vec3 up = new Vec3(0.0, BAR_HALF_HEIGHT, 0.0);
        Vec3[] corners = new Vec3[8];
        for (int t = 0; t < 2; t++) {
            Vec3 base = t == 0 ? a : b;
            for (int side = 0; side < 2; side++) {
                Vec3 offset = side == 0 ? perpendicular : perpendicular.scale(-1.0);
                for (int upSign = 0; upSign < 2; upSign++) {
                    Vec3 vertical = upSign == 0 ? up : up.scale(-1.0);
                    corners[t * 4 + side * 2 + upSign] = base.add(offset).add(vertical);
                }
            }
        }
        return corners;
    }

    private record ScanResult(
            List<RenderedSection> sections,
            List<RenderedSignal> signals
    ) {
    }

    record RenderedSection(
            String id,
            BlockPos labelPos,
            int fillColor,
            int strokeColor,
            List<TrackVisual> tracks
    ) {
    }

    record TrackVisual(
            GridPos start,
            GridPos end
    ) {
    }

    record RenderedSignal(
            String id,
            BlockPos cell,
            GridDirection direction,
            String type
    ) {
    }
}
