package net.sorenon.images.mixin.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.sorenon.images.accessor.BannerMixinAccessor;
import net.sorenon.images.accessor.Lemon;
import net.sorenon.images.api.DownloadedImage;
import net.sorenon.images.api.ImagesApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BannerBlockEntityRenderer.class)
abstract class BannerBlockEntityRendererMixin {

    @Unique
    private static final RenderLayer DEPTH_ONLY = new RenderLayer.MultiPhase("images_depth_only", VertexFormats.POSITION, 7, 256, false, false, RenderLayer.MultiPhaseParameters.builder().writeMaskState(RenderPhase.DEPTH_MASK).build(false));

    @Unique
    private static RenderLayer getUniqueRenderLayer(Identifier texture) {
        return new RenderLayer.MultiPhase("images_background_texture",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                7,
                256,
                false,
                false,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false, false))
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP).diffuseLighting(RenderPhase.ENABLE_DIFFUSE_LIGHTING).writeMaskState(RenderPhase.COLOR_MASK).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(
                        false
                ));
    }

    @Inject(at = @At("HEAD"), method = "renderCanvas", cancellable = true)
    private static void inject_renderCanvas(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, ModelPart canvas, SpriteIdentifier baseSprite, boolean isBanner, List<Pair<BannerPattern, DyeColor>> patterns, boolean bl, CallbackInfo ci) {
        if (Lemon.latestBanner != null && vertexConsumers instanceof VertexConsumerProvider.Immediate) {
            DownloadedImage image = ImagesApi.getInstance().getImageOrPlaceholder(Lemon.latestBanner);
            Lemon.latestBanner = null;

            /*
                This is all rather hacky atm but it's the only reliable way I can think of to render all the layers without causing artifacts or performance issues
             */

            RenderLayer renderLayer = getUniqueRenderLayer(baseSprite.getAtlasId());
            canvas.render(matrices, baseSprite.getSprite().getTextureSpecificVertexConsumer(ItemRenderer.getDirectItemGlintConsumer(vertexConsumers, renderLayer, true, bl)), light, overlay);
//            ((VertexConsumerProvider.Immediate) vertexConsumers).draw(renderLayer); //Not necessary but making it explicit makes the rendering code easier to understand

            for (int i = 0; i < 17 && i < patterns.size(); ++i) {
                Pair<BannerPattern, DyeColor> pair = patterns.get(i);
                float[] fs = pair.getSecond().getColorComponents();
                SpriteIdentifier spriteIdentifier = new SpriteIdentifier(isBanner ? TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE : TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, pair.getFirst().getSpriteId(isBanner));
                canvas.render(matrices, spriteIdentifier.getVertexConsumer(vertexConsumers, BannerBlockEntityRendererMixin::getUniqueRenderLayer), light, overlay, fs[0], fs[1], fs[2], 1.0F);
            }

            renderLayer = image.getRenderLayer();
            renderModelPartWithImage(canvas, matrices, vertexConsumers.getBuffer(renderLayer), light, overlay, isBanner, image);
//            renderModelPartWithImage(canvas, matrices, vertexConsumers.getBuffer(getUniqueSpriteRenderLayer(image.getTexture())), light, overlay, isBanner, image);
            ((VertexConsumerProvider.Immediate) vertexConsumers).draw(renderLayer); //Not necessary but making it explicit makes the rendering code easier to understand


            renderLayer = DEPTH_ONLY;
            canvas.render(matrices, vertexConsumers.getBuffer(renderLayer), light, overlay);
            ((VertexConsumerProvider.Immediate) vertexConsumers).draw(renderLayer); //Necessary as it could be the final draw call
            ci.cancel();
        }
    }

    @Unique
    private static void renderModelPartWithImage(ModelPart canvas, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, boolean isBanner, DownloadedImage image) {
        matrices.push();
        canvas.rotate(matrices);
        Matrix3f matrix3f = matrices.peek().getNormal();

        for (ModelPart.Cuboid cuboid : canvas.cuboids) {
            for (ModelPart.Quad quad : cuboid.sides) {
                if (quad.direction.equals(Vector3f.NEGATIVE_Z) || isBanner && quad.direction.equals(Vector3f.POSITIVE_Z)) {
                    Vector3f vector3f = quad.direction.copy();
                    vector3f.transform(matrix3f);
                    float f = vector3f.getX();
                    float g = vector3f.getY();
                    float h = vector3f.getZ();

                    float sizeX = (cuboid.maxX - cuboid.minX) / 16;
                    float sizeY = (cuboid.maxY - cuboid.minY) / 16;

                    float height = image.getHeight();
                    float width = image.getWidth();
                    float scale = Math.min(sizeX / width, sizeY / height);
                    float imgWidth = width * scale;
                    float imgHeight = height * scale;
                    float startX = (sizeX - imgWidth) / 2.0f;
                    float startY = (sizeY - imgHeight) / 2.0f;
                    if (!isBanner) {
                        startX += 1 / 16.0f;
                        startY += 1 / 16.0f;
                    }

                    float endX = sizeX - startX;
                    float endY = sizeY - startY;

                    matrices.push();
                    matrices.translate(cuboid.minX / 16, cuboid.minY / 16, cuboid.minZ / 16);
                    if (!isBanner) {
                        matrices.translate(0, 0, 0.005);
                    }
                    Matrix4f matrix4f2 = matrices.peek().getModel();
                    matrices.pop();
                    vertexConsumer.vertex(matrix4f2, endX, startY, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(overlay).light(light).normal(f, g, h).next();
                    vertexConsumer.vertex(matrix4f2, startX, startY, 0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(overlay).light(light).normal(f, g, h).next();
                    vertexConsumer.vertex(matrix4f2, startX, endY, 0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(overlay).light(light).normal(f, g, h).next();
                    vertexConsumer.vertex(matrix4f2, endX, endY, 0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(overlay).light(light).normal(f, g, h).next();

//                    float[] u = {1, 0, 0, 1};
//                    float[] v = {0, 0, 1, 1};
//                    for (int i = 0; i < 4; ++i) {
//                        ModelPart.Vertex vertex = quad.vertices[i];
//                        float x = vertex.pos.getX() / 16.0F;
//                        float y = vertex.pos.getY() / 16.0F;
//                        float z = vertex.pos.getZ() / 16.0F;
//                        if (!isBanner) {
//                            x -= Math.copySign(1 / 16.0f, x);
//                            y -= Math.copySign(1 / 16.0f, y);
//                            z -= 0.1 / 16.0f;
//                        }
//
//                        Vector4f vector4f = new Vector4f(x, y, z, 1.0F);
//                        vector4f.transform(matrix4f);
//                        vertexConsumer.vertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), 1.0f, 1.0f, 1.0f, 1.0f, u[i], v[i], overlay, light, f, g, h);
//                    }
                }
            }
        }

        matrices.pop();
    }

    @Inject(at = @At("HEAD"), method = "render")
    void inject_render(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        if (bannerBlockEntity instanceof BannerMixinAccessor) {
            Lemon.latestBanner = ((BannerMixinAccessor) bannerBlockEntity).getURL();
        }
    }
}
