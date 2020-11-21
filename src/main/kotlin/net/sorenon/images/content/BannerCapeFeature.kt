package net.sorenon.images.content

import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.AbstractBannerBlock
import net.minecraft.block.entity.BannerBlockEntity
import net.minecraft.client.model.ModelPart
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer
import net.minecraft.client.render.entity.PlayerModelPart
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.MathHelper
import net.sorenon.images.accessor.Lemon
import net.sorenon.images.init.ImagesMod
import java.net.MalformedURLException
import java.net.URL

class BannerCapeFeature(context: FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>?) :
    FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>(context) {

    private val capeBannerModels = arrayOf(ModelPart(this.contextModel, 0, 0), ModelPart(this.contextModel, 0, 0))

    init {
        capeBannerModels[0].setTextureSize(32, 32)
        capeBannerModels[1].setTextureSize(32, 32)
        capeBannerModels[0].addCuboid(-5.0f, 0.0f, -1.5f, 10.0f, 20.0f, 0.5f, 0.0f)
        capeBannerModels[1].addCuboid(-5.0f, 0.0f, -1.0f, 10.0f, 20.0f, 0.5f, 0.0f)

        // BORDGOR CAPE!! capeModel.addCuboid(-5.0f, 0.0f, -2.0f, 10.0f, 20.0f, 0.5f, 0.0f)
    }

    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        entity: AbstractClientPlayerEntity,
        limbAngle: Float,
        limbDistance: Float,
        tickDelta: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        if (entity.canRenderCapeTexture() && !entity.isInvisible && entity.isPartVisible(
                PlayerModelPart.CAPE
            )
            && ImagesMod.isTrinketsInstalled()) {
            val stack: ItemStack = TrinketsApi.getTrinketComponent(entity).getStack(SlotGroups.CHEST, Slots.CAPE)
            val item = stack.item
            if (item is BlockItem && item.block is AbstractBannerBlock) {
                //START: mojang code
                matrices.push()
                matrices.translate(0.0, 0.0, 0.125)
                val d = MathHelper.lerp(
                    tickDelta.toDouble(),
                    entity.prevCapeX,
                    entity.capeX
                ) - MathHelper.lerp(tickDelta.toDouble(), entity.prevX, entity.getX())
                val e = MathHelper.lerp(
                    tickDelta.toDouble(),
                    entity.prevCapeY,
                    entity.capeY
                ) - MathHelper.lerp(tickDelta.toDouble(), entity.prevY, entity.getY())
                val m = MathHelper.lerp(
                    tickDelta.toDouble(),
                    entity.prevCapeZ,
                    entity.capeZ
                ) - MathHelper.lerp(tickDelta.toDouble(), entity.prevZ, entity.getZ())
                val entityYaw: Float = entity.prevBodyYaw + (entity.bodyYaw - entity.prevBodyYaw)
                val o = MathHelper.sin(entityYaw * 0.017453292f).toDouble()
                val p = (-MathHelper.cos(entityYaw * 0.017453292f)).toDouble()
                var q = e.toFloat() * 10.0f
                q = MathHelper.clamp(q, -6.0f, 32.0f)
                var r = (d * o + m * p).toFloat() * 100.0f
                r = MathHelper.clamp(r, 0.0f, 150.0f)
                var s = (d * p - m * o).toFloat() * 100.0f
                s = MathHelper.clamp(s, -20.0f, 20.0f)
                val t =
                    MathHelper.lerp(
                        tickDelta,
                        entity.prevStrideDistance,
                        entity.strideDistance
                    )
                q += MathHelper.sin(
                    (MathHelper.lerp(
                        tickDelta,
                        entity.prevHorizontalSpeed,
                        entity.horizontalSpeed
                    ) * 6.0f)
                ) * 32.0f * t
                if (entity.isInSneakingPose) {
                    q += 25.0f
                }
                matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(6.0f + r / 2.0f + q))
                matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(s / 2.0f))
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0f - s / 2.0f))
                //END: mojang code

                val color = (item.block as AbstractBannerBlock).color
                val list = BannerBlockEntity.getPatternListTag(stack)
                val tag = stack.getSubTag("BlockEntityTag")
                if (tag != null && tag.contains("sorenon_imageURL", NbtType.STRING)) {
                    try {
                        Lemon.latestBanner = URL(tag.getString("sorenon_imageURL"))
                    } catch (exception: MalformedURLException) {
                        Lemon.latestBanner = null
                    }
                }

                for (capeModel in capeBannerModels) {
                    capeModel.pivotY = this.contextModel.cape.pivotY
                    capeModel.pivotZ = this.contextModel.cape.pivotZ
                    BannerBlockEntityRenderer.renderCanvas(
                        matrices,
                        vertexConsumers,
                        light,
                        OverlayTexture.DEFAULT_UV,
                        capeModel,
                        ModelLoader.BANNER_BASE,
                        true,
                        BannerBlockEntity.method_24280(color, list),
                        false
                    )
                }

                matrices.pop()
            }
        }
    }
}