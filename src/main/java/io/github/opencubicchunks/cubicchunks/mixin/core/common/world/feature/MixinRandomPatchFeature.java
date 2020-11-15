package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RandomPatchFeature.class)
public class MixinRandomPatchFeature {

//    @Inject(at = @At("HEAD"), method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/feature/configurations/RandomPatchConfiguration;)Z", cancellable = true)
//    private void cancelPatch(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, RandomPatchConfiguration randomPatchConfiguration, CallbackInfoReturnable<Boolean> cir) {
//        cir.cancel();
//    }
}
