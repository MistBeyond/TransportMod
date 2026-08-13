package com.mistbeyond.transport.entity.rail;

import com.mistbeyond.transport.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Ids.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<TestTrainEntity>> TEST_TRAIN = ENTITY_TYPES.register(
            "test_train",
            () -> EntityType.Builder.of(TestTrainEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .passengerAttachments(0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Ids.thisMod("test_train")))
    );

    private Entities() {
    }
}
