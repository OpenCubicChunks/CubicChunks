package io.github.opencubicchunks.cubicchunks.mixin.core.common.blockentity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
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
public abstract class MixinBeaconBlockEntity {
    /**
     * @author OverInfrared & NotStirred
     * @reason Beacons now only affect players within +256 of the block. This prevents near infinite cube loading in the beacon's column
     */
    // TODO: redirect getHeight()
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB limitBeaconEffectYRange(AABB aabb, double x, double y, double z, Level level, BlockPos pos, int beaconLevel, @Nullable MobEffect primaryEffect,
                                                @Nullable MobEffect secondaryEffect) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return aabb.expandTowards(x, y, z);
        }
        CubicChunks.LOGGER.warn("on$expand called");
        // TODO: limit this to vertical view distance?
        return aabb.expandTowards(x, level.dimensionType().height(), z);
    }

}