package io.github.opencubicchunks.cubicchunks.utils;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkIoMainThreadTaskUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final LinkedBlockingQueue<Runnable> MAIN_THREAD_QUEUE = new LinkedBlockingQueue<>();

    public static void executeMain(Runnable command) {
        MAIN_THREAD_QUEUE.add(command);
    }

    public static void drainQueue() {
        Runnable command;
        while ((command = MAIN_THREAD_QUEUE.poll()) != null) {
            try {
                command.run();
            } catch (Throwable t) {
                LOGGER.error("Error while executing main thread task", t);
            }
        }
    }

}
