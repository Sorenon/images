package net.sorenon.images.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sorenon.images.init.ImagesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin extends PlayerEntity {

    @Shadow private boolean usingItem;
    private boolean usingItemSave = false;

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }


    @Inject(at = @At(value = "HEAD"), method = "tickMovement")
    public void tickMovement(CallbackInfo ci){
        this.usingItemSave = this.isUsingItem();
        if (usingItemSave && this.getActiveItem().getItem() == ImagesMod.Companion.getPRINTAXE_ITEM()) {
            this.usingItem = false;
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "tickMovement")
    public void tickMovementEnd(CallbackInfo ci){
        this.usingItem = usingItemSave;
    }
}
