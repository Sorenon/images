package net.sorenon.images.mixin.client;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.sorenon.images.init.ImagesModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {

    @Shadow protected abstract void debugWarn(String string, Object... objects);

    @Shadow private long debugCrashStartTime;

    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("RETURN"), method = "processF3", cancellable = true)
    public void processF3(int key, CallbackInfoReturnable<Boolean> cir){
        if (this.debugCrashStartTime > 0L && this.debugCrashStartTime < Util.getMeasuringTimeMs() - 100L) {
            return;
        }

        if (key == 'V' || key == 'T') {
            this.debugWarn("debug.reload_images.message");
            ImagesModClient.Companion.reloadConfig();
            ImagesModClient.Companion.getImageDB().reload();
            cir.setReturnValue(true);
        } else if (key == 'Q') {
            ChatHud chatHud = this.client.inGameHud.getChatHud();
            chatHud.addMessage(new TranslatableText("debug.images.help"));
        }
    }
}
