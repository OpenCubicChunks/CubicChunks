package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveWorldCarver.class)
public abstract class MixinCaveWorldCarver {

    @Shadow protected abstract int getCaveY(Random random);

    @Inject(method = "carve", at = @At("HEAD"), cancellable = true)
    private void cancelCaves(ChunkAccess chunkAccess, Function<BlockPos, Biome> function, Random random, int i, int j, int k, int l, int m, BitSet bitSet,
                             ProbabilityFeatureConfiguration probabilityFeatureConfiguration, CallbackInfoReturnable<Boolean> cir) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return;
        }

        int cubeMinY = chunkAccess.getMinBuildHeight();
        if (cubeMinY > 128 || cubeMinY < -128) {
            cir.setReturnValue(false);
        }
    }


    @Redirect(method = "carve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/carver/CaveWorldCarver;getCaveY(Ljava/util/Random;)I"))
    private int setYForCube(CaveWorldCarver caveWorldCarver, Random random, ChunkAccess chunkAccess, Function<BlockPos, Biome> function, Random random2, int i, int j, int k, int l, int m,
                            BitSet bitSet, ProbabilityFeatureConfiguration probabilityFeatureConfiguration) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return this.getCaveY(random);
        }

        return caveYForCube(chunkAccess, random);
    }

    private static int caveYForCube(ChunkAccess chunk, Random random) {
        if (chunk.getMinBuildHeight() == 0) {
            return random.nextInt(random.nextInt(chunk.getHeight() - 8) + 8);
        } else {
            return random.nextInt(random.nextInt(chunk.getHeight()) + 1);
        }
    }
}
