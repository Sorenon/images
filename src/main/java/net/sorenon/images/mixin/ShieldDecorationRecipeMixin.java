package net.sorenon.images.mixin;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.ShieldDecorationRecipe;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ShieldDecorationRecipe.class)
abstract class ShieldDecorationRecipeMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getSubTag(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;", ordinal = 0),
            method = "craft",
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    void craft(CraftingInventory craftingInventory, CallbackInfoReturnable<ItemStack> ci, ItemStack bannerStack, ItemStack shieldStack) {
        PrintableComponent bannerComponent = ImagesComponents.getPRINTABLE().get(bannerStack);
        PrintableComponent shieldComponent = ImagesComponents.getPRINTABLE().get(shieldStack);
        CompoundTag tag = new CompoundTag();
        bannerComponent.getPrint().serialize(tag);
        Print copy = new Print();
        copy.deserialize(tag);
        shieldComponent.setPrint(copy);
    }
}
