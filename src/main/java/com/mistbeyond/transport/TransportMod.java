package com.mistbeyond.transport;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Ids.MOD_ID)
public class TransportMod {
    public TransportMod(IEventBus modBus, ModContainer modContainer) {
        Init.registerCommon(modBus, modContainer);
    }
}
