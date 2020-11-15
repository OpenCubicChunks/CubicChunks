package io.github.opencubicchunks.cubicchunks.world.lighting;

import net.minecraft.world.level.BlockGetter;

import javax.annotation.Nullable;

public interface ICubeLightProvider {

    @Nullable BlockGetter getCubeForLighting(int sectionX, int sectionY, int sectionZ);
}