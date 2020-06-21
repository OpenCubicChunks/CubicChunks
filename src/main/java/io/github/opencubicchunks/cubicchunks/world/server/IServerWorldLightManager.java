package io.github.opencubicchunks.cubicchunks.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.concurrent.ITaskExecutor;

import java.util.concurrent.CompletableFuture;

public interface IServerWorldLightManager {
    void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
            ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor);

    void setCubeStatusEmpty(CubePos cubePos);

    CompletableFuture<IBigCube> lightCube(IBigCube icube, boolean p_215593_2_);
}
