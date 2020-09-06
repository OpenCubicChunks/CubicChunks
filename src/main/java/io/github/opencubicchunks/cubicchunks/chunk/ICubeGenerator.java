package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.feature.structure.StructureManager;

public interface ICubeGenerator {
    // func_230352_b_, fillFromNoise(IWorld var1, StructureManager var2, IChunk var3);
    default void makeBase(IWorld worldIn, StructureManager var2, IBigCube chunkIn) {
    }

    default void decorate(CubeWorldGenRegion region, StructureManager structureManager) {
    }
}