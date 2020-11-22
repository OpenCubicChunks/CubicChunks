package io.github.opencubicchunks.cubicchunks.world.biome;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;

public interface BiomeGetter {

    void generate(StructureFeatureManager structureManager, ChunkGenerator chunkGenerator, CubeWorldGenRegion region, long seed, CubeWorldGenRandom random, BlockPos blockPos);
}
