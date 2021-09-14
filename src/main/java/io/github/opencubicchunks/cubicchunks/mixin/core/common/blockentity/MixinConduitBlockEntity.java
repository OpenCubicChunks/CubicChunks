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
    //COMMENTED OUT BECAUSE OF A MIXIN BUG. Mixin AP is not recursively checking interfaces, only the first, so it's not added to refmap
    //TODO: NotStirred merge this with BeaconTileEntity to reduce duplicated code
//    @Redirect(method = {"addEffectsToPlayers()V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I"))
//    private int on$getHeight(World world) {
//        return 256;
//    }
    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB on$expand(AABB axisAlignedBB, double x, double y, double z, Level world, BlockPos pos, List<BlockPos> activatingBlocks) {
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return axisAlignedBB.expandTowards(x, y, z);
        }


        return axisAlignedBB.expandTowards(x, world.dimensionType().height(), z);
    }
}