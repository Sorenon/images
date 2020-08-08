package net.sorenon.images.api;

import net.sorenon.images.init.ImagesModClient;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public interface ImagesApi {

    static ImagesApi getInstance() {
        return ImagesModClient.Companion.getImageDB();
    }


    @Nullable
    DownloadedImage getDownloadedImage(URL url);

    ImageState getImageState(URL url);

    default DownloadedImage getImageOrPlaceholder(URL url) {
        DownloadedImage image = getDownloadedImage(url);
        if (image == null) {
            image = getPlaceholderForState(getImageState(url));
        }
        return image;
    }

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
