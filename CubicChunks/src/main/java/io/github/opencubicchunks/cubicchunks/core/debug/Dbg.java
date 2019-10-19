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
package io.github.opencubicchunks.cubicchunks.core.debug;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Class for debug tracing
 * <p>
 * Instead of using normal logging, it creates special file.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Dbg {

    private static PrintWriter pw;

    static {
        if (!CubicChunks.DEBUG_ENABLED) {
            pw = null;
        } else {
            restart();
        }
    }

    public static void restart() {
        PrintWriter p;
        if (pw != null) {
            pw.close();
        }
        try {
            p = new PrintWriter(new File("DEBUG_" + System.currentTimeMillis()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        pw = p;
        Runtime.getRuntime().addShutdownHook(new Thread(pw::close));
    }

    public static void p(String format, Object... objs) {
        if (!CubicChunks.DEBUG_ENABLED) {
            return;
        }
        pw.printf(format, objs);
    }

    public static void l(String format, Object... objs) {
        if (!CubicChunks.DEBUG_ENABLED) {
            return;
        }
        p("[%s] ", Thread.currentThread().getName());
        p(format, objs);
        pw.println();
    }


}
