package net.sorenon.images.mixin.client;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DyeColor;
import net.sorenon.images.accessor.BannerMixinAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.MalformedURLException;
import java.net.URL;

@Mixin(BannerBlockEntity.class)
abstract class BannerBlockEntityMixin {

    @Inject(at = @At("HEAD"), method = "readFrom")
    void inject_readFrom(ItemStack stack, DyeColor baseColor, CallbackInfo ci){
        ((BannerMixinAccessor)this).setURL(getURLFromStack(stack));
    }

    @Inject(at = @At("RETURN"), method = "getPickStack")
    void inject_getPickStack(BlockState state, CallbackInfoReturnable<ItemStack> cir){
        ItemStack stack = cir.getReturnValue();
        URL url = ((BannerMixinAccessor)this).getURL();
        if (url != null) {
            stack.getOrCreateSubTag("BlockEntityTag").putString("sorenon_imageURL", url.toString());
        }
    }

    @Unique
    @Nullable
    private static URL getURLFromStack(ItemStack stack){
        CompoundTag tag = stack.getSubTag("BlockEntityTag");
        if (tag != null && tag.contains("sorenon_imageURL", NbtType.STRING)) {
            try {
                return new URL(tag.getString("sorenon_imageURL"));
            } catch (MalformedURLException ignored) { }
        }
        return null;
    }
}
