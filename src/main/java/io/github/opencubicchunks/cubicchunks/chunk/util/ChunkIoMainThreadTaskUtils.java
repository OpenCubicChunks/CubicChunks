package io.github.opencubicchunks.cubicchunks.chunk.util;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkIoMainThreadTaskUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final LinkedBlockingQueue<Runnable> mainThreadQueue = new LinkedBlockingQueue<>();

    public static void executeMain(Runnable command) {
        mainThreadQueue.add(command);
    }

    public static void drainQueue() {
        Runnable command;
        while ((command = mainThreadQueue.poll()) != null) {
            try {
                command.run();
            } catch (Throwable t) {
                LOGGER.error("Error while executing main thread task", t);
            }
        }
    }

}
