package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public class MixinNaturalSpawner {
    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void cancelSpawnForChunk(ServerLevel serverLevel, LevelChunk levelChunk, NaturalSpawner.SpawnState spawnState, boolean bl,
                                            boolean bl2, boolean bl3, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            return;
        }

        ci.cancel(); // TODO: mob spawning
    }
}
