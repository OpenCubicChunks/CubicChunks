/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.worldgen;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class WorldgenHangWatchdog {

    public static final boolean ENABLED = "true".equalsIgnoreCase(System.getProperty("cubicchunks.wgen_hang_watchdog", "true"));

    private static final WorldgenHangWatchdog INSTANCE = new WorldgenHangWatchdog();

    private static final Thread thread = init();

    private final WeakHashMap<Thread, Entry> entries = new WeakHashMap<>();

    private static volatile String crashInfo = null;

    private WorldgenHangWatchdog() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already initialized");
        }
    }

    public static String getCrashInfo() {
        return crashInfo;
    }

    public static void startWorldGen() {
        synchronized (INSTANCE.entries) {
            INSTANCE.entries.compute(Thread.currentThread(), (t, old) -> {
                if (old == null) {
                    return new Entry();
                }
                old.count++;
                return old;
            });
        }
    }

    public static void endWorldGen() {
        synchronized (INSTANCE.entries) {
            Entry e = INSTANCE.entries.get(Thread.currentThread());
            if (e != null) {
                if (e.count <= 0) {
                    INSTANCE.entries.remove(Thread.currentThread());
                } else {
                    e.count--;
                }
            }
        }
    }

    private static Thread init() {
        Thread t = new Thread(INSTANCE::run);
        t.setName("WorldGen hang watchdog thread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    @SuppressWarnings("deprecation")
    private void run() {
        if (!ENABLED) {
            return;
        }
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (entries) {
                for (Iterator<Map.Entry<Thread, Entry>> iterator = entries.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<Thread, Entry> entry = iterator.next();
                    Thread t = entry.getKey();
                    Entry e = entry.getValue();

                    e.samples.add(t.getStackTrace());

                    long currentTime = System.nanoTime();

                    long dt = currentTime - e.startTime;

                    if (dt > TimeUnit.MILLISECONDS.toNanos(CubicChunksConfig.worldgenWatchdogTimeLimit)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("World generation taking ").append(dt / (double) TimeUnit.SECONDS.toNanos(1))
                                .append(" seconds, should be less than 50ms. Stopping the server.\n");

                        sb.append("Samples collected during world generation:\n");
                        int i = 1;
                        for (StackTraceElement[] stacktrace : e.samples) {
                            sb.append("--------------------------------------------\n");

                            Set<String> likelyModsInvolved = CompatHandler.getModsForStacktrace(stacktrace);
                            sb.append("SAMPLE #").append(i).append(", likely mods involved: ").append(String.join(", ", likelyModsInvolved))
                                    .append('\n');
                            for (StackTraceElement traceElement : stacktrace) {
                                sb.append("\tat ").append(traceElement).append('\n');
                            }
                            i++;
                        }
                        String msg = sb.toString();
                        crashInfo = msg;
                        CubicChunks.LOGGER.fatal(msg);
                        t.stop();
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static class Entry {

        long startTime;
        int count;
        List<StackTraceElement[]> samples = new ArrayList<>();

        Entry() {
            startTime = System.nanoTime();
        }
    }
}
