/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.perf;

import java.util.HashMap;
import java.util.PriorityQueue;

public class Profiler {
	
	private static HashMap<String,ProfilerCounter> m_counters;
	
	static {
		reset();
	}
	
	public static void start(String name) {
		ProfilerCounter counter = m_counters.get(name);
		
		if (counter == null) {
			counter = new ProfilerCounter(name);
			m_counters.put(name, counter);
		}
		
		counter.start();
	}
	
	public static void stop(String name) {
		ProfilerCounter counter = m_counters.get(name);
		counter.stop();
	}
	
	public static void stopStart(String stopName, String startName) {
		stop(stopName);
		start(startName);
	}
	
	public static void reset() {
		m_counters = new HashMap<String,ProfilerCounter>();
	}
	
	public static String getReport() {
		// calculate total time
		long totalTime = 0;
		for (ProfilerCounter counter : m_counters.values()) {
			totalTime += counter.getElapsedMilliseconds();
		}
		
		// update percentages
		for (ProfilerCounter counter : m_counters.values()) {
			counter.setPercentTime(100.0 * (double)counter.getElapsedMilliseconds() / (double)totalTime);
		}
		
		// sort the counters
		PriorityQueue<ProfilerCounter> order = new PriorityQueue<ProfilerCounter>();
		for (ProfilerCounter counter : m_counters.values()) {
			order.add(counter);
		}
		
		// build the report
		StringBuilder buf = new StringBuilder();
		buf.append("Profiling Report:\n");
		ProfilerCounter counter = null;
		while ( (counter = order.poll()) != null) {
			buf.append(String.format("%8.2f", (double)counter.getElapsedMilliseconds() / 1000.0));
			buf.append("s (");
			buf.append(String.format("%6.2f", counter.getPercentTime()));
			buf.append("%): ");
			buf.append(counter.getName());
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	public static String getMemoryUsed() {
		long usedBytes = Runtime.getRuntime().totalMemory();
		
		double usedKibibytes = (double)usedBytes / 1024.0;
		if (usedKibibytes < 1000.0) {
			return String.format("%.2f", usedKibibytes) + "KiB";
		}
		
		double usedMebibytes = usedKibibytes / 1024.0;
		if (usedMebibytes < 1000.0) {
			return String.format("%.2f", usedMebibytes) + "MiB";
		}
		
		double usedGibibytes = usedMebibytes / 1024.0;
		return String.format("%.2f", usedGibibytes) + "GiB";
	}
}
