package com.mistbeyond.transport;

import com.mistbeyond.registry.CommonRegistrar;
import com.mistbeyond.transport.block.Blocks;
import com.mistbeyond.transport.entity.rail.Entities;
import com.mistbeyond.transport.item.CreativeTabs;
import com.mistbeyond.transport.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Init {
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
        CreativeTabs.CREATIVE_TABS.register(modBus);
        Entities.ENTITY_TYPES.register(modBus);
    }
}
