package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(BeaconBlockEntity.class)
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
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB on$expand(AABB axisAlignedBB, double x, double y, double z, Level world, BlockPos pos, int beaconLevel, @Nullable MobEffect primaryEffect,
                                  @Nullable MobEffect secondaryEffect) {
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return axisAlignedBB.expandTowards(x, y, z);
        }

        CubicChunks.LOGGER.warn("on$expand called");
        return axisAlignedBB.expandTowards(x, world.dimensionType().height(), z);
    }

}