package cubicchunks.world;

public class NotCubicChunksWorldException extends RuntimeException {
	public NotCubicChunksWorldException() {
		super();
	}

	public NotCubicChunksWorldException(String message) {
		super(message);
	}

	public NotCubicChunksWorldException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotCubicChunksWorldException(Throwable cause) {
		super(cause);
	}

	protected NotCubicChunksWorldException(String message, Throwable cause,
	                           boolean enableSuppression,
	                           boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
