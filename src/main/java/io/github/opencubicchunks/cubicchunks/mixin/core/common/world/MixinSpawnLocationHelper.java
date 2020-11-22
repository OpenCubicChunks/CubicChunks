package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.SpawnPlaceFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerRespawnLogic.class)
public abstract class MixinSpawnLocationHelper {

    /**
     * @author NotStirred
     * @reason Overwriting finding spawn location
     */
    @Nullable
    @Overwrite
    public static BlockPos getOverworldRespawnPos(ServerLevel p_241092_0_, int posX, int posZ, boolean checkValid) {
        return SpawnPlaceFinder.getTopBlockBisect(p_241092_0_, new BlockPos(posX, 0, posZ), checkValid);
    }
}