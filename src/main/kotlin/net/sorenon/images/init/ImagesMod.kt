package net.sorenon.images.init

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.sorenon.images.content.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URI
import java.net.URL
import java.util.function.Supplier

class ImagesMod : ModInitializer {
    companion object {
        val LOGGER: Logger = LogManager.getLogger("images")

        val C2S_SET_TEXTURE = Identifier("images", "set")
        val S2C_OPEN_SCREEN = Identifier("images", "open_screen")
        val S2C_PRINT_BOOM = Identifier("images", "particles")

        val WALLPAPER_BLOCK = ImageBlock.WallPaperBlock(
            FabricBlockSettings.of(Material.SUPPORTED).strength(0.1f).noCollision()
                .sounds(
                    BlockSoundGroup(
                        1.0f,
                        1.0f,
                        SoundEvents.ENTITY_PAINTING_BREAK,
                        SoundEvents.BLOCK_WOOD_STEP,
                        SoundEvents.ENTITY_PAINTING_PLACE,
                        SoundEvents.BLOCK_WOOD_HIT,
                        SoundEvents.BLOCK_WOOD_FALL
                    )
                )
        )

        val PICTURE_FRAME_BLOCK = ImageBlock.PictureFrameBlock(
            FabricBlockSettings.of(Material.WOOD).strength(1.0F).sounds(BlockSoundGroup.WOOD)
        )

        val WALLPAPER_BLOCK_ENTITY: BlockEntityType<WallpaperBlockEntity> = BlockEntityType.Builder.create(
            Supplier { WallpaperBlockEntity() },
            WALLPAPER_BLOCK
        ).build(null)

        val PICTURE_FRAME_BLOCK_ENTITY: BlockEntityType<PictureFrameBlockEntity> = BlockEntityType.Builder.create(
            Supplier { PictureFrameBlockEntity() },
            PICTURE_FRAME_BLOCK
        ).build(null)

        val PRINTAXE_ITEM = PrintAxe(Item.Settings().group(ItemGroup.TOOLS))

        val PRINTAXE_PARTICLES = FabricParticleTypes.simple()

        fun sanitizeURL(string: String): URL? {
            return try {
                val url = URI(string).normalize().toURL()
                if (url.protocol == "http" || url.protocol == "https") {
                    URL(url.protocol, url.host, -1, url.path)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun onInitialize() {
        registerBlock(WALLPAPER_BLOCK, "wallpaper")
        registerBlock(PICTURE_FRAME_BLOCK, "picture_frame")
        registerItem(WallpaperItem(WALLPAPER_BLOCK, Item.Settings().group(ItemGroup.MISC)), "wallpaper")
        registerItem(BlockItem(PICTURE_FRAME_BLOCK, Item.Settings().group(ItemGroup.MISC)), "picture_frame")
        registerItem(PRINTAXE_ITEM, "printaxe")
        Registry.register(
            Registry.BLOCK_ENTITY_TYPE, "images:wallpaper",
            WALLPAPER_BLOCK_ENTITY
        )
        Registry.register(
            Registry.BLOCK_ENTITY_TYPE, "images:picture_frame",
            PICTURE_FRAME_BLOCK_ENTITY
        )

        ServerSidePacketRegistry.INSTANCE.register(C2S_SET_TEXTURE) { context, buffer ->
            val urlStr = buffer.readString()
            context.taskQueue.execute {
                val url = sanitizeURL(urlStr)
                if (url != null) {
                    val player = context.player
                    if (player.getStackInHand(Hand.MAIN_HAND).item is PrintAxe) {
                        player.getStackInHand(Hand.MAIN_HAND).orCreateTag.putString("url", url.toString())
                    } else if (player.getStackInHand(Hand.OFF_HAND).item is PrintAxe) {
                        player.getStackInHand(Hand.OFF_HAND).orCreateTag.putString("url", url.toString())
                    }
                }
            }
        }
    }

    private fun registerBlock(block: Block, name: String) {
        Registry.register(
            Registry.BLOCK, Identifier("images", name),
            block
        )
    }

    private fun registerItem(item: Item, name: String) {
        Registry.register(
            Registry.ITEM,
            Identifier("images", name),
            item
        )
    }
}