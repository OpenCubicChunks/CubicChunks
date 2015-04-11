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
package cubicchunks.util;

public class FastIntQueue {
	
	private int[] m_queue;
	private int m_start;
	private int m_stop;
	
	public FastIntQueue() {
		m_queue = new int[32768];
		clear();
	}
	
	public boolean hasRoomFor(int n) {
		return m_stop + n <= m_queue.length;
	}
	
	public void add(int val) {
		m_queue[m_stop++] = val;
	}
	
	public boolean hasNext() {
		return m_start < m_stop;
	}
	
	public int get() {
		return m_queue[m_start++];
	}
	
	public int size() {
		return m_stop;
	}
	
	public void clear() {
		m_start = 0;
		m_stop = 0;
	}
	
	public void reset() {
		m_start = 0;
	}
}