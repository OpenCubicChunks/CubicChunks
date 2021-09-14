package io.github.opencubicchunks.cubicchunks.chunk.util;

import java.util.concurrent.ForkJoinPool;

public class ExecutorUtils {

    public static final ForkJoinPool SERIALIZER = new ForkJoinPool(
        Math.min(Runtime.getRuntime().availableProcessors(), 4),
        new CCForkJoinWorkerThreadFactory("ChunkSerializer-%d", Thread.NORM_PRIORITY - 1),
        null,
        true
    );

}
