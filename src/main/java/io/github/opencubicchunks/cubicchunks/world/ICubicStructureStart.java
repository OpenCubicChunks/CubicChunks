package io.github.opencubicchunks.cubicchunks.world;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface ICubicStructureStart {

    void init3dPlacement(int sectionY);

    //We use a BlockPos as our final parameter in place of a chunk position.
    void placeInCube(WorldGenLevel worldGenLevel, StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, BlockPos chunkPos);

    boolean has3DPlacement();
}
