package io.github.opencubicchunks.cubicchunks.world.server;

import java.util.concurrent.CompletableFuture;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.thread.ProcessorHandle;

public interface IServerWorldLightManager {
    void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
                              ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor);

    void setCubeStatusEmpty(CubePos cubePos);

    CompletableFuture<IBigCube> lightCube(IBigCube icube, boolean p_215593_2_);
}