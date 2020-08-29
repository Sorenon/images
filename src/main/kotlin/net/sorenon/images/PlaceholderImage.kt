package net.sorenon.images

import net.minecraft.util.Identifier
import net.sorenon.images.api.ImagesApi
import net.sorenon.images.api.DownloadedImage

class PlaceholderImage(val identifier: Identifier, private val type: ImagesApi.ImageState):
    DownloadedImage {
    override fun getHeight(): Int {
        return 1
    }

    override fun getWidth(): Int {
        return 1
    }

    override fun getPlaceholderType(): ImagesApi.ImageState {
        return type
    }

    override fun getTexture(): Identifier {
        return identifier
    }
}