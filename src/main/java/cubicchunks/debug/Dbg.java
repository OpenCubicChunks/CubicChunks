/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;

import cubicchunks.CubicChunks;

/**
 * Class for debug tracing
 * <p>
 * Instead of using normal logging, it creates special file.
 */
public class Dbg {
	private static final PrintWriter pw;

	static {
		if (!CubicChunks.DEBUG_ENABLED) {
			pw = null;
		} else {
			PrintWriter p;
			try {
				p = new PrintWriter(new File("DEBUG_" + new Date()));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			pw = p;
			Runtime.getRuntime().addShutdownHook(new Thread(pw::close));
		}
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
