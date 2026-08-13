package com.mistbeyond.transport.item.rail;

import com.mistbeyond.registry.RegisterItem;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.block.rail.TestTrackBlock;
import com.mistbeyond.transport.core.rail.RailNetworkService;
import com.mistbeyond.transport.entity.rail.Entities;
import com.mistbeyond.transport.entity.rail.TestTrainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

@RegisterItem
@SuppressWarnings("unused")
public class TestTrainSpawnerItem extends Item {
    public TestTrainSpawnerItem(Item.Properties properties) {
        super(properties);
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(ItemRegistration registration) {
        registration.register(
                "test_train_spawner",
                TestTrainSpawnerItem::new,
                properties -> properties.stacksTo(1)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        if (!(context.getLevel().getBlockState(clicked).getBlock() instanceof TestTrackBlock)) {
            return InteractionResult.PASS;
        }

        GridPos startPos = new GridPos(clicked.getX(), clicked.getY(), clicked.getZ());
        RailGraphView graph = RailNetworkService.collectGraph(TestTrackBlock.source(context.getLevel()), startPos);
        if (graph.nodeAt(startPos).isEmpty()) {
            return InteractionResult.FAIL;
        }

        TestTrainEntity train = Entities.TEST_TRAIN.get().create(
                context.getLevel(),
                EntitySpawnReason.SPAWN_ITEM_USE
        );
        if (train != null) {
            train.begin(graph, startPos);
            train.setPos(clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5);
            context.getLevel().addFreshEntity(train);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.FAIL;
    }
}
