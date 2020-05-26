package cubicchunks.cc.chunk.util;

public class Utils {
    @SuppressWarnings("unchecked")
    public static <I, O> O unsafeCast(I obj) {
        return (O) obj;
    }
}
