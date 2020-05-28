package cubicchunks.cc.chunk;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.biome.BiomeContainer;

import javax.annotation.Nullable;

public interface IColumn {

    void readSection(int cubeY, @Nullable BiomeContainer biomeContainerIn, PacketBuffer dataBuffer, CompoundNBT nbtIn, boolean cubeExists);
}
