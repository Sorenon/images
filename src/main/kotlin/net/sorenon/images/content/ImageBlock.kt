package net.sorenon.images.content

import com.google.common.collect.Maps
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.state.property.Properties.WATERLOGGED
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Util
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import net.sorenon.images.mixin.DyeColorAccessor
import java.util.*

abstract class ImageBlock(settings: Settings?) : Block(settings), BlockEntityProvider {
    companion object {
        val DOWN_SHAPE: VoxelShape =
            createCuboidShape(0.0, 15.0, 0.0, 16.0, 16.0, 16.0)
        val UP_SHAPE: VoxelShape =
            createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)
        val NORTH_SHAPE: VoxelShape =
            createCuboidShape(0.0, 0.0, 15.0, 16.0, 16.0, 16.0)
        val SOUTH_SHAPE: VoxelShape =
            createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 1.0)
        val WEST_SHAPE: VoxelShape =
            createCuboidShape(15.0, 0.0, 0.0, 16.0, 16.0, 16.0)
        val EAST_SHAPE: VoxelShape =
            createCuboidShape(0.0, 0.0, 0.0, 1.0, 16.0, 16.0)

        val UP: BooleanProperty = Properties.UP
        val DOWN: BooleanProperty = Properties.DOWN
        val NORTH: BooleanProperty = Properties.NORTH
        val EAST: BooleanProperty = Properties.EAST
        val SOUTH: BooleanProperty = Properties.SOUTH
        val WEST: BooleanProperty = Properties.WEST
        val FACING_PROPERTIES: Map<Direction, BooleanProperty> = ConnectingBlock.FACING_PROPERTIES
        val SHAPES: Map<Direction, VoxelShape> = Util.make(Maps.newEnumMap<Direction, VoxelShape>(
            Direction::class.java
        ),
            { enumMap: EnumMap<Direction, VoxelShape> ->
                enumMap[Direction.NORTH] = NORTH_SHAPE
                enumMap[Direction.EAST] = EAST_SHAPE
                enumMap[Direction.SOUTH] = SOUTH_SHAPE
                enumMap[Direction.WEST] = WEST_SHAPE
                enumMap[Direction.UP] = UP_SHAPE
                enumMap[Direction.DOWN] = DOWN_SHAPE
            }
        )
    }

    init {
        defaultState =
            stateManager.defaultState.with(UP, false).with(
                DOWN, false
            ).with(NORTH, false).with(
                EAST, false
            ).with(SOUTH, false)
                .with(WEST, false)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val blockState = ctx.world.getBlockState(ctx.blockPos)
        return if (blockState.isOf(this)) {
            blockState.with(FACING_PROPERTIES[ctx.side], true)
        } else {
            defaultState.with(FACING_PROPERTIES[ctx.side], true)
        }
    }

    override fun canReplace(state: BlockState, context: ItemPlacementContext): Boolean {
        return context.stack.item === asItem() && !state.get(FACING_PROPERTIES[context.side])
    }

    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        newState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        posFrom: BlockPos
    ): BlockState? {
        if (state.get(FACING_PROPERTIES[direction.opposite]) && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.defaultState
        }
        return state
    }

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
        if (state == defaultState)
            return false

        for (pair in FACING_PROPERTIES) {
            if (state.get(pair.value)) {
                val blockPos = pos.offset(pair.key.opposite)
                val blockState = world.getBlockState(blockPos)
                if (!blockState.material.isSolid && !AbstractRedstoneGateBlock.isRedstoneGate(blockState) || blockState.block is ImageBlock) {
                    return false
                }
            }
        }

        return true
    }

    override fun getRenderType(state: BlockState): BlockRenderType {
        return if (state.isOpaque) BlockRenderType.MODEL else BlockRenderType.ENTITYBLOCK_ANIMATED
    }

    override fun appendProperties(builder: StateManager.Builder<Block?, BlockState?>) {
        builder.add(
            ConnectingBlock.NORTH,
            ConnectingBlock.EAST,
            ConnectingBlock.SOUTH,
            ConnectingBlock.WEST,
            ConnectingBlock.UP,
            ConnectingBlock.DOWN
        )
    }

    override fun getOutlineShape(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape? {
        var voxelShape = VoxelShapes.empty()
        if (state.get(Properties.UP)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                UP_SHAPE
            )
        }
        if (state.get(Properties.DOWN)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                DOWN_SHAPE
            )
        }
        if (state.get(Properties.NORTH)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                NORTH_SHAPE
            )
        }
        if (state.get(Properties.EAST)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                EAST_SHAPE
            )
        }
        if (state.get(Properties.SOUTH)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                SOUTH_SHAPE
            )
        }
        if (state.get(Properties.WEST)) {
            voxelShape = VoxelShapes.union(
                voxelShape,
                WEST_SHAPE
            )
        }
        return voxelShape
    }

    class WallPaperBlock(settings: Settings) : ImageBlock(settings) {
        override fun createBlockEntity(world: BlockView): BlockEntity {
            return WallpaperBlockEntity()
        }
    }

    class PictureFrameBlock(settings: Settings) : ImageBlock(settings), Waterloggable {
        init {
            defaultState =
                defaultState.with(WATERLOGGED, false)
        }

        override fun createBlockEntity(world: BlockView?): BlockEntity? {
            return PictureFrameBlockEntity()
        }

        override fun buildTooltip(
            stack: ItemStack,
            world: BlockView?,
            tooltip: MutableList<Text>,
            options: TooltipContext
        ) {
            tooltip.add(TranslatableText("images.picture_frame.description").formatted(Formatting.BLUE))
        }

        override fun getDroppedStacks(state: BlockState, builder: LootContext.Builder): List<ItemStack> {
            //LOOT TABLES ARE DUMB

            var count = 0
            for (prop in FACING_PROPERTIES.values) {
                if (state.get(prop)) {
                    count++
                }
            }
            return listOf(ItemStack(asItem(), count))
        }

        override fun onUse(
            state: BlockState,
            world: World,
            pos: BlockPos,
            player: PlayerEntity,
            hand: Hand,
            hit: BlockHitResult
        ): ActionResult {
            if (!world.isClient) {
                val stack = player.getStackInHand(hand)
                if (stack.item is DyeItem) {
                    val side = hit.side
                    val blockEntity = (world.getBlockEntity(pos) as PictureFrameBlockEntity).getMaster(side)
                    if (blockEntity != null) {
                        val face = blockEntity.faces[side] as PictureFrameBlockEntity.Face.Master
                        face.colour = ((stack.item as DyeItem).color as DyeColorAccessor).color
                        blockEntity.markDirty()
                        blockEntity.sync()
                        if (!player.isCreative) stack.decrement(1)
                        return ActionResult.SUCCESS
                    }
                }
            }

            return super.onUse(state, world, pos, player, hand, hit)
        }

        override fun getStateForNeighborUpdate(
            state: BlockState,
            direction: Direction,
            newState: BlockState,
            world: WorldAccess,
            pos: BlockPos,
            posFrom: BlockPos
        ): BlockState? {
            if (state.get(WATERLOGGED) as Boolean) {
                world.fluidTickScheduler.schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
            }
            return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom)
        }

        override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
            val fluidState = ctx.world.getFluidState(ctx.blockPos)
            return super.getPlacementState(ctx)?.with(WATERLOGGED, fluidState.fluid == Fluids.WATER)
        }

        override fun getFluidState(state: BlockState): FluidState {
            return if (state.get(AbstractSignBlock.WATERLOGGED)) Fluids.WATER.getStill(false)
            else super.getFluidState(state)
        }

        override fun appendProperties(builder: StateManager.Builder<Block?, BlockState?>) {
            super.appendProperties(builder)
            builder.add(WATERLOGGED)
        }
    }
}