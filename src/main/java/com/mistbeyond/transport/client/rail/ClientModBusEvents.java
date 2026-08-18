package com.mistbeyond.transport.client.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.client.rail.model.TrackCellDynamicModel;
import com.mistbeyond.transport.core.rail.RailSignalsPayload;
import com.mistbeyond.transport.entity.rail.Entities;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;

@EventBusSubscriber(modid = Ids.MOD_ID, value = Dist.CLIENT)
public class ClientModBusEvents {
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

    @SubscribeEvent
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(RailSignalsPayload.TYPE, (payload, _) -> RailSignalsCache.update(payload.states()));
    }

    @SubscribeEvent
    public static void onRegisterBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(
                Identifier.fromNamespaceAndPath(Ids.MOD_ID, "track_cell"),
                TrackCellDynamicModel.CODEC
        );
    }
}
