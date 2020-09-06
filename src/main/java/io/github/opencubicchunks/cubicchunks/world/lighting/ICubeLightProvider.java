package io.github.opencubicchunks.cubicchunks.world.lighting;

import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public interface ICubeLightProvider {

    @Nullable IBlockReader getCubeForLight(int sectionX, int sectionY, int sectionZ);
}