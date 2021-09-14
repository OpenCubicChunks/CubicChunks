package io.github.opencubicchunks.cubicchunks.world;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.TickNextTickData;

public interface CubicServerTickList<T> {
    List<TickNextTickData<T>> fetchTicksInCube(CubePos pos, boolean updateState, boolean getStaleTicks);
    ListTag save(CubePos cubePos);
}
