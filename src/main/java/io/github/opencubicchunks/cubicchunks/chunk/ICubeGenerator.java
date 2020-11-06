package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;

public interface ICubeGenerator {
    // func_230352_b_, fillFromNoise(IWorld var1, StructureManager var2, IChunk var3);
    default void makeBase(LevelAccessor worldIn, StructureFeatureManager var2, IBigCube chunkIn) {
    }

    default void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager) {
    }
}