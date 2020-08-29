package net.sorenon.images.content

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.server.PlayerStream
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.*
import net.minecraft.block.entity.BannerPattern
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
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
import net.sorenon.images.accessor.BannerMixinAccessor
import net.sorenon.images.init.ImagesMod
import net.sorenon.images.init.ImagesMod.Companion.S2C_PRINT_BOOM
import java.io.Closeable
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

class PrintAxe(settings: Settings) : BannerPatternItem(BannerPattern.FLOWER, settings) {
    init {
        this.maxDamage = 1561
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        tooltip.add(TranslatableText("images.printaxe.description").formatted(Formatting.GRAY))
        ItemInstance(stack).use {
            if (it.url != null) {
                var str = it.url!!.toString()
                if (str.length > 24) {
                    str = str.substring(0, 24).plus('…')
                }
                tooltip.add(LiteralText(str).formatted(Formatting.GREEN))
            }
        }
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        val data = PacketByteBuf(Unpooled.buffer())
        data.writeInt(target.entityId)

        PlayerStream.watching(target).forEach {
            ServerSidePacketRegistry.INSTANCE.sendToPlayer(it, S2C_PRINT_BOOM, data)
        }

        stack.damage<LivingEntity>(2, attacker) { e: LivingEntity ->
            e.sendEquipmentBreakStatus(
                EquipmentSlot.MAINHAND
            )
        }
        return super.postHit(stack, target, attacker)
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
                val url = data.url
                if (url == null) {
                    val player = context.player
                    if (player != null) openGUI(player, context.hand, data)
                    data.start = context.blockPos
                    data.side = context.side
                    return ActionResult.SUCCESS
                }

                val start = data.start
                if (start == null) {
                    data.start = context.blockPos
                    data.side = context.side
                } else {
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
                            url,
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
        else if (blockState.block is AbstractBannerBlock){
            val blockEntity = world.getBlockEntity(placePos)
            if (blockEntity is BannerMixinAccessor) {
                ItemInstance(context.stack).use {
                    if (blockEntity.url != it.url) {
                        if (world is ServerWorld) {
                            blockEntity.url = it.url
                            blockEntity.markDirty()
                            world.chunkManager.markForUpdate(placePos)
                        }
                        return ActionResult.success(world.isClient)
                    }
                }
            }
        }
        return axe_useOnBlock(context)
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
                    data.url = blockEntity.getOrMakeFace(data.side).url

                    player.setCurrentHand(hand)
                    return TypedActionResult.consume(itemStack)
                }
            }
        }
        return TypedActionResult.pass(itemStack)
    }

    private fun openGUI(player: PlayerEntity, hand: Hand, data: ItemInstance){
        data.clear()
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeString(data.url?.toString() ?: "")
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ImagesMod.S2C_OPEN_SCREEN, buf)
    }

    override fun usageTick(world: World, user: LivingEntity, stack: ItemStack, remainingUseTicks: Int) {
        if (world.isClient || user !is PlayerEntity) return

        ItemInstance(stack).use { data: ItemInstance ->
            val start = data.start
            val texture = data.url
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
                            if (entity.url == null || entity.url == texture) {
                                entity.xSize = xRange + 1
                                entity.ySize = yRange + 1
                                entity.url = texture

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
        var url: URL?

        init {
            val tag = stack.orCreateTag
            start = getBP("start", tag)
            lastEnd = getBP("lastEnd", tag)
            side = Direction.byId(tag.getInt("side"))
            facing = Direction.byId(tag.getInt("facing"))
            url = try {
                URL(tag.getString("url"))
            } catch (_: MalformedURLException) {
                null
            }
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
            tag.putString("url", url?.toString() ?: "")
            tag.putInt("side", side.id)
            tag.putInt("facing", facing.id)
            stack.tag = tag
        }
    }

    //AXE STUFF
    private val material = ToolMaterials.IRON
    private val miningSpeed = material.miningSpeedMultiplier
    private val attackDamage = 5.0f + material.attackDamage
    private val attributeModifiers: Multimap<EntityAttribute, EntityAttributeModifier>

    init {
        val builder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        builder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE, EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier",
                attackDamage.toDouble(), EntityAttributeModifier.Operation.ADDITION
            )
        )
        builder.put(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Tool modifier",
                -3.0,
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        attributeModifiers = builder.build()
    }

    override fun getMiningSpeedMultiplier(stack: ItemStack, state: BlockState): Float {
        val material = state.material
        return if (AxeItem.field_23139.contains(material) || AxeItem.EFFECTIVE_BLOCKS.contains(state.block)) miningSpeed else 1.0f
    }

    fun axe_useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val blockPos = context.blockPos
        val blockState = world.getBlockState(blockPos)
        val block = AxeItem.STRIPPED_BLOCKS[blockState.block]
        return if (block != null) {
            val playerEntity = context.player
            world.playSound(playerEntity, blockPos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0f, 1.0f)
            if (!world.isClient) {
                world.setBlockState(
                    blockPos,
                    block.defaultState.with(PillarBlock.AXIS, blockState.get(PillarBlock.AXIS)) as BlockState,
                    11
                )
                if (playerEntity != null) {
                    context.stack.damage<LivingEntity>(1, playerEntity) { e: LivingEntity ->
                        e.sendEquipmentBreakStatus(
                            EquipmentSlot.MAINHAND
                        )
                    }
                }
            }
            ActionResult.success(world.isClient)
        } else {
            ActionResult.PASS
        }
    }

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: BlockState,
        pos: BlockPos?,
        miner: LivingEntity?
    ): Boolean {
        if (!world.isClient && state.getHardness(world, pos) != 0.0f) {
            stack.damage<LivingEntity>(1, miner) { e: LivingEntity ->
                e.sendEquipmentBreakStatus(
                    EquipmentSlot.MAINHAND
                )
            }
        }
        return true
    }

    override fun getAttributeModifiers(slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
        return if (slot == EquipmentSlot.MAINHAND) attributeModifiers else super.getAttributeModifiers(slot)
    }

    fun getAttackDamage(): Float {
        return attackDamage
    }

    override fun getEnchantability(): Int {
        return material.enchantability
    }

    override fun canRepair(stack: ItemStack?, ingredient: ItemStack?): Boolean {
        return material.repairIngredient.test(ingredient) || super.canRepair(stack, ingredient)
    }
}