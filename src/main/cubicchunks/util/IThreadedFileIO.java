package cubicchunks.util;

public interface IThreadedFileIO {

    /**
     * Returns a boolean stating if the write was successful.
     * 
     * @return TRUE - write successful
     *         <p>
     *         FALSE - write failed
     */
    boolean writeNextIO();
}
