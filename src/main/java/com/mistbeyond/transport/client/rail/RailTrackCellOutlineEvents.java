package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.api.distmarker.Dist;

import java.util.List;

/**
 * Subscribes the smooth block-outline renderer for rail track cells. The vanilla block outline for a track cell traces
 * the collision shape's axis-aligned boxes, which reads as a staircase on 45-degree strips; this class attaches
 * {@link TrackCellOutlineRenderer} to every {@code mtm:rail_track_cell} outline so the aimed-at wireframe shows the
 * smooth ideal outline (rectangle / parallelogram) instead. The collision boxes themselves are untouched.
 */
@EventBusSubscriber(modid = Ids.MOD_ID, value = Dist.CLIENT)
public class RailTrackCellOutlineEvents {
    private RailTrackCellOutlineEvents() {
    }

    @SubscribeEvent
    public static void onExtractOutline(ExtractBlockOutlineRenderStateEvent event) {
        if (!(event.getBlockState().getBlock() instanceof RailTrackCellBlock)) {
            return;
        }
        BlockPos pos = event.getBlockPos();
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        List<List<Vec3>> polygons = TrackCellOutlineRenderer.polygonsAt(pos, RailTrackCellBlock.source(event.getLevel()).placementsAt(cell));
        if (!polygons.isEmpty()) {
            event.addCustomRenderer(new TrackCellOutlineRenderer(pos, polygons));
        }
    }
}
