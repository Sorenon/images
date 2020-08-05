package net.sorenon.images

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.MissingSprite
import net.minecraft.util.Identifier
import net.sorenon.images.api.DownloadedImage
import net.sorenon.images.api.ImagesApi
import org.apache.logging.log4j.LogManager


class ClientImageDB : ImagesApi {
    companion object {
        private val nullPlaceholder =
            PlaceholderImage(MissingSprite.getMissingSpriteId(), ImagesApi.ImageState.NULL)
        private val loadingPlaceholder =
            PlaceholderImage(Identifier("images", "loading_image.png"), ImagesApi.ImageState.LOADING)
        private val errorPlaceholder =
            PlaceholderImage(Identifier("images", "bad_image.png"), ImagesApi.ImageState.BROKEN)

        val LOGGER = LogManager.getLogger("Images")
    }

    private var imageCache = hashMapOf<String, ImageEnum>()
    private var ticks = 0
    private var maxIdleTicks = 20 * 60 * 2

    /*
     * Public API start
     */
    override fun getDownloadedImage(identifier: String): DownloadedImage? {
        val image = getImageEnum(identifier)
        if (image is ImageEnum.Loaded) {
            return image.downloadedImage
        }

        return null
    }

    override fun getImageOrPlaceholder(identifier: String): DownloadedImage {
        if (identifier.isEmpty()) return nullPlaceholder
        return getDownloadedImage(identifier) ?: getPlaceholderForState(getImageState(identifier))
    }

    override fun getImageState(identifier: String): ImagesApi.ImageState {
        return when (getImageEnum(identifier)) {
            is ImageEnum.Loaded -> ImagesApi.ImageState.LOADED
            is ImageEnum.Loading -> ImagesApi.ImageState.LOADING
            is ImageEnum.Error -> ImagesApi.ImageState.BROKEN
            null -> ImagesApi.ImageState.NULL
        }
    }

    override fun getPlaceholderForState(state: ImagesApi.ImageState): DownloadedImage {
        return when (state) {
            ImagesApi.ImageState.LOADED -> loadingPlaceholder
            ImagesApi.ImageState.LOADING -> loadingPlaceholder
            ImagesApi.ImageState.BROKEN -> errorPlaceholder
            ImagesApi.ImageState.NULL -> nullPlaceholder
        }
    }

    /*
     * Public API end
     */

    private fun getImageEnum(identifier: String): ImageEnum? {
        return getOrCreateCachedImage(identifier)
    }

    private fun getOrCreateCachedImage(identifier: String): ImageEnum? {
        var image = imageCache[identifier]
        if (image == null) {
            if (imageCache.size < 100) {
                image = ImageEnum.Loading(DownloadingImage(imageCache, identifier))
                imageCache[identifier] = image
            } else {
                ticks += maxIdleTicks - 20 // Hacky way of cleaning the cache
            }
        }
        image?.lastUsed = ticks
        return image
    }

    fun tick() {
        check(RenderSystem.isOnRenderThread())

        ticks++

        val idleTicks = 20 * 60 * 1.5
        imageCache.values.removeIf { netImage ->
            if (netImage != ImageEnum.Error && ticks - netImage.lastUsed > idleTicks) {
                netImage.close()
                true
            } else {
                false
            }
        }
    }

    fun reload() {
        check(RenderSystem.isOnRenderThread())
        for (pair in imageCache) {
            pair.value.close()
        }
        imageCache.clear()
    }

    sealed class ImageEnum {
        data class Loading(var downloadingImage: DownloadingImage) : ImageEnum()
        data class Loaded(var downloadedImage: DownloadedImageImpl) : ImageEnum()
        object Error : ImageEnum()

        var lastUsed: Int = 0

        fun close() {
            check(RenderSystem.isOnRenderThread())

            when (this) {
                is Loading -> {
                    downloadingImage.close()
                }
                is Loaded -> {
                    MinecraftClient.getInstance().textureManager.destroyTexture(downloadedImage.textureID)
                    LOGGER.debug("Destroying texture ${downloadedImage.textureID}")
                }
                is Error -> {

                }
            }
        }
    }
}