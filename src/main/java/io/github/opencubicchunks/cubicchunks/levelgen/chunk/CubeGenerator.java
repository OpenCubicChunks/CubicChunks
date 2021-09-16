package io.github.opencubicchunks.cubicchunks.levelgen.chunk;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import net.minecraft.world.level.StructureFeatureManager;

public interface CubeGenerator {
    default void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager, ProtoCube cube) {
    }
}