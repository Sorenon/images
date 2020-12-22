package net.sorenon.images.init

import dev.onyxstudios.cca.api.v3.block.BlockComponents
import mcp.mobius.waila.api.*
import net.minecraft.block.entity.BannerBlockEntity
import net.minecraft.block.entity.BedBlockEntity
import net.minecraft.text.Text
import net.sorenon.images.content.ImageBlock
import net.sorenon.images.content.PictureFrameBlockEntity

class ImagesWalia : IWailaPlugin {
    override fun register(registrar: IRegistrar) {
        val defaultPrintedComponentProvider = DefaultPrintedComponentProvider()
        registrar.registerComponentProvider(defaultPrintedComponentProvider, TooltipPosition.BODY, BannerBlockEntity::class.java)
        registrar.registerComponentProvider(defaultPrintedComponentProvider, TooltipPosition.BODY, BedBlockEntity::class.java)
        registrar.registerComponentProvider(PictureFrameComponentProvider(), TooltipPosition.BODY, ImageBlock::class.java)
    }

    class DefaultPrintedComponentProvider : IComponentProvider {
        override fun appendBody(tooltip: MutableList<Text>, accessor: IDataAccessor, config: IPluginConfig) {
            val printable = BlockComponents.get(ImagesComponents.PRINTABLE, accessor.blockEntity) ?: return
            printable.print.appendTooltip(tooltip, accessor.world, accessor.player.isSneaking, 450)
        }
    }

    class PictureFrameComponentProvider : IComponentProvider {
        override fun appendBody(tooltip: MutableList<Text>, accessor: IDataAccessor, config: IPluginConfig) {
            val masterBlockEntity = (accessor.blockEntity as PictureFrameBlockEntity).getMaster(accessor.side)
            if (masterBlockEntity != null) {
                val face = masterBlockEntity.faces[accessor.side] as PictureFrameBlockEntity.Face.Master
                face.print.appendTooltip(tooltip, accessor.world, accessor.player.isSneaking, 450)
            }
        }
    }
}