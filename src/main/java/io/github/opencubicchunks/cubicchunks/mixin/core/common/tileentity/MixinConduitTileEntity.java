package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import net.minecraft.tileentity.ConduitTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitTileEntity.class)
public class MixinConduitTileEntity {
    /**
     * @author NotStirred
     * @reason Conduits now only affect players within +256 of the block. This prevents near infinite cube loading in the conduit's column
     */
    //COMMENTED OUT BECAUSE OF A MIXIN BUG. It's not recursively checking interfaces, only the first
    //TODO: NotStirred merge this with BeaconTileEntity to reduce duplicated code
//    @Redirect(method = {"addEffectsToPlayers()V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I"))
//    private int on$getHeight(World world) {
//        return 256;
//    }
    @Redirect(method = "addEffectsToPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/AxisAlignedBB;expand(DDD)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB on$expand(AxisAlignedBB axisAlignedBB, double x, double y, double z) {
        return axisAlignedBB.expandTowards(x, 256, z);
    }
}