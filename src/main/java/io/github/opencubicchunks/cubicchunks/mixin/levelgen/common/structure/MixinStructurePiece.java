package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.structure;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class MixinStructurePiece {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getChunk(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/ChunkAccess;"),
        method = "placeBlock")
    private ChunkAccess getCube(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        if (((CubicLevelHeightAccessor) worldGenLevel).generates2DChunks()) {
            return worldGenLevel.getChunk(blockPos);
        }

        return (ChunkAccess) ((CubicLevelAccessor) worldGenLevel).getCube(blockPos);
    }

    @Redirect(method = "fillColumnDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMinBuildHeight()I"))
    private int fillToRegionBottom(WorldGenLevel level) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return level.getMinBuildHeight();
        }

        return Coords.cubeToMinBlock(((CubeWorldGenRegion) level).getMinCubeY());
    }
}
