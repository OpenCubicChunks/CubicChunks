package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;

public interface IClientCubeProvider extends ICubeProvider {

    void unloadCube(int x, int y, int z);

    void setCenter(int x, int y, int z);

    Cube loadCube(int cubeX, int cubeY, int cubeZ, @Nullable CubeBiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn,
            boolean cubeExists);
}
