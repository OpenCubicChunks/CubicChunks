package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.WorldGenRegion;

public interface ICubeGenerator {
    default void makeBase(IWorld worldIn, ICube chunkIn) {
    }

    default void decorate(CubeWorldGenRegion region) {
    }
}
