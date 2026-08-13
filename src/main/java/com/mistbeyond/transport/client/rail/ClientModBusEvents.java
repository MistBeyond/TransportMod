package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.entity.rail.Entities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;

@EventBusSubscriber(modid = Ids.MOD_ID, value = Dist.CLIENT)
public final class ClientModBusEvents {
    private ClientModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Entities.TEST_TRAIN.get(), TestTrainRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterDebugRenderers(RegisterDebugRenderersEvent event) {
        event.register(RailSectionDebugRenderer::new);
    }

}
