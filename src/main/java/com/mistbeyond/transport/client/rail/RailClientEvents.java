package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.Ids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Client-side game-bus listeners for rail state, e.g. dropping the cached signal states when leaving a level.
 */
@EventBusSubscriber(modid = Ids.MOD_ID, value = Dist.CLIENT)
public class RailClientEvents {
    private RailClientEvents() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RailSignalsCache.clear();
        }
    }
}