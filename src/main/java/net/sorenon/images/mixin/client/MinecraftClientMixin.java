package net.sorenon.images.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.sorenon.images.init.ImagesModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {

    @Inject(at = @At(value = "RETURN"), method = "render")
    public void render(boolean tick, CallbackInfo ci){
        if (tick) {
            ImagesModClient.Companion.getImageDB().tick();
        }
    }
}
