package net.sorenon.images.content.gui

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ScreenTexts
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget.PressAction
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.sorenon.images.init.ImagesMod

class SelectImageScreen(private val currentID: String) : Screen(TranslatableText("selectImage.title")){
    private var imgurIDWidget: TextFieldWidget? = null

    override fun init() {
        super.init()
        imgurIDWidget = TextFieldWidget(textRenderer, width / 2 - 35, height / 2 - 20, 70, 20, LiteralText(""))
        imgurIDWidget!!.text = currentID
        imgurIDWidget!!.setMaxLength(10)
        imgurIDWidget!!.setTextPredicate(ImagesMod.isValidImgurID)
        imgurIDWidget!!.setSelected(true)
        focused = imgurIDWidget

        children.add(imgurIDWidget)

        addButton(
            ButtonWidget(
                width / 2 - 100,
                height / 2 + 40,
                200,
                20,
                ScreenTexts.DONE,
                PressAction {
                    val buf = PacketByteBuf(Unpooled.buffer())
                    buf.writeString(imgurIDWidget!!.text)
                    ClientSidePacketRegistry.INSTANCE.sendToServer(ImagesMod.C2S_SET_TEXTURE, buf)
                    onClose()
                }
            )
        )
        addButton(
            ButtonWidget(
                width / 2 - 100,
                height / 2 + 40 + 24,
                200,
                20,
                ScreenTexts.CANCEL,
                PressAction { onClose() }
            )
        )
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) {
            client!!.player!!.closeHandledScreen()
        }
        return if (!this.imgurIDWidget!!.keyPressed(
                keyCode,
                scanCode,
                modifiers
            ) && !this.imgurIDWidget!!.isActive
        ) super.keyPressed(keyCode, scanCode, modifiers) else true
    }

    override fun tick() {
        super.tick()
        imgurIDWidget!!.tick()
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawCenteredText(matrices, textRenderer, title, width / 2, height / 2 - 70, 16777215) /*   :)    */
        drawCenteredText(matrices, textRenderer, LiteralText("\"https://i.imgur.com/                   .jpg\"                   "), width / 2, height / 2 - 15, 16777215)
        imgurIDWidget!!.render(matrices, mouseX, mouseY, delta)
    }
}