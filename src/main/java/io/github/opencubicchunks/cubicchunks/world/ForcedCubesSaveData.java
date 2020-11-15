package io.github.opencubicchunks.cubicchunks.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class ForcedCubesSaveData extends SavedData {
    private LongSet cubes = new LongOpenHashSet();

    public ForcedCubesSaveData() {
        this(new LongOpenHashSet());
    }

    public ForcedCubesSaveData(LongOpenHashSet cubes) {
        this.cubes = cubes;
    }

    public static ForcedCubesSaveData load(CompoundTag compoundTag) {
        return new ForcedCubesSaveData(new LongOpenHashSet(compoundTag.getLongArray("Forced")));
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.putLongArray("Forced", this.cubes.toLongArray());
        return compound;
    }

    public LongSet getCubes() {
        return this.cubes;
    }
}