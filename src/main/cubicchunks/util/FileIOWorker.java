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
package cubicchunks.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIOWorker implements Runnable {

	/** Instance of FileIOWorker */
	public static final FileIOWorker THREADED_IO_INSTANCE;
	private static final Logger LOGGER = LoggerFactory.getLogger(FileIOWorker.class);

	private BlockingQueue<IThreadedFileIO> queuedIO = new LinkedBlockingQueue<IThreadedFileIO>();

	static {
		THREADED_IO_INSTANCE = new FileIOWorker();
	}

	public FileIOWorker() {
		Thread var1 = new Thread(this, "File IO Thread");
		var1.setPriority(1);
		var1.start();
	}

	@Override
	public void run() {
		while (true) { // Run continuously
			// Get the next item on the queue, without removing it from the queue
			IThreadedFileIO nextIO = this.queuedIO.peek();

			boolean moreIO = false;
			if (nextIO != null) {
				// do the work
				moreIO = nextIO.write();

				// if there is no more work for the current IO, remove the head
				if (!moreIO) {
					LOGGER.debug("No more IO to write! Removing it from the head.");
					this.queuedIO.remove();
				}
			}
		}
	}

	/**
	 * threaded io
	 */
	public void queueIO(final IThreadedFileIO threadedFileIO) {
		if (!this.queuedIO.contains(threadedFileIO)) {
			try {
				this.queuedIO.put(threadedFileIO);
			} catch (InterruptedException e) {
				LOGGER.info("Interrupted when trying to put a object into the queue!");
				e.printStackTrace();
			}
		}
	}

	public static FileIOWorker getThread() {
		return THREADED_IO_INSTANCE;
	}
}
