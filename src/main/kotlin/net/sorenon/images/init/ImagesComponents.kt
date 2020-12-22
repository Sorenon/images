package net.sorenon.images.init

import dev.onyxstudios.cca.api.v3.block.BlockComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.block.BlockComponentInitializer
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import dev.onyxstudios.cca.api.v3.item.ItemComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.item.ItemComponentInitializer
import net.minecraft.block.entity.BannerBlockEntity
import net.minecraft.block.entity.BedBlockEntity
import net.minecraft.item.BannerItem
import net.minecraft.item.BedItem
import net.minecraft.item.Item
import net.minecraft.item.ShieldItem
import net.minecraft.util.Identifier
import net.sorenon.images.api.PrintableComponent
import net.sorenon.images.content.BasePrintableComponent
import net.sorenon.images.content.BasePrintableComponentItem
import net.sorenon.images.content.BedPrintableComponent

@Suppress("UnstableApiUsage")
class ImagesComponents : BlockComponentInitializer, ItemComponentInitializer {
    companion object {
        @JvmStatic
        val PRINTABLE: ComponentKey<PrintableComponent> = ComponentRegistry.getOrCreate(
            Identifier("images", "printable"),
            PrintableComponent::class.java
        )
    }

    override fun registerBlockComponentFactories(registry: BlockComponentFactoryRegistry) {
        registry.registerFor(BannerBlockEntity::class.java, PRINTABLE) { be -> BasePrintableComponent(be) }
        registry.registerFor(
            BedBlockEntity::class.java,
            PRINTABLE
        ) { blockEntity ->
            BedPrintableComponent(blockEntity)
        }
    }

    override fun registerItemComponentFactories(registry: ItemComponentFactoryRegistry) {
        registry.registerFor(
            { item: Item -> item is BannerItem || item is BedItem || item is ShieldItem },
            PRINTABLE,
            { stack -> BasePrintableComponentItem(stack) }
        )
    }
}