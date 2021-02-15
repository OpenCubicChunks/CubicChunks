package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.SpawnPlaceFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRespawnLogic.class)
public abstract class MixinSpawnLocationHelper {

    /**
     * @author NotStirred
     * @reason Overwriting finding spawn location
     */
    @Nullable
    @Inject(method = "getOverworldRespawnPos", at = @At("HEAD"), cancellable = true)
    private static void getOverworldRespawnPos(ServerLevel world, int posX, int posZ, boolean checkValid, CallbackInfoReturnable<BlockPos> cir) {
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return;
        }


        cir.setReturnValue(SpawnPlaceFinder.getTopBlockBisect(world, new BlockPos(posX, 0, posZ), checkValid));
    }
}