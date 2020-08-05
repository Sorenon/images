package net.sorenon.images.mixin;

import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPhase.class)
public interface RenderPhaseAccessor {

    @Accessor
    static RenderPhase.WriteMaskState getCOLOR_MASK() {
        return null;
    }

    @Accessor
    static RenderPhase.DepthTest getLEQUAL_DEPTH_TEST() {
        return null;
    }

    @Accessor
    static RenderPhase.Lightmap getENABLE_LIGHTMAP() {
        return null;
    }
}
