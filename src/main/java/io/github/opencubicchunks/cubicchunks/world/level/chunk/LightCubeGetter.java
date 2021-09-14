package io.github.opencubicchunks.cubicchunks.world.level.chunk;

import javax.annotation.Nullable;

import net.minecraft.world.level.BlockGetter;

public interface LightCubeGetter {
    @Nullable BlockGetter getCubeForLighting(int sectionX, int sectionY, int sectionZ);
}