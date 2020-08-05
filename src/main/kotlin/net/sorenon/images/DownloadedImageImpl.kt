package net.sorenon.images

import net.minecraft.util.Identifier
import net.sorenon.images.api.ImagesApi
import net.sorenon.images.api.DownloadedImage

data class DownloadedImageImpl(var widthIn: Int, var heightIn: Int, var identifier: Identifier):
    DownloadedImage {
    override fun getHeight(): Int {
        return heightIn
    }

    override fun getWidth(): Int {
        return widthIn
    }

    override fun getTextureID(): Identifier {
        return identifier
    }

    override fun getPlaceholderType(): ImagesApi.ImageState {
        return ImagesApi.ImageState.LOADED
    }
}