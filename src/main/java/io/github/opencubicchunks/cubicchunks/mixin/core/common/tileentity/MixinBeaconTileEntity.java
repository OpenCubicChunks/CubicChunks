package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.ConduitTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(BeaconTileEntity.class)
public abstract class MixinBeaconTileEntity {
    /**
     * @author OverInfrared & NotStirred
     * @reason Beacons now only affect players within +256 of the block. This prevents near infinite cube loading in the beacon's column
     */
    //COMMENTED OUT BECAUSE OF A MIXIN BUG. Mixin AP is not recursively checking interfaces, only the first, so it's not added to refmap
//    @Redirect(method = {"addEffectsToPlayers()V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I", args="log=true"))
//    private int on$getHeight(World world) {
//        return 256;
//    }
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/AxisAlignedBB;expandTowards(DDD)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB on$expand(AxisAlignedBB axisAlignedBB, double x, double y, double z) {
        CubicChunks.LOGGER.warn("on$expand called");
        return axisAlignedBB.expandTowards(x, 256, z);
    }

}