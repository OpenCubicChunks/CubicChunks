package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.world.SpawnPlaceFinder;
import net.minecraft.entity.player.SpawnLocationHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(SpawnLocationHelper.class)
public abstract class MixinSpawnLocationHelper {

    /**
     * @author NotStirred
     * @reason Overwriting finding spawn location
     */
    @Nullable
    @Overwrite
    public static BlockPos func_241092_a_(ServerWorld p_241092_0_, int posX, int posZ, boolean checkValid) {
        return SpawnPlaceFinder.getTopBlockBisect(p_241092_0_, new BlockPos(posX, 0, posZ), checkValid);
    }
}
