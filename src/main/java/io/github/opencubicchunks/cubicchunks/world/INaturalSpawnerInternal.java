package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

//TODO: Implement in the transformer
public interface INaturalSpawnerInternal {


    static BlockPos getRandomPosWithinCube(Level level, ChunkAccess chunkAccess) {
        CubePos pos = ((CubeAccess) chunkAccess).getCubePos();
        int blockX = pos.minCubeX() + level.random.nextInt(CubeAccess.DIAMETER_IN_BLOCKS);
        int blockZ = pos.minCubeZ() + level.random.nextInt(CubeAccess.DIAMETER_IN_BLOCKS);

        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) + 1; //This is wrong, we need to use the one from the BigCube(ChunkAccess)

        int minY = pos.minCubeY();
        if (minY > height) {
            return new BlockPos(blockX, Integer.MIN_VALUE, blockZ);
        }

        if (pos.maxCubeY() <= height) {
            int blockY = minY + level.random.nextInt(CubeAccess.DIAMETER_IN_BLOCKS);
            return new BlockPos(blockX, blockY, blockZ);
        }

        return new BlockPos(blockX, Mth.randomBetweenInclusive(level.random, minY, height), blockZ);
    }
}
