package net.sorenon.images.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.util.Util;
import net.sorenon.images.init.ImagesModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {

    @Shadow protected abstract void debugWarn(String string, Object... objects);

    @Shadow private long debugCrashStartTime;

    @Inject(at = @At("HEAD"), method = "processF3", cancellable = true)
    public void processF3(int key, CallbackInfoReturnable<Boolean> cir){
        if (this.debugCrashStartTime > 0L && this.debugCrashStartTime < Util.getMeasuringTimeMs() - 100L) {
            return;
        }

        if (key == 'A' || key == 'I') {
            ImagesModClient.Companion.getImageDB().reload();

            if (key == 'I') {
                this.debugWarn("debug.reload_images.message");
                cir.setReturnValue(true);
            }
        }
    }
}
