package net.sorenon.images.mixin;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BannerItem.class)
abstract class BannerItemMixin {

    @Inject(at = @At("HEAD"), method = "appendTooltip")
    void inject_appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci){
        CompoundTag tag = stack.getSubTag("BlockEntityTag");
        if (tag != null && tag.contains("sorenon_imageURL", NbtType.STRING)) {
            String url = tag.getString("sorenon_imageURL");
            if (url.length() > 24) {
                url = url.substring(0, 24) + '…';
            }
            tooltip.add(new LiteralText(url).formatted(Formatting.GREEN));
        }
    }
}
