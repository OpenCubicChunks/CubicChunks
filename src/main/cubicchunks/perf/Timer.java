/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.perf;

public class Timer {
	
	private String m_name;
	private long m_startTime;
	private long m_stopTime;
	private boolean m_isRunning;
	
	public Timer() {
		this("Timer");
	}
	
	public Timer(String name) {
		m_name = name;
		m_startTime = 0;
		m_stopTime = 0;
		m_isRunning = false;
	}
	
	public String getName() {
		return m_name;
	}
	
	public long getStartTime() {
		return m_startTime;
	}
	
	public long getStopTime() {
		return m_stopTime;
	}
	
	public boolean isRunning() {
		return m_isRunning;
	}
	
	@Override
	public String toString() {
		return m_name + " : " + getElapsedTime();
	}
	
	public void start() {
		m_isRunning = true;
		m_startTime = getTime();
		m_stopTime = -1;
	}
	
	public void stop() {
		m_isRunning = false;
		m_stopTime = getTime();
	}
	
	public long getElapsedMilliseconds() {
		if (m_isRunning) {
			return getTime() - m_startTime;
		} else {
			return m_stopTime - m_startTime;
		}
	}
	
	public float getElapsedSeconds() {
		return getElapsedMilliseconds() / 1000.0f;
	}
	
	public float getElapsedMinutes() {
		return getElapsedMilliseconds() / 1000.0f / 60.0f;
	}
	
	public float getElapsedHours() {
		return getElapsedMilliseconds() / 1000.0f / 60.0f / 60.0f;
	}
	
	public String getElapsedTime() {
		float seconds = getElapsedSeconds();
		if (seconds < 60.0) {
			return String.format("%.2fs", seconds);
		}
		
		float minutes = getElapsedMinutes();
		if (minutes < 60) {
			return String.format("%.2fm", minutes);
		}
		
		float hours = getElapsedHours();
		return String.format("%.2fh", hours);
	}
	
	private long getTime() {
		return System.currentTimeMillis();
	}
}
