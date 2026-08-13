package com.mistbeyond.transport.data.lang

import com.mistbeyond.transport.Ids
import com.mistbeyond.transport.block.Blocks
import com.mistbeyond.transport.entity.rail.Entities
import com.mistbeyond.transport.item.Items
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import net.minecraft.world.item.BlockItem
import net.neoforged.neoforge.common.data.LanguageProvider

class ModLanguageProvider(output: PackOutput) : LanguageProvider(output, Ids.MOD_ID, "en_us") {
    override fun addTranslations() {
        addBlocksAndItems()
        addEntities()
    }

    private fun addBlocksAndItems() {
        for (holder in Blocks.BLOCKS.entries) {
            addBlock(holder, English.byId(holder.id, English::capitalize))
        }

        for (holder in Items.ITEMS.entries) {
            if (holder.get() !is BlockItem) {
                addItem(holder, English.byId(holder.id, English::capitalize))
            }
        }
    }

    private fun addEntities() {
        for (holder in Entities.ENTITY_TYPES.entries) {
            addEntityType(holder, English.byId(holder.id, English::capitalize))
        }
    }

    object English {
        fun byId(id: Identifier, formatter: (String) -> String): String {
            return byKey(Util.makeDescriptionId("", id), formatter)
        }

        fun byKey(key: String, formatter: (String) -> String): String {
            val delimiter = key.lastIndexOf(".")
            return formatter(key.substring(delimiter + 1).replace("_", " "))
        }

        fun capitalize(string: String): String {
            return string.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar(Char::uppercase)
            }
        }
    }
}
