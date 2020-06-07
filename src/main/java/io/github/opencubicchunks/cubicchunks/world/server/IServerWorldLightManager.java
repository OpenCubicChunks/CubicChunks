package io.github.opencubicchunks.cubicchunks.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;

public interface IServerWorldLightManager {
    void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
            ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor);

    void updateCubeStatus(CubePos cubePos);
}
