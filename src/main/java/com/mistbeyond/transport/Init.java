package com.mistbeyond.transport;

import com.mistbeyond.registry.CommonRegistrar;
import com.mistbeyond.transport.block.Blocks;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
import com.mistbeyond.transport.block.rail.RailTrackCellBlockEntities;
import com.mistbeyond.transport.core.rail.RailSignalsBroadcaster;
import com.mistbeyond.transport.core.rail.RailNetworkManager;
import com.mistbeyond.transport.core.rail.RailSignalsPayload;
import com.mistbeyond.transport.entity.rail.Entities;
import com.mistbeyond.transport.item.CreativeTabs;
import com.mistbeyond.transport.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Init {
    private static final RailSignalsBroadcaster SIGNALS_BROADCASTER = new RailSignalsBroadcaster();

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ids.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Ids.MOD_ID);

    public static final CommonRegistrar REGISTRAR =
            CommonRegistrar.of(Ids.MOD_ID, Blocks.BLOCKS, Items.ITEMS, BLOCK_ENTITIES, MENUS);

    private Init() {
    }

    public static void registerCommon(IEventBus modBus, ModContainer modContainer) {
        REGISTRAR.registerCommon(modBus, modContainer);
        RailTrackCellBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        CreativeTabs.CREATIVE_TABS.register(modBus);
        Entities.ENTITY_TYPES.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(RailSignalsPayload.TYPE, RailSignalsPayload.STREAM_CODEC));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                RailNetworkManager.of(serverLevel).setSource(RailTrackCellBlock.source(serverLevel));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
                RailNetworkManager.of(serverLevel).tickAutomation();
            }
            SIGNALS_BROADCASTER.onServerTick(event.getServer());
        });
    }
}
