package com.mistbeyond.transport.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mistbeyond.transport.entity.rail.TestTrainEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;

public class TestTrainRenderer extends EntityRenderer<TestTrainEntity, TestTrainRenderState> {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public TestTrainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public TestTrainRenderState createRenderState() {
        return new TestTrainRenderState();
    }

    @Override
    public void extractRenderState(TestTrainEntity entity, TestTrainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        blockModelResolver.update(state.chassis, Blocks.IRON_BLOCK.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
        blockModelResolver.update(state.body, Blocks.RED_CONCRETE.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
        blockModelResolver.update(state.cab, Blocks.BLACK_CONCRETE.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    @Override
    public void submit(
            TestTrainRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        submitBox(
                state.chassis,
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                state.outlineColor,
                -0.5F,
                -0.35F,
                -0.5F,
                1.0F,
                0.35F,
                1.0F
        );
        submitBox(
                state.body,
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                state.outlineColor,
                -0.5F,
                0.0F,
                -0.5F,
                0.9F,
                0.45F,
                1.0F
        );
        submitBox(
                state.cab,
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                state.outlineColor,
                0.2F,
                0.0F,
                -0.25F,
                0.35F,
                0.5F,
                0.5F
        );
        poseStack.popPose();
    }

    private static void submitBox(
            BlockModelRenderState model,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int outlineColor,
            float x,
            float y,
            float z,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scaleX, scaleY, scaleZ);
        model.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }
}
