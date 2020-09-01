package net.sorenon.images.mixin;


import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sorenon.images.accessor.BannerMixinAccessor;
import net.sorenon.images.init.ImagesMod;
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
        if (blockEntity instanceof BannerMixinAccessor) {
            URL url = ((BannerMixinAccessor) blockEntity).getURL();
            if (url != null) {
                for (ItemStack stack : stacks){
                    if (stack.getItem() instanceof BannerItem) {
                        stack.getOrCreateSubTag("BlockEntityTag").putString("sorenon_imageURL", url.toString());
                    }
                }
            }
        }

        return stacks;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() != ImagesMod.Companion.getPRINTAXE_ITEM()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BannerMixinAccessor) {
                URL url = ((BannerMixinAccessor) blockEntity).getURL();
                if (url != null) {
                    String urlStr = url.toString();
                    if (world.isClient) {
                        Text text = Texts.bracketed(new LiteralText(urlStr)).styled(style -> style.withClickEvent(
                                new ClickEvent(
                                        ClickEvent.Action.OPEN_URL,
                                        urlStr
                                )
                        ).setHoverEvent(
                                new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        new TranslatableText("images.open_or_copy_link")
                                )
                        )).formatted(Formatting.GREEN);

                        player.sendMessage(text, false);
                    }
                    return ActionResult.success(world.isClient);
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
}
