package io.github.opencubicchunks.cubicchunks.chunk;

import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;

public interface ICubeGenerator {
    default void makeBase(IWorld worldIn, ICube chunkIn) {
    }
}
