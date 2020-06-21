package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.IWorld;

public interface ICubeGenerator {
    default void makeBase(IWorld worldIn, IBigCube chunkIn) {
    }

    default void decorate(CubeWorldGenRegion region) {
    }
}
