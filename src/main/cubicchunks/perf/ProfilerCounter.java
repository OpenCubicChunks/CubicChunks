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

public class ProfilerCounter implements Comparable<ProfilerCounter> {
	
	private String m_name;
	private long m_elapsedMilliseconds;
	private Timer m_timer;
	private double m_percentTime;
	
	public ProfilerCounter(String name) {
		// save parameters
		m_name = name;
		
		// init defaults
		m_elapsedMilliseconds = 0;
		m_timer = null;
		m_percentTime = 0.0;
	}
	
	public String getName() {
		return m_name;
	}
	
	public long getElapsedMilliseconds() {
		return m_elapsedMilliseconds;
	}
	
	public double getPercentTime() {
		return m_percentTime;
	}
	
	public void setPercentTime(double val) {
		m_percentTime = val;
	}
	
	public void start() {
		m_timer = new Timer();
		m_timer.start();
	}
	
	public void stop() {
		m_timer.stop();
		m_elapsedMilliseconds += m_timer.getElapsedMilliseconds();
		m_timer = null;
	}
	
	@Override
	public int compareTo(ProfilerCounter other) {
		if (m_percentTime > other.m_percentTime) {
			return -1;
		} else {
			return 1;
		}
	}
}
