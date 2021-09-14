package io.github.opencubicchunks.cubicchunks.world.level.chunk.storage;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.IOWorker;

public interface CubicSectionStorage {

    void flush(CubePos cubePos);

    void updateCube(CubePos pos, CompoundTag tag);

    IOWorker getIOWorker();

}
