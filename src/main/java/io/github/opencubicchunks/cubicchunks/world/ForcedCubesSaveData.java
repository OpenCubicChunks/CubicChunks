package io.github.opencubicchunks.cubicchunks.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;

public class ForcedCubesSaveData extends WorldSavedData {
    private LongSet cubes = new LongOpenHashSet();

    public ForcedCubesSaveData() {
        super("cubes");
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    public void read(CompoundNBT nbt) {
        this.cubes = new LongOpenHashSet(nbt.getLongArray("Forced"));
    }

    public CompoundNBT write(CompoundNBT compound) {
        compound.putLongArray("Forced", this.cubes.toLongArray());
        return compound;
    }

    public LongSet getCubes() {
        return this.cubes;
    }
}