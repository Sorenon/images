package net.sorenon.images.mixin;


import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.CompoundTag;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;

import java.net.URL;
import java.util.List;

@Mixin(AbstractBannerBlock.class)
abstract class BannerBlockMixin extends Block {
    public BannerBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        List<ItemStack> stacks = super.getDroppedStacks(state, builder);
        BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);

        PrintableComponent component = BlockComponents.get(ImagesComponents.getPRINTABLE(), blockEntity);
        if (component != null) {
            Print print = component.getPrint();
            if (print.url != null) {
                for (ItemStack stack : stacks) {
                    ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(c -> c.setPrint(print.copy()));
                }
            }
        }

        return stacks;
    }

//    @Override
//    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
//        ItemStack stack = player.getStackInHand(hand);
//        if (stack.getItem() != ImagesMod.Companion.getPRINTAXE_ITEM()) {
//            BlockEntity blockEntity = world.getBlockEntity(pos);
//            if (blockEntity instanceof Printable) {
//                Printable.Print print = ((Printable) blockEntity).getPrint();
//                if (print.url != null) {
//                    if (player instanceof ServerPlayerEntity) {
//                        GameMessageS2CPacket gameMessageS2CPacket = new GameMessageS2CPacket(print.toText(world), MessageType.CHAT, Util.NIL_UUID);
//                        ((ServerPlayerEntity) player).networkHandler.sendPacket(gameMessageS2CPacket);
//                    }
//                    return ActionResult.success(world.isClient);
//                }
//            }
//        }
//
//        return super.onUse(state, world, pos, player, hand, hit);
//    }
}
