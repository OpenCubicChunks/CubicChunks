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
package cubicchunks.util.processor;

import com.google.common.collect.Sets;
import cubicchunks.util.ArrayBatchedQueue;
import cubicchunks.world.provider.ICubeCache;

import java.util.Set;

public abstract class QueueProcessor<T> {

	protected String name;
	protected ICubeCache cache;
	private int batchSize;
	private ArrayBatchedQueue<T> queue;
	protected Set<T> incomingAddresses;
	protected Set<T> processedAddresses;
	protected Set<T> deferredAddresses;

	public QueueProcessor(String name, ICubeCache cache, int batchSize) {
		this.name = name;
		this.cache = cache;
		this.batchSize = batchSize;

		this.queue = new ArrayBatchedQueue<>();
		this.incomingAddresses = Sets.newHashSet();
		this.processedAddresses = Sets.newHashSet();
		this.deferredAddresses = Sets.newHashSet();
	}

	public String getName() {
		return this.name;
	}

	public void add(T address) {
		if (address == null) {
			throw new NullPointerException();
		}
		this.queue.add(address);
	}

	public int getNumInQueue() {
		return this.queue.size();
	}

	public int processQueueUntil(long timeStop) {
		this.processedAddresses.clear();
		this.deferredAddresses.clear();

		// is there time left?
		while (System.currentTimeMillis() < timeStop) {
			// get a batch of addresses
			this.incomingAddresses.clear();
			this.queue.getBatch(this.incomingAddresses, this.batchSize);

			// nothing left to do?
			if (this.incomingAddresses.isEmpty()) {
				break;
			}

			// process it
			processBatch();

			this.queue.removeAll(this.processedAddresses);
		}

		// put the deferred addresses back on the queue
		this.queue.addAll(this.deferredAddresses);

		return this.processedAddresses.size();
	}

	public int processQueue() {
		this.processedAddresses.clear();
		this.deferredAddresses.clear();

		// process all the addresses
		this.incomingAddresses.clear();
		this.queue.getAll(this.incomingAddresses);
		processBatch();

		// put the deferred addresses back on the queue
		this.queue.addAll(this.deferredAddresses);
		this.queue.removeAll(this.processedAddresses);

		return this.processedAddresses.size();
	}

	public Set<T> getProcessedAddresses() {
		return this.processedAddresses;
	}

	public abstract void processBatch();
}
