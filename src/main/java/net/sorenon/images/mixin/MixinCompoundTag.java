package net.sorenon.images.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompoundTag.class)
abstract class MixinCompoundTag {

    //Issue: When trinkets is installed and an item component doesn't write things to nbt, things go wrong when saving chunks
    //In cc's MixinItemStack ->
    //cir.getReturnValue().put(AbstractComponentContainer.NBT_KEY, this.serializedComponents.get(AbstractComponentContainer.NBT_KEY));
    //can run when this.serializedComponents.get(AbstractComponentContainer.NBT_KEY) == null
    @Inject(at = @At("HEAD"), method = "put", cancellable = true)
    void put(String key, Tag tag, CallbackInfoReturnable<Tag> cir){
        if (tag == null) {
            cir.setReturnValue(null);
        }
    }
}
