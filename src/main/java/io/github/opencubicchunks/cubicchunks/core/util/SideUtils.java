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
package io.github.opencubicchunks.cubicchunks.core.util;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Methods for running code on client/server side.
 *
 * Based on ideas from Forge 1.14.x DistExecutor.
 */
public class SideUtils {
    public static <T> T getForSide(Supplier<Supplier<T>> client, Supplier<Supplier<T>> server) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return client.get().get();
        } else {
            return server.get().get();
        }
    }

    public static <T, R> R getForSide(T param, Supplier<Function<T, R>> client, Supplier<Function<T, R>> server) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return client.get().apply(param);
        } else {
            return server.get().apply(param);
        }
    }

    public static void runForSide(Supplier<Runnable> client, Supplier<Runnable> server) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            client.get().run();
        } else {
            server.get().run();
        }
    }

    public static void runForClient(Supplier<Runnable> toRun) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            toRun.get().run();
        }
    }
}
