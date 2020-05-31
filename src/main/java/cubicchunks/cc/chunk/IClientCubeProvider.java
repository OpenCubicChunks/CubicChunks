package cubicchunks.cc.chunk;

import cubicchunks.cc.chunk.biome.CubeBiomeContainer;
import cubicchunks.cc.chunk.cube.Cube;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public interface IClientCubeProvider extends ICubeProvider {

    void unloadCube(int x, int y, int z);

    void setCenter(int x, int y, int z);

    Cube loadCube(int cubeX, int cubeY, int cubeZ, @Nullable CubeBiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn,
            boolean cubeExists);
}
