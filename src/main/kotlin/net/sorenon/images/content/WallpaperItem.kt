package net.sorenon.images.content

import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.util.ActionResult

class WallpaperItem(block: Block?, settings: Settings?) : BlockItem(block, settings) {
    override fun place(context: ItemPlacementContext): ActionResult {
        val result = super.place(context)
        if (result.isAccepted) {
            val be = context.world.getBlockEntity(context.blockPos) as WallpaperBlockEntity

            val side = be.getOrMakeFace(context.side)
            if (context.side.horizontal == -1) {
                side.rotation = context.playerFacing.opposite
            }
        }
        return result
    }
}