package net.sorenon.images.content.gui

import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ScreenTexts
import net.minecraft.client.gui.widget.AbstractButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget.PressAction
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.sorenon.images.init.ImagesMod
import java.util.function.Consumer

class SelectImageScreenOld : Screen(TranslatableText("manageImages.title")) {
    private var list: ImagesListWidget? = null
    private var textWidgets = arrayListOf<TextFieldWidget>()

    override fun tick() {
        for (widget in textWidgets) widget.tick()
    }

    override fun init() {
        list = ImagesListWidget(this)
        list!!.addBoi("foo")
        list!!.addBoi("bar")
        list!!.addBoi("1")
        list!!.addBoi("2")
        list!!.addBoi("3")
        list!!.addBoi("4")
        list!!.addBoi("5e")
        list!!.addBoi("i5ife")
        list!!.addBoi("i m5ss5e")
        list!!.addBoi("i5e")
        list!!.addBoi("i5fe")
        this.children.add(list)

        addButton(ButtonWidget(
                width / 2 - 155 + 160,
                height - 29,
                150,
                20,
                ScreenTexts.DONE,
                PressAction { client!!.openScreen(null) }
            )
        )
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        list!!.render(matrices, mouseX, mouseY, delta)
        drawCenteredText(matrices, textRenderer, title, width / 2, 8, 16777215)

        super.render(matrices, mouseX, mouseY, delta)
    }

    /**
     * BELOW IS DUMB
     */
    class ImagesListWidget(private val parent: SelectImageScreenOld) :
        ElementListWidget<ImageEntry>(parent.client, parent.width, parent.height, 32, parent.height - 32, 25) {
        override fun getScrollbarPositionX(): Int {
            return super.getScrollbarPositionX() + 15
        }

        override fun getRowWidth(): Int {
            return super.getRowWidth() + 32
        }

        fun addBoi(name: String) {
            val startX = width / 2 - 175

            val imageIDWidget = TextFieldWidget(parent.textRenderer, startX + 150, 0, 70, 40, LiteralText(""))
            imageIDWidget.setMaxLength(16)
            imageIDWidget.setTextPredicate(ImagesMod.isValidImgurID)
            parent.children.add(imageIDWidget)
            parent.textWidgets.add(imageIDWidget)

            addEntry(ImageEntry(arrayListOf(
                ButtonWidget(startX, 0, 140, 20, LiteralText(name)) {},
                imageIDWidget,
                ButtonWidget(startX + 150 + 80, 0, 20, 20, LiteralText("X")) {}
            )))
        }

        override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
            super.render(matrices, mouseX, mouseY, delta)
        }
    }

    class ImageEntry(private val children: List<AbstractButtonWidget>) : ElementListWidget.Entry<ImageEntry>() {
        override fun render(
            matrices: MatrixStack?,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            this.children.forEach(Consumer { abstractButtonWidget: AbstractButtonWidget ->
                abstractButtonWidget.y = y
                abstractButtonWidget.render(matrices, mouseX, mouseY, tickDelta)
            })
        }

        override fun children(): List<Element> {
            return children
        }
    }
}