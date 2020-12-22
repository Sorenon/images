package net.sorenon.images.mixin;

import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.content.BedPrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import net.sorenon.images.init.ImagesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;
import java.util.List;

@Mixin(BedBlock.class)
abstract class BedBlockMixin extends Block {

    public BedBlockMixin(Settings settings) {
        super(settings);
    }

    /**
     * Prevent player from sleeping when they try to print on the bed
     */
    @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)
    void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        for (Hand hand1 : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand1);
            if (stack.getItem() == ImagesMod.Companion.getPRINTAXE_ITEM()) {
                ActionResult result = stack.useOnBlock(new ItemUsageContext(player, hand1, hit));
                if (result.isAccepted()) {
                    cir.setReturnValue(result);
                }
            }
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        List<ItemStack> stacks = super.getDroppedStacks(state, builder);
        BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);

        PrintableComponent component = BlockComponents.get(ImagesComponents.getPRINTABLE(), blockEntity);
        if (component instanceof BedPrintableComponent) {
            Print print = ((BedPrintableComponent) component).getPrintRaw();
            if (print != null) {
                if (print.url != null) {
                    for (ItemStack stack : stacks) {
                        ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(c -> c.setPrint(print.copy()));
                    }
                }
            }
        }

        return stacks;
    }
}
