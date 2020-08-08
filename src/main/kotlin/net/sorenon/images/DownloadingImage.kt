package net.sorenon.images

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureUtil
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.sorenon.images.init.ImagesMod
import net.sorenon.images.init.ImagesModClient
import org.apache.commons.io.IOUtils
import org.lwjgl.system.MemoryUtil
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.min


class DownloadingImage(var map: HashMap<String, ImagesAPIImpl.ImageEnum>, var url: URL) {
    var textureIdentifier: Identifier = Identifier("images", UUID.randomUUID().toString())
    var loader: CompletableFuture<Void>
    var texture: NativeImageBackedTexture? = null
    var image: NativeImage? = null

    init {
        loader = CompletableFuture.runAsync(Runnable {
            ImagesMod.LOGGER.info("[Images] Downloading image from $url as $textureIdentifier")

            var stream: InputStream? = null
            var buffer: ByteBuffer? = null

            try {
                stream = url.openStream()
                buffer = readToBuffer(stream, ImagesModClient.CFG_MAX_SIZE) ?: return@Runnable
                buffer.position(0)

                image = NativeImage.read(NativeImage.Format.ABGR, buffer)

                MinecraftClient.getInstance().execute {
                    try {
                        RenderSystem.recordRenderCall {
                            this.uploadTexture()
                        }
                    } catch (e: Exception) {
                        ImagesMod.LOGGER.info("[Images] Couldn't read texture from $url, $e")
                        map[url.toString()] = ImagesAPIImpl.ImageEnum.Error
                    }
                }
            } catch (e: Exception) {
                ImagesMod.LOGGER.info("[Images] Couldn't download texture from $url, $e")
                map[url.toString()] = ImagesAPIImpl.ImageEnum.Error
            }
            finally {
                MemoryUtil.memFree(buffer)
                if (stream != null) IOUtils.closeQuietly(stream)
            }
        }, Util.getServerWorkerExecutor())
    }

    private fun readToBuffer(stream: InputStream, limit: Int): ByteBuffer? {
        var buffer = MemoryUtil.memAlloc(8192)
        val readableByteChannel = Channels.newChannel(stream)
        while (readableByteChannel.read(buffer) != -1) {
            if (buffer.remaining() == 0) {
                if (buffer.capacity() >= limit){
                    ImagesMod.LOGGER.info("[Images] Image $url was above the max image size (this can be increased in the config)")
                    map[url.toString()] = ImagesAPIImpl.ImageEnum.Error
                    return null
                }
                buffer = MemoryUtil.memRealloc(buffer, min(buffer.capacity() * 2, limit))
            }
        }
        return buffer
    }

    private fun uploadTexture() {
        check(RenderSystem.isOnRenderThread())

        if (!loader.isCancelled) {
            texture = NativeImageBackedTexture(image)
            MinecraftClient.getInstance().textureManager.registerTexture(textureIdentifier, texture)
            image!!.close()

            val downloadedImage =
                ImagesAPIImpl.ImageEnum.Loaded(DownloadedImageImpl(image!!.width, image!!.height, textureIdentifier))
            downloadedImage.lastUsed = map[url.toString()]?.lastUsed ?: 0
            map[url.toString()] = downloadedImage
            ImagesMod.LOGGER.info("[Images] Successfully downloaded $url")
        }
    }

    fun close() {
        check(RenderSystem.isOnRenderThread())

        if (!loader.isDone) {
            loader.cancel(true)
            ImagesMod.LOGGER.info("[Images] Canceling download of $url")
        }

        var destroyed = false
        if (texture != null) {
            texture!!.close()
            destroyed = true
        }
        if (image != null) {
            image!!.close()
            destroyed = true
        }
        if (destroyed) {
            ImagesMod.LOGGER.info("[Images] Destroying downloaded image $url")
        }
    }
}