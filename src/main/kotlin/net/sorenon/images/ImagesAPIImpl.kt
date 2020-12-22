package net.sorenon.images

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.MissingSprite
import net.minecraft.util.Identifier
import net.sorenon.images.api.DownloadedImage
import net.sorenon.images.api.ImagesApi
import net.sorenon.images.init.ImagesMod
import net.sorenon.images.init.ImagesModClient
import java.net.URL


class ImagesAPIImpl : ImagesApi {
    companion object {
        private val nullPlaceholder =
            PlaceholderImage(MissingSprite.getMissingSpriteId(), ImagesApi.ImageState.NULL)
        private val loadingPlaceholder =
            PlaceholderImage(Identifier("images", "textures/image_placeholders/loading_image.png"), ImagesApi.ImageState.LOADING)
        private val errorPlaceholder =
            PlaceholderImage(Identifier("images", "textures/image_placeholders/bad_image.png"), ImagesApi.ImageState.BROKEN)
        private val tooBigPlaceholder =
            PlaceholderImage(Identifier("images", "textures/image_placeholders/image_too_big.png"), ImagesApi.ImageState.TOO_BIG)
    }

    private var imageCache = hashMapOf<String, ImageEnum>()
    private var ticks = 0

    /*
     * Public API start
     */
    override fun getDownloadedImage(url: URL): DownloadedImage? {
        val image = getImageEnum(url)
        if (image is ImageEnum.Loaded) {
            return image.downloadedImage
        }

        return null
    }

    override fun getImageState(url: URL): ImagesApi.ImageState {
        return when (getImageEnum(url)) {
            is ImageEnum.Loaded -> ImagesApi.ImageState.LOADED
            is ImageEnum.Loading -> ImagesApi.ImageState.LOADING
            is ImageEnum.Error -> ImagesApi.ImageState.BROKEN
            is ImageEnum.TooBig -> ImagesApi.ImageState.TOO_BIG
            null -> ImagesApi.ImageState.NULL
        }
    }

    override fun getPlaceholderForState(state: ImagesApi.ImageState): DownloadedImage {
        return when (state) {
            ImagesApi.ImageState.LOADED -> loadingPlaceholder
            ImagesApi.ImageState.LOADING -> loadingPlaceholder
            ImagesApi.ImageState.BROKEN -> errorPlaceholder
            ImagesApi.ImageState.TOO_BIG -> tooBigPlaceholder
            ImagesApi.ImageState.NULL -> nullPlaceholder
        }
    }

    /*
     * Public API end
     */

    private fun getImageEnum(url: URL): ImageEnum? {
        return getOrCreateCachedImage(url)
    }

    private fun getOrCreateCachedImage(url: URL): ImageEnum? {
        var image = imageCache[url.toString()]
        if (image == null) {
            image = ImageEnum.Loading(DownloadingImage(imageCache, url))
            imageCache[url.toString()] = image
        }
        image.lastUsed = ticks
        return image
    }

    fun tick() {
        check(RenderSystem.isOnRenderThread())

        ticks++

        val idleTicks = ImagesModClient.CFG_IDLE_TICKS
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
        object TooBig : ImageEnum()

        var lastUsed: Int = 0

        fun close() {
            check(RenderSystem.isOnRenderThread())

            when (this) {
                is Loading -> {
                    downloadingImage.close()
                }
                is Loaded -> {
                    MinecraftClient.getInstance().textureManager.destroyTexture(downloadedImage.texture)
                    ImagesMod.LOGGER.debug("Destroying texture ${downloadedImage.texture}")
                }
                else -> {

                }
            }
        }
    }
}