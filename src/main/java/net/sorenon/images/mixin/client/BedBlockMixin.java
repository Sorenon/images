package net.sorenon.images.mixin.client;

import dev.onyxstudios.cca.api.v3.block.BlockComponents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.sorenon.images.api.Print;
import net.sorenon.images.api.PrintableComponent;
import net.sorenon.images.init.ImagesComponents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BedBlock.class)
public class BedBlockMixin extends Block {

    public BedBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack stack = super.getPickStack(world, pos, state);

        PrintableComponent component = BlockComponents.get(ImagesComponents.getPRINTABLE(), world.getBlockEntity(pos));
        if (component != null) {
            Print print = component.getPrint();
            if (print.url != null) {
                ImagesComponents.getPRINTABLE().maybeGet(stack).ifPresent(c -> c.setPrint(print.copy()));
            }
        }
        return stack;
    }
}
