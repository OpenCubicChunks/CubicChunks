package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import net.minecraft.world.level.levelgen.feature.VinesFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VinesFeature.class)
public class MixinVineFeature {

//    @Inject(at = @At("HEAD"), method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/feature/configurations/NoneFeatureConfiguration;)Z", cancellable = true)
//    private void cancelVines(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, NoneFeatureConfiguration noneFeatureConfiguration, CallbackInfoReturnable<Boolean> cir) {
//        cir.cancel();
//    }
}
