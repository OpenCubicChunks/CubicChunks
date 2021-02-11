package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import java.util.BitSet;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.chunk.carver.GenHeightSetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldCarver.class)
public class MixinWorldCarver implements GenHeightSetter {


    @Mutable @Shadow @Final protected int genHeight;

    @Override public void setGenHeight(int genHeight) {
        this.genHeight = genHeight - 1;
    }

    @Redirect(method = "carveSphere", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1))
    private int useChunkMinY(int a, int b, ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, long seed, int seaLevel, int chunkX, int chunkZ, double x, double y, double z, double yaw,
                             double pitch, BitSet carvingMask) {
        return Math.max(Mth.floor(y - pitch) - 1, chunk.getMinBuildHeight());
    }

    @Redirect(method = "carveSphere", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 1))
    private int useChunkY(int a, int b, ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, long seed, int seaLevel, int chunkX, int chunkZ, double x, double y, double z, double yaw,
                          double pitch, BitSet carvingMask) {
        return Math.max(Mth.floor(y + pitch) + 1, chunk.getMaxBuildHeight());
    }

    @ModifyConstant(method = "hasWater", constant =  {@Constant(intValue = 1, ordinal = 0), @Constant(intValue = 1, ordinal = 1)})
    private int changeYCheckRange(int arg0) { //TODO: Fix water check
        return 0;
    }
}
