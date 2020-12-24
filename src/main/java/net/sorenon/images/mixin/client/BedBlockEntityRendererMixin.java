package net.sorenon.images.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.render.block.entity.LightmapCoordinatesRetriever;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Matrix4f;
import net.sorenon.images.api.DownloadedImage;
import net.sorenon.images.api.ImagesApi;
import net.sorenon.images.content.BedPrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.MalformedURLException;
import java.net.URL;

@Mixin(BedBlockEntityRenderer.class)
abstract class BedBlockEntityRendererMixin {

    @Inject(at = @At("RETURN"), method = "render")
    void render(BedBlockEntity blockEntity, float f, MatrixStack matrix, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        int light;
        float rotation;
        URL url;
        if (blockEntity.hasWorld()) {
            BlockState blockState = blockEntity.getCachedState();
            if (blockState.get(BedBlock.PART) != BedPart.HEAD) return;
            url = BlockComponents.get(ImagesComponents.getPRINTABLE(), blockEntity).getPrint().url;
            if (url == null) return;
            DoubleBlockProperties.PropertySource<? extends BedBlockEntity> propertySource = DoubleBlockProperties.toPropertySource(BlockEntityType.BED, BedBlock::getBedPart, BedBlock::getOppositePartDirection, ChestBlock.FACING, blockState, blockEntity.getWorld(), blockEntity.getPos(), (worldAccess, blockPos) -> false);
            light = propertySource.apply(new LightmapCoordinatesRetriever<BedBlockEntity>()).get(i);
            rotation = blockState.get(BedBlock.FACING).asRotation();
        } else {
            light = i;
            rotation = 0.0f;
            BedPrintableComponent bedPrintableComponent = (BedPrintableComponent) BlockComponents.get(ImagesComponents.getPRINTABLE(), blockEntity);
            if (bedPrintableComponent.getPrintRaw() == null || bedPrintableComponent.getPrintRaw().url == null)
                return;
            url = bedPrintableComponent.getPrintRaw().url;
        }

        matrix.push();
        matrix.translate(0.0D, 0.5625D, 0.0D);
        matrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90.0F));
        matrix.translate(0.5D, 0.5D, 0.5D);
        matrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F + rotation));
        matrix.translate(-0.5D, -0.5D, -0.5D);
        matrix.translate(0.0, 8.0 / 16.0, 0.0);

        DownloadedImage image = ImagesApi.getInstance().getImageOrPlaceholder(url);

        float canvasWidth = 24.0f / 16.0f;
        float canvasHeight = 28.0f / 16.0f;

        float height = image.getHeight();
        float width = image.getWidth();
        float scale = Math.min(canvasWidth / width, canvasHeight / height);
        float imgWidth = width * scale;
        float imgHeight = height * scale;

        float startX = (canvasWidth - imgWidth) / 2.0f;
        float startY = (canvasHeight - imgHeight) / 2.0f;
        float endX = canvasWidth - startX;
        float endY = canvasHeight - startY;

        float rightStartX = 20.0f / 16;
        float leftEndX = 4.0f / 16;

        float offset = 0.1f / 16;

        float topImgWidth = Math.min(imgWidth, 1.0f);
        float startU = (1 - topImgWidth / imgWidth) / 2;
        float endU = 1 - startU;

        float topImgHeight = Math.min(endY, 24.0f / 16);
        float endV = topImgHeight / endY;

        Vector3f normal = new Vector3f(0, 0, -1);

        RenderSystem.disableCull();
        matrix.push(); //Start top
        matrix.translate(-4.0 / 16, 0.0, -offset);
        Matrix4f matrix4f = matrix.peek().getModel();
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(image.getTexture()));
        normal.transform(matrix.peek().getNormal());
        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, topImgHeight + offset, 0.0f).color(255, 255, 255, 255).texture(endU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, startY, 0.0f).color(255, 255, 255, 255).texture(endU, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, startY, 0.0f).color(255, 255, 255, 255).texture(startU, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, topImgHeight + offset, 0.0f).color(255, 255, 255, 255).texture(startU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        matrix.pop(); //End top

        matrix.push(); //Start right (top down)
        matrix.translate(1.0f + offset, 0.0, -rightStartX);
        matrix4f = matrix.peek().getModel();
        float startXR = Math.max(startX, rightStartX);
        normal.set(1, 0, 0);
        normal.transform(matrix.peek().getNormal());
        vertexConsumer.vertex(matrix4f, 0.0f, topImgHeight + offset, endX).color(255, 255, 255, 255).texture(1.0f, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, endX).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, startXR - offset).color(255, 255, 255, 255).texture(endU, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, topImgHeight + offset, startXR - offset).color(255, 255, 255, 255).texture(endU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        matrix.pop(); //End right

        matrix.push(); //Start left (top down)
        matrix.translate(-offset, 0.0, 0.0);
        matrix4f = matrix.peek().getModel();
        float startXL = Math.max(endX - startXR, -offset);
        normal.set(-1, 0, 0);
        normal.transform(matrix.peek().getNormal());
        vertexConsumer.vertex(matrix4f, 0.0f, topImgHeight + offset, -offset).color(255, 255, 255, 255).texture(startU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, -offset).color(255, 255, 255, 255).texture(startU, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, startXL).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, 0.0f, topImgHeight + offset, startXL).color(255, 255, 255, 255).texture(0.0f, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        matrix.pop(); //End left

        matrix.push(); //Start bottom
        matrix.translate(-4.0 / 16, 24.0f / 16 + offset, -offset);
        matrix4f = matrix.peek().getModel();
        normal.set(0, 1, 0);
        normal.transform(matrix.peek().getNormal());
        float z = Math.max(imgHeight - topImgHeight + offset, 0);
        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, 0.0f, z).color(255, 255, 255, 255).texture(endU, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, 0.0f, startY).color(255, 255, 255, 255).texture(endU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, 0.0f, startY).color(255, 255, 255, 255).texture(startU, endV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, 0.0f, z).color(255, 255, 255, 255).texture(startU, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal.getX(), normal.getY(), normal.getZ()).next();
        matrix.pop(); //End bottom

        RenderSystem.enableCull();

        matrix.pop();


 /*
        matrix.push();
        matrix.translate(0.0D, 0.5625D, 0.0D);
        matrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90.0F));
        matrix.translate(0.5D, 0.5D, 0.5D);
        matrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F + rotation));
        matrix.translate(-0.5D, -0.5D, -0.5D);
        matrix.translate(0.0, 8.0 / 16.0, 0.0);

        DownloadedImage image = ImagesApi.getInstance().getImageOrPlaceholder(url);

        float canvasWidth = 24.0f / 16.0f;
        float canvasHeight = 24.0f / 16.0f;

        float height = image.getHeight();
        float width = image.getWidth();
        float scale = Math.min(canvasWidth / width, canvasHeight / height);
        float imgWidth = width * scale;
        float imgHeight = height * scale;

        float startX = (canvasWidth - imgWidth) / 2.0f;
        float startY = (canvasHeight - imgHeight) / 2.0f;
        float endX = canvasWidth - startX;
        float endY = canvasHeight - startY;

        float rightStartX = 20.0f / 16;
        float leftEndX = 4.0f / 16;

        float offset = 0.1f / 16;

        RenderSystem.disableCull();
        matrix.push();
        matrix.translate(-4.0 / 16, 0.0, -offset);
        Matrix4f matrix4f = matrix.peek().getModel();
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(image.getRenderLayer());

        float topImgWidth = Math.min(imgWidth, 1.0f);
        float startU = (1 - topImgWidth / imgWidth) / 2;
        float endU = 1 - startU;

        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, endY, 0.0f).color(255, 255, 255, 255).texture(endU, 1.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, Math.min(endX, rightStartX) + offset, startY, 0.0f).color(255, 255, 255, 255).texture(endU, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, startY, 0.0f).color(255, 255, 255, 255).texture(startU, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, Math.max(startX, leftEndX) - offset, endY, 0.0f).color(255, 255, 255, 255).texture(startU, 1.0f).light(light).next();
        matrix.pop();

        matrix.push();
        matrix.translate(1.0f + offset, 0.0, -rightStartX);
        matrix4f = matrix.peek().getModel();
        float startXR = Math.max(startX, rightStartX);
        vertexConsumer.vertex(matrix4f, 0.0f, endY, endX).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, endX).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, startXR - offset).color(255, 255, 255, 255).texture(endU, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, endY, startXR - offset).color(255, 255, 255, 255).texture(endU, 1.0f).light(light).next();
        matrix.pop();

        matrix.push();
        matrix.translate(-offset, 0.0, 0.0);
        matrix4f = matrix.peek().getModel();
        float startXL = startX - offset;
        float endXL = Math.min(endX, leftEndX);
        vertexConsumer.vertex(matrix4f, 0.0f, endY, startXL).color(255, 255, 255, 255).texture(startU, 1.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, startXL).color(255, 255, 255, 255).texture(startU, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, startY, endXL).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(light).next();
        vertexConsumer.vertex(matrix4f, 0.0f, endY, endXL).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(light).next();
        matrix.pop();
        RenderSystem.enableCull();

        matrix.pop();*/
    }
}
