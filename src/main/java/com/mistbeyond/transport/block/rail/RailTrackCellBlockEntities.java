package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.block.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class RailTrackCellBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ids.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailTrackCellBlockEntity>> TRACK_CELL =
            BLOCK_ENTITY_TYPES.register("rail_track_cell", RailTrackCellBlockEntities::createType);

    private RailTrackCellBlockEntities() {
    }

    private static BlockEntityType<RailTrackCellBlockEntity> createType() {
        Block block = Blocks.BLOCKS.getEntries().stream()
                .filter(holder -> holder.getId().getPath().equals("rail_track_cell"))
                .findFirst()
                .orElseThrow()
                .value();
        return new BlockEntityType<>(
                RailTrackCellBlockEntity::new,
                Set.of(block),
                false
        );
    }
}
