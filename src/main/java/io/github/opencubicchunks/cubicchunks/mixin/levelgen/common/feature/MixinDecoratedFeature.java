package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.util.BlockPosHeightMapDoubleMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.DecoratedFeature;
import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecoratedFeature.class)
public class MixinDecoratedFeature {


    @SuppressWarnings("UnresolvedMixinReference") @Inject(method = "lambda$place$0(Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;Lnet/minecraft/world/level/WorldGenLevel;"
        + "Lnet/minecraft/world/level/chunk/ChunkGenerator;Ljava/util/Random;Lorg/apache/commons/lang3/mutable/MutableBoolean;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"),
        cancellable = true)
    private static void dirtyPosHacks(ConfiguredFeature<?, ?> configuredFeature, WorldGenLevel level, ChunkGenerator generator, Random random, MutableBoolean mutable, BlockPos pos,
                               CallbackInfo ci) {
        if (!(configuredFeature.feature instanceof RandomPatchFeature) && pos instanceof BlockPosHeightMapDoubleMarker && ((BlockPosHeightMapDoubleMarker) pos).isEmpty()) {
            ci.cancel();
        }
    }
}
