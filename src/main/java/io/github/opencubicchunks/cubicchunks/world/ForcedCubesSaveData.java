package io.github.opencubicchunks.cubicchunks.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class ForcedCubesSaveData extends SavedData {
    private LongSet cubes = new LongOpenHashSet();

    public ForcedCubesSaveData() {
        super("cubes");
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    public void load(CompoundTag nbt) {
        this.cubes = new LongOpenHashSet(nbt.getLongArray("Forced"));
    }

    public CompoundTag save(CompoundTag compound) {
        compound.putLongArray("Forced", this.cubes.toLongArray());
        return compound;
    }

    public LongSet getCubes() {
        return this.cubes;
    }
}