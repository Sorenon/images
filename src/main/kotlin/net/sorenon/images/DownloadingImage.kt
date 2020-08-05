package net.sorenon.images

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture

class DownloadingImage(var map: HashMap<String, ClientImageDB.ImageEnum>, var imgurID: String) {
    var textureIdentifier: Identifier = Identifier("images", UUID.randomUUID().toString())
    var loader: CompletableFuture<Void>
    var texture: NativeImageBackedTexture? = null
    var image: NativeImage? = null

    init {
        loader = CompletableFuture.runAsync(Runnable {
            ClientImageDB.LOGGER.debug("Downloading imgur texture with id $imgurID as $textureIdentifier")

            try {
                val bytes = URL("https://i.imgur.com/$imgurID.jpg").openStream()
                image = NativeImage.read(NativeImage.Format.ABGR, bytes)

                MinecraftClient.getInstance().execute {
                    try {
                        RenderSystem.recordRenderCall {
                            this.uploadTexture()
                        }
                    } catch (e: Exception) {
                        ClientImageDB.LOGGER.debug("Couldn't read texture from $imgurID, $e")
                        map[imgurID] = ClientImageDB.ImageEnum.Error
                    }
                }
            } catch (e: Exception) {
                ClientImageDB.LOGGER.debug("Couldn't download texture from $imgurID, $e")
                map[imgurID] = ClientImageDB.ImageEnum.Error
            }
        }, Util.getServerWorkerExecutor())
    }

    private fun uploadTexture() {
        check(RenderSystem.isOnRenderThread())

        if (!loader.isCancelled) {
            texture = NativeImageBackedTexture(image)
            MinecraftClient.getInstance().textureManager.registerTexture(textureIdentifier, texture)
            image!!.close()

            val downloadedImage = ClientImageDB.ImageEnum.Loaded(DownloadedImageImpl(image!!.width, image!!.height, textureIdentifier))
            downloadedImage.lastUsed = map[imgurID]?.lastUsed ?: 0
            map[imgurID] = downloadedImage
            ClientImageDB.LOGGER.debug("Successfully downloaded texture $textureIdentifier")
        }
    }

    fun close() {
        check(RenderSystem.isOnRenderThread())

        if (!loader.isDone) {
            loader.cancel(true)
            ClientImageDB.LOGGER.debug("Canceling download of texture $textureIdentifier")
        }

        var b = false
        if (texture != null) {
            texture!!.close()
            b = true
        }
        if (image != null) {
            image!!.close()
            b = true
        }
        if (b) {
            ClientImageDB.LOGGER.debug("Destroying texture $textureIdentifier")
        }
    }
}