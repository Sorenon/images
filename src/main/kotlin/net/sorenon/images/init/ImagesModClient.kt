package net.sorenon.images.init

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.sorenon.images.ClientImageDB
import net.sorenon.images.content.PictureFrameRenderer
import net.sorenon.images.content.PrintAxe
import net.sorenon.images.content.WallpaperRenderer
import net.sorenon.images.content.gui.SelectImageScreen

class ImagesModClient : ClientModInitializer {
    companion object {
        val imageDB = ClientImageDB()
    }

    override fun onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ImagesMod.WALLPAPER_BLOCK_ENTITY) { d -> WallpaperRenderer(d) }
        BlockEntityRendererRegistry.INSTANCE.register(ImagesMod.PICTURE_FRAME_BLOCK_ENTITY) { d -> PictureFrameRenderer(d) }

        ClientSidePacketRegistry.INSTANCE.register(ImagesMod.S2C_OPEN_SCREEN) { context, buffer ->
            val id = buffer.readString()
            context.taskQueue.execute {
                MinecraftClient.getInstance().openScreen(SelectImageScreen(id))
            }
        }
    }
}