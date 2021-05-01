package io.github.opencubicchunks.cubicchunks.chunk.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

public class CCForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final AtomicLong serial = new AtomicLong(0);
    private final String namePattern;
    private final int priority;

    public CCForkJoinWorkerThreadFactory(String namePattern, int priority) {
        this.namePattern = namePattern;
        this.priority = priority;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        final CCForkJoinWorkerThread worker = new CCForkJoinWorkerThread(pool);
        worker.setName(String.format(namePattern, serial.incrementAndGet()));
        worker.setPriority(priority);
        worker.setDaemon(true);
        return worker;
    }

    private static class CCForkJoinWorkerThread extends ForkJoinWorkerThread {

        /**
         * Creates a ForkJoinWorkerThread operating in the given pool.
         *
         * @param pool the pool this thread works in
         * @throws NullPointerException if pool is null
         */
        protected CCForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

    }
}
