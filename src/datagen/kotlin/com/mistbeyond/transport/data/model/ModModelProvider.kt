@file:Suppress("LombokKotlinCompilerPlugin")

package com.mistbeyond.transport.data.model

import com.mistbeyond.transport.Ids
import com.mistbeyond.transport.block.Blocks
import com.mistbeyond.transport.block.rail.RailTrackCellBlock
import com.mistbeyond.transport.client.rail.model.TrackCellDynamicModel
import com.mistbeyond.transport.item.Items
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.ModelProvider
import net.minecraft.client.data.models.MultiVariant
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator
import net.minecraft.client.data.models.blockstates.PropertyDispatch
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.core.Holder
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder
import java.util.stream.Stream

class ModModelProvider(output: PackOutput) : ModelProvider(output, Ids.MOD_ID) {
    override fun registerModels(blockModels: BlockModelGenerators, itemModels: ItemModelGenerators) {
        val testTrack = Blocks.BLOCKS.entries.stream()
            .filter { it.id.path == "rail_track_cell" }
            .findFirst()
            .orElseThrow()
            .value()
        val trackModel = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "block/track")
        // The track cells render through the dynamic track cell model (ADR 0006): the same block cannot distinguish a
        // simple from a complex cell via its BlockState (both store one of the four axes), so the dynamic model
        // resolves geometry per block entity from model data and falls back to the matching axis geometry. The
        // datagen simple models (block/track, block/track_diagonal) are still generated below and stay usable.
        val trackCellModel = CustomBlockStateModelBuilder.Simple(TrackCellDynamicModel.INSTANCE)
        blockModels.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(testTrack).with(
                PropertyDispatch.initial(RailTrackCellBlock.DIRECTION).generate { _ ->
                    MultiVariant.of(trackCellModel)
                }
            )
        )
        blockModels.registerSimpleItemModel(testTrack, trackModel)
        // The diagonal 45 straight model is generated programmatically from the same parametric
        // geometry; the hand-written Blockbench export it replaces was not symmetric.
        TrackModelGenerator.generateDiagonal(blockModels)

        val trainSpawner = Items.ITEMS.entries.stream()
            .filter { it.id.path == "test_train_spawner" }
            .findFirst()
            .orElseThrow()
            .value()
        itemModels.generateFlatItem(trainSpawner, net.minecraft.world.item.Items.MINECART, ModelTemplates.FLAT_ITEM)
        // Placeholder item models for the two signal items: block signal (red) and path signal (amethyst).
        // The real signal models are not ready yet, so we reference vanilla item textures as stand-ins via datagen
        // (no handwritten JSON, per AGENTS.md). Fetch via BuiltInRegistries to cover @RegisterItem registration
        // which is not reflected in Items.ITEMS DeferredHolder at datagen time.
        val itemRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM
        val signalPlaceholders = mapOf(
            "rail_block_signal" to net.minecraft.world.item.Items.REDSTONE,
            "rail_path_signal" to net.minecraft.world.item.Items.AMETHYST_SHARD
        )
        for ((id, texture) in signalPlaceholders) {
            val key = Identifier.fromNamespaceAndPath(Ids.MOD_ID, id)
            val item = itemRegistry.getValue(key)
            if (item !== net.minecraft.world.item.Items.AIR) {
                itemModels.generateFlatItem(item, texture, ModelTemplates.FLAT_ITEM)
            }
        }
    }

    override fun getKnownBlocks(): Stream<out Holder<Block>> = Blocks.BLOCKS.entries.stream()

    override fun getKnownItems(): Stream<out Holder<Item>> = Items.ITEMS.entries.stream()
}
