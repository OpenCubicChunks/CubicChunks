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
            //Get the next item on the queue, without removing it from the queue
            IThreadedFileIO nextIO = (IThreadedFileIO) this.threadedIOQueue.peek();
            
            // do the work
            boolean moreIO = nextIO.writeNextIO();
            
            // if there is no more work for the current IO, remove the head
            if (!moreIO) {
                this.threadedIOQueue.remove();
                
                //give other threads a chance to run, since we finished writing one set of IO
                Thread.yield();
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
