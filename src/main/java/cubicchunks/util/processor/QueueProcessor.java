/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.util.processor;

import java.util.List;

import com.google.common.collect.Lists;

import cubicchunks.util.ArrayBatchedQueue;
import cubicchunks.util.Progress;
import cubicchunks.world.ICubeCache;

public abstract class QueueProcessor {
	
	protected String name;
	protected ICubeCache cache;
	private int batchSize;
	private ArrayBatchedQueue<Long> queue;
	protected List<Long> incomingAddresses;
	protected List<Long> processedAddresses;
	protected List<Long> deferredAddresses;
	
	public QueueProcessor(String name, ICubeCache cache, int batchSize) {
		this.name = name;
		this.cache = cache;
		this.batchSize = batchSize;
		
		this.queue = new ArrayBatchedQueue<Long>();
		this.incomingAddresses = Lists.newArrayList();
		this.processedAddresses = Lists.newArrayList();
		this.deferredAddresses = Lists.newArrayList();
	}
	
	public String getName() {
		return this.name;
	}
	
	public void add(long address) {
		this.queue.add(address);
	}
	
	public void addAll(List<Long> addresses) {
		this.queue.addAll(addresses);
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
		}
		
		// put the deferred addresses back on the queue
		this.queue.addAll(this.deferredAddresses);
		
		return this.processedAddresses.size();
	}
	
	public int processQueue(Progress progress) {
		this.processedAddresses.clear();
		this.deferredAddresses.clear();
		
		// process all the addresses
		this.incomingAddresses.clear();
		this.queue.getAll(this.incomingAddresses);
		processBatch(progress);
		
		// put the deferred addresses back on the queue
		this.queue.addAll(this.deferredAddresses);
		
		return this.processedAddresses.size();
	}
	
	public List<Long> getProcessedAddresses() {
		return this.processedAddresses;
	}
	
	public String getProcessingReport() {
		return String.format("\t%15s: %3d processed, %d remaining", this.name, this.processedAddresses.size(), this.queue.size());
	}
	
	public void processBatch() {
		processBatch(null);
	}
	
	public abstract void processBatch(Progress progress);
}
