package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeaconTileEntity.class)
public class MixinBeaconTileEntity {
    /**
     * @author OverInfrared & NotStirred
     * @reason Beacons now only affect players within +256 of the block. This prevents near infinite cube loading in the beacon's column
     */
    @Redirect(method = {"addEffectsToPlayers()V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I"))
    private int on$getHeight(World world) {
        return 256;
    }
}