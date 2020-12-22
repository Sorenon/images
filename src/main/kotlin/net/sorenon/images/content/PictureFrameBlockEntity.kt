package net.sorenon.images.content

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.sorenon.images.api.Print
import net.sorenon.images.init.ImagesMod
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class PictureFrameBlockEntity : BlockEntity(ImagesMod.PICTURE_FRAME_BLOCK_ENTITY), BlockEntityClientSerializable {
    val faces = hashMapOf<Direction, Face>()

    fun setFace(side: Direction, face: Face) {
        val oldFace = faces[side]
        if (oldFace is Face.Slave) {
            removeMaster(oldFace, side)
        }
        sync()
        markDirty()

        faces[side] = face
    }

    fun getMaster(side: Direction): PictureFrameBlockEntity? {
        return when (val face = faces[side]) {
            is Face.Master -> this
            is Face.Slave -> getMasterEntity(face, side)
            null -> null
        }
    }

    fun getMasterEntity(slave: Face.Slave, side: Direction): PictureFrameBlockEntity? {
        val blockEntity = world?.getBlockEntity(slave.masterPos)
        if (blockEntity is PictureFrameBlockEntity) {
            val face = blockEntity.faces[side]
            if (face is Face.Master && face.uuid == slave.uuid) {
                return blockEntity
            }
        }
        return null
    }

    private fun removeMaster(slave: Face.Slave, side: Direction) {
        val blockEntity = getMasterEntity(slave, side)
        if (blockEntity != null) {
            blockEntity.faces.remove(side)
            blockEntity.markDirty()
            blockEntity.sync()
        }
    }

    override fun markRemoved() {
        super.markRemoved()

        if (world != null && !world!!.isClient) {
            for (pairs in faces) {
                val face = pairs.value
                if (face is Face.Slave) {
                    removeMaster(face, pairs.key)
                }
            }
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        for (pair in faces) {
            val face = pair.value
            val dir = pair.key
            tag.put(dir.getName(), face.toTag())
        }
        return tag
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        faces.clear()
        super.fromTag(state, tag)
        for (dir in Direction.values()) {
            val key = dir.getName()
            if (tag.contains(key, 10)) {
                faces[dir] = Face.fromTag(dir.axis == Direction.Axis.Y, tag.getCompound(key))
            }
        }
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(null, tag)
    }

    sealed class Face(val uuid: UUID) {
        class Slave(val masterPos: BlockPos, uuid: UUID) : Face(uuid)
        class Master(
            val print: Print,
            val height: Int,
            val width: Int,
            val direction: Direction?,
            uuid: UUID,
            var colour: Int = 0
        ) : Face(uuid)

        fun toTag(): CompoundTag {
            val tag = CompoundTag()
            tag.putUuid("id", uuid)
            when (this) {
                is Master -> {
                    print.serialize(tag)
                    tag.putInt("height", height)
                    tag.putInt("width", width)
                    tag.putInt("colour", colour)
                    if (direction != null) tag.putInt("direction", direction.id)
                }
                is Slave -> {
                    tag.put("masterPos", NbtHelper.fromBlockPos(masterPos))
                }
            }
            return tag
        }

        companion object {
            fun fromTag(isVertical: Boolean, tag: CompoundTag): Face {
                val uuid = tag.getUuid("id")

                try {
                    val print = Print()
                    print.deserialize(tag)
                    if (print.url != null) {
                        val direction = if (isVertical) {
                            val dir = Direction.byId(tag.getInt("direction"))
                            if (dir.axis != Direction.Axis.Y) dir else Direction.NORTH
                        } else null

                        return Master(
                            print,
                            tag.getInt("height"),
                            tag.getInt("width"),
                            direction,
                            uuid,
                            tag.getInt("colour")
                        )
                    }
                } catch (_: MalformedURLException) {
                }
                return Slave(NbtHelper.toBlockPos(tag.getCompound("masterPos")), uuid)
            }
        }
    }
}