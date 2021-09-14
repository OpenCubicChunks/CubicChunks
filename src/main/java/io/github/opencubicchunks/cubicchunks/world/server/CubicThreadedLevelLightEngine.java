package io.github.opencubicchunks.cubicchunks.world.server;

import java.util.concurrent.CompletableFuture;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.server.level.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.util.thread.ProcessorHandle;

public interface CubicThreadedLevelLightEngine {
    void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
                              ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor);

    void setCubeStatusEmpty(CubePos cubePos);

    CompletableFuture<CubeAccess> lightCube(CubeAccess icube, boolean p_215593_2_);
}