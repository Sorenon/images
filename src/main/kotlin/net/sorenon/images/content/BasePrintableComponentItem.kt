package net.sorenon.images.content

import dev.onyxstudios.cca.api.v3.item.ItemComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.sorenon.images.api.Print
import net.sorenon.images.api.PrintableComponent


class BasePrintableComponentItem(stack: ItemStack) : ItemComponent(stack), PrintableComponent {
    private var print: Print? = null

    override fun getPrint(): Print {
        if (print == null) {
            print = Print()
            if (stack.tag?.contains(rootTagKey) == true) {
                print!!.deserialize(orCreateRootTag)
            }
        }
        return print!!
    }

    override fun setPrint(print: Print): Boolean {
        this.print = print
        val tag = orCreateRootTag
        print.serialize(tag)
        return true
    }

    override fun getRootTagKey(): String {
        return "cardinal_cogdnoangado"
    }

    override fun onTagInvalidated() {
        super.onTagInvalidated() // Must call super!
        this.print = null
    }
}