package io.github.opencubicchunks.cubicchunks.client.multiplayer;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeSource;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public interface ClientCubeCache extends CubeSource {

    void drop(int x, int y, int z);

    void updateViewCenter(int x, int y, int z);

    void updateCubeViewRadius(int hDistance, int vDistance);

    LevelCube replaceWithPacketData(int cubeX, int cubeY, int cubeZ, @Nullable ChunkBiomeContainer biomes, FriendlyByteBuf readBuffer, CompoundTag tag,
                                    boolean cubeExists);
}