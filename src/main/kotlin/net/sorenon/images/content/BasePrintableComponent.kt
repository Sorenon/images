package net.sorenon.images.content

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import net.minecraft.block.entity.BannerBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.sorenon.images.api.Print
import net.sorenon.images.api.PrintableComponent
import net.sorenon.images.init.ImagesComponents

open class BasePrintableComponent(private val parent: Any) : PrintableComponent, AutoSyncedComponent {
    var print111 = Print()

    override fun readFromNbt(compoundTag: CompoundTag) {
        print111.deserialize(compoundTag)
    }

    override fun writeToNbt(compoundTag: CompoundTag) {
        print111.serialize(compoundTag)
    }

    override fun getPrint(): Print {
        return print111
    }

    override fun setPrint(print: Print): Boolean {
        assert(parent is BannerBlockEntity)
        ImagesComponents.PRINTABLE.sync(parent)
        this.print111 = print
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasePrintableComponent

        if (print111.url != other.print111.url) return false
        if (print111.player != other.print111.player) return false

        return true
    }

    override fun hashCode(): Int {
        return print111.url.hashCode() xor print111.player.hashCode()
    }
}