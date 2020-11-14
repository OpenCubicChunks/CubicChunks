package io.github.opencubicchunks.cubicchunks.world.biome;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public interface BiomeGetter {

    void generate(StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, CubeWorldGenRegion worldGenRegion, long seed, WorldgenRandom worldgenRandom, BlockPos blockPos);
}
