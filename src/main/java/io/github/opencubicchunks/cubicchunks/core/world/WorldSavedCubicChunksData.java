package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;

public class WorldSavedCubicChunksData extends WorldSavedData {

    public boolean isCubicChunks = false;
    public int minHeight = 0, maxHeight = 256;

    public WorldSavedCubicChunksData() {
        super("cubicChunksData");
    }

    public WorldSavedCubicChunksData(boolean isCC) {
        this();
        if (isCC) {
            minHeight = Coords.MIN_BLOCK_Y;
            maxHeight = Coords.MAX_BLOCK_Y;
            isCubicChunks = true;
        }
    }

    @Override
    public void read(CompoundNBT nbt) {
        minHeight = nbt.getInt("minHeight");
        maxHeight = nbt.getInt("maxHeight");
        isCubicChunks = !nbt.contains("isCubicChunks") || nbt.getBoolean("isCubicChunks");
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.putInt("minHeight", minHeight);
        compound.putInt("maxHeight", maxHeight);
        compound.putBoolean("isCubicChunks", isCubicChunks);
        return compound;
    }

}
