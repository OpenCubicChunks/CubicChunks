package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.chunk.carver.GenHeightSetter;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConfiguredWorldCarver.class)
public class MixinConfiguredWorldCarver {


    @Shadow @Final private WorldCarver worldCarver;

    @Inject(method = "carve", at = @At("HEAD"), cancellable = true)
    private void dynamicallySetGenHeight(ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, Random random, int seaLevel, int chunkX, int chunkZ, int mainChunkX, int mainChunkZ,
                                         BitSet carvingMask, CallbackInfoReturnable<Boolean> cir) {

        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return;
        }

        ((GenHeightSetter) worldCarver).setGenHeight(chunk.getMaxBuildHeight());
    }
}
