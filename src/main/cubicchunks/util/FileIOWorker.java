package cubicchunks.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileIOWorker implements Runnable {

	/** Instance of FileIOWorker */
	public static final FileIOWorker THREADED_IO_INSTANCE;

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
					this.queuedIO.remove();
				}
			} else {
				// give other threads a chance to run, since there is nothing to write
				// Thread.yield();
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
				System.out.println("Interrupted when trying to put a object into the queue!");
				e.printStackTrace();
			}
		}
	}

	public static FileIOWorker getThread() {
		return THREADED_IO_INSTANCE;
	}
}
