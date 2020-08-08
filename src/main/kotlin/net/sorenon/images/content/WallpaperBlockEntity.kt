package net.sorenon.images.content

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.Direction
import net.sorenon.images.init.ImagesMod
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.min

class WallpaperBlockEntity : BlockEntity(ImagesMod.WALLPAPER_BLOCK_ENTITY), BlockEntityClientSerializable {
    var faces: Array<Face?> = arrayOfNulls(Direction.values().size)

    fun getOrMakeFace(dir: Direction): Face {
        var side = faces[dir.id]
        if (side == null) {
            side = Face()
            faces[dir.id] = side
        }
        return side
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        for (dir in Direction.values()) {
            val face = faces[dir.id]
            if (face != null){
                tag.put(dir.getName(), face.toTag(dir.horizontal == -1))
            }
        }
        return tag
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        for (dir in Direction.values()) {
            val key = dir.getName()
            if (tag.contains(key, 10)){
                faces[dir.id] = Face()
                    .fromTag(tag.getCompound(key), dir.horizontal == -1)
            }
        }
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(null, tag)
    }

    class Face {
        var uStart = 0
        var vStart = 0
        var xSize = 1
        var ySize = 1
        var rotation = Direction.NORTH
        var url: URL? = null

        fun toTag(isVertical: Boolean): CompoundTag {
            val tag = CompoundTag()
            tag.putInt("uStart", uStart)
            tag.putInt("vStart", vStart)
            tag.putInt("xSize", xSize)
            tag.putInt("ySize", ySize)
            if (url != null) {
                tag.putString("url", url.toString())
            }
            if (isVertical) {
                tag.putByte("rotation", rotation.id.toByte())
            }
            return tag
        }

        fun fromTag(tag: CompoundTag, isVertical: Boolean): Face {
            uStart = tag.getInt("uStart")
            vStart = tag.getInt("vStart")
            xSize = tag.getInt("xSize").coerceAtLeast(1)
            ySize = tag.getInt("ySize").coerceAtLeast(1)
            if (tag.contains("url")) {
                url = try {
                    URL(tag.getString("url"))
                } catch (_: MalformedURLException) {
                    null
                }
            }
            if (isVertical) {
                rotation = byteToDir(tag.getByte("rotation"))
            }

            return this
        }

        private fun byteToDir(byte: Byte): Direction {
            return Direction.values()[min(byte.toInt(), 5)]
        }
    }
}