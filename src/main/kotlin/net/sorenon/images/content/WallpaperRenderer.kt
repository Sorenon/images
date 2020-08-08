package net.sorenon.images.content

import net.minecraft.block.BlockState
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Matrix4f
import net.sorenon.images.api.ImagesApi
import net.sorenon.images.api.DownloadedImage
import net.sorenon.images.init.ImagesMod
import kotlin.math.max
import kotlin.math.min

class WallpaperRenderer(dispatcher: BlockEntityRenderDispatcher?) : BlockEntityRenderer<WallpaperBlockEntity>(dispatcher) {
    private val nullTexture = Identifier("textures/block/item_frame.png")
    private val voidTexture = Identifier("textures/item/structure_void.png")

    override fun render(
        entity: WallpaperBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        lightIn: Int,
        overlay: Int
    ) {
        var light = lightIn
        val blockState: BlockState = entity.cachedState ?: ImagesMod.WALLPAPER_BLOCK.defaultState

        for (pair in ImageBlock.FACING_PROPERTIES) {
            if (blockState.get(pair.value)) {
                val blockFace = pair.key
                val pitch = when (blockFace) {
                    Direction.DOWN -> 90.0f
                    Direction.UP -> 270.0f
                    else -> 0.0f
                }

                val face = entity.getOrMakeFace(blockFace)
                val yaw = if (blockFace.axis == Direction.Axis.Y) {
                    face.rotation.asRotation()
                } else blockFace.asRotation()

                renderFace(face, matrices, vertexConsumers, light, yaw, pitch)
            }
        }
    }

    private fun renderFace(
        face: WallpaperBlockEntity.Face,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        yaw: Float,
        pitch: Float
    ) {
        matrices.push()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-yaw))
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(pitch))
        matrices.translate(-0.5, -0.5, -0.5 + 0.01f / 16.0f)

//        val matrix4f = matrices.peek().model
//        val imageID = face.imageID
//        if (imageID == null) {
//            renderSimpleQuad(vertexConsumers, nullTexture, matrix4f, light)
//        }
//        else {
//            val image = ImagesApi.getInstance().getImageOrPlaceholder(imageID)
//            renderImage(face, image, vertexConsumers, matrix4f, light)
//        }

        matrices.pop()
    }

    private fun renderSimpleQuad(
        vertexConsumers: VertexConsumerProvider,
        texture: Identifier,
        matrix4f: Matrix4f,
        light: Int
    ) {
        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(texture))
        vertexConsumer.vertex(matrix4f, 0.0f, 0.0f, 0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, 1.0f, 0.0f, 0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, 1.0f, 1.0f, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, 0.0f, 1.0f, 0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f)
            .light(light).next()
    }

    private fun renderImage(
        face: WallpaperBlockEntity.Face,
        netClientImage: DownloadedImage,
        vertexConsumers: VertexConsumerProvider,
        matrix4f: Matrix4f,
        light: Int
    ) {
        val vertexConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(netClientImage.textureID))
        val height = netClientImage.height.toDouble()
        val width = netClientImage.width.toDouble()

        val sizeX = face.xSize
        val sizeY = face.ySize
        val scale = min(sizeX / width, sizeY / height)
        val imgWidth = width * scale
        val imgHeight = height * scale
        val borderX = (sizeX - imgWidth) / 2.0f
        val borderY = (sizeY - imgHeight) / 2.0f

        val startX = max(face.uStart.toDouble(), borderX)
        val endX = min(face.uStart + 1.0, sizeX - borderX)
        if (startX >= endX) {
            if (netClientImage.isPlaceholder) {
                renderSimpleQuad(vertexConsumers, netClientImage.textureID, matrix4f, light)
            }
            else {
                renderSimpleQuad(vertexConsumers, voidTexture, matrix4f, light)
            }
            return
        }

        val startY = max(face.vStart.toDouble(), borderY)
        val endY = min(face.vStart + 1.0, sizeY - borderY)
        if (startY >= endY) {
            if (netClientImage.isPlaceholder) {
                renderSimpleQuad(vertexConsumers, netClientImage.textureID, matrix4f, light)
            }
            else {
                renderSimpleQuad(vertexConsumers, voidTexture, matrix4f, light)
            }
            return
        }

        val u0 = ((startX - borderX) / imgWidth).toFloat()
        val u1 = ((endX - borderX) / imgWidth).toFloat()
        val v0 = ((startY - borderY) / imgHeight).toFloat()
        val v1 = ((endY - borderY) / imgHeight).toFloat()

        val subX = startX - startX % 1
        val subY = startY - startY % 1

        val x0 = (startX - subX).toFloat()
        val x1 = (endX - subX).toFloat()
        val y0 = (startY - subY).toFloat()
        val y1 = (endY - subY).toFloat()

        vertexConsumer.vertex(matrix4f, x0, 1 - y1, 0.0f).color(255, 255, 255, 255).texture(u0, v1)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x1, 1 - y1, 0.0f).color(255, 255, 255, 255).texture(u1, v1)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x1, 1 - y0, 0.0f).color(255, 255, 255, 255).texture(u1, v0)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x0, 1 - y0, 0.0f).color(255, 255, 255, 255).texture(u0, v0)
            .light(light).next()
    }

    /*
     private fun renderImage(
        face: ImageBlockEntity.Face,
        netClientImage: ClientImageDB.NetImage,
        vertexConsumers: VertexConsumerProvider,
        matrix4f: Matrix4f,
        light: Int
    ) {
        val vertexConsumer: VertexConsumer = vertexConsumers.getBuffer(netClientImage.cachedRenderLayer)
        val height = netClientImage.image!!.height.toDouble()
        val width = netClientImage.image!!.width.toDouble()

        val sizeX = face.xSize
        val sizeY = face.ySize
        val scale = min(sizeX / width, sizeY / height)
        val imgWidth = width * scale
        val imgHeight = height * scale
        val borderX = (sizeX - imgWidth) / 2.0f
        val borderY = (sizeY - imgHeight) / 2.0f

        val startX = max(face.uStart.toDouble(), borderX)
        val endX = min(face.uStart + 1.0, sizeX - borderX)
        if (startX >= endX) {
            renderSimpleQuad(vertexConsumers, imageDB.voidTexture, matrix4f, light)
            return
        }

        val startY = max(face.vStart.toDouble(), borderY)
        val endY = min(face.vStart + 1.0, sizeY - borderY)
        if (startY >= endY) {
            renderSimpleQuad(vertexConsumers, imageDB.voidTexture, matrix4f, light)
            return
        }

        val u0 = ((startX - borderX) / imgWidth).toFloat()
        val u1 = ((endX - borderX) / imgWidth).toFloat()
        val v0 = ((startY - borderY) / imgHeight).toFloat()
        val v1 = ((endY - borderY) / imgHeight).toFloat()

        val subX = startX - startX % 1
        val subY = startY - startY % 1

        val x0 = (startX - subX).toFloat()
        val x1 = (endX - subX).toFloat()
        val y0 = (startY - subY).toFloat()
        val y1 = (endY - subY).toFloat()

        vertexConsumer.vertex(matrix4f, x0, 1 - y1, 0.0f).color(255, 255, 255, 255).texture(u0, v1)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x1, 1 - y1, 0.0f).color(255, 255, 255, 255).texture(u1, v1)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x1, 1 - y0, 0.0f).color(255, 255, 255, 255).texture(u1, v0)
            .light(light).next()
        vertexConsumer.vertex(matrix4f, x0, 1 - y0, 0.0f).color(255, 255, 255, 255).texture(u0, v0)
            .light(light).next()
    }
     */
}