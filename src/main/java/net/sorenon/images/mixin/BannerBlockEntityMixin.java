package net.sorenon.images.mixin;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.item.ItemStack;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BannerBlockEntity.class)
abstract class BannerBlockEntityMixin {

    @Inject(at = @At("RETURN"), method = "getPatternCount", cancellable = true)
    private static void inject_getPatternCount(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        //See BannerDuplicateRecipe
        if (ImagesComponents.getPRINTABLE().get(stack).getPrint().url != null) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), 1));
        }
    }
}
