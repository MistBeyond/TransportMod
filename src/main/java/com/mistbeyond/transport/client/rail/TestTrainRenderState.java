package com.mistbeyond.transport.client.rail;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class TestTrainRenderState extends EntityRenderState {
    public float yRot;
    public final BlockModelRenderState chassis = new BlockModelRenderState();
    public final BlockModelRenderState body = new BlockModelRenderState();
    public final BlockModelRenderState cab = new BlockModelRenderState();
}
