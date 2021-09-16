package io.github.opencubicchunks.cubicchunks.mixin.core.common.blockentity;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitBlockEntity.class)
public class MixinConduitBlockEntity {
    /**
     * @author NotStirred
     * @reason Conduits now only affect players within +256 of the block. This prevents near infinite cube loading in the conduit's column
     */
    // TODO: NotStirred merge this with BeaconTileEntity to reduce duplicated code
    // TODO: redirect getHeight()
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB on$expand(AABB aabb, double x, double y, double z, Level level, BlockPos pos, List<BlockPos> activatingBlocks) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return aabb.expandTowards(x, y, z);
        }
        // TODO: limit this to vertical view distance?
        return aabb.expandTowards(x, level.dimensionType().height(), z);
    }
}