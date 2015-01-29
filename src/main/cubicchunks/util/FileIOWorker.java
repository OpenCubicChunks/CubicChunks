package cubicchunks.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileIOWorker implements Runnable {

    /** Instance of ThreadedFileIOBase */
    public static final FileIOWorker THREADED_IO_INSTANCE;

    private BlockingQueue<IThreadedFileIO> threadedIOQueue = new LinkedBlockingQueue<IThreadedFileIO>();
    
    static {
    	THREADED_IO_INSTANCE = new FileIOWorker();
    }

    private FileIOWorker() {
        Thread var1 = new Thread(this, "File IO Thread");
        var1.setPriority(1);
        var1.start();
    }

    @Override
    public void run() {
        while (true) { // Run continuously
            try {
                //Get the next item on the queue, if there isn't anything on it, block the thread until something is on it
                IThreadedFileIO var2 = (IThreadedFileIO) this.threadedIOQueue.take();
                var2.writeNextIO();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted!");
                e.printStackTrace();
            }
        }
    }

    /**
     * threaded io
     */
    public void queueIO(IThreadedFileIO threadedFileIO) {
        if (!this.threadedIOQueue.contains(threadedFileIO)) {
            try {
                this.threadedIOQueue.put(threadedFileIO);
            } catch (InterruptedException e) {
                System.out.println("Interrupted when trying to put a object into the queue!");
                e.printStackTrace();
            }
        }
    }
}
