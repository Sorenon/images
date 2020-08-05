package net.sorenon.images.content

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.ToolMaterials
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.*
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.RayTraceContext
import net.minecraft.world.World
import net.sorenon.images.init.ImagesMod
import net.sorenon.images.mixin.ItemAccessor
import java.io.Closeable
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PrintAxe(settings: Settings) : AxeItem(ToolMaterials.IRON, 5.0f, -3.0f, settings) {
    init {
        @Suppress("CAST_NEVER_SUCCEEDS")
        (this as ItemAccessor).setMaxDamage(1561)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        tooltip.add(TranslatableText("images.printaxe.description").formatted(Formatting.BLUE))
    }

    override fun getUseAction(stack: ItemStack?): UseAction? {
        return UseAction.BLOCK
    }

    override fun getMaxUseTime(stack: ItemStack?): Int {
        return 0
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val placePos = context.blockPos
        val blockState = world.getBlockState(placePos)

        if (!world.isClient && blockState.block == ImagesMod.PICTURE_FRAME_BLOCK) {
            ItemInstance(context.stack).use { data: ItemInstance ->
                if (data.texture.isEmpty()) {
                    val player = context.player
                    if (player != null) openGUI(player, context.hand, data)
                }

                val start = data.start
                if (start == null) {
                    println("start")
                    data.start = context.blockPos
                    data.side = context.side
                } else {
                    println("try end")
                    data.start = null
                    val side = data.side

                    if (side.axis.choose(start.x, start.y, start.z) !=
                        side.axis.choose(placePos.x, placePos.y, placePos.z)
                    ) return ActionResult.SUCCESS

                    val max =
                        BlockPos(max(placePos.x, start.x), max(placePos.y, start.y), max(placePos.z, start.z))
                    val min =
                        BlockPos(min(placePos.x, start.x), min(placePos.y, start.y), min(placePos.z, start.z))
                    val scan = BlockPos.Mutable()
                    val prop = ImageBlock.FACING_PROPERTIES[side]
                    for (x in min.x..max.x) {
                        for (y in min.y..max.y) {
                            for (z in min.z..max.z) {
                                scan.set(x, y, z)
                                val state = world.getBlockState(scan)
                                if (state.block != ImagesMod.PICTURE_FRAME_BLOCK || !state.get(prop)) {
                                    return ActionResult.PASS
                                }
                            }
                        }
                    }

                    val id = UUID.randomUUID()
                    val masterPos = BlockPos(min.x, min.y, min.z)
                    //Make slaves
                    for (x in min.x..max.x) {
                        for (y in min.y..max.y) {
                            for (z in min.z..max.z) {
                                scan.set(x, y, z)
                                val blockEntity = world.getBlockEntity(scan) as PictureFrameBlockEntity
                                blockEntity.setFace(side, PictureFrameBlockEntity.Face.Slave(masterPos, id))
                                blockEntity.markDirty()
                                blockEntity.sync()
                            }
                        }
                    }
                    //Make master
                    val masterBlockEntity =
                        world.getBlockEntity(scan.set(min.x, min.y, min.z)) as PictureFrameBlockEntity
                    val xyRange = getImageSize(max.x - min.x, max.y - min.y, max.z - min.z, side, null)
                    val direction = when (side) {
                        Direction.DOWN -> context.playerFacing
                        Direction.UP -> context.playerFacing.opposite
                        else -> null
                    }
                    masterBlockEntity.setFace(
                        side,
                        PictureFrameBlockEntity.Face.Master(
                            data.texture,
                            xyRange.right + 1,
                            xyRange.left + 1,
                            direction,
                            id
                        )
                    )
                    masterBlockEntity.markDirty()
                    masterBlockEntity.sync()

                    context.stack.damage(1, context.player, {})
                }
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.PASS
    }

    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack = player.getStackInHand(hand)

        if (player.isSneaking) {
            if (!world.isClient) {
                ItemInstance(itemStack).use { data: ItemInstance ->
                    openGUI(player, hand, data)
                }
            }
            return TypedActionResult.consume(itemStack)
        }

        if (!world.isClient) {
            val trace = getSelectedWallpaper(player)
            if (trace != null) {
                ItemInstance(itemStack).use { data: ItemInstance ->
                    data.start = trace.blockPos
                    data.side = trace.side
                    data.facing = player.horizontalFacing.opposite

                    val blockEntity = world.getBlockEntity(data.start) as WallpaperBlockEntity
                    data.texture = blockEntity.getOrMakeFace(data.side).imageID ?: ""

                    player.setCurrentHand(hand)
                    return TypedActionResult.consume(itemStack)
                }
            }
        }
        return TypedActionResult.pass(itemStack)
    }

    fun openGUI(player: PlayerEntity, hand: Hand, data: ItemInstance){
        data.clear()
        player.setCurrentHand(hand)
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeString(data.texture)
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ImagesMod.S2C_OPEN_SCREEN, buf)
    }

    override fun usageTick(world: World, user: LivingEntity, stack: ItemStack, remainingUseTicks: Int) {
        if (world.isClient || user !is PlayerEntity) return

        ItemInstance(stack).use { data: ItemInstance ->
            val start = data.start
            val texture = data.texture
            val facing = data.facing

            if (start == null) return

            val trace = getSelectedWallpaper(user)

            if (trace == null || trace.blockPos == data.lastEnd) return

            val placePos = trace.blockPos
            val side = data.side

            if (side.axis.choose(start.x, start.y, start.z) !=
                side.axis.choose(placePos.x, placePos.y, placePos.z)
            ) return
            data.lastEnd = placePos

            val scan = BlockPos.Mutable()
            val max =
                BlockPos(max(placePos.x, start.x), max(placePos.y, start.y), max(placePos.z, start.z))
            val min =
                BlockPos(min(placePos.x, start.x), min(placePos.y, start.y), min(placePos.z, start.z))

            val xyRange = getImageSize(max.x - min.x, max.y - min.y, max.z - min.z, side, facing.axis)
            val xRange = xyRange.left
            val yRange = xyRange.right

            val sideProperty = ImageBlock.FACING_PROPERTIES[side]
            for (x in min.x..max.x) {
                for (y in min.y..max.y) {
                    for (z in min.z..max.z) {
                        scan.set(x, y, z)
                        val state = world.getBlockState(scan)

                        if (state.block is ImageBlock && state.get(sideProperty)) {
                            val blockEntity = world.getBlockEntity(scan) as WallpaperBlockEntity
                            val entity = blockEntity.getOrMakeFace(side)
                            if (entity.imageID == null || entity.imageID == texture) {
                                entity.xSize = xRange + 1
                                entity.ySize = yRange + 1
                                entity.imageID = texture

                                var u = 1
                                var v = 1
                                when (side.axis!!) {
                                    Direction.Axis.X -> {
                                        u = -(z - max.z)
                                        v = -(y - max.y)
                                    }
                                    Direction.Axis.Z -> {
                                        u = -(x - max.x)
                                        v = -(y - max.y)
                                    }
                                    Direction.Axis.Y -> {
                                        entity.rotation = facing
                                        when (facing.axis) {
                                            Direction.Axis.X -> {
                                                u = -(z - max.z)
                                                v = yRange + (x - max.x)
                                            }
                                            Direction.Axis.Z -> {
                                                u = -(x - max.x)
                                                v = -(z - max.z)
                                            }
                                            else -> {
                                            }
                                        }
                                        if (facing == Direction.WEST || facing == Direction.SOUTH) {
                                            u = xRange - u
                                            v = yRange - v
                                        }
                                    }
                                }
                                if (side == Direction.WEST || side == Direction.SOUTH) {
                                    u = xRange - u
                                }

                                entity.uStart = u
                                entity.vStart = v
                                blockEntity.markDirty()
                                blockEntity.sync()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getImageSize(dx: Int, dy: Int, dz: Int, side: Direction, facingAxis: Direction.Axis?): Pair<Int, Int> {
        var xRange = 0
        var yRange = dy
        when (side.axis!!) {
            Direction.Axis.X -> xRange = dz
            Direction.Axis.Z -> xRange = dx
            Direction.Axis.Y -> when (facingAxis) {
                Direction.Axis.Z -> {
                    xRange = dz
                    yRange = dx
                }
                Direction.Axis.X -> {
                    xRange = dx
                    yRange = dz
                }
                else -> {
                    xRange = dx
                    yRange = dz
                }
            }
        }

        return Pair(xRange, yRange)
    }

    override fun onStoppedUsing(stack: ItemStack, world: World, user: LivingEntity, remainingUseTicks: Int) {
        val data = ItemInstance(stack)
        data.use {
            data.start = null
            data.lastEnd = null
        }
    }

    private fun getSelectedWallpaper(user: PlayerEntity): BlockHitResult? {
        val trace = rayTrace(user.world, user, RayTraceContext.FluidHandling.NONE)
        if (trace.type == HitResult.Type.BLOCK && user.world.getBlockState(trace.blockPos).block == ImagesMod.WALLPAPER_BLOCK) {
            return trace
        }
        return null
    }

    class ItemInstance(private val stack: ItemStack) : Closeable {
        var start: BlockPos?
        var lastEnd: BlockPos?
        var side: Direction
        var facing: Direction
        var texture = ""

        init {
            val tag = stack.orCreateTag
            start = getBP("start", tag)
            lastEnd = getBP("lastEnd", tag)
            side = Direction.byId(tag.getInt("side"))
            facing = Direction.byId(tag.getInt("facing"))
            texture = tag.getString("texture")
        }

        private fun getBP(name: String, tag: CompoundTag): BlockPos? {
            if (tag.contains(name, NbtType.COMPOUND)) {
                return NbtHelper.toBlockPos(tag.getCompound(name))
            }
            return null
        }

        fun clear() {
            start = null
            lastEnd = null
        }

        override fun close() {
            val tag = stack.orCreateTag
            if (start != null) tag.put("start", NbtHelper.fromBlockPos(start)) else tag.remove("start")
            if (lastEnd != null) tag.put("lastEnd", NbtHelper.fromBlockPos(lastEnd)) else tag.remove("lastEnd")
            tag.putString("texture", texture)
            tag.putInt("side", side.id)
            tag.putInt("facing", facing.id)
            stack.tag = tag
        }
    }
}