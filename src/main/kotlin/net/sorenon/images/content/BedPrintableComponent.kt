package net.sorenon.images.content

import dev.onyxstudios.cca.api.v3.block.BlockComponents
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BedBlockEntity
import net.minecraft.block.enums.BedPart
import net.minecraft.nbt.CompoundTag
import net.sorenon.images.api.Print
import net.sorenon.images.api.PrintableComponent
import net.sorenon.images.init.ImagesComponents

class BedPrintableComponent(val blockEntity: BedBlockEntity) : PrintableComponent, AutoSyncedComponent {

    private var print: Print? = null

    override fun readFromNbt(tag: CompoundTag) {
        if (tag.size > 0) {
            print = Print()
            print!!.deserialize(tag)
        }
    }

    override fun writeToNbt(tag: CompoundTag) {
        if (print != null) {
            print!!.serialize(tag)
        }
    }

    override fun getPrint(): Print {
        if (isHead()) {
            return if (print == null) Print()
            else print!!
        }

        val bedBlock = getHead() ?: return Print()
        val component = BlockComponents.get(ImagesComponents.PRINTABLE, bedBlock) as? BedPrintableComponent
            ?: return Print()
        return component.getPrint()
    }

    override fun setPrint(print: Print): Boolean {
        if (isHead()) {
            this.print = print
            ImagesComponents.PRINTABLE.sync(blockEntity)
        }

        val component = BlockComponents.get(ImagesComponents.PRINTABLE, getHead() ?: return false) ?: return false
        if (component is BedPrintableComponent && component.isHead()) {
            return component.setPrint(print)
        }
        return false
    }

    private fun getHead(): BedBlockEntity? {
        return blockEntity.world?.getBlockEntity(blockEntity.pos.offset(blockEntity.cachedState.get(BedBlock.FACING))) as? BedBlockEntity
    }

    fun isHead(): Boolean {
        val world = blockEntity.world ?: return false
        val state: BlockState = world.getBlockState(blockEntity.pos)
        return state[BedBlock.PART] == BedPart.HEAD
    }

    fun getPrintRaw(): Print? {
        return print
    }

    fun setPrintRaw(print: Print?) {
        this.print = print
    }
}