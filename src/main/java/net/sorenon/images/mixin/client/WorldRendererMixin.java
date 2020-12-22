package net.sorenon.images.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.sorenon.images.accessor.Lemon;
import net.sorenon.images.api.DownloadedImage;
import net.sorenon.images.api.ImagesApi;
import net.sorenon.images.content.ImageBlock;
import net.sorenon.images.content.PictureFrameRenderer;
import net.sorenon.images.content.PrintAxe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(at = @At(value = "HEAD"), method = "render")
    public void preRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        Objects.requireNonNull(PictureFrameRenderer.Companion.getINSTANCE()).getAlreadyDrawn().clear();
    }

    @Inject(at = @At(value = "RETURN"), method = "render")
    public void postRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        PlayerEntity player = client.player;

        if (player != null) {
            Vec3d vec3d = camera.getPos();
            double d = vec3d.getX();
            double e = vec3d.getY();
            double f = vec3d.getZ();
            VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
            VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());
            drawPrintaxeSelection(buffer, matrices, player, Hand.MAIN_HAND, d, e, f);
            drawPrintaxeSelection(buffer, matrices, player, Hand.OFF_HAND, d, e, f);
            immediate.draw();
        }
    }

    public void drawPrintaxeSelection(VertexConsumer buffer, MatrixStack matrices, PlayerEntity player, Hand hand, double d, double e, double f) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof PrintAxe) {
            PrintAxe.ItemInstance data = new PrintAxe.ItemInstance(stack);
            BlockPos pos = data.getStart();
            if (pos != null) {
                double x = pos.getX() - d;
                double y = pos.getY() - e;
                double z = pos.getZ() - f;

                Matrix4f matrix = matrices.peek().getModel();
                ImageBlock.Companion.getSHAPES().get(data.getSide()).forEachEdge((k, l, m, n, o, p) -> {
                    buffer.vertex(matrix, (float) (k + x), (float) (l + y), (float) (m + z)).color(1, 0, 0, 0.4f).next();
                    buffer.vertex(matrix, (float) (n + x), (float) (o + y), (float) (p + z)).color(1, 0, 0, 0.4f).next();
                });
            }
        }
    }
}
