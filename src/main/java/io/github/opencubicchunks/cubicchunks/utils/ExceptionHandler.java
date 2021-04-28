package io.github.opencubicchunks.cubicchunks.utils;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.util.StringRepresentable;

/**
 * Simple enum representing a failure.
 */
public enum ExceptionHandler implements StringRepresentable {
    EXCEPTION_THROWN(true, true, "When this fails, the exception will be thrown causing a crash."),
    EXCEPTION_LOGGED(true, false, "When this fails, the exception is caught and logged."),
    EXCEPTION_SILENCED(false, false, "When this fails, the exception is caught and not logged.");

    private final boolean logException;
    private final boolean crash;
    private final String description;

    ExceptionHandler(boolean logException, boolean crash, String description) {
        this.logException = logException;
        this.crash = crash;
        this.description = description;
    }

    public boolean isLogException() {
        return logException;
    }

    public boolean isCrash() {
        return crash;
    }

    public void wrapException(Throwable throwable) {
        if (crash) {
            try {
                throw throwable;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else if (logException) {
            CubicChunks.LOGGER.error(throwable.getStackTrace());
        }
    }

    @Override public String getSerializedName() {
        return this.name() + ": " + description;
    }
}
