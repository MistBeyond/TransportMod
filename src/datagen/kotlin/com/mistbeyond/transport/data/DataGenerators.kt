package com.mistbeyond.transport.data

import com.mistbeyond.transport.Ids
import com.mistbeyond.transport.data.lang.ModLanguageProvider
import com.mistbeyond.transport.data.model.ModModelProvider
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent

@EventBusSubscriber(modid = Ids.MOD_ID, value = [Dist.CLIENT])
object DataGenerators {
    @JvmStatic
    @SubscribeEvent
    fun onGatherData(event: GatherDataEvent.Client) {
        event.createProvider { output -> ModLanguageProvider(output) }
        event.createProvider { output -> ModModelProvider(output) }
    }
}
