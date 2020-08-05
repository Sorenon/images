package net.sorenon.images.api;

import net.minecraft.util.Identifier;
import net.sorenon.images.init.ImagesModClient;
import org.jetbrains.annotations.Nullable;

public interface ImagesApi {

    static ImagesApi getInstance() {
        return ImagesModClient.Companion.getImageDB();
    }

    /**
     * Returns an image registered under the matching Identifier if it has been downloaded and decoded successfully
     */
    @Nullable
    DownloadedImage getDownloadedImage(String identifier);

    DownloadedImage getImageOrPlaceholder(String identifier);

    ImageState getImageState(String identifier);

    /**
     * Returns a square placeholder image to replace a DownloadedImage with the matching image state
     * LOADING -> loading_image.png
     * BROKEN  -> bad_image.png
     * NULL    -> MISSINGNO
     */
    DownloadedImage getPlaceholderForState(ImageState state);

    enum ImageState {
        LOADED,
        LOADING,
        BROKEN,
        NULL
    }
}
