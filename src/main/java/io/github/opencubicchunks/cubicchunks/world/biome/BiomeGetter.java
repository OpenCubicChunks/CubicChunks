package io.github.opencubicchunks.cubicchunks.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public interface BiomeGetter {

    void generate(StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, WorldGenLevel worldGenRegion, long seed, WorldgenRandom worldgenRandom, BlockPos blockPos);
}
