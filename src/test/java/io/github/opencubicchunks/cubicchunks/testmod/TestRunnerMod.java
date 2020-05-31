/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2020 OpenCubicChunks
 *  Copyright (c) 2015-2020 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.testmod;

import io.github.opencubicchunks.cubicchunks.test.MinecraftTestRunner;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.github.opencubicchunks.cubicchunks.test.MinecraftTestRunner.TEST_WRAPPER_MESSAGE;

@Mod(TestRunnerMod.MODID)
public class TestRunnerMod {

    public static final LinkedBlockingQueue<FutureMinecraftTask> runQueue = new LinkedBlockingQueue<>(16);
    public static final AtomicBoolean isRunning = new AtomicBoolean(true);

    public static final String MODID = "testrunner";

    private static final Logger LOGGER = LogManager.getLogger();

    public TestRunnerMod() {
        LOGGER.info("Initializing TestRunnerMod");
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::gatherData);

        ClassLoader appClassLoader = this.getClass().getClassLoader().getClass().getClassLoader();
        try {
            final Class<?> minecraftTestRunnerClass = Class.forName("io.github.opencubicchunks.cubicchunks.test.MinecraftTestRunner", false, appClassLoader);
            Field theClassLoader = minecraftTestRunnerClass.getDeclaredField("gameClassLoader");
            theClassLoader.set(null, this.getClass().getClassLoader());

            Field runnableConsumer = minecraftTestRunnerClass.getDeclaredField("runnableConsumer");
            runnableConsumer.set(null, (Consumer<Runnable>) runnable -> {
                final FutureMinecraftTask e = new FutureMinecraftTask(runnable);
                runQueue.add(e);
                try {
                    e.get();
                } catch (InterruptedException ignored) {
                } catch (ExecutionException ex) {
                    throw new RuntimeException(TEST_WRAPPER_MESSAGE, ex.getCause());
                }
            });
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void gatherData(GatherDataEvent event) {
        LOGGER.info("Running gatherData");
        while (isRunning.get()) {
            System.out.println("Getting from run queue, classloader=" + MinecraftTestRunner.class.getClassLoader() + ", parent=" + MinecraftTestRunner.class.getClassLoader().getClass().getClassLoader());

            try {
                FutureMinecraftTask task = runQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    LOGGER.info("Got task " + task);
                    task.doRun();
                }
            } catch (InterruptedException ignored) {
            }
        }
        LOGGER.info("Finishing...");
    }


    public static class FutureMinecraftTask implements Future<Void> {

        private final Runnable runnable;
        private volatile Throwable throwable;
        private volatile boolean isDone = false;
        private volatile boolean isCancelled = false;
        private volatile Thread runningThread;

        private final Object waitLock = new Object();

        public FutureMinecraftTask(Runnable runnable) {
            this.runnable = runnable;
        }

        public void doRun() {
            if (isCancelled || isDone) {
                return;
            }
            runningThread = Thread.currentThread();
            try {
                runnable.run();
            } catch (Throwable e) {
                while (e instanceof RuntimeException && TEST_WRAPPER_MESSAGE.equals(e.getMessage())) {
                    e = e.getCause();
                }
                throwable = e;
            } finally {
                isDone = true;
                synchronized (waitLock) {
                    waitLock.notifyAll();
                }
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isCancelled) {
                return true;
            }
            if (isDone) {
                return false;
            }
            isCancelled = true;
            isDone = true;
            if (runningThread != null && mayInterruptIfRunning) {
                runningThread.interrupt();
            }
            synchronized (waitLock) {
                waitLock.notifyAll();
            }
            return true;
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public boolean isDone() {
            return isDone;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            synchronized (waitLock) {
                while (!isDone) {
                    waitLock.wait();
                }
            }
            if (throwable != null) {
                throw new ExecutionException(throwable);
            }
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            synchronized (waitLock) {
                waitLock.wait(unit.toMillis(timeout));
            }
            if (!isDone) {
                throw new TimeoutException();
            }
            if (throwable != null) {
                throw new ExecutionException(throwable);
            }
            return null;
        }

    }
}
