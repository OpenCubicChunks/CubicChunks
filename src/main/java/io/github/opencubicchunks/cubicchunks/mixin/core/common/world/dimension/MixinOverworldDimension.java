package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.dimension;

import io.github.opencubicchunks.cubicchunks.world.SpawnPlaceFinder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(OverworldDimension.class)
public abstract class MixinOverworldDimension extends Dimension {

    public MixinOverworldDimension(World p_i225788_1_, DimensionType p_i225788_2_, float p_i225788_3_) {
        super(p_i225788_1_, p_i225788_2_, p_i225788_3_);
    }

    /**
     * @author NotStirred
     * @reason Overwriting finding spawn location
     */
    @Nullable
    @Overwrite
    public BlockPos findSpawn(int posX, int posZ, boolean checkValid) {
        return SpawnPlaceFinder.getTopBlockBisect(this.world, new BlockPos(posX, 0, posZ));
    }
}
