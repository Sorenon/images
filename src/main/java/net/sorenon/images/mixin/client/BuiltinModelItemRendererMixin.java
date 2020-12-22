package net.sorenon.images.mixin.client;

import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.sorenon.images.accessor.Lemon;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.content.BedPrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltinModelItemRenderer.class)
abstract class BuiltinModelItemRendererMixin {

    @Shadow
    @Final
    private BedBlockEntity renderBed;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BannerBlockEntityRenderer;renderCanvas(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/SpriteIdentifier;ZLjava/util/List;Z)V"), method = "render")
    void inject_render(ItemStack stack, ModelTransformation.Mode mode, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        //For shield
        Lemon.latestBanner = ImagesComponents.getPRINTABLE().get(stack).getPrint().url;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BedBlockEntity;setColor(Lnet/minecraft/util/DyeColor;)V"), method = "render")
    void render(ItemStack stack, ModelTransformation.Mode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        //For bed
        ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(stackComponent -> {
            PrintableComponent component = BlockComponents.get(ImagesComponents.getPRINTABLE(), renderBed);
            if (component instanceof BedPrintableComponent) {
                ((BedPrintableComponent) component).setPrintRaw(stackComponent.getPrint());
            }
        });
    }
}