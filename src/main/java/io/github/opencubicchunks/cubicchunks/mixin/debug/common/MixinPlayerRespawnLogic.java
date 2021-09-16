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
    private static final boolean DEBUG_LOAD_ORDER_ENABLED = System.getProperty("cubicchunks.debug.loadorder", "false").equals("true");

    @Inject(method = "getOverworldRespawnPos", at = @At("HEAD"), cancellable = true)
    private static void fakeRespawnLocation(ServerLevel level, int x, int z, boolean validSpawnNeeded, CallbackInfoReturnable<BlockPos> cir) {
        if (!DEBUG_LOAD_ORDER_ENABLED) {
            return;
        }
        cir.setReturnValue(new BlockPos(0, 0, 0));
    }
}
