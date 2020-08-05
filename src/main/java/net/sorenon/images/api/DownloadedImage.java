package net.sorenon.images.api;

import net.minecraft.util.Identifier;

public interface DownloadedImage {

    /**
     * Returns the identifier to be used with RenderLayer.getText(**)
     */
    Identifier getTextureID();

    int getHeight();

    int getWidth();

    default boolean isPlaceholder() {
        return getPlaceholderType() != ImagesApi.ImageState.LOADED;
    }

    /**
     * What ImageState does this image represent if it is a placeholder
     */
    ImagesApi.ImageState getPlaceholderType();
}
