package net.sorenon.images.mixin.client;

import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DyeColor;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;

@Mixin(BannerBlockEntity.class)
abstract class BannerBlockEntityMixin {

    @Inject(at = @At("HEAD"), method = "readFrom")
    void inject_readFrom(ItemStack stack, DyeColor baseColor, CallbackInfo ci) {
        PrintableComponent blockComponent = BlockComponents.get(ImagesComponents.getPRINTABLE(), (BlockEntity) (Object) this);
        PrintableComponent itemComponent = ImagesComponents.getPRINTABLE().get(stack);
        if (blockComponent != null)
            blockComponent.getPrint().url = itemComponent.getPrint().url;
    }

    @Inject(at = @At("RETURN"), method = "getPickStack")
    void inject_getPickStack(BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();

        PrintableComponent component = BlockComponents.get(ImagesComponents.getPRINTABLE(), (BlockEntity) (Object) this);
        if (component != null) {
            Print print = component.getPrint();
            if (print.url != null) {
                ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(c -> c.setPrint(print.copy()));
            }
        }
    }
}
