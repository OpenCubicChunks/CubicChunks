package io.github.opencubicchunks.cubicchunks.world.lighting;

import javax.annotation.Nullable;

import net.minecraft.world.level.BlockGetter;

public interface ICubeLightProvider {
    @Nullable BlockGetter getCubeForLighting(int sectionX, int sectionY, int sectionZ);
}