package net.sorenon.images.api;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public interface DownloadedImage {

    Identifier getTexture();

    default RenderLayer getRenderLayer() {
        return RenderLayer.getText(getTexture());
    }

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
