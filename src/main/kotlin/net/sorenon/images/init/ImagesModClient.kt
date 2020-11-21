package net.sorenon.images.init

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.particle.DustParticleEffect
import net.sorenon.images.ImagesAPIImpl
import net.sorenon.images.content.PictureFrameRenderer
import net.sorenon.images.content.WallpaperRenderer
import net.sorenon.images.content.gui.SelectImageScreen
import org.lwjgl.system.MemoryUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ImagesModClient : ClientModInitializer {
    companion object {
        val imageDB = ImagesAPIImpl()

        var CFG_MAX_SIZE = 1024 * 1024 * 2
        var CFG_IDLE_TICKS = 2 * 60 * 20

        fun reloadConfig() {
            val configDir = FabricLoader.getInstance().configDirectory

            if (!configDir.exists()) {
                if (!configDir.mkdir()) {
                    ImagesMod.LOGGER.warn("[Images] Could not create configuration directory: " + configDir.absolutePath)
                }
            }

            val configFile = File(configDir, "images.properties")
            val properties = Properties()

            if (configFile.exists()) {
                try {
                    FileInputStream(configFile).use { stream -> properties.load(stream) }
                } catch (e: IOException) {
                    ImagesMod.LOGGER.warn(
                        "[Images] Could not read property file '" + configFile.absolutePath + "'",
                        e
                    )
                }
            }

            CFG_MAX_SIZE = max((properties["maxImageSize"] as? String)?.toIntOrNull() ?: 1024 * 1024 * 2, 0)
            CFG_IDLE_TICKS = max((properties["imageIdleTicks"] as? String)?.toIntOrNull() ?: 2 * 60 * 20, 0)
            properties["maxImageSize"] = CFG_MAX_SIZE.toString()
            properties["imageIdleTicks"] = CFG_IDLE_TICKS.toString()

            try {
                FileOutputStream(configFile).use { stream -> properties.store(stream,
                    "Images properties file" +
                        "\nmaxImageSize: The maximum allowed file size for an image in bytes (Default 2MB)" +
                        "\nimageIdleTicks: The amount time of not being used until an image is deleted in ticks (Default 2min)") }
            } catch (e: IOException) {
                ImagesMod.LOGGER.warn("[Images] Could not store property file '" + configFile.absolutePath + "'", e)
            }
        }
    }

    override fun onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(ImagesMod.WALLPAPER_BLOCK_ENTITY) { d -> WallpaperRenderer(d) }
        BlockEntityRendererRegistry.INSTANCE.register(ImagesMod.PICTURE_FRAME_BLOCK_ENTITY) { d ->
            PictureFrameRenderer(
                d
            )
        }

        ClientSidePacketRegistry.INSTANCE.register(ImagesMod.S2C_OPEN_SCREEN) { context, buffer ->
            val id = buffer.readString()
            context.taskQueue.execute {
                MinecraftClient.getInstance().openScreen(SelectImageScreen(id))
            }
        }

        ClientSidePacketRegistry.INSTANCE.register(ImagesMod.S2C_PRINT_BOOM) { context, buffer ->
            val id = buffer.readInt()
            context.taskQueue.execute {
                val client = MinecraftClient.getInstance()
                val entity = client.world?.getEntityById(id)
                if (entity != null) {
                    client.particleManager.addEmitter(entity, DustParticleEffect(1.0f, 0.0f, 0.0f, 1.0f))
                    client.particleManager.addEmitter(entity, DustParticleEffect(1.0f, 1.0f, 0.0f, 1.0f))
                    client.particleManager.addEmitter(entity, DustParticleEffect(0.0f, 0.0f, 1.0f, 1.0f))
                }
            }
        }
        reloadConfig()
    }
}