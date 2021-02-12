package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;

public interface ICubeGenerator {
    default void decorate(CubeWorldGenRegion region, StructureFeatureManager structureManager) {
    }
}