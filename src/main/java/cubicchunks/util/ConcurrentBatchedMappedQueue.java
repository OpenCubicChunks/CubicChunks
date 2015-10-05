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
package cubicchunks.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ConcurrentBatchedMappedQueue<k, T> {
	private LinkedHashMap<k, T> queue;

	public ConcurrentBatchedMappedQueue() {
		queue = new LinkedHashMap<k, T>();
	}

	public synchronized void add(k key, T val) {
		queue.put(key, val);
	}

	public synchronized void addAll(Map<k, T> vals) {
		queue.putAll(vals);
	}

	public synchronized boolean contains(k key) {
		return queue.containsKey(key);
	}

	public synchronized T get() {
		T val = queue.entrySet().iterator().next().getValue();
		return queue.remove(val);
	}

	public synchronized T get(k key) {
		return queue.get(key);
	}

	public synchronized boolean getBatch(LinkedHashMap<k, T> out, int size) {
		// copy the batch to the out list
		Iterator<Entry<k, T>> iter = queue.entrySet().iterator();
		for (int i = 0; i < size; i++) {
			if (!iter.hasNext()) {
				break;
			}
			Map.Entry<k, T> entry = iter.next();
			k key = entry.getKey();
			T val = entry.getValue();
			iter.remove();
			if (val == null) {
				break;
			}
			out.put(key, val);
		}

		// are there more entries?
		return !queue.isEmpty();
	}

	public synchronized int size() {
		return queue.size();
	}
}
