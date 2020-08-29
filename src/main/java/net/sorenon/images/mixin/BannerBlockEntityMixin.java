package net.sorenon.images.mixin;

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
abstract class BannerBlockEntityMixin implements BannerMixinAccessor {

    @Nullable
    @Unique
    private URL imageURL = null;

    @Inject(at = @At("HEAD"), method = "toTag")
    void inject_toTag(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (imageURL != null) {
            tag.putString("sorenon_imageURL", imageURL.toString());
        }
    }

    @Inject(at = @At("HEAD"), method = "fromTag")
    void inject_fromTag(BlockState state, CompoundTag tag, CallbackInfo ci) throws MalformedURLException {
        if (tag.contains("sorenon_imageURL", NbtType.STRING)) {
            imageURL = new URL(tag.getString("sorenon_imageURL"));
        }
        else {
            imageURL = null;
        }
    }

    @Inject(at = @At("HEAD"), method = "readFrom")
    void inject_readFrom(ItemStack stack, DyeColor baseColor, CallbackInfo ci){
        imageURL = getURLFromStack(stack);
    }

    @Inject(at = @At("RETURN"), method = "getPickStack")
    void inject_getPickStack(BlockState state, CallbackInfoReturnable<ItemStack> cir){
        ItemStack stack = cir.getReturnValue();
        if (imageURL != null) {
            stack.getOrCreateSubTag("BlockEntityTag").putString("sorenon_imageURL", imageURL.toString());
        }
    }

    @Inject(at = @At("RETURN"), method = "getPatternCount", cancellable = true)
    private static void inject_getPatternCount(ItemStack stack, CallbackInfoReturnable<Integer> cir){
        if (getURLFromStack(stack) != null) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), 1));
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

    @Override
    public URL getURL() {
        return imageURL;
    }

    @Override
    public void setURL(URL url) {
        imageURL = url;
    }
}
