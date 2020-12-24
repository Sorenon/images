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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.max

class ImagesModClient : ClientModInitializer {
    companion object {
        val imageDB = ImagesAPIImpl()

        var CFG_MAX_SIZE = 1024 * 1024 * 2
        var CFG_IDLE_TICKS = 2 * 60 * 20
        var CFG_WAILA_URL = true
        var CFG_WAILA_PLAYER = false
        var CFG_TOOLTIP_URL = true
        var CFG_TOOLTIP_PLAYER = true

        fun reloadConfig() {
            val configFile = FabricLoader.getInstance().configDir.resolve("images.properties").toFile()

            if (!configFile.exists()) {
                if (!configFile.createNewFile()) {
                    ImagesMod.LOGGER.warn("[Images] Could not create config file: " + configFile.absolutePath)
                }
            }

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
            CFG_WAILA_URL = (properties["wailaURL"] as? String)?.toBoolean() ?: true
            CFG_WAILA_PLAYER = (properties["wailaPlayer"] as? String)?.toBoolean() ?: false
            CFG_TOOLTIP_URL = (properties["tooltipURL"] as? String)?.toBoolean() ?: true
            CFG_TOOLTIP_PLAYER = (properties["tooltipPlayer"] as? String)?.toBoolean() ?: true
        }

        fun saveConfig() {
            val configFile = FabricLoader.getInstance().configDir.resolve("images.properties").toFile()

            if (!configFile.exists()) {
                if (!configFile.mkdir()) {
                    ImagesMod.LOGGER.warn("[Images] Could not create config file: " + configFile.absolutePath)
                }
            }

            val properties = Properties()

            properties["maxImageSize"] = CFG_MAX_SIZE.toString()
            properties["imageIdleTicks"] = CFG_IDLE_TICKS.toString()
            properties["wailaURL"] = CFG_WAILA_URL.toString()
            properties["wailaPlayer"] = CFG_WAILA_PLAYER.toString()
            properties["tooltipURL"] = CFG_TOOLTIP_URL.toString()
            properties["tooltipPlayer"] = CFG_TOOLTIP_PLAYER.toString()

            try {
                FileOutputStream(configFile).use { stream ->
                    properties.store(
                        stream,
                        "Images properties file" +
                                "\nmaxImageSize: The maximum allowed file size for an image in bytes (Default 2MB)" +
                                "\nimageIdleTicks: The amount of ticks an image can go unused before being deleted (Default 2min)"
                    )
                }
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
        saveConfig()
    }
}