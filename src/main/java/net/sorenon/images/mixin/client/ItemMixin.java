package net.sorenon.images.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(at = @At("HEAD"), method = "appendTooltip")
    void inject_appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci){
        ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(itemComponent -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean isSneaking = InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.keySneak.getBoundKeyTranslationKey()).getCode());
            itemComponent.getPrint().appendTooltip(tooltip, world, isSneaking, 200);
        });
    }
}
