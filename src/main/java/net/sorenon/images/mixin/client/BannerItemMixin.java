package net.sorenon.images.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BannerItem.class)
abstract class BannerItemMixin {

    @Inject(at = @At("HEAD"), method = "appendBannerTooltip")
    private static void inject_appendBannerTooltip(ItemStack stack, List<Text> tooltip, CallbackInfo ci){
        PrintableComponent itemComponent = ImagesComponents.getPRINTABLE().get(stack);
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isSneaking = InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.keySneak.getBoundKeyTranslationKey()).getCode());
        itemComponent.getPrint().appendTooltip(tooltip, isSneaking, 200, false);
    }
}
