package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.chunk.carver.GenHeightSetter;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldCarver.class)
public class MixinWorldCarver implements GenHeightSetter {


    @Mutable @Shadow @Final protected int genHeight;

    @Override public void setGenHeight(int genHeight) {
        this.genHeight = genHeight - 1;
    }

    @Redirect(method = "carveSphere", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1))
    private int useChunkMinY(int a, int b, ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, long seed, int seaLevel, int chunkX, int chunkZ, double x, double y, double z, double yaw,
                             double pitch, BitSet carvingMask) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return Math.max(a, b);
        }

        return Math.max(Mth.floor(y - pitch) - 1, chunk.getMinBuildHeight());
    }

    @Redirect(method = "carveSphere", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 1))
    private int useChunkY(int a, int b, ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, long seed, int seaLevel, int chunkX, int chunkZ, double x, double y, double z, double yaw,
                          double pitch, BitSet carvingMask) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return Math.min(a, b);
        }

        return Math.max(Mth.floor(y + pitch) + 1, chunk.getMaxBuildHeight());
    }

    @ModifyConstant(method = "hasWater", constant = { @Constant(intValue = 1, ordinal = 0), @Constant(intValue = 1, ordinal = 1) })
    private int changeYCheckRange(int arg0) { //TODO: VANILLA CHUNKS: Use arg0. Fix water check
        return 0;
    }

    @ModifyVariable(method = "carveBlock", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;get(I)Z", ordinal = 0, shift = At.Shift.BEFORE), ordinal = 8)
    private int useRelativeY(int arg0, ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, BitSet carvingMask, Random random, BlockPos.MutableBlockPos mutableBlockPos,
                             BlockPos.MutableBlockPos mutableBlockPos2, BlockPos.MutableBlockPos mutableBlockPos3, int seaLevel, int mainChunkX, int mainChunkZ, int x, int z, int relativeX,
                             int y, int relativeZ, MutableBoolean mutableBoolean) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return relativeX | relativeZ << 4 | y << 8;
        }

        return relativeX | relativeZ << 4 | (y - chunk.getMinBuildHeight()) << 8;
    }

    @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
    private void checkYIsInCube(ChunkAccess chunk, Function<BlockPos, Biome> posToBiome, BitSet carvingMask, Random random, BlockPos.MutableBlockPos mutableBlockPos,
                                BlockPos.MutableBlockPos mutableBlockPos2, BlockPos.MutableBlockPos mutableBlockPos3, int seaLevel, int mainChunkX, int mainChunkZ, int x, int z,
                                int relativeX, int y, int relativeZ, MutableBoolean mutableBoolean, CallbackInfoReturnable<Boolean> cir) {

        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return;
        }

        if (y >= chunk.getMaxBuildHeight() || y < chunk.getMinBuildHeight()) {
            cir.setReturnValue(false);
        }
    }
}
