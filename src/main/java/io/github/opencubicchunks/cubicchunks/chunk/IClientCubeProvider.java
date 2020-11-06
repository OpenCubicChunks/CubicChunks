package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public interface IClientCubeProvider extends ICubeProvider {

    void drop(int x, int y, int z);

    void setCenter(int x, int y, int z);

    BigCube replaceWithPacketData(int cubeX, int cubeY, int cubeZ, @Nullable CubeBiomeContainer biomes, FriendlyByteBuf readBuffer, CompoundTag nbtTagIn,
                     boolean cubeExists);
}