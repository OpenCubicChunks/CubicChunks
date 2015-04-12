package cubicchunks.util;

public interface IThreadedFileIO {

    /**
     * Returns a boolean stating if there is more IO to write.
     * 
     * @return TRUE - more IO to write in the current object.
     *         <p>
     *         FALSE - no more IO to write in the current object.
     */
    boolean tryWrite();
}
