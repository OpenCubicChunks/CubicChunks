package io.github.opencubicchunks.cubicchunks.utils;

public class Utils {
    @SuppressWarnings("unchecked")
    public static <I, O> O unsafeCast(I obj) {
        return (O) obj;
    }
}