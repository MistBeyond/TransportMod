@file:Suppress("LombokKotlinCompilerPlugin")

package com.mistbeyond.transport.data

import com.mojang.math.Quadrant
import com.mistbeyond.transport.Ids
import com.mistbeyond.transport.block.Blocks
import com.mistbeyond.transport.block.rail.TestTrackBlock
import com.mistbeyond.transport.item.Items
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.ModelProvider
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator
import net.minecraft.client.data.models.blockstates.PropertyDispatch
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.core.Holder
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import java.util.stream.Stream

class ModModelProvider(output: PackOutput) : ModelProvider(output, Ids.MOD_ID) {
    override fun registerModels(blockModels: BlockModelGenerators, itemModels: ItemModelGenerators) {
        val testTrack = Blocks.BLOCKS.entries.stream()
            .filter { it.id.path == "test_track" }
            .findFirst()
            .orElseThrow()
            .value()
        val trackModel = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "block/track")
        blockModels.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(testTrack).with(
                PropertyDispatch.initial(TestTrackBlock.AXIS).generate { axis ->
                    val variant = BlockModelGenerators.plainModel(trackModel).withYRot(
                        if (axis == TestTrackBlock.TrackAxis.EAST_WEST) Quadrant.R90 else Quadrant.R0
                    )
                    BlockModelGenerators.variant(variant)
                }
            )
        )
        blockModels.registerSimpleItemModel(testTrack, trackModel)

        val trainSpawner = Items.ITEMS.entries.stream()
            .filter { it.id.path == "test_train_spawner" }
            .findFirst()
            .orElseThrow()
            .value()
        itemModels.generateFlatItem(trainSpawner, net.minecraft.world.item.Items.MINECART, ModelTemplates.FLAT_ITEM)
    }

    override fun getKnownBlocks(): Stream<out Holder<Block>> = Blocks.BLOCKS.entries.stream()

    override fun getKnownItems(): Stream<out Holder<Item>> = Items.ITEMS.entries.stream()
}
