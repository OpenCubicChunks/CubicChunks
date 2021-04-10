package io.github.opencubicchunks.cubicchunks.mixin.debug.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerRespawnLogic.class, priority = 999)
public class MixinPlayerRespawnLogic {
    @Inject(method = "getOverworldRespawnPos", at = @At("HEAD"), cancellable = true)
    private static void fakeRespawnLocation(ServerLevel world, int x, int z, boolean validSpawnNeeded, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(new BlockPos(0, 0, 0));
    }
}
