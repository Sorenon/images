package net.sorenon.images.mixin;

import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.MalformedURLException;

@Mixin(BlockItem.class)
abstract class BlockItemMixin {

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/Block;onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V"), method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;")
    void place(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(context.getBlockPos());
            if (blockEntity != null) {
                ImagesComponents.getPRINTABLE().maybeGet(context.getStack()).ifPresent(itemComponent -> {
                    PrintableComponent blockComponent = BlockComponents.get(ImagesComponents.getPRINTABLE(), blockEntity);
                    if (blockComponent != null) {
                        Print copy = new Print();
                        CompoundTag tag = new CompoundTag();
                        itemComponent.getPrint().serialize(tag);
                        copy.deserialize(tag);
                        blockComponent.setPrint(copy);
                    }
                });
            }
        }
    }
}
