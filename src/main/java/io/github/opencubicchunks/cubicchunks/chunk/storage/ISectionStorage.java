package io.github.opencubicchunks.cubicchunks.chunk.storage;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.IOWorker;

public interface ISectionStorage {

    void flush(CubePos cubePos);

    void updateCube(CubePos pos, CompoundTag tag);

    IOWorker getIOWorker();

}
