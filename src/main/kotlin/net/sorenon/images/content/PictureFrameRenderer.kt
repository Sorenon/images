package net.sorenon.images.content

import net.minecraft.client.render.*
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Matrix4f
import net.sorenon.images.api.DownloadedImage
import net.sorenon.images.api.ImagesApi
import net.sorenon.images.mixin.RenderPhaseAccessor
import java.util.*
import kotlin.math.min

class PictureFrameRenderer(dispatcher: BlockEntityRenderDispatcher?) :
    BlockEntityRenderer<PictureFrameBlockEntity>(dispatcher) {
    companion object {
        var INSTANCE: PictureFrameRenderer? = null
    }

    @Suppress("INACCESSIBLE_TYPE")
    private val backgroundRenderLayer = RenderLayer.of(
        "images_colour", VertexFormats.POSITION_COLOR_LIGHT, 7, 256,
        RenderLayer.MultiPhaseParameters.builder().lightmap(RenderPhaseAccessor.getENABLE_LIGHTMAP()).writeMaskState(RenderPhaseAccessor.getCOLOR_MASK()).build(false)
    )

    val alreadyDrawn = hashSetOf<UUID>()

    init {
        INSTANCE = this
    }

    override fun render(
        entity: PictureFrameBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        for (pair in entity.faces) {
            val side = pair.key
            val face = pair.value
            val uuid = face.uuid
            if (alreadyDrawn.add(uuid)) {
                val masterEntity = when (face) {
                    is PictureFrameBlockEntity.Face.Master -> entity
                    is PictureFrameBlockEntity.Face.Slave -> entity.getMasterEntity(face, side)
                } ?: return

                val masterFace = masterEntity.faces[side] as PictureFrameBlockEntity.Face.Master
                val pitch = when (side) {
                    Direction.DOWN -> 90.0f
                    Direction.UP -> 270.0f
                    else -> 0.0f
                }

                matrices.push()
                if (masterEntity != entity) {
                    //Translate to the master position
                    val masterPos = masterEntity.pos
                    val slavePos = entity.pos
                    matrices.translate(
                        (masterPos.x - slavePos.x).toDouble(),
                        (masterPos.y - slavePos.y).toDouble(),
                        (masterPos.z - slavePos.z).toDouble()
                    )
                }

                val width = masterFace.width.toFloat()
                val height = masterFace.height.toFloat()
                var rotatedWidth = width
                var rotatedHeight = height

                val yaw = if (side.axis == Direction.Axis.Y) {
                    val direction = masterFace.direction ?: Direction.NORTH
                    if (direction.axis == Direction.Axis.X) {
                        rotatedWidth = height
                        rotatedHeight = width
                    }
                    direction.asRotation()
                } else side.asRotation()

                val hWidth = width / 2.0
                val hHeight = height / 2.0
                //Translate to center
                when (side.axis!!) {
                    Direction.Axis.X -> matrices.translate(0.5, hHeight, hWidth)
                    Direction.Axis.Y -> matrices.translate(hWidth, 0.5, hHeight)
                    Direction.Axis.Z -> matrices.translate(hWidth, hHeight, 0.5)
                }
                //Rotate
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-yaw))
                matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(pitch))
                //Translate to the corner
                matrices.translate(-rotatedWidth / 2.0, -rotatedHeight / 2.0, -0.5 + 1.1f / 16.0f)

                val image = ImagesApi.getInstance().getImageOrPlaceholder(masterFace.texture)
                renderFace(
                    image,
                    masterFace.colour,
                    rotatedHeight,
                    rotatedWidth,
                    light,
                    matrices.peek().model,
                    vertexConsumers
                )
                matrices.pop()
            }
        }
    }

    fun renderFace(
        image: DownloadedImage,
        colour: Int,
        sizeY: Float,
        sizeX: Float,
        light: Int,
        matrix4f: Matrix4f,
        vertexConsumers: VertexConsumerProvider
    ) {

        @Suppress("INACCESSIBLE_TYPE")
        val vertexBuilderColour: VertexConsumer = vertexConsumers.getBuffer(backgroundRenderLayer)

        val r = (0xFF0000 and colour) shr 16
        val g = (0x00FF00 and colour) shr 8
        val b = (0x0000FF and colour) shr 0

        vertexBuilderColour.vertex(matrix4f, 0.0f, 0.0f, 0.0f).color(r, g, b, 255).light(light).next()
        vertexBuilderColour.vertex(matrix4f, sizeX, 0.0f, 0.0f).color(r, g, b, 255).light(light).next()
        vertexBuilderColour.vertex(matrix4f, sizeX, sizeY, 0.0f).color(r, g, b, 255).light(light).next()
        vertexBuilderColour.vertex(matrix4f, 0.0f, sizeY, 0.0f).color(r, g, b, 255).light(light).next()

        val vertexBuilderTexture: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(image.textureID))

        val height = image.height.toFloat()
        val width = image.width.toFloat()

        val scale = min(sizeX / width, sizeY / height)
        val imgWidth = width * scale
        val imgHeight = height * scale
        val borderX = (sizeX - imgWidth) / 2.0f
        val borderY = (sizeY - imgHeight) / 2.0f

        val endX = sizeX - borderX
        val endY = sizeY - borderY

        vertexBuilderTexture.vertex(matrix4f, borderX, borderY, 0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f)
            .light(light).next()
        vertexBuilderTexture.vertex(matrix4f, endX, borderY, 0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f)
            .light(light).next()
        vertexBuilderTexture.vertex(matrix4f, endX, endY, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f)
            .light(light).next()
        vertexBuilderTexture.vertex(matrix4f, borderX, endY, 0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f)
            .light(light).next()
    }

    class RenderLayerImpl(
        name: String?,
        vertexFormat: VertexFormat?,
        drawMode: Int,
        expectedBufferSize: Int,
        startAction: Runnable?,
        endAction: Runnable?
    ) : RenderLayer(
        name,
        vertexFormat,
        drawMode,
        expectedBufferSize,
        false,
        false,
        startAction,
        endAction
    )
}